package com.reversemind.nicobar

import com.netflix.nicobar.core.archive.ModuleId
import com.netflix.nicobar.core.archive.ScriptArchive
import com.netflix.nicobar.core.archive.ScriptModuleSpec
import com.netflix.nicobar.core.module.ScriptModule
import com.netflix.nicobar.core.module.ScriptModuleLoader
import com.netflix.nicobar.core.module.ScriptModuleUtils
import com.reversemind.nicobar.utils.NicobarUtils
import groovy.util.logging.Slf4j

import javax.annotation.Nullable
import javax.validation.constraints.NotNull

/**
 *
 */
@Slf4j
class ScriptContainer {

    public static final String KEY_MAIN_SCRIPT = "mainScript";

    private static ScriptModuleLoader scriptModuleLoader
    private static ScriptContainer scriptContainer = new ScriptContainer();

    private ScriptContainer() {}

    public static ScriptContainer getInstance() {
        return scriptContainer;
    }

    public static ScriptModuleLoader getScriptModuleLoader() {
        if (scriptModuleLoader == null) {
            scriptModuleLoader = NicobarUtils.createFullScriptModuleLoader().build()
        }
        return scriptModuleLoader;
    }

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
