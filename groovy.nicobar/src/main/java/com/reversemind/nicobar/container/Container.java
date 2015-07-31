package com.reversemind.nicobar.container;

import com.netflix.nicobar.core.archive.ModuleId;
import com.netflix.nicobar.core.archive.ScriptArchive;
import com.netflix.nicobar.core.module.ScriptModule;
import com.netflix.nicobar.core.module.ScriptModuleLoader;
import com.netflix.nicobar.core.module.ScriptModuleUtils;
import com.reversemind.nicobar.container.utils.ContainerUtils;
import groovy.lang.Binding;
import lombok.extern.slf4j.Slf4j;
import org.jboss.modules.ModuleLoadException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 */
@Slf4j
public class Container {

    private static Container container;

    private static ContainerModuleLoader moduleLoader;
    private static Builder innerBuilder;

    private static Path srcPath;
    private static Path classesPath;
    private static Path libsPath;

    private static ConcurrentHashMap<ModuleId, Boolean> moduleMap = new ConcurrentHashMap<ModuleId, Boolean>();

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
//                    Path modulePath = new ModuleBuilder(moduleId, baseDirectory).getModuleSrcPath()
//                    new PathWatcher(moduleId, this, modulePath, true, 100, 5000).processEvents();
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
                        if (moduleIdMap.get(moduleId)) {
//                    Path modulePath = new ModuleBuilder(moduleId, baseDirectory).getModuleSrcPath()
//                    new PathWatcher(moduleId, this, modulePath, true, 100, 5000).processEvents();
                        }
                    }
                }
            }

            // build from source to classes directory
            getModuleLoader().updateScriptArchives(scriptArchiveSet);
        }

        return getInstance();
    }

    /**
     *
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
            Map<ModuleId, Boolean> moduleIdMap = new HashMap<>();
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
                    log.error("Unable to execute script ${scriptName} for module:${moduleId}", ex);
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
                    log.error("Unable to execute script ${scriptName} for module:${moduleId} with binding:${binding}", ex);
                }
            }
        }
        return getInstance();
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
        }
    }

    public static class Builder {
        private ContainerModuleLoader moduleLoader;
        private Path srcPath;
        private Path classesPath;
        private Path libsPath;
        private Set<Path> runtimeJarLibs = new HashSet<Path>();

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

        public void build() throws IOException, ModuleLoadException {
            container = new Container(this);
        }
    }

}
