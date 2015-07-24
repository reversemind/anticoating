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

    def 'sub path validation'() {
        setup:
        log.info ""

        Path basePath = Paths.get("src/test/resources/base-path/").toAbsolutePath()
        boolean result = BuildModule.validateAndCreateModulePaths(basePath);

        when:
        log.info ""

        then:
        log.info ""
        result == true
    }
}
