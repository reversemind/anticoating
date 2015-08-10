package com.reversemind.nicobar.container.utils
import com.netflix.nicobar.core.archive.ModuleId
import com.reversemind.nicobar.container.TestHelper
import groovy.util.logging.Slf4j
import spock.lang.Specification

import java.nio.file.Path
import java.nio.file.Paths

/**
 *
 */
@Slf4j
class ContainerUtilsTest extends Specification{

    def 'get module id list for path'(){
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

}
