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

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 *
 */
public class BuildModule {

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
    public void toJar(Path targetDirectory) throws BuildException {
        String targetName = targetDirectory.toAbsolutePath().toString() + File.separator + createModuleNameForJarFile(this.moduleId.getName(), this.moduleId.getVersion()) + ".jar";

        Jar jar = new Jar();
        jar.setDestFile(new File(targetName));
        jar.setBasedir(new File(Paths.get(this.compiledClassesPath).toAbsolutePath().toString()));
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
}