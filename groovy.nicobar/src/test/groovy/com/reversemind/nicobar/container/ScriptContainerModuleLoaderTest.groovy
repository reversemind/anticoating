package com.reversemind.nicobar.container

import com.netflix.nicobar.core.archive.ModuleId
import com.netflix.nicobar.core.archive.PathScriptArchive
import com.netflix.nicobar.core.archive.ScriptArchive
import com.netflix.nicobar.core.archive.ScriptModuleSpec
import com.netflix.nicobar.core.module.ScriptModuleLoader
import com.netflix.nicobar.core.plugin.BytecodeLoadingPlugin
import com.netflix.nicobar.groovy2.plugin.Groovy2CompilerPlugin
import com.reversemind.nicobar.container.utils.NicobarUtils
import groovy.util.logging.Slf4j
import spock.lang.Specification

import java.nio.file.Path
import java.nio.file.Paths
import java.text.SimpleDateFormat

/**
 *
 */
@Slf4j
class ScriptContainerModuleLoaderTest extends Specification{

    def 'recompile to path'(){
        setup:
        log.info ""

        def Path compilationDirectory = Paths.get("/opt/_del/modules").toAbsolutePath()

        def

        ScriptContainerModuleLoader moduleLoader = CustomScriptModuleBuilder.createScriptModuleLoader()
                .withCompilationRootDir(compilationDirectory)
                .build()

        ScriptModuleSpec moduleSpec = new ScriptModuleSpec.Builder(ModuleId.create("moduleName", "moduleVersion"))
                .addCompilerPluginId(BytecodeLoadingPlugin.PLUGIN_ID)
                .addCompilerPluginId(Groovy2CompilerPlugin.PLUGIN_ID)
                .build()

//        def scriptRootPath = Paths.get("/opt/_del/modules/moduleName.moduleVersion:1438258136872").toAbsolutePath()
        def scriptRootPath = Paths.get("/opt/_del/modules.src/moduleName_moduleVersion").toAbsolutePath()
        ScriptArchive scriptArchive = new PathScriptArchive.Builder(scriptRootPath)
                .setRecurseRoot(true)
                .setModuleSpec(moduleSpec)
                .build();

        moduleLoader.updateScriptArchives(new LinkedHashSet<ScriptArchive>(Arrays.asList(scriptArchive)));


        when:
        log.info ""

        then:
        log.info ""
    }

    def 'go again'(){
        setup:
        ScriptModuleLoader moduleLoader = NicobarUtils
                .createLightScriptModuleLoader()
//                .withCompilationRootDir(Paths.get("src/test/resources/auto/modules").toAbsolutePath())
//                .withCompilationRootDir(Paths.get("/opt/_del/modules").toAbsolutePath())
                .withCompilationRootDir(Paths.get("/opt/_del/modules/").toAbsolutePath())

                .build()

        ScriptModuleSpec moduleSpec = new ScriptModuleSpec.Builder(ModuleId.create("moduleName", "moduleVersion"))
                .addCompilerPluginId(BytecodeLoadingPlugin.PLUGIN_ID)
                .addCompilerPluginId(Groovy2CompilerPlugin.PLUGIN_ID)
                .build()

//        def scriptRootPath = Paths.get("src/test/resources/auto/moduleName_moduleVersion").toAbsolutePath()
//        def scriptRootPath = Paths.get("/opt/_del/_go").toAbsolutePath()
        def scriptRootPath = Paths.get("/opt/_del/modules/moduleName.moduleVersion:1438258136872").toAbsolutePath()
        ScriptArchive scriptArchive = new PathScriptArchive.Builder(scriptRootPath)
                .setRecurseRoot(true)
                .setModuleSpec(moduleSpec)
                .build();

        moduleLoader.updateScriptArchives(new LinkedHashSet<ScriptArchive>(Arrays.asList(scriptArchive)));

        when:
        log.info ""

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-DD-mm HH:MM:ss:SSS")

        println "date:" + simpleDateFormat.format(new Date(1438257039246L))

        then:
        log.info ""
    }
}
