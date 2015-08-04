package com.reversemind.nicobar.container.experiments

import com.netflix.nicobar.core.archive.ModuleId
import com.reversemind.nicobar.container.Container
import com.reversemind.nicobar.container.utils.ContainerUtils
import groovy.util.logging.Slf4j
import spock.lang.Specification

import java.nio.file.Path
import java.nio.file.Paths

/**
 *
 */
@Slf4j
class BuildJarFromSrc extends Specification {

    def 'build jar from src'() {
        setup:
        log.info "setup:"

        final String BASE_PATH = "src/test/resources/build-jar-from-src";

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

        ModuleId moduleId = ModuleId.create("external", "v1")

        container.addModule(moduleId, false)


        ContainerUtils.packToJar( ContainerUtils.getModulePath(classesPath, moduleId),
                Paths.get(libPath.toAbsolutePath().toString(), "external.v1.jar")
                , moduleId)


        then:
        log.info "then:"
    }
}
