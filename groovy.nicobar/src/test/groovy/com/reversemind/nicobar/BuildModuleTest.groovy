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

    final String moduleName = "moduleName"
    final String moduleVersion = "moduleVersion"
    final ModuleId moduleId = ModuleId.create(moduleName, moduleVersion);

    def 'generate default module spec'() {
        setup:

        final Path BASE_PATH = Paths.get("src/test/resources/base-path-right/").toAbsolutePath()
        ScriptModuleSpec expected = new BuildModule(moduleId, BASE_PATH).getDefaultScriptModuleSpec()

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
        BuildModule buildModule = new BuildModule(moduleId, BASE_PATH);

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
        BuildModule buildModule = new BuildModule(moduleId, BASE_PATH);

        when:
        log.info ""

        boolean result = buildModule.validateAndCreateModulePaths();

        then:
        log.info ""
        result == true
    }
}
