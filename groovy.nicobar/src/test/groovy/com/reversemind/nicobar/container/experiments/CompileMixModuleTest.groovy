package com.reversemind.nicobar.container.experiments

import com.netflix.nicobar.core.archive.ModuleId
import com.reversemind.nicobar.container.Container
import com.reversemind.nicobar.container.TestHelper
import com.reversemind.nicobar.container.utils.ContainerUtils
import groovy.util.logging.Slf4j
import spock.lang.Ignore
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 */
@Slf4j
class CompileMixModuleTest extends Specification {

    def 'build jar from src'() {
        setup:

        log.info "setup:"

        final String BASE_PATH = "src/test/resources/base-path-build-module-src-plus-jar";

        Path srcPath = Paths.get(BASE_PATH, "src").toAbsolutePath();
        Path classesPath = Paths.get(BASE_PATH, "classes").toAbsolutePath();
        Path libPath = Paths.get(BASE_PATH, "libs").toAbsolutePath();

        TestHelper.delete(classesPath)
        TestHelper.delete(libPath)

        try{
            Files.createDirectories(classesPath)
            Files.createDirectories(libPath)
        }catch (Exception ignore){}


        Set<Path> runtimeJars = new HashSet<>();
        runtimeJars.add(Paths.get("src/test/resources/libs/spock-core-0.7-groovy-2.0.jar").toAbsolutePath())
        // because of AntBuilder inside MixBytecodeLoader
        runtimeJars.add(Paths.get("src/test/resources/libs/ant-1.9.6.jar").toAbsolutePath())


        when:
        log.info "when:"

        TestHelper.resetContainer()

        new Container.Builder(srcPath, classesPath, libPath)
                .setModuleLoader(
                ContainerUtils.createMixContainerModuleLoaderBuilder(runtimeJars)
                        .withCompilationRootDir(classesPath)
                        .build()
        )
                .setRuntimeJarLibs(runtimeJars)
                .build()

        Container container = Container.getInstance();


        ModuleId moduleId = ModuleId.create("moduleName", "moduleVersion")
        container.addModule(moduleId, false)

        then:
        log.info "then:"
        container.destroy()
    }

    @Ignore
    def 'Groovy script engine'() {
        setup:
        log.info "OUT"

        final String BASE_PATH = "src/test/resources/base-path-build-module-src-plus-jar";

        Path srcPath = Paths.get(BASE_PATH, "src", "moduleName.moduleVersion").toAbsolutePath();

        Path jarFilePath = Paths.get(BASE_PATH, "src", "moduleName.moduleVersion", "external.v1.jar").toAbsolutePath();
        String[] roots = [srcPath.toAbsolutePath().toString(), jarFilePath.toAbsolutePath().toString()];

//        String[] roots = [srcPath.toAbsolutePath().toString()];
        GroovyScriptEngine groovyScriptEngine = new GroovyScriptEngine(roots);

//        def scriptName = "com.company.script.groovy";
//        def scriptName = "script.groovy";
        def scriptName = "com/company/script.groovy";
        Class script = groovyScriptEngine.loadScriptByName(scriptName);

        println "script:" + script
    }

}
