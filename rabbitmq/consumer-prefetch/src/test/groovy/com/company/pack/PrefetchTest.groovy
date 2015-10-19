package com.company.pack

import groovy.util.logging.Slf4j
import spock.lang.Specification

/**
 *
 */
@Slf4j
class PrefetchTest extends Specification {

    // https://github.com/spring-projects/spring-integration-samples

    def 'send and receive a message'() {
        setup:
        log.info "setup:"

        when:
        log.info "when:"

        then:
        log.info "then:"
    }
}
