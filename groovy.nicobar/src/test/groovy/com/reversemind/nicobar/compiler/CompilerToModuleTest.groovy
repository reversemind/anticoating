package com.reversemind.nicobar.compiler

import com.netflix.nicobar.core.archive.JarScriptArchive
import com.netflix.nicobar.core.archive.ModuleId
import com.netflix.nicobar.core.archive.PathScriptArchive
import com.netflix.nicobar.core.archive.ScriptArchive
import com.netflix.nicobar.core.archive.ScriptModuleSpec
import com.netflix.nicobar.core.plugin.BytecodeLoadingPlugin
import com.netflix.nicobar.groovy2.internal.compile.Groovy2CompilerHelper
import com.netflix.nicobar.groovy2.plugin.Groovy2CompilerPlugin
import com.reversemind.nicobar.BuildModule
import org.codehaus.groovy.tools.GroovyClass
import spock.lang.Specification

import java.nio.file.Path
import java.nio.file.Paths

/**
 *
 */
class CompilerToModuleTest extends Specification{

    def 'compile a module with spec'(){
        setup:

        ScriptModuleSpec moduleSpec = new ScriptModuleSpec.Builder(ModuleId.create('precompiled','v0_1-SNAPSHOT'))
                .addCompilerPluginId(BytecodeLoadingPlugin.PLUGIN_ID)
                .addCompilerPluginId(Groovy2CompilerPlugin.PLUGIN_ID)
                .build();

        Path scriptRootPath = Paths.get('src/main/resources/module').toAbsolutePath()
        PathScriptArchive scriptArchive = new PathScriptArchive.Builder(scriptRootPath)
                .setRecurseRoot(true)
                .setModuleSpec(moduleSpec)
                .build();

        Set<GroovyClass> compiledClasses = new Groovy2CompilerHelper(Paths.get('src/main/resources/compileTo').toAbsolutePath())
                .addScriptArchive(scriptArchive)
                .compile();

        // #1 check that directory contains all files + after that add - non .groovy files

        // #2 combine into single .jar

        // #3

    }

    def 'load by specs'(){
        setup:
        ScriptArchive scriptArchive = new JarScriptArchive.Builder(Paths.get('src/test/resources/libs/precompiled.jar').toAbsolutePath())

                .build();

        println "Module spec" + scriptArchive.getModuleSpec()
    }

    def 'compile a module with spec2'(){
        setup:

        ScriptModuleSpec moduleSpec = new ScriptModuleSpec.Builder(ModuleId.create('precompiled','v0_1-SNAPSHOT'))
                .addCompilerPluginId(BytecodeLoadingPlugin.PLUGIN_ID)
                .addCompilerPluginId(Groovy2CompilerPlugin.PLUGIN_ID)
                .build();

        Path scriptRootPath = Paths.get('src/test/resources/bunch/source').toAbsolutePath()
        PathScriptArchive scriptArchive = new PathScriptArchive.Builder(scriptRootPath)
                .setRecurseRoot(true)
                .setModuleSpec(moduleSpec)
                .build();

        Set<GroovyClass> compiledClasses = new Groovy2CompilerHelper(Paths.get('src/main/resources/compileTo').toAbsolutePath())
                .addScriptArchive(scriptArchive)
                .compile();

        // #1 check that directory contains all files + after that add - non .groovy files

        // #2 combine into single .jar

        // #3

    }



}
