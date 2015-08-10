package com.reversemind.nicobar.container

import com.netflix.nicobar.core.archive.ModuleId
import com.netflix.nicobar.core.archive.PathScriptArchive
import com.netflix.nicobar.core.archive.ScriptArchive
import com.netflix.nicobar.core.archive.ScriptModuleSpec
import com.netflix.nicobar.core.module.BaseScriptModuleListener
import com.netflix.nicobar.core.module.ScriptModule
import com.netflix.nicobar.core.plugin.BytecodeLoadingPlugin
import com.netflix.nicobar.groovy2.plugin.Groovy2CompilerPlugin
import com.reversemind.nicobar.container.utils.ContainerUtils
import groovy.util.logging.Slf4j
import spock.lang.Specification

import java.nio.file.Path
import java.nio.file.Paths

/**
 *
 */
@Slf4j
class ContainerModuleLoaderTest extends Specification {

    def 'reuse compiled modules and compile from sources again'() {
        setup:
        log.info ""

        final String BASE_PATH = "src/test/resources/base-path/modules";

        Path MODULES_CLASSES = Paths.get(BASE_PATH, "classes").toAbsolutePath()
        Path MODULES_SRC = Paths.get(BASE_PATH, "src").toAbsolutePath()
        Path MODULES_LIBS = Paths.get(BASE_PATH, "libs").toAbsolutePath()

        ModuleId moduleId = ModuleId.create("moduleName", "moduleVersion")

        Set<Path> runtimeJars = new HashSet<>();
        runtimeJars.add(Paths.get("src/test/resources/libs/spock-core-0.7-groovy-2.0.jar").toAbsolutePath())

        TestHelper.resetContainer()

        new Container.Builder(MODULES_SRC, MODULES_CLASSES, MODULES_LIBS)
                .setRuntimeJarLibs(runtimeJars)
                .build()

        ContainerModuleLoader moduleLoader = Container.getInstance()
                .getModuleLoader()

        // #1 build from source to classes directory
        ScriptArchive scriptArchive = getScriptArchiveAtPath(MODULES_SRC, moduleId);
        moduleLoader.updateScriptArchives(new LinkedHashSet<ScriptArchive>(Arrays.asList(scriptArchive)));
        log.info "#1"

        Thread.sleep(2000);

        // #2 then load from compiled classes
        // isModuleExistAtPath(MODULES_CLASSES, moduleId)
        scriptArchive = getScriptArchiveAtPath(MODULES_CLASSES, moduleId);
        moduleLoader.updateScriptArchives(new LinkedHashSet<ScriptArchive>(Arrays.asList(scriptArchive)));
        log.info "#2"

        ContainerUtils
                .packToJar(
                    getModulePath(MODULES_CLASSES, moduleId).toAbsolutePath(),
                    Paths.get(MODULES_LIBS.toAbsolutePath().toString(), moduleId.toString() + ".jar").toAbsolutePath(),
                    moduleId
        );

        when:
        log.info ""

        then:
        log.info ""
    }

    private static boolean isModuleExistAtPath(Path basePath, ModuleId moduleId) {
        return getModulePath(basePath, moduleId).toFile().exists()
    }

    /**
     * basePath/
     *          moduleName.moduleVersion/
     *                                  src/com/company/package
     * @param basePath
     * @param moduleId
     * @return
     */
    private static ScriptArchive getScriptArchiveAtPath(Path basePath, ModuleId moduleId) {
        ScriptModuleSpec moduleSpec = new ScriptModuleSpec.Builder(moduleId)
                .addCompilerPluginId(BytecodeLoadingPlugin.PLUGIN_ID)
                .addCompilerPluginId(Groovy2CompilerPlugin.PLUGIN_ID)
                .build()

        ScriptArchive scriptArchive = new PathScriptArchive.Builder(getModulePath(basePath, moduleId).toAbsolutePath())
                .setRecurseRoot(true)
                .setModuleSpec(moduleSpec)
                .build();

        return scriptArchive;
    }

    private static Path getModulePath(Path basePath, ModuleId moduleId) {
        if (!basePath.isAbsolute()) {
            throw new IllegalArgumentException("Base path should be absolute")
        }
        return basePath.resolve(moduleId.toString())
    }

}
