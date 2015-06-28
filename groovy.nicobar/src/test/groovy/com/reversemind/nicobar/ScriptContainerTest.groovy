package com.reversemind.nicobar

import com.netflix.nicobar.core.archive.JarScriptArchive
import com.netflix.nicobar.core.archive.ModuleId
import com.netflix.nicobar.core.archive.ScriptArchive
import com.netflix.nicobar.core.archive.ScriptModuleSpec
import com.netflix.nicobar.core.plugin.BytecodeLoadingPlugin
import com.netflix.nicobar.groovy2.internal.compile.Groovy2CompilerHelper
import com.netflix.nicobar.groovy2.plugin.Groovy2CompilerPlugin
import org.codehaus.groovy.tools.GroovyClass
import spock.lang.Specification

import java.nio.file.Paths

/**
 *
 */
class ScriptContainerTest extends Specification {

    def 'init scriptContainer'(){
        setup:

        ScriptModuleSpec moduleSpec = new ScriptModuleSpec.Builder(ModuleId.create('sublevel'))
                .addCompilerPluginId(BytecodeLoadingPlugin.PLUGIN_ID)
                .addCompilerPluginId(Groovy2CompilerPlugin.PLUGIN_ID)
                .build()
        ScriptArchive scriptArchive = new JarScriptArchive.Builder(Paths.get('src/test/resources/libs/sublevel.jar').toAbsolutePath())
//                .setModuleSpec(moduleSpec)
                .build();

//        Set<GroovyClass> compiledClasses = new Groovy2CompilerHelper(Paths.get('/opt/CompileGroovy2'))
//                .addScriptArchive(scriptArchive)
//                .compile();
//

        ScriptContainer scriptContainer = ScriptContainer.getInstance()
        scriptContainer.updateScriptArchive(scriptArchive)

        ModuleId moduleId = scriptArchive.getModuleSpec().getModuleId()

        scriptContainer.executeModule(moduleId)
    }

}
