package com.reversemind.nicobar.container

import com.netflix.nicobar.core.archive.ModuleId
import com.netflix.nicobar.core.archive.PathScriptArchive
import com.netflix.nicobar.core.archive.ScriptArchive
import com.netflix.nicobar.core.archive.ScriptModuleSpec
import com.netflix.nicobar.core.module.BaseScriptModuleListener
import com.netflix.nicobar.core.module.ScriptModule
import com.netflix.nicobar.core.plugin.BytecodeLoadingPlugin
import com.netflix.nicobar.groovy2.plugin.Groovy2CompilerPlugin
import groovy.util.logging.Slf4j
import spock.lang.Specification

import java.nio.file.Path
import java.nio.file.Paths

/**
 *
 */
@Slf4j
class ScriptContainerModuleLoaderTest extends Specification {

    def 'reuse compiled modules and compile from sources again'() {
        setup:
        log.info ""

        Path MODULES_CLASSES = Paths.get("src/test/resources/stage2/modules/classes").toAbsolutePath()
        Path MODULES_SRC = Paths.get("src/test/resources/stage2/modules/src").toAbsolutePath()
        Path MODULES_LIBS = Paths.get("src/test/resources/stage2/modules/libs").toAbsolutePath()

        ModuleId moduleId = ModuleId.create("moduleName", "moduleVersion")

        ScriptContainerModuleLoader moduleLoader = CustomScriptModuleBuilder.createScriptModuleLoader()
                .withCompilationRootDir(MODULES_CLASSES)
                .addListener(new BaseScriptModuleListener() {                // add an example listener for module updates
                    public void moduleUpdated(ScriptModule newScriptModule, ScriptModule oldScriptModule) {
                        System.out.printf("\n\n----------------------------------------" +
                                "\nReceived module update event. newModule: %s,  oldModule: %s%n" +
                                "\n\n", newScriptModule, oldScriptModule);
                    }
                })
                .build()

        // #1 build from source to classes directory
        ScriptArchive scriptArchive = getScriptArchiveAtPath(MODULES_SRC, moduleId);
        moduleLoader.updateScriptArchives(new LinkedHashSet<ScriptArchive>(Arrays.asList(scriptArchive)));
        log.info "#1"

        Thread.sleep(2000);

        // #2 then load from compiled classes
        // isModuleExistAtPath(MODULES_CLASSES, moduleId)
        scriptArchive = getScriptArchiveAtPath(MODULES_CLASSES, moduleId);
        moduleLoader.updateScriptArchives(new LinkedHashSet<ScriptArchive>(Arrays.asList(scriptArchive)));
        log.info "#2"


        ModuleBuilder moduleBuilder = new ModuleBuilder(moduleId, MODULES_CLASSES);
        moduleBuilder.packToJar(
                getModulePath(MODULES_CLASSES, moduleId).toAbsolutePath(),
                Paths.get(MODULES_LIBS.toAbsolutePath().toString(), moduleId.toString() + ".jar").toAbsolutePath()
        );

        when:
        log.info ""

        then:
        log.info ""
    }

    private static boolean isModuleExistAtPath(Path basePath, ModuleId moduleId) {
        return getModulePath(basePath, moduleId).toFile().exists()
    }

    /**
     * basePath/
     *          moduleName.moduleVersion/
     *                                  src/com/company/package
     * @param basePath
     * @param moduleId
     * @return
     */
    private static ScriptArchive getScriptArchiveAtPath(Path basePath, ModuleId moduleId) {
        ScriptModuleSpec moduleSpec = new ScriptModuleSpec.Builder(moduleId)
                .addCompilerPluginId(BytecodeLoadingPlugin.PLUGIN_ID)
                .addCompilerPluginId(Groovy2CompilerPlugin.PLUGIN_ID)
                .build()

        ScriptArchive scriptArchive = new PathScriptArchive.Builder(getModulePath(basePath, moduleId).toAbsolutePath())
                .setRecurseRoot(true)
                .setModuleSpec(moduleSpec)
                .build();

        return scriptArchive;
    }

    private static Path getModulePath(Path basePath, ModuleId moduleId) {
        if (!basePath.isAbsolute()) {
            throw new IllegalArgumentException("Base path should be absolute")
        }
        return basePath.resolve(moduleId.toString())
    }

//    def 'go again'(){
//        setup:
//        ScriptModuleLoader moduleLoader = NicobarUtils
//                .createLightScriptModuleLoader()
////                .withCompilationRootDir(Paths.get("src/test/resources/auto/modules").toAbsolutePath())
////                .withCompilationRootDir(Paths.get("/opt/_del/modules").toAbsolutePath())
//                .withCompilationRootDir(Paths.get("/opt/_del/modules/").toAbsolutePath())
//
//                .build()
//
//        ScriptModuleSpec moduleSpec = new ScriptModuleSpec.Builder(ModuleId.create("moduleName", "moduleVersion"))
//                .addCompilerPluginId(BytecodeLoadingPlugin.PLUGIN_ID)
//                .addCompilerPluginId(Groovy2CompilerPlugin.PLUGIN_ID)
//                .build()
//
////        def scriptRootPath = Paths.get("src/test/resources/auto/moduleName_moduleVersion").toAbsolutePath()
////        def scriptRootPath = Paths.get("/opt/_del/_go").toAbsolutePath()
//        def scriptRootPath = Paths.get("/opt/_del/modules/moduleName.moduleVersion:1438258136872").toAbsolutePath()
//        ScriptArchive scriptArchive = new PathScriptArchive.Builder(scriptRootPath)
//                .setRecurseRoot(true)
//                .setModuleSpec(moduleSpec)
//                .build();
//
//        moduleLoader.updateScriptArchives(new LinkedHashSet<ScriptArchive>(Arrays.asList(scriptArchive)));
//
//        when:
//        log.info ""
//
//        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-DD-mm HH:MM:ss:SSS")
//
//        println "date:" + simpleDateFormat.format(new Date(1438257039246L))
//
//        then:
//        log.info ""
//    }
}
