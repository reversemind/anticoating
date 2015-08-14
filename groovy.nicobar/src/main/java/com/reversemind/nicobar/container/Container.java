package com.reversemind.nicobar.container;

import com.netflix.nicobar.core.archive.ModuleId;
import com.netflix.nicobar.core.archive.ScriptArchive;
import com.netflix.nicobar.core.module.ScriptModule;
import com.netflix.nicobar.core.module.ScriptModuleUtils;
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
//    private final static Object lock = new Object();

    private static ContainerModuleLoader moduleLoader;
    private static Builder innerBuilder;

    private static Path srcPath;
    private static Path classesPath;
    private static Path libsPath;

    private static ConcurrentHashMap<ModuleId, Boolean> moduleMap = new ConcurrentHashMap<ModuleId, Boolean>();

    private static PathWatcher pathWatcher;

    private Container(Builder builder) throws IOException, ModuleLoadException {
        this.build(builder);
    }

    public static Container getInstance() {
        if (innerBuilder == null) {
            throw new IllegalStateException("Container was not initialized by Builder");
        }
        return container;
    }

    public Container addSrcModuleAndCompile(final ModuleId moduleId, boolean isSynchronize) throws IOException {
        if (moduleId != null) {
            if (!moduleMap.containsKey(moduleId)) {
                moduleMap.put(moduleId, isSynchronize);
            }

            // build from source to classes directory
            ScriptArchive scriptArchive = ContainerUtils.getMixScriptArchiveAtPath(srcPath, moduleId);
            getModuleLoader().updateScriptArchives(new LinkedHashSet<ScriptArchive>(Arrays.asList(scriptArchive)));

            if (isSynchronize) {
                Path modulePath = ContainerUtils.getModulePath(srcPath, moduleId).toAbsolutePath();
                pathWatcher.register(moduleId, modulePath, true);
            }
        }
        return getInstance();
    }

    private Container loadCompiledModules(final Map<ModuleId, Boolean> moduleIdMap) throws IOException {
        if (moduleIdMap != null && !moduleIdMap.isEmpty()) {

            LinkedHashSet<ScriptArchive> scriptArchiveSet = new LinkedHashSet<ScriptArchive>();
            for (ModuleId moduleId : moduleIdMap.keySet()) {
                if (moduleId != null) {
                    if (!moduleMap.containsKey(moduleId)) {
                        moduleMap.put(moduleId, moduleIdMap.get(moduleId));
                        scriptArchiveSet.add(ContainerUtils.getScriptArchiveAtPath(classesPath, moduleId));
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

    public Container loadCompiledModules(boolean isUseClassPath, final Set<ModuleId> moduleIdSet, boolean isSynchronized) throws IOException {
        Path path = isUseClassPath ? classesPath : srcPath;

        if (moduleIdSet != null && !moduleIdSet.isEmpty()) {

            LinkedHashSet<ScriptArchive> scriptArchiveSet = new LinkedHashSet<ScriptArchive>();
            for (ModuleId moduleId : moduleIdSet) {
                if (moduleId != null) {
                    scriptArchiveSet.add(ContainerUtils.getScriptArchiveAtPath(path, moduleId));
                    if (!moduleMap.containsKey(moduleId)) {
                        moduleMap.put(moduleId, true);

                        if (isSynchronized) {
                            Path modulePath = ContainerUtils.getModulePath(path, moduleId).toAbsolutePath();
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
            loadCompiledModules(moduleIdMap);
        }

        return getInstance();
    }

    /**
     * One of the important methods in logic of loading precompiled modules
     *
     * @return - return an container instance
     * @throws IOException - if some paths are not exists
     */
    public Container loadModulesAtStart() throws IOException {

        // TODO implements logic about number of modules in classes & src sub dirs
        // TODO compare modules compiled and src file - may be via special hash function - like git

        ContainerUtils.Pair<Set<ModuleId>, Set<ModuleId>> pair = ContainerUtils.getModuleToLoadAndCompile(classesPath, srcPath);

        if(pair != null){
            Set<ModuleId> modulesToLoad = pair.getT1();
            Set<ModuleId> modulesToCompile = pair.getT2();

            // TODO logging
            System.out.println("modulesToLoad:" + modulesToLoad);
            System.out.println("modulesToCompile:" + modulesToCompile);

            long beginTime = System.currentTimeMillis();
            System.out.println("Started loading from classes at:" + new Date(beginTime) + " timestamp:" + beginTime);
            if(modulesToLoad != null && !modulesToLoad.isEmpty()){
                Map<ModuleId, Boolean> moduleIdMap = new HashMap<ModuleId, Boolean>();
                for (ModuleId moduleId : modulesToLoad) {
                    moduleIdMap.put(moduleId, true);
                }
                loadCompiledModules(moduleIdMap);
            }
            System.out.println("Loaded compiled modules for:" + (System.currentTimeMillis() - beginTime) + " ms");


            beginTime = System.currentTimeMillis();
            System.out.println("Started compiling modules from source at:" + new Date(beginTime) + " timestamp:" + beginTime);
            if(modulesToCompile != null && !modulesToCompile.isEmpty()){

                for (ModuleId moduleId : modulesToCompile) {
                    System.out.println("Compile module:" + moduleId);
                    this.addSrcModuleAndCompile(moduleId, true);
                }
            }
            System.out.println("Compiled modules from src for:" + (System.currentTimeMillis() - beginTime) + " ms");



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
//                moduleLoader = ContainerUtils.createContainerModuleLoaderBuilder(builder.getRuntimeJarLibs())
                moduleLoader = ContainerUtils.createMixContainerModuleLoaderBuilder(builder.getRuntimeJarLibs())
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
        // TODO make it sync !!!!
        try {
            if(moduleIdSet != null && !moduleIdSet.isEmpty()){
                for(ModuleId moduleId: moduleIdSet){
                    addSrcModuleAndCompile(moduleId, false);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    public static void destroy() throws InterruptedException, IOException {

            // unload all modules
            if (moduleLoader.getAllScriptModules() != null && !moduleLoader.getAllScriptModules().isEmpty()) {
                Set<ModuleId> _keys = moduleLoader.getAllScriptModules().keySet();
                for (ModuleId moduleId : _keys) {
                    try {
                        moduleLoader.removeScriptModule(moduleId);
                        moduleMap.remove(moduleId);
                    } catch (Exception ignore) {
                    }
                }
            }

            container = null;

            moduleLoader = null;
            innerBuilder = null;

            srcPath = null;
            classesPath = null;
            libsPath = null;

            moduleMap.clear();

            pathWatcher.destroy();
            pathWatcher = null;

            Runtime.getRuntime().gc();
//        }
    }

}
