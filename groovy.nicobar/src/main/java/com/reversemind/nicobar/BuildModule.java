package com.reversemind.nicobar;

import com.netflix.nicobar.core.archive.*;
import com.netflix.nicobar.core.compile.ScriptCompilationException;
import com.netflix.nicobar.core.plugin.BytecodeLoadingPlugin;
import com.netflix.nicobar.groovy2.internal.compile.Groovy2CompilerHelper;
import com.netflix.nicobar.groovy2.plugin.Groovy2CompilerPlugin;
import org.apache.commons.lang3.StringUtils;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Jar;
import org.codehaus.groovy.tools.GroovyClass;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;

/**
 *
 */
public class BuildModule {

    /*
        Structure of project is close to idea of maven & gradle structure project

        BASE_PATH/
                   modules/ - ready to tun modules

                   moduleId.name_moduleId.version/ - or just moduleId.name

                                                    src/main/groovy
                                                            /com/company/packagename

                                                    build
                                                         /classes - compiled classes
                                                         /libs - packed jar of module

        one more time

         BASE_PATH/
                  modules/moduleName_moduleVersion.jar
                  moduleName_moduleVersion/
                                            src/main/groovy
                                                    /com/company/packagename
                                            build
                                                 /classes
                                                         /com/company/packagename/*.class
                                                 /libs/moduleName_moduleVersion.jar

     */
    private static final String BUILD_NAME = "build";
    private static final String CLASSES_SUBPATH = BUILD_NAME + File.separator + "classes";
    private static final String LIBS_SUBPATH = BUILD_NAME + File.separator + "libs";
    private static final String MODULES_SUBPATH = "modules";
    private static final String SRC_MAIN_SCRIPT_SUBPATH = "src" + File.separator + "main" + File.separator + "groovy";

    private static final String[] SUB_PATHS = {BUILD_NAME, CLASSES_SUBPATH, LIBS_SUBPATH};

    public static final String DEFAULT_MODULE_VERSION = "v0_1-SNAPSHOT";

    private final static ScriptModuleSpecSerializer DEFAULT_MODULE_SPEC_SERIALIZER = new GsonScriptModuleSpecSerializer();

    private ModuleId moduleId;
    private Path basePath;
    private String jsonDescriptor;

    public BuildModule(ModuleId moduleId, Path basePath) {
        if (moduleId == null) {
            throw new IllegalArgumentException("ModuleId name can not be null or empty.");
        }

        if (moduleId.getName() == null || StringUtils.isBlank(moduleId.getName())) {
            throw new IllegalArgumentException("Module name can not be null or empty.");
        }

        if (basePath == null) {
            throw new IllegalArgumentException("Base path could not be an empty");
        }

        this.moduleId = moduleId;
        this.basePath = basePath;
    }

    public BuildModule(String name, String version, Path basePath) {
        this(ModuleId.create(name, version), basePath);
    }

    /**
     * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
     * <p/>
     * NEED MAKE moduleSpec - INCLUDE info about a
     * <p/>
     * 'Cause need to specify a compilerPlugins=[bytecode]
     *
     * @throws BuildException
     */
    public void packToJar() throws IOException, BuildException {

        // moduleName_moduleVersion/build/libs/moduleName_moduleVersion.jar
        String moduleName_moduleVersion = createModuleNameForJarFile(this.moduleId.getName(), this.moduleId.getVersion());
        File targetJarName = Paths.get(
                basePath.toAbsolutePath().toString(),
                moduleName_moduleVersion,
                LIBS_SUBPATH,
                moduleName_moduleVersion + ".jar")
                .toAbsolutePath()
                .toFile();

        Jar jar = new Jar();
        jar.setDestFile(targetJarName);

        // moduleName_moduleVersion/build/classes
        File compiledClasses = Paths.get(
                basePath.toAbsolutePath().toString(),
                moduleName_moduleVersion,
                CLASSES_SUBPATH)
                .toAbsolutePath()
                .toFile();
        jar.setBasedir(compiledClasses);

        // write default moduleSpec.json
        writeToCompiledClassesDefaultModuleSpec(compiledClasses.getPath());

        jar.setProject(new Project());
        jar.execute();
    }

    /**
     * in this case we should not compile a content of jar file
     *
     * @return
     */
    protected ScriptModuleSpec getDefaultScriptModuleSpec() {
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
    public boolean validateExistenceOfSubPath(Path basePath, String... subPaths) {
        if (basePath == null) {
            throw new IllegalArgumentException("Base path could not be an empty");
        }
        if (subPaths == null) {
            return true;
        }
        return Paths.get(basePath.toAbsolutePath().toString(), subPaths).toFile().exists();
    }

    public boolean validateAndCreateModulePaths() throws IOException {
        for (String subPath : SUB_PATHS) {
            if (!validateExistenceOfSubPath(basePath, createModuleNameForJarFile(this.moduleId), subPath)) {
                if (!Paths.get(basePath.toAbsolutePath().toString(), createModuleNameForJarFile(this.moduleId), subPath).toFile().mkdirs()) {
                    throw new IOException("Unable create to create sub paths:" + subPath);
                }
            }
        }
        if (!validateExistenceOfSubPath(basePath, MODULES_SUBPATH)) {
            if (!Paths.get(basePath.toAbsolutePath().toString(), MODULES_SUBPATH).toFile().mkdirs()) {
                throw new IOException("Unable create to create sub paths:" + MODULES_SUBPATH);
            }
        }
        return true;
    }

    public boolean validateModulePaths() throws IOException {
        for (String subPath : SUB_PATHS) {
            if (!validateExistenceOfSubPath(basePath, createModuleNameForJarFile(this.moduleId), subPath)) {
                return false;
            }
        }
        if (!validateExistenceOfSubPath(basePath, MODULES_SUBPATH)) {
            return false;
        }
        return true;
    }


    /**
     * Main method
     *
     * @throws IOException
     */
    public synchronized void build() throws IOException, ScriptCompilationException {

        // BASE_PATH/moduleName_moduleVersion/src/main/groovy
        Path groovyScriptRootPath = Paths.get(
                this.basePath.toAbsolutePath().toString(),
                createModuleNameForJarFile(this.moduleId),
                SRC_MAIN_SCRIPT_SUBPATH)
                .toAbsolutePath();

        // two plugins need to compile groovy & use extra .jar from
        ScriptModuleSpec moduleSpec = new ScriptModuleSpec.Builder(this.moduleId)
                .addCompilerPluginId(BytecodeLoadingPlugin.PLUGIN_ID)
                .addCompilerPluginId(Groovy2CompilerPlugin.PLUGIN_ID)
                .build();

        PathScriptArchive scriptArchive = new PathScriptArchive.Builder(groovyScriptRootPath)
                .setRecurseRoot(true)
                .setModuleSpec(moduleSpec)
                .build();

        Set<GroovyClass> compiledClasses = new Groovy2CompilerHelper(
                        Paths.get(
                                this.basePath.toAbsolutePath().toString(),
                                createModuleNameForJarFile(this.moduleId),
                                CLASSES_SUBPATH
                        )
                        .toAbsolutePath()
                )
                .addScriptArchive(scriptArchive)
                .compile();

        for(GroovyClass groovyClass: compiledClasses){
            System.out.println("DEBUG INFO class:" + groovyClass.getName());
        }

        packToJar();
        copyToModuleDirectory();
    }

    private void copyToModuleDirectory() throws IOException {
        // copy to runnable dir - 'modules'
        Path sourceJarPath = Paths.get(
                this.basePath.toAbsolutePath().toString(),
                createModuleNameForJarFile(this.moduleId),
                LIBS_SUBPATH,
                createModuleNameForJarFile(this.moduleId) + ".jar")
                .toAbsolutePath();

        Path targetJarPath = getModuleJarPath();

        // TODO need extra checks !!! if module file exists
        Files.copy(sourceJarPath, targetJarPath, StandardCopyOption.REPLACE_EXISTING);
    }

    public Path getModuleJarPath(){
        return Paths.get(
                this.basePath.toAbsolutePath().toString(),
                MODULES_SUBPATH,
                createModuleNameForJarFile(this.moduleId) + ".jar")
                .toAbsolutePath();
    }

    /**
     * 'Cause some collision between some names
     *
     * @param name
     * @param version
     * @return
     */
    public String createModuleNameForJarFile(String name, String version) {
        if (name == null || name.equals("")) {
            throw new IllegalArgumentException("Module name can not be null or empty.");
        }
        return StringUtils.isNotBlank(version) ? name + "_" + version : name;
    }

    public String createModuleNameForJarFile(ModuleId moduleId) {
        if (moduleId == null) {
            throw new IllegalArgumentException("ModuleId can not be null or empty.");
        }

        if (moduleId.getName() == null || moduleId.getName().equals("")) {
            throw new IllegalArgumentException("Module name can not be null or empty.");
        }

        return StringUtils.isNotBlank(moduleId.getVersion()) ? moduleId.getName() + "_" + moduleId.getVersion() : moduleId.getName();
    }
}