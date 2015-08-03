package com.reversemind.nicobar.container

import com.netflix.nicobar.core.archive.ModuleId
import groovy.util.logging.Slf4j
import spock.lang.Specification

import java.nio.file.Path
import java.nio.file.Paths


/**
 *
 */
@Slf4j
class ContainerTest extends Specification {

    def 'container was not initialized'() {
        setup:
        log.info "setup:"

        when:
        log.info "when:"

        Container container = Container.getInstance();

        then:
        log.info "then:"
        thrown IllegalStateException
    }

    def 'build and init container'() {
        setup:
        log.info "setup:"

        final String BASE_PATH = "src/test/resources/stage2/modules";

        Path srcPath = Paths.get(BASE_PATH, "src").toAbsolutePath();
        Path classesPath = Paths.get(BASE_PATH, "classes").toAbsolutePath();
        Path libPath = Paths.get(BASE_PATH, "libs").toAbsolutePath();

        Set<Path> runtimeJars = new HashSet<>();
        runtimeJars.add(Paths.get("src/test/resources/libs/spock-core-0.7-groovy-2.0.jar").toAbsolutePath())


        when:
        log.info "when:"

        new Container.Builder(srcPath, classesPath, libPath)
                .setRuntimeJarLibs(runtimeJars)
                .build()

        Container container = Container.getInstance();

        ModuleId moduleId = ModuleId.create("moduleName", "moduleVersion")

        container.addModule(moduleId, false)
        container.executeScript(moduleId, "com.company.script")

        then:
        log.info "then:"
    }

    def 'auto rebuild scripts and reload'(){
        setup:
        log.info "setup:"

        final String BASE_PATH = "src/test/resources/base-path/modules";

        Path srcPath = Paths.get(BASE_PATH, "src").toAbsolutePath();
        Path classesPath = Paths.get(BASE_PATH, "classes").toAbsolutePath();
        Path libPath = Paths.get(BASE_PATH, "libs").toAbsolutePath();

        Set<Path> runtimeJars = new HashSet<>();
        runtimeJars.add(Paths.get("src/test/resources/libs/spock-core-0.7-groovy-2.0.jar").toAbsolutePath())


        when:
        log.info "when:"

        new Container.Builder(srcPath, classesPath, libPath)
                .setRuntimeJarLibs(runtimeJars)
                .build()

        Container container = Container.getInstance();

        ModuleId moduleId = ModuleId.create("moduleName", "moduleVersion")

        container.addModule(moduleId, true)


        10.times(){
            container.executeScript(moduleId, "com.company.script")
            Thread.sleep(2000);
        }

        then:
        log.info "then:"
    }
}
