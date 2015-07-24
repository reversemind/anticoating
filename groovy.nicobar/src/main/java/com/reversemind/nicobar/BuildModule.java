package com.reversemind.nicobar;

import com.netflix.nicobar.core.archive.GsonScriptModuleSpecSerializer;
import com.netflix.nicobar.core.archive.ModuleId;
import com.netflix.nicobar.core.archive.ScriptModuleSpec;
import com.netflix.nicobar.core.archive.ScriptModuleSpecSerializer;
import com.netflix.nicobar.core.plugin.BytecodeLoadingPlugin;
import org.apache.commons.lang3.StringUtils;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Jar;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 *
 */
public class BuildModule {

    /*

        Structure

        level at src - is a base path

        BASE_PATH/
                    src/main/script
                            /com/company/packagename

                    build
                         /classes - compiled classes
                         /libs - packed jar of module
                         /modules - ready to tun modules

     */
    private static final String BUILD_NAME = "build";
    private static final String CLASSES_SUBPATH = BUILD_NAME + File.separator + "classes";
    private static final String LIBS_SUBPATH = BUILD_NAME + File.separator + "libs";
    private static final String MODULES_SUBPATH = BUILD_NAME + File.separator + "modules";
    private static final String SRC_MAIN_SCRIPT_SUBPATH = "src" + File.separator + "main" + File.separator + "script";

    private static final String[] SUB_PATHS = {BUILD_NAME, CLASSES_SUBPATH, LIBS_SUBPATH, MODULES_SUBPATH};


    public static final String DEFAULT_MODULE_VERSION = "v0_1-SNAPSHOT";

    private final static ScriptModuleSpecSerializer DEFAULT_MODULE_SPEC_SERIALIZER = new GsonScriptModuleSpecSerializer();

    private ModuleId moduleId;
    private String compiledClassesPath;
    private String jsonDescriptor;

    public BuildModule(ModuleId moduleId, String compiledClassesPath) {
        if (moduleId == null) {
            throw new IllegalArgumentException("ModuleId name can not be null or empty.");
        }

        if (moduleId.getName() == null || moduleId.getName().equals("")) {
            throw new IllegalArgumentException("Module name can not be null or empty.");
        }

        this.moduleId = moduleId;
        this.compiledClassesPath = compiledClassesPath;
    }

    public BuildModule(String name, String version, String compiledClassesPath) {
        if (name == null || name.equals("")) {
            throw new IllegalArgumentException("Module name can not be null or empty.");
        }
        this.moduleId = ModuleId.create(name, version);
        this.compiledClassesPath = compiledClassesPath;
    }

    /**
     * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
     * <p/>
     * NEED MAKE moduleSpec - INCLUDE info about a
     * <p/>
     * 'Cause need to specify a compilerPlugins=[bytecode]
     *
     * @param targetDirectory
     * @throws BuildException
     */
    public void toJar(Path targetDirectory) throws IOException, BuildException {
        String targetName = targetDirectory.toAbsolutePath().toString() + File.separator + createModuleNameForJarFile(this.moduleId.getName(), this.moduleId.getVersion()) + ".jar";

        Jar jar = new Jar();
        jar.setDestFile(new File(targetName));
        jar.setBasedir(new File(Paths.get(this.compiledClassesPath).toAbsolutePath().toString()));

        // write default moduleSpec.json
        writeToCompiledClassesDefaultModuleSpec(this.compiledClassesPath);

        jar.setProject(new Project());
        jar.execute();
    }

    public static String createModuleNameForJarFile(String name, String version) {
        if (name == null || name.equals("")) {
            throw new IllegalArgumentException("Module name can not be null or empty.");
        }
        return StringUtils.isNotBlank(version) ? name + "_" + version : name;
    }

    public ScriptModuleSpec getDefaultScriptModuleSpec() {
        return new ScriptModuleSpec.Builder(this.moduleId)
                .addCompilerPluginId(BytecodeLoadingPlugin.PLUGIN_ID) // in this case we should not compile a content of jar file
                .build();
    }

    private void writeToCompiledClassesDefaultModuleSpec(String path) throws IOException {
        final String json = DEFAULT_MODULE_SPEC_SERIALIZER.serialize(getDefaultScriptModuleSpec());
        if (StringUtils.isBlank(json)) {
            return;
        }

        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(
                Paths.get(path,
                        GsonScriptModuleSpecSerializer.DEFAULT_MODULE_SPEC_FILE_NAME).toAbsolutePath()
                        .toFile()
        )
        );
        bufferedWriter.write(json);
        bufferedWriter.flush();
        bufferedWriter.close();
    }

    // TODO need tests
    public static boolean validateExistenceOfSubPath(Path basePath, String... subPaths) {
        if (basePath == null) {
            throw new IllegalArgumentException("Base path could not be an empty");
        }
        if (subPaths == null) {
            return true;
        }
        return Paths.get(basePath.toAbsolutePath().toString(), subPaths).toFile().exists();
    }

    public static boolean validateAndCreateModulePaths(Path basePath) throws IOException {
        if (basePath == null) {
            throw new IllegalArgumentException("Base path could not be an empty");
        }
        for (String subPath : SUB_PATHS) {
            if (!validateExistenceOfSubPath(basePath, subPath)) {
                if (!Paths.get(basePath.toAbsolutePath().toString(), subPath).toFile().mkdirs()) {
                    throw new IOException("Unable create to create sub paths:" + subPath);
                }
            }
        }
        return true;
    }
}