package com.reversemind.nicobar

import com.netflix.nicobar.core.archive.*
import com.netflix.nicobar.core.module.ScriptModule
import com.netflix.nicobar.core.module.ScriptModuleLoader
import com.netflix.nicobar.core.module.ScriptModuleUtils
import com.netflix.nicobar.core.plugin.BytecodeLoadingPlugin
import com.netflix.nicobar.groovy2.internal.compile.Groovy2CompilerHelper
import com.netflix.nicobar.groovy2.plugin.Groovy2CompilerPlugin
import com.reversemind.nicobar.utils.NicobarUtils
import groovy.util.logging.Slf4j
import org.codehaus.groovy.tools.GroovyClass

import javax.validation.constraints.NotNull
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap

/**
 *
 */
@Slf4j
class ScriptContainer {

    // TODO make map <canonicalClassName, moduleId>

    public static final String KEY_MAIN_SCRIPT = "mainScript";

    private static ScriptModuleLoader scriptModuleLoader
    private static ScriptContainer scriptContainer = new ScriptContainer();

    private static BuildModule buildModule;

    private final static ScriptModuleSpecSerializer DEFAULT_MODULE_SPEC_SERIALIZER = new GsonScriptModuleSpecSerializer();

    private static ConcurrentHashMap<ModuleId, Path> modulePathMap = new ConcurrentHashMap<ModuleId, Path>();


    private ScriptContainer() {}

    public static ScriptContainer getInstance() {
        return scriptContainer;
    }

    public static void addScriptSourceDirectory(String moduleName, String moduleVersion, Path scriptSourceDirectory, boolean isSynchronize) {

        // TODO validate directory structure for base path

        modulePathMap.put(ModuleId.create(moduleName, moduleVersion), scriptSourceDirectory);
    }

    /**
     * // TODO Describe script directory structure
     *
     *
     * @param moduleId
     * @param scriptSourceDirectory
     * @param isSynchronize
     */
    public static void addScriptSourceDirectory(ModuleId moduleId, Path scriptSourceDirectory, boolean isSynchronize) {
        if(moduleId != null){

            // TODO validate directory structure for base path
            modulePathMap.put(moduleId, scriptSourceDirectory);
        }
    }

    public static void fullReBuildModule(ModuleId moduleId){
        if(moduleId == null){
            return;
        }

        Path basePath = modulePathMap.get(moduleId);

        if(basePath == null){
            // TODO log message
            return;
        }

        BuildModule.validateAndCreateModulePaths(basePath);

        // #1 compile
        ScriptModuleSpec moduleSpec = new ScriptModuleSpec.Builder(moduleId)
                .addCompilerPluginId(BytecodeLoadingPlugin.PLUGIN_ID)
                .addCompilerPluginId(Groovy2CompilerPlugin.PLUGIN_ID)
                .build();

//        PathScriptArchive scriptArchive = new PathScriptArchive.Builder(scriptRootPath)
//                .setRecurseRoot(true)
//                .setModuleSpec(moduleSpec)
//                .build();

//        Set<GroovyClass> compiledClasses = new Groovy2CompilerHelper(Paths.get(BASE_PATH, "build", "classes").toAbsolutePath())
//                .addScriptArchive(scriptArchive)
//                .compile();


    }

    // TODO need assign lib directory for different types shareable jar's
    public static ScriptModuleLoader getScriptModuleLoader() {
        if (scriptModuleLoader == null) {
            scriptModuleLoader = NicobarUtils.createFullScriptModuleLoader().build()
        }
        return scriptModuleLoader;
    }

    // TODO add only a jar script archive?! what about a spec inside
    public static void updateScriptArchive(ScriptArchive scriptArchive) {
        getScriptModuleLoader().updateScriptArchives(new LinkedHashSet<ScriptArchive>(Arrays.asList(scriptArchive)));
    }

    public static void executeScript(ModuleId moduleId, String scriptName){
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

    public static void executeModule(ModuleId moduleId) {
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

}
