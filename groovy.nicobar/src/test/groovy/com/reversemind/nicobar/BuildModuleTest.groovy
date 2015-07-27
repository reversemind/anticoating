package com.reversemind.nicobar

import com.netflix.nicobar.core.archive.GsonScriptModuleSpecSerializer
import com.netflix.nicobar.core.archive.ModuleId
import com.netflix.nicobar.core.archive.ScriptModuleSpec
import com.netflix.nicobar.core.archive.ScriptModuleSpecSerializer
import groovy.util.logging.Slf4j
import spock.lang.Specification

import java.nio.file.Path
import java.nio.file.Paths

/**
 *
 */
@Slf4j
class BuildModuleTest extends Specification {

    def 'generate default module spec'() {
        setup:
        ModuleId moduleId = ModuleId.create("name", "version");
        ScriptModuleSpec expected = new BuildModule(moduleId, null).getDefaultScriptModuleSpec()

        when:
        ScriptModuleSpecSerializer serializer = new GsonScriptModuleSpecSerializer();
        String json = serializer.serialize(expected);

        log.info "json:${json}"

        ScriptModuleSpec deserialized = serializer.deserialize(json);

        then:
        deserialized.equals(expected);
    }

    def 'sub path validate right subdirectory structure'() {
        setup:
        log.info ""

        final Path BASE_PATH = Paths.get("src/test/resources/base-path-right/").toAbsolutePath()
        BuildModule buildModule = new BuildModule("moduleName", "moduleVersion", BASE_PATH);

        when:
        log.info ""

        boolean result = buildModule.validateAndCreateModulePaths();

        then:
        log.info ""
        result == true
    }

    def 'sub path validate wrong subdirectory structure'() {
        setup:
        log.info ""

        final Path BASE_PATH = Paths.get("src/test/resources/base-path-wrong").toAbsolutePath()
        BuildModule buildModule = new BuildModule("moduleName", "moduleVersion", BASE_PATH);

        when:
        log.info ""

        boolean result = buildModule.validateAndCreateModulePaths();

        then:
        log.info ""
        result == true
    }
}
