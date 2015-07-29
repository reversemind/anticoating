package com.reversemind.nicobar.container.utils

import com.netflix.nicobar.core.module.ScriptModuleLoader
import com.netflix.nicobar.core.plugin.BytecodeLoadingPlugin
import com.netflix.nicobar.core.plugin.ScriptCompilerPluginSpec
import com.netflix.nicobar.core.utils.ClassPathUtils
import com.netflix.nicobar.example.groovy2.ExampleResourceLocator
import com.netflix.nicobar.groovy2.internal.compile.Groovy2Compiler
import com.netflix.nicobar.groovy2.plugin.Groovy2CompilerPlugin

import java.nio.file.Path
import java.nio.file.Paths

/**
 *
 */
class NicobarUtils {

    def
    static ScriptModuleLoader.Builder createFullScriptModuleLoader() throws Exception {

        // #1
        /////////////////////////////////
        // Groovy2CompilerPlugin
        ScriptCompilerPluginSpec pluginSpecGroovy2 = buildGroovy2CompilerSpec()

        // #2
        /////////////////////////////////
        // BytecodeLoadingPlugin
        ScriptCompilerPluginSpec pluginSpecByteCode = new ScriptCompilerPluginSpec.Builder(BytecodeLoadingPlugin.PLUGIN_ID)
                .addRuntimeResource(ExampleResourceLocator.getGroovyRuntime())
                .addRuntimeResource(ExampleResourceLocator.getGroovyPluginLocation())
                .addRuntimeResource(getByteCodeLoadingPluginPath())

        // TODO need make addRuntimeResourceS - add jar's from path - like a shared libraries
                .addRuntimeResource(Paths.get("src/test/resources/libs/junit-4.12.jar").toAbsolutePath())
                .addRuntimeResource(Paths.get("src/test/resources/libs/spock-core-0.7-groovy-2.0.jar").toAbsolutePath())
                .addRuntimeResource(Paths.get("src/test/resources/libs/joda-time-2.8.1.jar").toAbsolutePath())
                .addRuntimeResource(getCoberturaJar(NicobarUtils.class.getClassLoader()))
                .withPluginClassName(BytecodeLoadingPlugin.class.getName())
//                .withPluginClassName(Groovy2CompilerPlugin.class.getName())
                .build();

        // create and start the builder with the plugin
        return new ScriptModuleLoader.Builder()
                .addPluginSpec(pluginSpecGroovy2)
                .addPluginSpec(pluginSpecByteCode)
    }

    def
    static ScriptModuleLoader.Builder createLightScriptModuleLoader() throws Exception {

        // #1
        /////////////////////////////////
        // Groovy2CompilerPlugin
        ScriptCompilerPluginSpec pluginSpecGroovy2 = buildGroovy2CompilerSpec()

        // #2
        /////////////////////////////////
        // BytecodeLoadingPlugin
        ScriptCompilerPluginSpec pluginSpecByteCode = new ScriptCompilerPluginSpec.Builder(BytecodeLoadingPlugin.PLUGIN_ID)
                .addRuntimeResource(ExampleResourceLocator.getGroovyRuntime())
                .addRuntimeResource(ExampleResourceLocator.getGroovyPluginLocation())
                .addRuntimeResource(getByteCodeLoadingPluginPath())

        // TODO need make addRuntimeResourceS - add jar's from path - like a shared libraries
                .addRuntimeResource(Paths.get("src/test/resources/libs/junit-4.12.jar").toAbsolutePath())
                .addRuntimeResource(Paths.get("src/test/resources/libs/spock-core-0.7-groovy-2.0.jar").toAbsolutePath())
                .addRuntimeResource(getCoberturaJar(NicobarUtils.class.getClassLoader()))
                .withPluginClassName(BytecodeLoadingPlugin.class.getName())
//                .withPluginClassName(Groovy2CompilerPlugin.class.getName())
                .build();

        // create and start the builder with the plugin
        return new ScriptModuleLoader.Builder()
                .addPluginSpec(pluginSpecGroovy2)
                .addPluginSpec(pluginSpecByteCode)
    }

    def
    static Path getByteCodeLoadingPluginPath() {
        String resourceName = ClassPathUtils.classNameToResourceName("com.netflix.nicobar.core.plugin.BytecodeLoadingPlugin");
        Path path = ClassPathUtils.findRootPathForResource(resourceName, NicobarUtils.class.getClassLoader());
        if (path == null) {
            throw new IllegalStateException("coudln't find BytecodeLoadingPlugin plugin jar in the classpath.");
        }
        return path
    }

    def
    static Path getNoCodePluginPath() {
        //com.netflix.nicobar.core.plugin.BytecodeLoadingPlugin
        String resourceName = ClassPathUtils.classNameToResourceName("com.netflix.nicobar.core.plugin.BytecodeLoadingPlugin");
        Path path = ClassPathUtils.findRootPathForResource(resourceName, NicobarUtils.class.getClassLoader());
        if (path == null) {
            throw new IllegalStateException("coudln't find BytecodeLoadingPlugin plugin jar in the classpath.");
        }
        return path
    }

    /**
     * Initially copied from Nicobar repo
     *
     * @param classLoader
     * @return
     */
    def
    static Path getCoberturaJar(ClassLoader classLoader) {
        return ClassPathUtils.findRootPathForResource("net/sourceforge/cobertura/coveragedata/HasBeenInstrumented.class", classLoader);
    }

    def
    static ScriptCompilerPluginSpec buildGroovy2CompilerSpec(){
        // #1
        /////////////////////////////////
        // Groovy2CompilerPlugin

        // create the groovy plugin spec. this plugin specified a new com.reversemind.nicobar.container.utils.module and classloader called "Groovy2Runtime"
        // which contains the groovy-all-n.n.n.jar and the jlee-nicobar project
        ScriptCompilerPluginSpec pluginSpec = new ScriptCompilerPluginSpec.Builder(Groovy2Compiler.GROOVY2_COMPILER_ID)
                .addRuntimeResource(ExampleResourceLocator.getGroovyRuntime())
                .addRuntimeResource(ExampleResourceLocator.getGroovyPluginLocation())
                .addRuntimeResource(getByteCodeLoadingPluginPath())

        // hack to make the gradle build work. still doesn't seem to properly instrument the code
        // should probably add a classloader dependency on the system classloader instead
                .addRuntimeResource(getCoberturaJar(NicobarUtils.class.getClassLoader()))
                .withPluginClassName(Groovy2CompilerPlugin.class.getName())
                .build();

        return pluginSpec;
    }

    def
    static ScriptCompilerPluginSpec buildNoCompilerSpec(){
        // #1
        /////////////////////////////////
        // Groovy2CompilerPlugin

        // create the groovy plugin spec. this plugin specified a new com.reversemind.nicobar.container.utils.module and classloader called "Groovy2Runtime"
        // which contains the groovy-all-n.n.n.jar and the jlee-nicobar project
        ScriptCompilerPluginSpec pluginSpec = new ScriptCompilerPluginSpec.Builder(Groovy2Compiler.GROOVY2_COMPILER_ID)
                .addRuntimeResource(ExampleResourceLocator.getGroovyRuntime())
                .addRuntimeResource(ExampleResourceLocator.getGroovyPluginLocation())
                .addRuntimeResource(getByteCodeLoadingPluginPath())

        // hack to make the gradle build work. still doesn't seem to properly instrument the code
        // should probably add a classloader dependency on the system classloader instead
                .addRuntimeResource(getCoberturaJar(NicobarUtils.class.getClassLoader()))
                .withPluginClassName(Groovy2CompilerPlugin.class.getName())
                .build();

        return pluginSpec;
    }
}
