package com.reversemind.nicobar.container;

import com.netflix.nicobar.core.archive.ModuleId;
import com.netflix.nicobar.core.archive.ScriptArchive;
import com.netflix.nicobar.core.compile.ScriptCompilationException;
import com.netflix.nicobar.core.module.*;
import com.netflix.nicobar.core.module.jboss.JBossModuleUtils;
import com.netflix.nicobar.core.module.jboss.JBossScriptModule;
import com.netflix.nicobar.core.plugin.ScriptCompilerPluginSpec;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleSpec;
import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

/**
 *
 */
@Slf4j
public class CustomScriptModuleLoader extends ScriptModuleLoader {

    protected CustomScriptModuleLoader(Set<ScriptCompilerPluginSpec> pluginSpecs, ClassLoader appClassLoader, Set<String> appPackagePaths, Set<ScriptModuleListener> listeners, Path compilationRootDir) throws ModuleLoadException {
        super(pluginSpecs, appClassLoader, appPackagePaths, listeners, compilationRootDir);
    }

    /**
     * Add or update the existing {@link ScriptModule}s with the given script archives.
     * This method will convert the archives to modules and then compile + link them in to the
     * dependency graph. It will then recursively re-link any modules depending on the new modules.
     * If this loader already contains an old version of the module, it will be unloaded on
     * successful compile of the new module.
     *
     * @param candidateArchives archives to load or update
     */
    @Override
    public synchronized void updateScriptArchives(Set<? extends ScriptArchive> candidateArchives)  {
        Objects.requireNonNull(candidateArchives);
        long updateNumber = System.currentTimeMillis();

        // map script module id to archive to be compiled
        Map<ModuleId, ScriptArchive> archivesToCompile = new HashMap<ModuleId, ScriptArchive>(candidateArchives.size()*2);

        // create an updated mapping of the scriptModuleId to latest revisionId including the yet-to-be-compiled archives
        Map<ModuleId, ModuleIdentifier> oldRevisionIdMap = jbossModuleLoader.getLatestRevisionIds();
        Map<ModuleId, ModuleIdentifier> updatedRevisionIdMap = new HashMap<ModuleId, ModuleIdentifier>((oldRevisionIdMap.size()+candidateArchives.size())*2);
        updatedRevisionIdMap.putAll(oldRevisionIdMap);

        // Map of the scriptModuleId to it's updated set of dependencies
        Map<ModuleId, Set<ModuleId>> archiveDependencies = new HashMap<ModuleId, Set<ModuleId>>();
        for (ScriptArchive scriptArchive : candidateArchives) {
            ModuleId scriptModuleId = scriptArchive.getModuleSpec().getModuleId();

            // filter out archives that have a newer module already loaded
            long createTime = scriptArchive.getCreateTime();
            ScriptModule scriptModule = loadedScriptModules.get(scriptModuleId);
            long latestCreateTime = scriptModule != null ? scriptModule.getCreateTime() : 0;
            if (createTime < latestCreateTime) {
                notifyArchiveRejected(scriptArchive, ArchiveRejectedReason.HIGHER_REVISION_AVAILABLE, null);
                continue;
            }

            // create the new revisionIds that should be used for the linkages when the new modules
            // are defined.
            ModuleIdentifier newRevisionId = JBossModuleUtils.createRevisionId(scriptModuleId, updateNumber);
            updatedRevisionIdMap.put(scriptModuleId, newRevisionId);

            archivesToCompile.put(scriptModuleId, scriptArchive);

            // create a dependency map of the incoming archives so that we can later build a candidate graph
            archiveDependencies.put(scriptModuleId, scriptArchive.getModuleSpec().getModuleDependencies());
        }

        // create a dependency graph with the candidates swapped in in order to figure out the
        // order in which the candidates should be loaded
        DirectedGraph<ModuleId, DefaultEdge> candidateGraph = jbossModuleLoader.getModuleNameGraph();
        GraphUtils.swapVertices(candidateGraph, archiveDependencies);

        // iterate over the graph in reverse dependency order
        Set<ModuleId> leaves = GraphUtils.getLeafVertices(candidateGraph);
        while (!leaves.isEmpty()) {
            for (ModuleId scriptModuleId : leaves) {
                ScriptArchive scriptArchive = archivesToCompile.get(scriptModuleId);
                if (scriptArchive == null) {
                    continue;
                }
                ModuleSpec moduleSpec;
                ModuleIdentifier candidateRevisionId = updatedRevisionIdMap.get(scriptModuleId);
                Path modulePath = Paths.get(candidateRevisionId.toString());
                final Path moduleCompilationRoot = compilationRootDir.resolve(modulePath);
                FileUtils.deleteQuietly(moduleCompilationRoot.toFile());
                try {
                    Files.createDirectories(moduleCompilationRoot);
                } catch (IOException ioe) {
                    notifyArchiveRejected(scriptArchive, ArchiveRejectedReason.ARCHIVE_IO_EXCEPTION, ioe);
                }

                try {
                    moduleSpec = createModuleSpec(scriptArchive, candidateRevisionId, updatedRevisionIdMap, moduleCompilationRoot);
                } catch (ModuleLoadException e) {
                    log.error("Exception loading archive " +
                            scriptArchive.getModuleSpec().getModuleId(), e);
                    notifyArchiveRejected(scriptArchive, ArchiveRejectedReason.ARCHIVE_IO_EXCEPTION, e);
                    continue;
                }

                // load and compile the module
                jbossModuleLoader.addModuleSpec(moduleSpec);
                Module jbossModule = null;
                try {
                    jbossModule = jbossModuleLoader.loadModule(candidateRevisionId);
                    compileModule(jbossModule, moduleCompilationRoot);

                    // Now refresh the resource loaders for this module, and load the set of
                    // compiled classes and populate into the module's local class cache.
                    jbossModuleLoader.rescanModule(jbossModule);

                    final Set<String> classesToLoad = new LinkedHashSet<String>();
                    Files.walkFileTree(moduleCompilationRoot, new SimpleFileVisitor<Path>() {
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            String relativePath = moduleCompilationRoot.relativize(file).toString();
                            if (relativePath.endsWith(".class")) {
                                String className = relativePath.replaceAll(".class", "").replace("/", ".");
                                classesToLoad.add(className);
                            }
                            return FileVisitResult.CONTINUE;
                        };
                    });
                    for (String loadClass: classesToLoad) {
                        Class<?> loadedClass = jbossModule.getClassLoader().loadClassLocal(loadClass, true);
                        if (loadedClass == null)
                            throw new ScriptCompilationException("Unable to load compiled class: " + loadClass);
                    }
                } catch (Exception e) {
                    // rollback
                    log.error("Exception loading module " + candidateRevisionId, e);
                    if (candidateArchives.contains(scriptArchive)) {
                        // this spec came from a candidate archive. Send reject notification
                        notifyArchiveRejected(scriptArchive, ArchiveRejectedReason.COMPILE_FAILURE, e);
                    }
                    if (jbossModule != null) {
                        jbossModuleLoader.unloadModule(jbossModule);
                    }
                    continue;
                }

                // commit the change by removing the old module
                ModuleIdentifier oldRevisionId = oldRevisionIdMap.get(scriptModuleId);
                if (oldRevisionId != null) {
                    jbossModuleLoader.unloadModule(oldRevisionId);
                }

                JBossScriptModule scriptModule = new JBossScriptModule(scriptModuleId, jbossModule, scriptArchive);
                ScriptModule oldModule = loadedScriptModules.put(scriptModuleId, scriptModule);
                notifyModuleUpdate(scriptModule, oldModule);

                // find dependents and add them to the to be compiled set
                Set<ModuleId> dependents = GraphUtils.getIncomingVertices(candidateGraph, scriptModuleId);
                for (ModuleId dependentScriptModuleId : dependents) {
                    if (!archivesToCompile.containsKey(dependentScriptModuleId)) {
                        ScriptModule dependentScriptModule = loadedScriptModules.get(dependentScriptModuleId);
                        if (dependentScriptModule != null) {
                            archivesToCompile.put(dependentScriptModuleId, dependentScriptModule.getSourceArchive());
                            ModuleIdentifier dependentRevisionId = JBossModuleUtils.createRevisionId(dependentScriptModuleId, updateNumber);
                            updatedRevisionIdMap.put(dependentScriptModuleId, dependentRevisionId);
                        }
                    }
                }
            }

            GraphUtils.removeVertices(candidateGraph, leaves);
            leaves = GraphUtils.getLeafVertices(candidateGraph);
        }
    }
}
