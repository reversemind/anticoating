package com.test.wathcer

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
        new WatchDirectory(directory, true).processEvents();
    }
}
