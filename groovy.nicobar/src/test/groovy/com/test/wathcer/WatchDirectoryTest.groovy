package com.test.wathcer

import com.netflix.nicobar.core.archive.ModuleId
import com.reversemind.nicobar.IScriptContainerListener
import com.reversemind.nicobar.watcher.WatchDirectory
import groovy.util.logging.Slf4j
import spock.lang.Specification

import java.nio.file.Path
import java.nio.file.Paths

/**
 *
 */
@Slf4j
class WatchDirectoryTest extends Specification {

    def 'watch dir'() {
        setup:
        log.info "go"

        Path directory = Paths.get('src/test/resources/watchdirectory').toAbsolutePath();

        new WatchDirectory(ModuleId.create("moduleName", "moduleVersion"),
                new IScriptContainerListener() {
                    @Override
                    void changed(ModuleId moduleId) {
                        log.info "Just changed module:" + moduleId;
                    }
                },
                directory,
                true).processEvents();

    }
}
