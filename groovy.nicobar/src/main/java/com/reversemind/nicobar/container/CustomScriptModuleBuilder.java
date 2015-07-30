package com.reversemind.nicobar.container;

import com.netflix.nicobar.core.plugin.BytecodeLoadingPlugin;
import com.netflix.nicobar.core.plugin.ScriptCompilerPluginSpec;
import com.netflix.nicobar.core.utils.ClassPathUtils;
import com.netflix.nicobar.example.groovy2.ExampleResourceLocator;
import com.netflix.nicobar.groovy2.internal.compile.Groovy2Compiler;
import com.netflix.nicobar.groovy2.plugin.Groovy2CompilerPlugin;
import com.reversemind.nicobar.container.utils.NicobarUtils;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 *
 */
public class CustomScriptModuleBuilder {

    public static ScriptContainerModuleLoader.Builder createScriptModuleLoader() throws Exception {

        // #1
        /////////////////////////////////
        // Groovy2CompilerPlugin
        ScriptCompilerPluginSpec pluginSpecGroovy2 = buildGroovy2CompilerSpec();

        // #2
        /////////////////////////////////
        // BytecodeLoadingPlugin
        ScriptCompilerPluginSpec pluginSpecByteCode = new ScriptCompilerPluginSpec.Builder(BytecodeLoadingPlugin.PLUGIN_ID)

                // spock-core-0.7-groovy-2.0.jar needs to run inside Spock tests
                .addRuntimeResource(Paths.get("src/test/resources/libs/spock-core-0.7-groovy-2.0.jar").toAbsolutePath())

                .addRuntimeResource(ExampleResourceLocator.getGroovyRuntime())
                .addRuntimeResource(ExampleResourceLocator.getGroovyPluginLocation())
                .addRuntimeResource(getCoberturaJar(NicobarUtils.class.getClassLoader()))

                .addRuntimeResource(getByteCodeLoadingPluginPath())
                .withPluginClassName(BytecodeLoadingPlugin.class.getName())
                .build();

        // create and start the builder with the plugin
        return new ScriptContainerModuleLoader.Builder()
                .addPluginSpec(pluginSpecGroovy2)
                .addPluginSpec(pluginSpecByteCode);
    }

    static ScriptCompilerPluginSpec buildGroovy2CompilerSpec() {
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

    private static Path getByteCodeLoadingPluginPath() {
        String resourceName = ClassPathUtils.classNameToResourceName("com.netflix.nicobar.core.plugin.BytecodeLoadingPlugin");
        Path path = ClassPathUtils.findRootPathForResource(resourceName, NicobarUtils.class.getClassLoader());
        if (path == null) {
            throw new IllegalStateException("coudln't find BytecodeLoadingPlugin plugin jar in the classpath.");
        }
        return path;
    }

    private static Path getCoberturaJar(ClassLoader classLoader) {
        return ClassPathUtils.findRootPathForResource("net/sourceforge/cobertura/coveragedata/HasBeenInstrumented.class", classLoader);
    }
}
