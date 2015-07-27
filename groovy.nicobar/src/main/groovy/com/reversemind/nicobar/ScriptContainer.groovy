package com.reversemind.nicobar
import com.netflix.nicobar.core.archive.*
import com.netflix.nicobar.core.module.ScriptModule
import com.netflix.nicobar.core.module.ScriptModuleLoader
import com.netflix.nicobar.core.module.ScriptModuleUtils
import com.reversemind.nicobar.utils.NicobarUtils
import com.reversemind.nicobar.watcher.WatchDirectory
import groovy.util.logging.Slf4j

import javax.validation.constraints.NotNull
import java.nio.file.FileSystems
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.PathMatcher
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.ConcurrentHashMap

import static java.nio.file.FileVisitOption.FOLLOW_LINKS
/**
 *
 */
@Slf4j
class ScriptContainer implements IScriptContainerListener{

    // TODO make map <canonicalClassName, moduleId>

    public static final String KEY_MAIN_SCRIPT = "mainScript";

    private static ScriptModuleLoader scriptModuleLoader
    private static ScriptContainer scriptContainer = new ScriptContainer();

    private final static ScriptModuleSpecSerializer DEFAULT_MODULE_SPEC_SERIALIZER = new GsonScriptModuleSpecSerializer();

    private static ConcurrentHashMap<ModuleId, Path> modulePathMap = new ConcurrentHashMap<ModuleId, Path>();
    private static ConcurrentHashMap<ModuleId, IPathWatcher> listenerPathMap = new ConcurrentHashMap<ModuleId, IPathWatcher>();


    private ScriptContainer() {}

    public static ScriptContainer getInstance() {
        return scriptContainer;
    }

    // TODO is it need to check existence of SRC of this jar module??
    public void loadModules(Path modulesPath){
        // TODO download .jar of modules from path
        // TODO validate that exist src directories only in this case download modules
        Set<Path> _modulesPath = new HashSet<>();

        if (modulesPath != null) {

            final int maxDepth = 1;
            final PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("glob:*.{jar}");

            Files.walkFileTree(modulesPath,
                    EnumSet.of(FOLLOW_LINKS),
                    maxDepth,
                    new SimpleFileVisitor<Path>() {

                        private void addPath(Path filePath) {
                            Path name = filePath.getFileName();
                            if (name != null) {
                                if (pathMatcher.matches(name)) {
                                    _modulesPath.add(filePath);
                                }
                            }
                        }

                        @Override
                        public FileVisitResult visitFile(Path filePath, BasicFileAttributes attrs) throws IOException {
                            this.addPath(filePath);
                            return FileVisitResult.CONTINUE;
                        }
                    }
            );
        }

        if(! _modulesPath.isEmpty()){

            for(Path path: _modulesPath){

                log.info "Going to load module:" + path.toAbsolutePath().toString()

                // TODO logging
                JarScriptArchive jarScriptArchive = new JarScriptArchive.Builder(path.toAbsolutePath())
                        .build();

                ScriptModuleSpec scriptModuleSpec = jarScriptArchive.getModuleSpec()
                log.info "module spec:" + scriptModuleSpec

                if(scriptModuleSpec != null){
                    ModuleId _moduleId = scriptModuleSpec.getModuleId()
                    log.info "module id:" + _moduleId

                    if(_moduleId != null){
                        if(! modulePathMap.containsKey(_moduleId)){
                            modulePathMap.put(_moduleId, path);
                        }
                        updateScriptArchive(jarScriptArchive);
                    }
                }
            }
        }

    }

    /**
     * // TODO Describe script directory structure
     *
     * @param moduleId
     * @param baseDirectory
     * @param isSynchronize
     */
    public void addScriptSourceDirectory(ModuleId moduleId, Path baseDirectory, boolean isSynchronize) {
        if(moduleId != null){
            if(!modulePathMap.containsKey(moduleId)){
                // TODO validate directory structure for base path before put in processing
                modulePathMap.put(moduleId, baseDirectory);
                if(isSynchronize){
                    Path modulePath = new BuildModule(moduleId, baseDirectory).getModuleSrcPath()
                    new WatchDirectory(moduleId, this, modulePath, true).processEvents();
                }
            }
        }
    }

    public void reBuildModule(ModuleId moduleId){
        if(moduleId == null){
            return;
        }

        Path basePath = modulePathMap.get(moduleId);

        if(basePath == null){
            // TODO log message
            return;
        }

        synchronized (basePath){
            BuildModule buildModule = new BuildModule(moduleId, basePath);
            if(!buildModule.validateAndCreateModulePaths()){
                return;
            }

            buildModule.build()
            Path moduleJarPath = buildModule.getModuleJarPath();

            JarScriptArchive jarScriptArchive = new JarScriptArchive.Builder(moduleJarPath)
                    .build();

            updateScriptArchive(jarScriptArchive);
        }

    }

    // TODO need assign lib directory for different types shareable jar's
    public ScriptModuleLoader getScriptModuleLoader() {
        if (scriptModuleLoader == null) {
            scriptModuleLoader = NicobarUtils.createFullScriptModuleLoader().build()
        }
        return scriptModuleLoader;
    }

    // TODO add only a jar script archive?! what about a spec inside
    public void updateScriptArchive(ScriptArchive scriptArchive) {
        log.info "updating script archive:" + scriptArchive.moduleSpec
        getScriptModuleLoader().updateScriptArchives(new LinkedHashSet<ScriptArchive>(Arrays.asList(scriptArchive)));
    }

    public void executeScript(ModuleId moduleId, String scriptName){
        final ScriptModule scriptModule = getScriptModuleLoader().getScriptModule(moduleId)
        if (scriptModule != null) {

            Class clazz = ScriptModuleUtils.findClass(scriptModule, scriptName)
            if (clazz != null) {
                try {
                    ScriptInvokerHelper.runScript(clazz)
                } catch (Exception ex) {
                    ex.printStackTrace()
                }
            }
        }
    }

    public static void executeScript(ModuleId moduleId, String scriptName, Binding binding){
        final ScriptModule scriptModule = getScriptModuleLoader().getScriptModule(moduleId)
        if (scriptModule != null) {

            Class clazz = ScriptModuleUtils.findClass(scriptModule, scriptName)
            if (clazz != null) {
                try {
                    ScriptInvokerHelper.runScript(clazz, binding)
                } catch (Exception ex) {
                    ex.printStackTrace()
                }
            }
        }
    }

    public static void executeModule(ModuleId moduleId, Binding binding) {
        final ScriptModule scriptModule = getScriptModuleLoader().getScriptModule(moduleId)
        if (scriptModule != null) {
            String mainScriptName = getMainScriptName(scriptModule.getSourceArchive())
            if (mainScriptName != null)
                mainScriptName = KEY_MAIN_SCRIPT;

            Class clazz = ScriptModuleUtils.findClass(scriptModule, mainScriptName)
            if (clazz != null) {
                try {
                    ScriptInvokerHelper.runScript(clazz, binding)
                } catch (Exception ex) {
                    ex.printStackTrace()
                }
            }

        }
    }

    public void executeModule(ModuleId moduleId) {
        log.info("Execute moduleId:", moduleId)
        final ScriptModule scriptModule = getScriptModuleLoader().getScriptModule(moduleId)
        if (scriptModule != null) {
            final String mainScriptName = getMainScriptName(scriptModule)
            Class clazz = ScriptModuleUtils.findClass(scriptModule, mainScriptName)
            if (clazz != null) {
                try {
                    ScriptInvokerHelper.runScript(clazz)
                } catch (Exception ex) {
                    ex.printStackTrace()
                }
            }else{
                log.error("No classes found for name:", mainScriptName)
            }
        }
    }

    @NotNull
    private static String getMainScriptName(final ScriptModule scriptModule) {
        if (scriptModule == null) {
            return KEY_MAIN_SCRIPT;
        }
        return getMainScriptName(scriptModule.getSourceArchive())
    }

    @NotNull
    private static String getMainScriptName(final ScriptArchive scriptArchive) {
        if (scriptArchive == null) {
            return KEY_MAIN_SCRIPT;
        }
        return getMainScriptName(scriptArchive.getModuleSpec());
    }

    @NotNull
    private static String getMainScriptName(final ScriptModuleSpec scriptModuleSpec) {
        if (scriptModuleSpec == null) {
            return KEY_MAIN_SCRIPT;
        }
        return scriptModuleSpec.getMetadata().get(KEY_MAIN_SCRIPT) != null ? scriptModuleSpec.getMetadata().get(KEY_MAIN_SCRIPT) : KEY_MAIN_SCRIPT;
    }

    @Override
    synchronized void changed(ModuleId moduleId) {
        new Thread(){
            @Override
            public void run(){
                reBuildModule(moduleId);
            }
        }.start();
    }
}
