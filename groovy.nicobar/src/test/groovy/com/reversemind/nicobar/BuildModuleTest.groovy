package com.reversemind.nicobar

import com.netflix.nicobar.core.archive.GsonScriptModuleSpecSerializer
import com.netflix.nicobar.core.archive.ModuleId
import com.netflix.nicobar.core.archive.ScriptModuleSpec
import com.netflix.nicobar.core.archive.ScriptModuleSpecSerializer
import spock.lang.Specification

/**
 *
 */
class BuildModuleTest extends Specification {

    def 'generate default module spec'() {
        setup:
        ModuleId moduleId = ModuleId.create("name", "version");
        ScriptModuleSpec expected = new BuildModule(moduleId, null).getDefaultScriptModuleSpec()

        when:
        ScriptModuleSpecSerializer serializer = new GsonScriptModuleSpecSerializer();
        String json = serializer.serialize(expected);

        ScriptModuleSpec deserialized = serializer.deserialize(json);

        then:
        deserialized.equals(expected);
    }
}
