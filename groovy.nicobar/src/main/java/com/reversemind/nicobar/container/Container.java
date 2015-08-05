package com.reversemind.nicobar.container;

import com.netflix.nicobar.core.archive.ModuleId;
import com.netflix.nicobar.core.archive.PathScriptArchive;
import com.netflix.nicobar.core.archive.ScriptArchive;
import com.netflix.nicobar.core.archive.ScriptModuleSpec;
import com.netflix.nicobar.core.module.ScriptModule;
import com.netflix.nicobar.core.module.ScriptModuleUtils;
import com.reversemind.nicobar.container.plugin.BytecodeMultiLoadingPlugin;
import com.reversemind.nicobar.container.utils.ContainerUtils;
import com.reversemind.nicobar.container.watcher.PathWatcher;
import groovy.lang.Binding;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jboss.modules.ModuleLoadException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 */
@Slf4j
public class Container implements IContainerListener {

    private static Container container;

    private static ContainerModuleLoader moduleLoader;
    private static Builder innerBuilder;

    private static Path srcPath;
    private static Path classesPath;
    private static Path libsPath;

    private static ConcurrentHashMap<ModuleId, Boolean> moduleMap = new ConcurrentHashMap<ModuleId, Boolean>();

    private PathWatcher pathWatcher;

    private Container(Builder builder) throws IOException, ModuleLoadException {
        this.build(builder);
    }

    public static Container getInstance() {
        if (innerBuilder == null) {
            throw new IllegalStateException("Container was not initialized by Builder");
        }
        return container;
    }

    public Container addModule(final ModuleId moduleId, boolean isSynchronize) throws IOException {
        if (moduleId != null) {
            if (!moduleMap.containsKey(moduleId)) {
                moduleMap.put(moduleId, isSynchronize);

                // build from source to classes directory
                ScriptArchive scriptArchive = ContainerUtils.getScriptArchiveAtPath(srcPath, moduleId);
                getModuleLoader().updateScriptArchives(new LinkedHashSet<ScriptArchive>(Arrays.asList(scriptArchive)));

                if (isSynchronize) {
                    Path modulePath = ContainerUtils.getModulePath(srcPath, moduleId).toAbsolutePath();
                    pathWatcher.register(moduleId, modulePath, true);
                }
            }
        }
        return getInstance();
    }

    public Container addExtraModule(final ModuleId moduleId, boolean isSynchronize) throws IOException {
        if (moduleId != null) {
            if (!moduleMap.containsKey(moduleId)) {
                moduleMap.put(moduleId, isSynchronize);


                ScriptModuleSpec moduleSpec = new ScriptModuleSpec.Builder(moduleId)
                        .addCompilerPluginId(BytecodeMultiLoadingPlugin.PLUGIN_ID)
                        .addCompilerPluginId(Groovy2MultiCompilerPlugin.PLUGIN_ID)
                        .build();

                PathScriptArchive pathScriptArchive = new PathScriptArchive.Builder(ContainerUtils.getModulePath(srcPath, moduleId).toAbsolutePath())
                        .setModuleSpec(moduleSpec).build();

                // build from source to classes directory
//                ScriptArchive scriptArchive = ContainerUtils.getScriptArchiveAtPath(srcPath, moduleId);
                getModuleLoader().updateScriptArchives(new LinkedHashSet<ScriptArchive>(Arrays.asList(pathScriptArchive)));

                if (isSynchronize) {
                    Path modulePath = ContainerUtils.getModulePath(srcPath, moduleId).toAbsolutePath();
                    pathWatcher.register(moduleId, modulePath, true);
                }
            }
        }
        return getInstance();
    }

    public Container addModules(final Map<ModuleId, Boolean> moduleIdMap) throws IOException {
        if (moduleIdMap != null && !moduleIdMap.isEmpty()) {

            LinkedHashSet<ScriptArchive> scriptArchiveSet = new LinkedHashSet<ScriptArchive>();
            for (ModuleId moduleId : moduleIdMap.keySet()) {
                if (moduleId != null) {
                    if (!moduleMap.containsKey(moduleId)) {
                        moduleMap.put(moduleId, moduleIdMap.get(moduleId));
                        scriptArchiveSet.add(ContainerUtils.getScriptArchiveAtPath(srcPath, moduleId));

                        // isSynchronize
                        if (moduleMap.get(moduleId)) {
                            Path modulePath = ContainerUtils.getModulePath(srcPath, moduleId).toAbsolutePath();
                            pathWatcher.register(moduleId, modulePath, true);
                        }
                    }
                }
            }

            if (!scriptArchiveSet.isEmpty()) {
                // build from source to classes directory
                getModuleLoader().updateScriptArchives(scriptArchiveSet);
            }

        }

        return getInstance();
    }

    public Container addModules(final Set<ModuleId> moduleIdSet, boolean isSynchronized) throws IOException {
        if (moduleIdSet != null && !moduleIdSet.isEmpty()) {

            LinkedHashSet<ScriptArchive> scriptArchiveSet = new LinkedHashSet<ScriptArchive>();
            for (ModuleId moduleId : moduleIdSet) {
                if (moduleId != null) {
                    scriptArchiveSet.add(ContainerUtils.getScriptArchiveAtPath(srcPath, moduleId));
                    if (!moduleMap.containsKey(moduleId)) {
                        moduleMap.put(moduleId, true);

                        if (isSynchronized) {
                            Path modulePath = ContainerUtils.getModulePath(srcPath, moduleId).toAbsolutePath();
                            pathWatcher.register(moduleId, modulePath);
                        }
                    }
                }
            }

            if (!scriptArchiveSet.isEmpty()) {
                // build from source to classes directory
                getModuleLoader().updateScriptArchives(scriptArchiveSet);
            }

        }

        return getInstance();
    }

    /**
     * @param isLoadCompiledFirst - is try to load from compiled classes of from src
     * @return
     * @throws IOException
     */
    public Container loadModules(boolean isLoadCompiledFirst) throws IOException {

        // TODO implements logic about number of modules in classes & src sub dirs
        // TODO compare modules compiled and src file - may be via special hash function - like git
        Path processPath = isLoadCompiledFirst ? classesPath : srcPath;
        Set<ModuleId> moduleIds = ContainerUtils.getModuleIdListAtPath(processPath);

        if (!moduleIds.isEmpty()) {
            Map<ModuleId, Boolean> moduleIdMap = new HashMap<ModuleId, Boolean>();
            for (ModuleId moduleId : moduleIds) {
                moduleIdMap.put(moduleId, true);
            }
            addModules(moduleIdMap);
        }

        return getInstance();
    }

    public Container executeScript(ModuleId moduleId, String scriptName) {
        final ScriptModule scriptModule = getModuleLoader().getScriptModule(moduleId);
        if (scriptModule != null) {

            Class clazz = ScriptModuleUtils.findClass(scriptModule, scriptName);
            if (clazz != null) {
                try {
                    GroovyScriptInvokerHelper.runGroovyScript(clazz);
                } catch (Exception ex) {
                    ex.printStackTrace();
//                    log.error("Unable to execute script ${scriptName} for module:${moduleId}", ex);
                }
            }
        }
        return getInstance();
    }

    public Container executeScript(ModuleId moduleId, String scriptName, Binding binding) {
        final ScriptModule scriptModule = getModuleLoader().getScriptModule(moduleId);
        if (scriptModule != null) {
            Class clazz = ScriptModuleUtils.findClass(scriptModule, scriptName);
            if (clazz != null) {
                try {
                    GroovyScriptInvokerHelper.runGroovyScript(clazz, binding);
                } catch (Exception ex) {
                    ex.printStackTrace();
//                    log.error("Unable to execute script ${scriptName} for module:${moduleId} with binding:${binding}", ex);
                }
            }
        }
        return getInstance();
    }

    public Class findClass(ModuleId moduleId, String canonicalClassName) {
        final ScriptModule scriptModule = getModuleLoader().getScriptModule(moduleId);
        if (scriptModule != null && StringUtils.isNotBlank(canonicalClassName)) {
            return ScriptModuleUtils.findClass(scriptModule, canonicalClassName);
        }
        return null;
    }

    protected ContainerModuleLoader getModuleLoader() {
        if (moduleLoader == null) {
            throw new IllegalStateException("Cannot get ContainerModuleLoader 'cause Container instance was not correctly initialized");
        }
        return moduleLoader;
    }

    private void build(Builder builder) throws IOException, ModuleLoadException {
        if (builder == null) {
            throw new IllegalArgumentException("Builder could not be an empty");
        }

        if (innerBuilder != null && !innerBuilder.equals(builder)) {
            throw new IllegalStateException("Builder called again and innerBuilder was already set and not equal to the new:" + builder + " innerBuilder:" + innerBuilder);
        }

        if (innerBuilder == null) {
            moduleLoader = builder.getModuleLoader();
            if (moduleLoader == null) {
                moduleLoader = ContainerUtils.createContainerModuleLoaderBuilder(builder.getRuntimeJarLibs())
                        .withCompilationRootDir(builder.getClassesPath())
                        .build();
            }
            srcPath = builder.getSrcPath();
            classesPath = builder.getClassesPath();
            libsPath = builder.getLibsPath();
            innerBuilder = builder;

            long watchPeriod = builder.getWatchPeriod() <= 0 ? 100 : builder.getWatchPeriod();
            long notifyPeriod = builder.getNotifyPeriod() <= 0 ? 5000 : builder.getNotifyPeriod();

            // TODO move parameters of watcher into Container.Builder
            pathWatcher = new PathWatcher(this, watchPeriod, notifyPeriod).start();
        }
    }

    @Override
    public void changed(final Set<ModuleId> moduleIdSet) {
        // TODO what about async - for changed caller
        new Thread() {
            @Override
            public void run() {
                try {
                    addModules(moduleIdSet, true);
                } catch (IOException e) {
                    // TODO temp solution
                    e.printStackTrace();
                }
            }
        }.start();
    }

    public static class Builder {
        private ContainerModuleLoader moduleLoader;
        private Path srcPath;
        private Path classesPath;
        private Path libsPath;
        private Set<Path> runtimeJarLibs = new HashSet<Path>();
        private long watchPeriod;
        private long notifyPeriod;

        public Builder(Path srcPath, Path classesPath, Path libsPath) {
            if (!srcPath.isAbsolute()) {
                throw new IllegalArgumentException("Path of source scripts should not be an empty");
            }

            if (!classesPath.isAbsolute()) {
                throw new IllegalArgumentException("Path of classes scripts should not be an empty");
            }

            if (!libsPath.isAbsolute()) {
                throw new IllegalArgumentException("Path of libs should not be an empty");
            }

            this.srcPath = srcPath;
            this.classesPath = classesPath;
            this.libsPath = libsPath;
        }

        private Set<Path> getRuntimeJarLibs() {
            return runtimeJarLibs;
        }

        public Builder setRuntimeJarLibs(Set<Path> runtimeJarLibs) {
            this.runtimeJarLibs = runtimeJarLibs;
            return this;
        }

        private Path getSrcPath() {
            return srcPath;
        }

        private Path getClassesPath() {
            return classesPath;
        }

        private Path getLibsPath() {
            return libsPath;
        }

        private ContainerModuleLoader getModuleLoader() {
            return moduleLoader;
        }

        public Builder setModuleLoader(ContainerModuleLoader moduleLoader) {
            this.moduleLoader = moduleLoader;
            return this;
        }

        private long getWatchPeriod() {
            return watchPeriod;
        }

        public Builder setWatchPeriod(long watchPeriod) {
            this.watchPeriod = watchPeriod;
            return this;
        }

        private long getNotifyPeriod() {
            return notifyPeriod;
        }

        public Builder setNotifyPeriod(long notifyPeriod) {
            this.notifyPeriod = notifyPeriod;
            return this;
        }

        public void build() throws IOException, ModuleLoadException {
            container = new Container(this);
        }
    }

}