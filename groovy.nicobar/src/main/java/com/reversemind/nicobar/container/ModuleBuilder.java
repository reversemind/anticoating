package com.reversemind.nicobar.container;

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
public class ModuleBuilder {

    /*
        Structure of project is close to maven & gradle structure project

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

    public ModuleBuilder(ModuleId moduleId, Path basePath) {
        if (moduleId == null) {
            throw new IllegalArgumentException("ModuleId name can not be null or empty.");
        }

        if (basePath == null) {
            throw new IllegalArgumentException("Base path could not be an empty");
        }

        this.moduleId = moduleId;
        this.basePath = basePath;
    }

    public ModuleBuilder(String name, String version, Path basePath) {
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

        // use Ant jar builder
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

        /*
        // TODO

            Exception in thread "Thread-7" com.netflix.nicobar.core.compile.ScriptCompilationException: Exception during script compilation
                at com.netflix.nicobar.groovy2.internal.compile.Groovy2CompilerHelper.compile(Groovy2CompilerHelper.java:130)
                at com.reversemind.nicobar.container.ModuleBuilder.build(ModuleBuilder.java:236)
                at com.reversemind.nicobar.container.ModuleBuilder$build$1.call(Unknown Source)
                at com.reversemind.nicobar.container.ScriptContainer.reBuildModule(ScriptContainer.groovy:86)
                at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
                at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
                at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
                at java.lang.reflect.Method.invoke(Method.java:497)
                at org.codehaus.groovy.reflection.CachedMethod.invoke(CachedMethod.java:90)
                at groovy.lang.MetaMethod.doMethodInvoke(MetaMethod.java:324)
                at groovy.lang.MetaClassImpl.invokeMethod(MetaClassImpl.java:1206)
                at org.codehaus.groovy.runtime.ScriptBytecodeAdapter.invokeMethodOnCurrentN(ScriptBytecodeAdapter.java:80)
                at com.reversemind.nicobar.container.ScriptContainer.this$dist$invoke$1(ScriptContainer.groovy)
                at com.reversemind.nicobar.container.ScriptContainer$1.methodMissing(ScriptContainer.groovy)
                at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
                at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
                at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
                at java.lang.reflect.Method.invoke(Method.java:497)
                at org.codehaus.groovy.reflection.CachedMethod.invoke(CachedMethod.java:90)
                at groovy.lang.MetaClassImpl.invokeMissingMethod(MetaClassImpl.java:932)
                at groovy.lang.MetaClassImpl.invokePropertyOrMissing(MetaClassImpl.java:1255)
                at groovy.lang.MetaClassImpl.invokeMethod(MetaClassImpl.java:1208)
                at groovy.lang.MetaClassImpl.invokeMethod(MetaClassImpl.java:1015)
                at org.codehaus.groovy.runtime.callsite.PogoMetaClassSite.callCurrent(PogoMetaClassSite.java:66)
                at org.codehaus.groovy.runtime.callsite.AbstractCallSite.callCurrent(AbstractCallSite.java:141)
                at com.reversemind.nicobar.container.ScriptContainer$1.run(ScriptContainer.groovy:204)
            Caused by: org.codehaus.groovy.control.MultipleCompilationErrorsException: startup failed:
            file:/opt/dev/github/reversemind/anticoating/groovy.nicobar/src/test/resources/auto/moduleName_moduleVersion/src/main/groovy/com/company/.%23script.groovy: /opt/dev/github/reversemind/anticoating/groovy.nicobar/src/test/resources/auto/moduleName_moduleVersion/src/main/groovy/com/company/.#script.groovy (No such file or directory)

            1 error

                at org.codehaus.groovy.control.ErrorCollector.failIfErrors(ErrorCollector.java:309)
                at org.codehaus.groovy.control.ErrorCollector.addError(ErrorCollector.java:106)
                at org.codehaus.groovy.control.ErrorCollector.addFatalError(ErrorCollector.java:148)
                at org.codehaus.groovy.control.SourceUnit.parse(SourceUnit.java:242)
                at org.codehaus.groovy.control.CompilationUnit$1.call(CompilationUnit.java:164)
                at org.codehaus.groovy.control.CompilationUnit.applyToSourceUnits(CompilationUnit.java:923)
                at org.codehaus.groovy.control.CompilationUnit.doPhaseOperation(CompilationUnit.java:585)
                at org.codehaus.groovy.control.CompilationUnit.processPhaseOperations(CompilationUnit.java:561)
                at org.codehaus.groovy.control.CompilationUnit.compile(CompilationUnit.java:538)
                at com.netflix.nicobar.groovy2.internal.compile.Groovy2CompilerHelper.compile(Groovy2CompilerHelper.java:128)
                ... 25 more


         */

        Set<GroovyClass> compiledClasses = null;
        try {
            compiledClasses = new Groovy2CompilerHelper(
                    Paths.get(
                            this.basePath.toAbsolutePath().toString(),
                            createModuleNameForJarFile(this.moduleId),
                            CLASSES_SUBPATH
                    )
                            .toAbsolutePath()
            )
                    .addScriptArchive(scriptArchive)
                    .compile();

        } catch (ScriptCompilationException ex) {
            ex.printStackTrace();
            System.out.println("\n\n\n unable to compile \n\n\n\n");
        }

        if (compiledClasses != null) {
            for (GroovyClass groovyClass : compiledClasses) {
                System.out.println("DEBUG INFO class:" + groovyClass.getName());
            }

            packToJar();
            copyToModuleDirectory();
        }
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
        Files.copy(sourceJarPath, targetJarPath, StandardCopyOption.REPLACE_EXISTING);
    }

    public Path getModuleJarPath() {
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
    public static String createModuleNameForJarFile(final String name, final String version) {
        if (name == null || name.equals("")) {
            throw new IllegalArgumentException("Module name can not be null or empty.");
        }
        return StringUtils.isNotBlank(version) ? name + "_" + version : name;
    }

    public static String createModuleNameForJarFile(final ModuleId moduleId) {
        if (moduleId == null) {
            throw new IllegalArgumentException("ModuleId can not be null or empty.");
        }

        if (moduleId.getName() == null || moduleId.getName().equals("")) {
            throw new IllegalArgumentException("Module name can not be null or empty.");
        }

        return StringUtils.isNotBlank(moduleId.getVersion()) ? moduleId.getName() + "_" + moduleId.getVersion() : moduleId.getName();
    }

    /**
     * Get path to runnable module
     *
     * @return BASE_PATH/modules/moduleName_moduleVersion.jar
     */
    public Path getModuleJarFilePath() {
        return Paths.get(
                this.basePath.toAbsolutePath().toString(),
                MODULES_SUBPATH,
                createModuleNameForJarFile(this.moduleId) + ".jar"
        );
    }

    /**
     * Get path of module root directory
     *
     * @return - BASE_PATH/moduleName_moduleVersion
     */
    public Path getModulePath() {
        return Paths.get(
                this.basePath.toAbsolutePath().toString(),
                createModuleNameForJarFile(this.moduleId)
        );
    }

    /**
     * Get path of script sources
     *
     * @return - BASE_PATH/moduleName_moduleVersion/src/main
     */
    public Path getModuleSrcPath() {
        return Paths.get(
                this.basePath.toAbsolutePath().toString(),
                createModuleNameForJarFile(this.moduleId),
                SRC_MAIN_SCRIPT_SUBPATH
        );
    }
}