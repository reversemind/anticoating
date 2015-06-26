package com.company.netflix.compile

import com.netflix.nicobar.core.archive.JarScriptArchive
import com.netflix.nicobar.core.archive.ModuleId
import com.netflix.nicobar.core.archive.PathScriptArchive
import com.netflix.nicobar.core.archive.ScriptArchive
import com.netflix.nicobar.core.archive.ScriptModuleSpec
import com.netflix.nicobar.core.module.ScriptModule
import com.netflix.nicobar.core.module.ScriptModuleLoader
import com.netflix.nicobar.core.module.ScriptModuleUtils
import com.netflix.nicobar.core.plugin.BytecodeLoadingPlugin
import com.netflix.nicobar.core.plugin.ScriptCompilerPluginSpec
import com.netflix.nicobar.core.utils.ClassPathUtils
import com.netflix.nicobar.example.groovy2.ExampleResourceLocator
import com.netflix.nicobar.groovy2.internal.compile.Groovy2Compiler
import com.netflix.nicobar.groovy2.plugin.Groovy2CompilerPlugin
import org.codehaus.groovy.runtime.InvokerHelper
import spock.lang.Specification

import java.nio.file.Path
import java.nio.file.Paths

import static org.testng.Assert.assertNotNull
import static org.testng.Assert.assertNotNull
import static org.testng.Assert.fail

/**
 *
 */
class LoadModules extends Specification{

    private static final String GROOVY2_COMPILER_PLUGIN = Groovy2CompilerPlugin.class.getName();

    def 'paths'(){
        setup:
        Path path = Paths.get('src/test/resources/libs/modules2/repository/spock-core.jar');
        println "path:${path} :" + path.toAbsolutePath()
    }

    def 'load'(){
        setup:
        ScriptModuleLoader moduleLoader = createGroovyModuleLoader().build();

        ScriptModuleSpec _moduleSpec = new ScriptModuleSpec.Builder(ModuleId.create("spock-core"))
                .addCompilerPluginId(BytecodeLoadingPlugin.PLUGIN_ID)
                .addCompilerPluginId(Groovy2CompilerPlugin.PLUGIN_ID)
                .build()
        ScriptArchive scriptArchive = new JarScriptArchive.Builder(Paths.get('src/test/resources/libs/modules2/repository/spock-core.jar').toAbsolutePath())
                .setCreateTime(new Date().getTime())
                .setModuleSpec(_moduleSpec)
                .build();
//        moduleLoader.updateScriptArchives(new LinkedHashSet<ScriptArchive>(Arrays.asList(scriptArchive)));



        _moduleSpec = new ScriptModuleSpec.Builder(ModuleId.create("nicobar-core"))
//                .addCompilerPluginId(BytecodeLoadingPlugin.PLUGIN_ID)
//                .addCompilerPluginId(Groovy2CompilerPlugin.PLUGIN_ID)
                .build()
        scriptArchive = new JarScriptArchive.Builder(Paths.get('src/test/resources/libs/modules2/repository/nicobar-core.jar').toAbsolutePath())
                .setCreateTime(new Date().getTime())
                .setModuleSpec(_moduleSpec)
                .build();
//        moduleLoader.updateScriptArchives(new LinkedHashSet<ScriptArchive>(Arrays.asList(scriptArchive)));


        ScriptModuleSpec moduleSpec = new ScriptModuleSpec.Builder(ModuleId.create("simple-module"))
//                .addModuleDependency("spock-core")
//                .addModuleDependency("nicobar-core")
                .addCompilerPluginId(BytecodeLoadingPlugin.PLUGIN_ID)
                .addCompilerPluginId(Groovy2CompilerPlugin.PLUGIN_ID)
                .build();

        def scriptRootPath = Paths.get('src/test/resources/libs/modules2/script').toAbsolutePath()
        scriptArchive = new PathScriptArchive.Builder(scriptRootPath)
                .setRecurseRoot(true)
                .setModuleSpec(moduleSpec)
                .build();

        moduleLoader.updateScriptArchives(new LinkedHashSet<ScriptArchive>(Arrays.asList(scriptArchive)));




        ScriptModule scriptModule = moduleLoader.getScriptModule("simple-module")

        println "size:" + scriptModule.getModuleClassLoader().getLoadedClasses().size()


        println "!!!" + scriptModule.getLoadedClasses()

        Class<?> clazz = ScriptModuleUtils.findClass(scriptModule, "go")
//        Class<?> clazz = findClassByName(scriptModule, "go");

        ((clazz.newInstance()).getMetaClass()).

        println "go!"
        Script script = InvokerHelper.createScript(clazz, new Binding());
        script.run()

    }

    private static Class<?> findClassByName(ScriptModule scriptModule, String className) {
        Set<Class<?>> classes = scriptModule.getLoadedClasses();
        for (Class<?> clazz : classes) {
            if (clazz.getName().equals(className)) {
                return clazz;
            }
        }
        fail("couldn't find class " + className);
        return null;
    }

    public static Path getCoberturaJar(ClassLoader classLoader) {
        return ClassPathUtils.findRootPathForResource("net/sourceforge/cobertura/coveragedata/HasBeenInstrumented.class", classLoader);
    }

    private ScriptModuleLoader.Builder createGroovyModuleLoader() throws Exception {

        // create the groovy plugin spec. this plugin specified a new module and classloader called "Groovy2Runtime"
        // which contains the groovy-all-2.1.6.jar and the nicobar-groovy2 project.
        ScriptCompilerPluginSpec pluginSpec = new ScriptCompilerPluginSpec.Builder(Groovy2Compiler.GROOVY2_COMPILER_ID)
                .addRuntimeResource(ExampleResourceLocator.getGroovyRuntime())
                .addRuntimeResource(ExampleResourceLocator.getGroovyPluginLocation())
                .addRuntimeResource(getByteCodeLoadingPluginLocation())
        .addRuntimeResource(Paths.get("src/test/resources/libs/modules2/lib").toAbsolutePath())

        // hack to make the gradle build work. still doesn't seem to properly instrument the code
        // should probably add a classloader dependency on the system classloader instead
                .addRuntimeResource(getCoberturaJar(getClass().getClassLoader()))
                .withPluginClassName(Groovy2CompilerPlugin.class.getName())
                .build();


        ScriptCompilerPluginSpec pluginSpecByteCode = new ScriptCompilerPluginSpec.Builder(BytecodeLoadingPlugin.PLUGIN_ID)
                .addRuntimeResource(ExampleResourceLocator.getGroovyRuntime())
                .addRuntimeResource(ExampleResourceLocator.getGroovyPluginLocation())
                .addRuntimeResource(getByteCodeLoadingPluginLocation())

                // TODO need make addRuntimeResourceS - add jar's from path - like a shared libraries
                .addRuntimeResource(Paths.get("src/test/resources/libs/modules2/lib").toAbsolutePath())
                .addRuntimeResource(Paths.get("src/test/resources/libs/modules2/lib/junit-4.12.jar").toAbsolutePath())
                .addRuntimeResource(Paths.get("src/test/resources/libs/modules2/lib/nicobar-core.jar").toAbsolutePath())
                .addRuntimeResource(Paths.get("src/test/resources/libs/modules2/lib/nicobar-groovy2-0.2.7-SNAPSHOT.jar").toAbsolutePath())
                .addRuntimeResource(Paths.get("src/test/resources/libs/modules2/lib/spock-core-0.7-groovy-2.0.jar").toAbsolutePath())
                .addRuntimeResource(Paths.get("src/test/resources/libs/modules2/lib/joda-time-2.8.1.jar").toAbsolutePath())
                .addRuntimeResource(getCoberturaJar(getClass().getClassLoader()))
                .withPluginClassName(BytecodeLoadingPlugin.class.getName())
                .build();



        // create and start the builder with the plugin
        return new ScriptModuleLoader.Builder()
                .addPluginSpec(pluginSpec)
                .addPluginSpec(pluginSpecByteCode);

    }


    public static Path getByteCodeLoadingPluginLocation() {
        String resourceName = ClassPathUtils.classNameToResourceName("com.netflix.nicobar.core.plugin.BytecodeLoadingPlugin");
        Path path = ClassPathUtils.findRootPathForResource(resourceName, ExampleResourceLocator.class.getClassLoader());
        if (path == null) {
            throw new IllegalStateException("coudln't find BytecodeLoadingPlugin plugin jar in the classpath.");
        }
        return path;
    }
}
