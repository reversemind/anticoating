package com.reversemind.nicobar

import com.netflix.nicobar.core.archive.JarScriptArchive
import com.netflix.nicobar.core.archive.ModuleId
import com.netflix.nicobar.core.archive.PathScriptArchive
import com.netflix.nicobar.core.archive.ScriptArchive
import com.netflix.nicobar.core.archive.ScriptModuleSpec
import com.netflix.nicobar.core.module.ScriptModule
import com.netflix.nicobar.core.module.ScriptModuleLoader
import com.netflix.nicobar.core.module.ScriptModuleUtils
import com.netflix.nicobar.core.plugin.BytecodeLoadingPlugin
import com.netflix.nicobar.groovy2.internal.compile.Groovy2CompilerHelper
import com.netflix.nicobar.groovy2.plugin.Groovy2CompilerPlugin
import com.reversemind.nicobar.utils.NicobarUtils
import groovy.util.logging.Slf4j
import org.codehaus.groovy.runtime.InvokerHelper
import org.codehaus.groovy.tools.GroovyClass
import spock.lang.Ignore
import spock.lang.Specification

import java.nio.file.Paths

/**
 *
 */
@Slf4j
class LoadModuleTest extends Specification {

    def 'compile groovy script into external directory and run it'() {
        setup:
        log.info 'Let\'s get it started'

        def scriptName = 'scriptRun01'
        def scriptRootPath = Paths.get('src/test/resources/script1').toAbsolutePath()
        def classCompiledPath = Paths.get('src/test/resources/script1/compiled').toAbsolutePath()

        ScriptArchive scriptArchive = new PathScriptArchive.Builder(scriptRootPath)
                .setRecurseRoot(false)
                .addFile(Paths.get(scriptName + '.groovy'))
                .build();

        Set<GroovyClass> compiledClasses = new Groovy2CompilerHelper(classCompiledPath)
                .addScriptArchive(scriptArchive)
                .compile();

        compiledClasses.each { klass ->
            log.info "" + klass.getName()
        }

        // Run as a Script class from GroovyScriptEngine
        GroovyClass groovyClass = compiledClasses.iterator().next()

        Class klass = getClass().getClassLoader().defineClass(groovyClass.getName(), groovyClass.getBytes(), 0, groovyClass.getBytes().length);

        Script script = InvokerHelper.createScript(klass, new Binding());
        script.run()
    }

    def 'use ScriptModuleLoader to compile groovy scriptForModule1.groovy'() {
        setup:
        ScriptModuleLoader moduleLoader = NicobarUtils.createFullScriptModuleLoader().build()

        def MODULE_NAME = 'module1'

        // Create Nicobar module
        ScriptModuleSpec moduleSpec = new ScriptModuleSpec.Builder(ModuleId.create(MODULE_NAME))
                // use BytecodeLoadingPlugin - 'cause we use spock-core-0.7-groovy-2.0.jar at runtime
                //
                // lib spock-core-0.7-groovy-2.0.jar
                // already added to moduleLoader via
                // .addRuntimeResource(Paths.get("src/test/resources/libs/spock-core-0.7-groovy-2.0.jar").toAbsolutePath())
                .addCompilerPluginId(BytecodeLoadingPlugin.PLUGIN_ID)
                .addCompilerPluginId(Groovy2CompilerPlugin.PLUGIN_ID)
                .build();

        def scriptRootPath = Paths.get("src/test/resources/${MODULE_NAME}").toAbsolutePath()
        ScriptArchive scriptArchive = new PathScriptArchive.Builder(scriptRootPath)
                .setRecurseRoot(true)
                .setModuleSpec(moduleSpec)
                .build();

        // at this stage groovy script should be compiled at /tmo/ScriptModuleLoader[6601967070332071266] directory
        //
        // for example
        // /tmp/ScriptModuleLoader234879710123481140/module1:1435319868587/scriptForModule1.class
        moduleLoader.updateScriptArchives(new LinkedHashSet<ScriptArchive>(Arrays.asList(scriptArchive)));


        when:
        ScriptModule scriptModule = moduleLoader.getScriptModule(MODULE_NAME)
        Set<Class<?>> classes = scriptModule.getLoadedClasses()

        then:
        scriptModule != null
        classes != null
        classes.size() == 1
    }

    def 'use ScriptModuleLoader with Dependency to compile groovy scriptForModule2.groovy'() {
        setup:
        ScriptModuleLoader moduleLoader = NicobarUtils.createLightScriptModuleLoader().build()

        def MODULE_NAME = 'module2'
        def DEPS_MODULE_NAME = 'joda-time'

        ScriptModuleSpec moduleSpec = new ScriptModuleSpec.Builder(ModuleId.create(DEPS_MODULE_NAME))
                .addCompilerPluginId(BytecodeLoadingPlugin.PLUGIN_ID)
                .addCompilerPluginId(Groovy2CompilerPlugin.PLUGIN_ID)
                .build()
        ScriptArchive scriptArchive = new JarScriptArchive.Builder(Paths.get('src/test/resources/libs/joda-time-2.8.1.jar').toAbsolutePath())
                .setCreateTime(new Date().getTime())
                .setModuleSpec(moduleSpec)
                .build();
        moduleLoader.updateScriptArchives(new LinkedHashSet<ScriptArchive>(Arrays.asList(scriptArchive)));


        // Create Nicobar module
        moduleSpec = new ScriptModuleSpec.Builder(ModuleId.create(MODULE_NAME))
                .addModuleDependency(DEPS_MODULE_NAME)

                // use BytecodeLoadingPlugin - 'cause we use spock-core-0.7-groovy-2.0.jar at runtime
                //
                // lib spock-core-0.7-groovy-2.0.jar
                // already added to moduleLoader via
                // .addRuntimeResource(Paths.get("src/test/resources/libs/spock-core-0.7-groovy-2.0.jar").toAbsolutePath())
                .addCompilerPluginId(BytecodeLoadingPlugin.PLUGIN_ID)
                .addCompilerPluginId(Groovy2CompilerPlugin.PLUGIN_ID)
                .build();

        def scriptRootPath = Paths.get("src/test/resources/${MODULE_NAME}").toAbsolutePath()
        scriptArchive = new PathScriptArchive.Builder(scriptRootPath)
                .setRecurseRoot(true)
                .setModuleSpec(moduleSpec)
                .build();

        // at this stage groovy script should be compiled at /tmo/ScriptModuleLoader[6601967070332071266] directory
        //
        // for example
        // /tmp/ScriptModuleLoader234879710123481140/module1:1435319868587/scriptForModule1.class
        moduleLoader.updateScriptArchives(new LinkedHashSet<ScriptArchive>(Arrays.asList(scriptArchive)));


        when:
        ScriptModule scriptModule = moduleLoader.getScriptModule(MODULE_NAME)
        Set<Class<?>> classes = scriptModule.getLoadedClasses()

        then:
        scriptModule != null
        classes != null
        classes.size() == 1
    }

    def 'run script'(){
        setup:
        ScriptModuleLoader moduleLoader = NicobarUtils.createFullScriptModuleLoader().build()

        def MODULE_NAME = 'module1'
        def SCRIPT_NAME = 'scriptForModule1'

        // Create Nicobar module
        ScriptModuleSpec moduleSpec = new ScriptModuleSpec.Builder(ModuleId.create(MODULE_NAME))
        // use BytecodeLoadingPlugin - 'cause we use spock-core-0.7-groovy-2.0.jar at runtime
        //
        // lib spock-core-0.7-groovy-2.0.jar
        // already added to moduleLoader via
        // .addRuntimeResource(Paths.get("src/test/resources/libs/spock-core-0.7-groovy-2.0.jar").toAbsolutePath())
                .addCompilerPluginId(BytecodeLoadingPlugin.PLUGIN_ID)
                .addCompilerPluginId(Groovy2CompilerPlugin.PLUGIN_ID)
                .build();

        def scriptRootPath = Paths.get("src/test/resources/${MODULE_NAME}").toAbsolutePath()
        ScriptArchive scriptArchive = new PathScriptArchive.Builder(scriptRootPath)
                .setRecurseRoot(true)
                .setModuleSpec(moduleSpec)
                .build();

        // at this stage groovy script should be compiled at /tmo/ScriptModuleLoader[6601967070332071266] directory
        //
        // for example
        // /tmp/ScriptModuleLoader234879710123481140/module1:1435319868587/scriptForModule1.class
        moduleLoader.updateScriptArchives(new LinkedHashSet<ScriptArchive>(Arrays.asList(scriptArchive)));


        when:
        ScriptModule scriptModule = moduleLoader.getScriptModule(MODULE_NAME)
        Set<Class<GroovyObject>> classes = scriptModule.getLoadedClasses()

        Class<GroovyObject> clazz = ScriptModuleUtils.findClass(scriptModule, SCRIPT_NAME)

        Set<Class> _classes = scriptModule.getLoadedClasses();
        Class<GroovyObject> _targetClass = null;
        for (Class<?> _clazz : _classes) {
            if (_clazz.getName().equals(SCRIPT_NAME)) {
                _targetClass = _clazz;
                break;
            }
        }

        _targetClass


        GroovyObject groovyObject;



        Object[] EMPTY_MAIN_ARGS = FakeClassTwo.EMPTY_MAIN_ARGS

        String[] EMPTY_STRINGS = []

        InvokerHelper.runScript(clazz, EMPTY_STRINGS);


        Object object = clazz.newInstance()
        Script script = new Script() {
            public Object run() {
                // pass throw bindings
                Object args = new Binding().getVariables().get("args");

                Object argsToPass = EMPTY_MAIN_ARGS;
                if (args != null && args instanceof String[]) {
                    argsToPass = args;
                }
                object.invokeMethod("main", argsToPass);
                return null;
            }
        };
        script.run()


        then:
        scriptModule != null
        classes != null
        classes.size() == 1
    }

    /**
     * Case with unable to resolve a dependency from external lib
     */
    @Ignore
    def 'exc'(){
//        ScriptCompilationException

        setup:
        ScriptModuleLoader moduleLoader = NicobarUtils.createLightScriptModuleLoader().build()

        def MODULE_NAME = 'module2'
        def DEPS_MODULE_NAME = 'joda-time'

        ScriptModuleSpec moduleSpec = new ScriptModuleSpec.Builder(ModuleId.create(DEPS_MODULE_NAME))
                .addCompilerPluginId(BytecodeLoadingPlugin.PLUGIN_ID)
                .addCompilerPluginId(Groovy2CompilerPlugin.PLUGIN_ID)
                .build()
        ScriptArchive scriptArchive = new JarScriptArchive.Builder(Paths.get('src/test/resources/libs/joda-time-2.8.1.jar').toAbsolutePath())
                .setCreateTime(new Date().getTime())
                .setModuleSpec(moduleSpec)
                .build();
        moduleLoader.updateScriptArchives(new LinkedHashSet<ScriptArchive>(Arrays.asList(scriptArchive)));


        // Create Nicobar module
        moduleSpec = new ScriptModuleSpec.Builder(ModuleId.create(MODULE_NAME))
//                .addModuleDependency(DEPS_MODULE_NAME)

        // use BytecodeLoadingPlugin - 'cause we use spock-core-0.7-groovy-2.0.jar at runtime
        //
        // lib spock-core-0.7-groovy-2.0.jar
        // already added to moduleLoader via
        // .addRuntimeResource(Paths.get("src/test/resources/libs/spock-core-0.7-groovy-2.0.jar").toAbsolutePath())
                .addCompilerPluginId(BytecodeLoadingPlugin.PLUGIN_ID)
                .addCompilerPluginId(Groovy2CompilerPlugin.PLUGIN_ID)
                .build();

        def scriptRootPath = Paths.get("src/test/resources/${MODULE_NAME}").toAbsolutePath()
        scriptArchive = new PathScriptArchive.Builder(scriptRootPath)
                .setRecurseRoot(true)
                .setModuleSpec(moduleSpec)
                .build();

        // at this stage groovy script should be compiled at /tmo/ScriptModuleLoader[6601967070332071266] directory
        //
        // for example
        // /tmp/ScriptModuleLoader234879710123481140/module1:1435319868587/scriptForModule1.class
        moduleLoader.updateScriptArchives(new LinkedHashSet<ScriptArchive>(Arrays.asList(scriptArchive)));


        when:


                // Build again module without dependency to joda-time module

                // Create Nicobar module
                moduleSpec = new ScriptModuleSpec.Builder(ModuleId.create(MODULE_NAME))

                // use BytecodeLoadingPlugin - 'cause we use spock-core-0.7-groovy-2.0.jar at runtime
                //
                // lib spock-core-0.7-groovy-2.0.jar
                // already added to moduleLoader via
                // .addRuntimeResource(Paths.get("src/test/resources/libs/spock-core-0.7-groovy-2.0.jar").toAbsolutePath())
                        .addCompilerPluginId(BytecodeLoadingPlugin.PLUGIN_ID)
                        .addCompilerPluginId(Groovy2CompilerPlugin.PLUGIN_ID)
                        .build();

                scriptArchive = new PathScriptArchive.Builder(scriptRootPath)
                        .setRecurseRoot(true)
                        .setModuleSpec(moduleSpec)
                        .build();

                moduleLoader.updateScriptArchives(new LinkedHashSet<ScriptArchive>(Arrays.asList(scriptArchive)));


        then:
        thrown(Throwable)

    }
}