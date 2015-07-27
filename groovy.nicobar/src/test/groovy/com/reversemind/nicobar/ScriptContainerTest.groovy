package com.reversemind.nicobar

import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.nicobar.core.archive.JarScriptArchive
import com.netflix.nicobar.core.archive.ModuleId
import com.netflix.nicobar.core.archive.ScriptArchive
import com.netflix.nicobar.core.archive.ScriptModuleSpec
import groovy.util.logging.Slf4j
import spock.lang.Specification

import java.nio.file.Paths

/**
 *
 */
@Slf4j
class ScriptContainerTest extends Specification {

    final String moduleName = "moduleName"
    final String moduleVersion = "moduleVersion"
    final ModuleId moduleId = ModuleId.create(moduleName, moduleVersion);

    def 'compile scripts and pack them into nicobar jar module'() {
        setup:

        def BASE_PATH = "src/test/resources/auto/"

        ScriptContainer scriptContainer = ScriptContainer.getInstance()

        ScriptContainer.addScriptSourceDirectory(moduleId, Paths.get(BASE_PATH), true);
        ScriptContainer.fullReBuildModule(moduleId)

        when:
        log.info ""

        ScriptContainer.executeScript(moduleId, "com.company.script")

        then:
        log.info ""
    }

    def 'load precompiled jar'() {
        setup:

        ScriptArchive scriptArchive = new JarScriptArchive.Builder(Paths.get('src/test/resources/libs/precompiled.jar').toAbsolutePath())
                .build();


        JarScriptArchive jarScriptArchive = new JarScriptArchive.Builder(Paths.get('src/test/resources/libs/precompiled.jar').toAbsolutePath())
                .build();

        ScriptContainer scriptContainer = ScriptContainer.getInstance()
        scriptContainer.updateScriptArchive(scriptArchive)

        ModuleId moduleId = scriptArchive.getModuleSpec().getModuleId()

        scriptContainer.executeModule(moduleId)


        println "go:" + jarScriptArchive.getArchiveEntryNames()


        ScriptModuleSpec scriptModuleSpec = new JarScriptArchive.Builder(Paths.get('src/test/resources/libs/precompiled.jar').toAbsolutePath())
                .build().getModuleSpec()

        println "go:" + scriptModuleSpec

        ObjectMapper mapper = new ObjectMapper();
        println "json:" + mapper.writeValueAsString(scriptModuleSpec);

        scriptModuleSpec = new ScriptModuleSpec();
    }

}
