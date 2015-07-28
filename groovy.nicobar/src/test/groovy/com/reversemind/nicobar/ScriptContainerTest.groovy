package com.reversemind.nicobar

import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.nicobar.core.archive.JarScriptArchive
import com.netflix.nicobar.core.archive.ModuleId
import com.netflix.nicobar.core.archive.ScriptArchive
import com.netflix.nicobar.core.archive.ScriptModuleSpec
import groovy.util.logging.Slf4j
import spock.lang.Specification

import java.nio.file.Path
import java.nio.file.Paths

/**
 *
 */
@Slf4j
class ScriptContainerTest extends Specification {

    final String moduleName = "moduleName"
    final String moduleVersion = "moduleVersion"
    final ModuleId moduleId = ModuleId.create(moduleName, moduleVersion);

    def 'load modules jar and run some script'() {
        setup:
        log.info ""

        def BASE_PATH = "src/test/resources/auto/"

        ScriptContainer scriptContainer = ScriptContainer.getInstance()

        def moduleId1 = ModuleId.create("moduleName", "moduleVersion")
        def moduleId1V2 = ModuleId.create("moduleName", "moduleVersion2")
        def moduleId2 = ModuleId.create("moduleName2", "moduleVersion2")

        scriptContainer
                .addModule(moduleId1, Paths.get(BASE_PATH), true)
                .reBuildModule(moduleId1)

                .addModule(moduleId2, Paths.get(BASE_PATH), true)
                .reBuildModule(moduleId1V2)

                .addModule(moduleId1V2, Paths.get(BASE_PATH), true)
                .reBuildModule(moduleId2);

        final Path MODULES_PATH = Paths.get("src/test/resources/auto/modules")

        when:
        log.info ""
        scriptContainer.loadModules(MODULES_PATH)

        then:
        log.info ""
        scriptContainer.executeScript(moduleId, "com.company.script")

    }

    def 'compile scripts and pack them into nicobar jar module'() {
        setup:

        def BASE_PATH = "src/test/resources/auto/"

        ScriptContainer scriptContainer = ScriptContainer.getInstance()

        scriptContainer
                .addModule(moduleId, Paths.get(BASE_PATH), true)
                .reBuildModule(moduleId)

        when:
        log.info ""

        scriptContainer.executeScript(moduleId, "com.company.script")
        Thread.sleep(1000);
        log.info "\n\n\n\n\nready to change\n\n\n\n"

        10000.times() {
            Thread.sleep(70);
            scriptContainer.executeScript(moduleId, "com.company.script")
        }

        then:
        log.info ""
    }

    def 'compile scripts and pack them into nicobar jar module make three modules'() {
        setup:

        def BASE_PATH = "src/test/resources/auto/"

        ScriptContainer scriptContainer = ScriptContainer.getInstance()

        def moduleId1 = ModuleId.create("moduleName", "moduleVersion")
        def moduleId1V2 = ModuleId.create("moduleName", "moduleVersion2")
        def moduleId2 = ModuleId.create("moduleName2", "moduleVersion2")

        scriptContainer
                .addModule(moduleId1, Paths.get(BASE_PATH), true)
                .reBuildModule(moduleId1)

                .addModule(moduleId2, Paths.get(BASE_PATH), true)
                .reBuildModule(moduleId1V2)

                .addModule(moduleId1V2, Paths.get(BASE_PATH), true)
                .reBuildModule(moduleId2);

        when:
        log.info ""

        scriptContainer
                .executeScript(moduleId1, "com.company.script")
                .executeScript(moduleId1V2, "com.company.script")
                .executeScript(moduleId2, "com.company.packagesuper.script")

        Thread.sleep(10000);
        scriptContainer
                .reBuildModule(moduleId1V2)
                .executeScript(moduleId1V2, "com.company.script")

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
