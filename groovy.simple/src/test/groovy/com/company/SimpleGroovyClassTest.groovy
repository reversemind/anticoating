package com.company

import spock.lang.Specification

/**
 *
 */
class SimpleGroovyClassTest extends Specification {

    def "add-test"() {
        given: "a new SimpleGroovyClass class is created"
        def simpleGroovyClass = new SimpleGroovyClass();

        expect: "Adding two numbers to return the sum"
        simpleGroovyClass.add(3, 4) == 7
    }
}
