package com.reversemind.nicobar.container.utils

import com.netflix.nicobar.core.archive.GsonScriptModuleSpecSerializer
import com.netflix.nicobar.core.archive.ModuleId
import com.netflix.nicobar.core.archive.ScriptModuleSpec
import com.netflix.nicobar.core.archive.ScriptModuleSpecSerializer
import com.netflix.nicobar.core.plugin.BytecodeLoadingPlugin
import com.netflix.nicobar.core.plugin.ScriptCompilerPluginSpec
import com.netflix.nicobar.core.utils.ClassPathUtils
import com.netflix.nicobar.example.groovy2.ExampleResourceLocator
import com.netflix.nicobar.groovy2.internal.compile.Groovy2Compiler
import com.netflix.nicobar.groovy2.plugin.Groovy2CompilerPlugin
import com.reversemind.nicobar.container.ContainerModuleLoader
import org.apache.commons.lang3.StringUtils
import org.apache.tools.ant.BuildException
import org.apache.tools.ant.Project
import org.apache.tools.ant.taskdefs.Jar

import java.nio.file.Path
import java.nio.file.Paths

/**
 *
 */
public class ContainerUtils {

    private final
    static ScriptModuleSpecSerializer DEFAULT_MODULE_SPEC_SERIALIZER = new GsonScriptModuleSpecSerializer();

    //
    // spock-core-0.7-groovy-2.0.jar needs to run inside Spock tests
    // .addRuntimeResource(Paths.get("src/test/resources/libs/spock-core-0.7-groovy-2.0.jar").toAbsolutePath())

    public
    static ContainerModuleLoader.Builder createContainerModuleLoaderBuilder(Set<Path> externalLibs) throws Exception {

        ScriptCompilerPluginSpec groovy2CompilerPluginSpec = buildGroovy2CompilerPluginSpec(externalLibs);
        ScriptCompilerPluginSpec byteCodeCompilerPluginSpec = buildByteCodeCompilerPluginSpec(externalLibs);

        // create and start the builder with the plugin
        return new ContainerModuleLoader.Builder()
                .addPluginSpec(groovy2CompilerPluginSpec)
                .addPluginSpec(byteCodeCompilerPluginSpec);
    }

    public static ContainerModuleLoader createContainerModuleLoader(Set<Path> externalLibs) throws Exception {

        ScriptCompilerPluginSpec groovy2CompilerPluginSpec = buildGroovy2CompilerPluginSpec(externalLibs);
        ScriptCompilerPluginSpec byteCodeCompilerPluginSpec = buildByteCodeCompilerPluginSpec(externalLibs);

        // create and start the builder with the plugin
        return new ContainerModuleLoader.Builder()
                .addPluginSpec(groovy2CompilerPluginSpec)
                .addPluginSpec(byteCodeCompilerPluginSpec)
                .build();
    }

    /**
     * Pack compiled class into .jar
     *
     * @param compiledClassesPath - path to compiled classes
     * @param pathToJar - where to put a packet .jar file - path should exis
     * @param moduleId - will be taken to construct a file name for jar file
     * @throws IOException
     * @throws BuildException
     */
    public
    static void packToJar(Path compiledClassesPath, Path pathToJar, ModuleId moduleId) throws IOException, BuildException {

        // use Ant jar builder
        Jar jar = new Jar();
        jar.setDestFile(pathToJar.toAbsolutePath().toFile());
        jar.setBasedir(compiledClassesPath.toAbsolutePath().toFile());

        // write default moduleSpec.json
        addDefaultModuleSpec(compiledClassesPath, moduleId);

        jar.setProject(new Project());
        jar.execute();
    }

    /**
     * Add default moduleSpec.json into compiled classes directory
     *
     * @param path
     * @param moduleId
     * @throws IOException
     */
    private static void addDefaultModuleSpec(Path path, ModuleId moduleId) throws IOException {
        final String json = DEFAULT_MODULE_SPEC_SERIALIZER.serialize(getDefaultScriptModuleSpec(moduleId));

        if (StringUtils.isBlank(json)) {
            return;
        }

        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(
                Paths.get(path.toAbsolutePath().toString(),
                        GsonScriptModuleSpecSerializer.DEFAULT_MODULE_SPEC_FILE_NAME).toAbsolutePath()
                        .toFile()
        )
        );

        bufferedWriter.write(json);
        bufferedWriter.flush();
        bufferedWriter.close();
    }

    protected static ScriptModuleSpec getDefaultScriptModuleSpec(ModuleId moduleId) {
        return new ScriptModuleSpec.Builder(moduleId)
                .addCompilerPluginId(BytecodeLoadingPlugin.PLUGIN_ID) // in this case we should not compile a content of jar file
                .build();
    }

    /**
     * ByteCode CompilerPluginSpec
     *
     * @param runTimeResourcesJar - set of paths to runtime .jar lib
     * @return
     */
    public static ScriptCompilerPluginSpec buildByteCodeCompilerPluginSpec(Set<Path> runTimeResourcesJar) {

        ScriptCompilerPluginSpec.Builder builder = new ScriptCompilerPluginSpec.Builder(BytecodeLoadingPlugin.PLUGIN_ID)

                .addRuntimeResource(ExampleResourceLocator.getGroovyRuntime())
                .addRuntimeResource(ExampleResourceLocator.getGroovyPluginLocation())
                .addRuntimeResource(getCoberturaJar(ContainerUtils.class.getClassLoader()))

                .addRuntimeResource(getByteCodeLoadingPluginPath())
                .withPluginClassName(BytecodeLoadingPlugin.class.getName());

        // in version higher 0.2.6 of Nicobar should be added some useful methods, but now needs to iterate
        // add run time .jar libs
        if (!runTimeResourcesJar.isEmpty()) {
            for (Path path : runTimeResourcesJar) {
                builder.addRuntimeResource(path.toAbsolutePath());
            }
        }

        return builder.build();
    }

    /**
     * Groovy2 CompilerPluginSpec
     *
     * @param runTimeResourcesJar - set of paths to runtime .jar lib
     * @return
     */
    public static ScriptCompilerPluginSpec buildGroovy2CompilerPluginSpec(Set<Path> runTimeResourcesJar) {
        // Groovy2CompilerPlugin

        // create the groovy plugin spec. this plugin specified a new module and classloader called "Groovy2Runtime"
        // which contains the groovy-all-n.n.n.jar
        ScriptCompilerPluginSpec.Builder builder = new ScriptCompilerPluginSpec.Builder(Groovy2Compiler.GROOVY2_COMPILER_ID)
                .addRuntimeResource(ExampleResourceLocator.getGroovyRuntime())
                .addRuntimeResource(ExampleResourceLocator.getGroovyPluginLocation())
                .addRuntimeResource(getByteCodeLoadingPluginPath())

        // hack to make the gradle build work. still doesn't seem to properly instrument the code
        // should probably add a classloader dependency on the system classloader instead
                .addRuntimeResource(getCoberturaJar(ContainerUtils.class.getClassLoader()))
                .withPluginClassName(Groovy2CompilerPlugin.class.getName());

        // in version higher 0.2.6 of Nicobar should be added some useful methods, but now needs to iterate
        // add run time .jar libs
        if (!runTimeResourcesJar.isEmpty()) {
            for (Path path : runTimeResourcesJar) {
                builder.addRuntimeResource(path.toAbsolutePath());
            }
        }

        return builder.build();
    }

    private static Path getByteCodeLoadingPluginPath() {
        String resourceName = ClassPathUtils.classNameToResourceName("com.netflix.nicobar.core.plugin.BytecodeLoadingPlugin");
        Path path = ClassPathUtils.findRootPathForResource(resourceName, ContainerUtils.class.getClassLoader());
        if (path == null) {
            throw new IllegalStateException("coudln't find BytecodeLoadingPlugin plugin jar in the classpath.");
        }
        return path;
    }

    private static Path getCoberturaJar(ClassLoader classLoader) {
        return ClassPathUtils.findRootPathForResource("net/sourceforge/cobertura/coveragedata/HasBeenInstrumented.class", classLoader);
    }
}
