package com.reversemind.nicobar;

import com.netflix.nicobar.core.archive.ModuleId;
import org.apache.commons.lang3.StringUtils;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Jar;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 *
 */
public class BuildJarModule {

    public static final String DEFAULT_MODULE_VERSION = "v0_1-SNAPSHOT";

    private ModuleId moduleId;
    private String compiledClassesPath;
    private String jsonDescriptor;
    private boolean isIncludeLastDirectoryAsPackageName;

    public BuildJarModule(ModuleId moduleId, String compiledClassesPath, boolean isIncludeLastDirectoryAsPackageName) {
        if (moduleId == null) {
            throw new IllegalArgumentException("ModuleId name can not be null or empty.");
        }

        if (moduleId.getName() == null || moduleId.getName().equals("")) {
            throw new IllegalArgumentException("Module name can not be null or empty.");
        }

        this.moduleId = moduleId;
        this.compiledClassesPath = compiledClassesPath;
        this.isIncludeLastDirectoryAsPackageName = isIncludeLastDirectoryAsPackageName;
        // TODO validate version according to Nicobar
    }

    public BuildJarModule(String name, String version, String compiledClassesPath, boolean isIncludeLastDirectoryAsPackageName) {
        if (name == null || name.equals("")) {
            throw new IllegalArgumentException("Module name can not be null or empty.");
        }
        this.moduleId = ModuleId.create(name, version);
        this.compiledClassesPath = compiledClassesPath;
        this.isIncludeLastDirectoryAsPackageName = isIncludeLastDirectoryAsPackageName;
        // TODO validate version according to Nicobar
    }

    public void toJar(Path targetDirectory) throws BuildException {
        String targetName = targetDirectory.toAbsolutePath().toString() + File.separator + createModuleNameForJarFile(this.moduleId.getName(), this.moduleId.getVersion()) + ".jar";

        Jar jar = new Jar();
        jar.setDestFile(new File(targetName));
        jar.setBasedir(new File(Paths.get(this.compiledClassesPath).toAbsolutePath().toString()));
        jar.setProject(new Project());

        jar.execute();
    }

    public static String createModuleNameForJarFile(String name, String version){
        if (name == null || name.equals("")) {
            throw new IllegalArgumentException("Module name can not be null or empty.");
        }
        return  StringUtils.isNotBlank(version) ? name + "_" + version: name;
    }
}