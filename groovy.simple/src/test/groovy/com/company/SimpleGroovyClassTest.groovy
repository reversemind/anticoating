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

    def "use GroovyScriptEngine"() {
        setup: "Prepare GroovyScriptEngine to run a script"

        def scriptName = "script.0001.groovy"

//        URL[] roots = [this.class.getResource("/scripts/${scriptName}")]
        URL[] roots = [this.class.getResource("/scripts/")]
        GroovyScriptEngine groovyScriptEngine = new GroovyScriptEngine(roots);

        when:
        groovyScriptEngine.run(scriptName, "");

        then: "Script is runnable"
    }
}
