package com.reversemind.nicobar.container.utils

import com.netflix.nicobar.core.archive.ModuleId
import com.reversemind.nicobar.container.Container
import com.reversemind.nicobar.container.TestHelper
import groovy.util.logging.Slf4j
import spock.lang.Specification

import java.nio.file.*
import java.text.SimpleDateFormat

/**
 *
 */
@Slf4j
class ContainerUtilsTest extends Specification {

    def 'get module id list for path'() {
        setup:
        log.info "setup:"

        Path BASE_PATH = Paths.get("src/test/resources/base-path-build-module-src-plus-jar").toAbsolutePath();
        Path modulesAtPath = Paths.get(BASE_PATH.toString(), "src").toAbsolutePath()

        when:
        log.info "when:"

        Set<ModuleId> set = ContainerUtils.getModuleIdListAtPath(modulesAtPath)

        then:
        log.info "then:"

        set.size() == 1
        new ArrayList<ModuleId>(set).get(0).toString().equals("moduleName.moduleVersion")
    }

    def 'recently modify date'() {
        setup:
        log.info "setup:"

        prepareFiles()

        Path BASE_PATH = Paths.get("src/test/resources/base-path-build-module-src-plus-jar").toAbsolutePath();
        Path srcAtPath = Paths.get(BASE_PATH.toString(), "src", "moduleName.moduleVersion").toAbsolutePath()
        Path classesAtPath = Paths.get(BASE_PATH.toString(), "classes", "moduleName.moduleVersion").toAbsolutePath()

        when:
        log.info "when:"

        Long dateSrc = ContainerUtils.recentlyModifyDate(srcAtPath)
        Long dateClasses = ContainerUtils.recentlyModifyDate(classesAtPath)

        log.info "src date:" + dateSrc + " formatted:" + (dateSrc != null ? simpleDateFormat.format(dateSrc) : null);
        log.info "class date:" + dateClasses + " formatted:" + (dateClasses != null ? simpleDateFormat.format(dateClasses) : null);

        Long nonExistDate = ContainerUtils.recentlyModifyDate(Paths.get(BASE_PATH.toString(), "classes", UUID.randomUUID().toString()).toAbsolutePath())

        then:
        log.info "then:"
        dateSrc != null
        dateClasses != null
        nonExistDate == null
        dateClasses >= dateSrc

    }

    static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")

    def
    static void prepareFiles(){
        ModuleId moduleId = ModuleId.create("moduleName", "moduleVersion")

        final String BASE_PATH = "src/test/resources/base-path-build-module-src-plus-jar"
        TestHelper.delete(Paths.get(BASE_PATH, "classes", moduleId.toString()))
        TestHelper.prepareJarAndClass(BASE_PATH)

        Path srcPath = Paths.get(BASE_PATH, "src").toAbsolutePath();
        Path classesPath = Paths.get(BASE_PATH, "classes").toAbsolutePath();
        Path libPath = Paths.get(BASE_PATH, "libs").toAbsolutePath();

        Set<Path> runtimeJars = new HashSet<>();
        // because of AntBuilder inside MixBytecodeLoader
        runtimeJars.add(Paths.get("src/test/resources/libs/ant-1.9.6.jar").toAbsolutePath())
        // 'cause it's run under spock tests
        runtimeJars.add(Paths.get("src/test/resources/libs/spock-core-0.7-groovy-2.0.jar").toAbsolutePath())

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


        1.times(){
            container.addModule(moduleId, false)
            Thread.sleep(2000);
            log.info "\n\nmake it again";
        }

        container.executeScript(moduleId, "com.company.script")
        container.destroy();
    }

}
