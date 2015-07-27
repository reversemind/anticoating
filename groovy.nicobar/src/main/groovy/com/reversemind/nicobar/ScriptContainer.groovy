package com.reversemind.nicobar

import com.netflix.nicobar.core.archive.*
import com.netflix.nicobar.core.module.ScriptModule
import com.netflix.nicobar.core.module.ScriptModuleLoader
import com.netflix.nicobar.core.module.ScriptModuleUtils
import com.reversemind.nicobar.utils.NicobarUtils
import com.reversemind.nicobar.watcher.WatchDirectory
import groovy.util.logging.Slf4j

import javax.validation.constraints.NotNull
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.ConcurrentHashMap

import static java.nio.file.FileVisitOption.FOLLOW_LINKS

/**
 *
 */
@Slf4j
class ScriptContainer implements IScriptContainerListener {

    // TODO make map <canonicalClassName, moduleId> ??

    public static final String KEY_MAIN_SCRIPT = "mainScript";

    private static ScriptModuleLoader scriptModuleLoader
    private static ScriptContainer scriptContainer = new ScriptContainer();

    private final
    static ScriptModuleSpecSerializer DEFAULT_MODULE_SPEC_SERIALIZER = new GsonScriptModuleSpecSerializer();

    private static ConcurrentHashMap<ModuleId, Path> modulePathMap = new ConcurrentHashMap<ModuleId, Path>();
    private
    static ConcurrentHashMap<ModuleId, IPathWatcher> listenerPathMap = new ConcurrentHashMap<ModuleId, IPathWatcher>();

    private ScriptContainer() {}

    public static ScriptContainer getInstance() {
        return scriptContainer;
    }

    // TODO is it need to check existence of SRC of this jar module??
    public ScriptContainer loadModules(Path modulesPath) {
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

        if (!_modulesPath.isEmpty()) {

            for (Path path : _modulesPath) {

                log.info "Going to load module:" + path.toAbsolutePath().toString()

                // TODO logging
                JarScriptArchive jarScriptArchive = new JarScriptArchive.Builder(path.toAbsolutePath())
                        .build();

                ScriptModuleSpec scriptModuleSpec = jarScriptArchive.getModuleSpec()
                log.info "module spec:" + scriptModuleSpec

                if (scriptModuleSpec != null) {
                    ModuleId _moduleId = scriptModuleSpec.getModuleId()
                    log.info "module id:" + _moduleId

                    if (_moduleId != null) {
                        if (!modulePathMap.containsKey(_moduleId)) {
                            modulePathMap.put(_moduleId, path);
                        }
                        updateScriptArchive(jarScriptArchive);
                    }
                }
            }
        }

        return getInstance();
    }

    /**
     * // TODO Describe script directory structure
     *
     * @param moduleId
     * @param baseDirectory
     * @param isSynchronize
     */
    public ScriptContainer addScriptSourceDirectory(ModuleId moduleId, Path baseDirectory, boolean isSynchronize) {
        if (moduleId != null) {
            if (!modulePathMap.containsKey(moduleId)) {
                // TODO validate directory structure for base path before put in processing
                modulePathMap.put(moduleId, baseDirectory);
                if (isSynchronize) {
                    Path modulePath = new ModuleBuilder(moduleId, baseDirectory).getModuleSrcPath()
                    new WatchDirectory(moduleId, this, modulePath, true).processEvents();
                }
            }
        }
        return getInstance();
    }

    /**
     *
     * @param moduleId
     */
    public ScriptContainer reBuildModule(ModuleId moduleId) {
        log.info "ReBuild module:" + moduleId

        if (moduleId == null) {
            return getInstance();
        }

        Path basePath = modulePathMap.get(moduleId);
        if (basePath == null) {
            // TODO log message
            return getInstance();
        }

        synchronized (basePath) {
            ModuleBuilder moduleBuilder = new ModuleBuilder(moduleId, basePath);
            if (!moduleBuilder.validateAndCreateModulePaths()) {
                return getInstance();
            }

            moduleBuilder.build()
            Path moduleJarPath = moduleBuilder.getModuleJarPath();

            JarScriptArchive jarScriptArchive = new JarScriptArchive.Builder(moduleJarPath)
                    .build();

            updateScriptArchive(jarScriptArchive);
        }
        return getInstance();
    }

    // TODO need assign lib directory for different types shareable jar's
    protected ScriptModuleLoader getScriptModuleLoader() {
        if (scriptModuleLoader == null) {
            scriptModuleLoader = NicobarUtils.createFullScriptModuleLoader().build()
        }
        return scriptModuleLoader;
    }

    public ScriptContainer updateScriptArchive(ScriptArchive scriptArchive) {
        log.info "updating script archive:" + scriptArchive.moduleSpec
        getScriptModuleLoader().updateScriptArchives(new LinkedHashSet<ScriptArchive>(Arrays.asList(scriptArchive)));
        return getInstance();
    }

    public ScriptContainer executeScript(ModuleId moduleId, String scriptName) {
        final ScriptModule scriptModule = getScriptModuleLoader().getScriptModule(moduleId)
        if (scriptModule != null) {

            Class clazz = ScriptModuleUtils.findClass(scriptModule, scriptName)
            if (clazz != null) {
                try {
                    ScriptInvokerHelper.runScript(clazz)
                } catch (Exception ex) {
                    log.error("Unable to execute script ${scriptName} for module:${moduleId}", ex)
                }
            }
        }
        return getInstance();
    }

    public ScriptContainer executeScript(ModuleId moduleId, String scriptName, Binding binding) {
        final ScriptModule scriptModule = getScriptModuleLoader().getScriptModule(moduleId)
        if (scriptModule != null) {
            Class clazz = ScriptModuleUtils.findClass(scriptModule, scriptName)
            if (clazz != null) {
                try {
                    ScriptInvokerHelper.runGroovyScript(clazz, binding)
                } catch (Exception ex) {
                    log.error("Unable to execute script ${scriptName} for module:${moduleId} with binding:${binding}", ex)
                }
            }
        }
        return getInstance();
    }

    public ScriptContainer executeModule(ModuleId moduleId) {
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
            } else {
                log.error("No classes found for name:", mainScriptName)
            }
        }
        return getInstance();
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
    public synchronized void changed(ModuleId moduleId) {
        // TODO change it - basically done for async
        new Thread() {
            @Override
            public void run() {
                reBuildModule(moduleId);
            }
        }.start();
    }
}
