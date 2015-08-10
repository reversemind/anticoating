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

        TestHelper.mixCompilationOfModule()
        Path BASE_PATH = Paths.get("src/test/resources/base-path-build-module-src-plus-jar").toAbsolutePath();
        Path modulesAtPath = Paths.get(BASE_PATH.toString(), "src").toAbsolutePath()

        when:
        log.info "when:"

        Set<ModuleId> set = ContainerUtils.getModuleIdListAtPath(modulesAtPath)

        then:
        log.info "then:"

        set.size() == 2
    }

    def 'recently modify date'() {
        setup:
        log.info "setup:"

        TestHelper.mixCompilationOfModule()

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

}
