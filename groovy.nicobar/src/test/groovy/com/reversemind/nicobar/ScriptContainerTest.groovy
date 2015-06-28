package com.reversemind.nicobar

import com.netflix.nicobar.core.archive.JarScriptArchive
import com.netflix.nicobar.core.archive.ModuleId
import com.netflix.nicobar.core.archive.ScriptArchive
import spock.lang.Specification

import java.nio.file.Paths

/**
 *
 */
class ScriptContainerTest extends Specification {

    def 'init scriptContainer'(){
        setup:

        ScriptArchive scriptArchive = new JarScriptArchive.Builder(Paths.get('src/test/resources/libs/sublevel.jar').toAbsolutePath())

                .build();

        ScriptContainer scriptContainer = ScriptContainer.getInstance()
        scriptContainer.updateScriptArchive(scriptArchive)

        ModuleId moduleId = scriptArchive.getModuleSpec().getModuleId()

        scriptContainer.executeModule(moduleId)
    }

}
