package com.company.meta

import org.codehaus.groovy.jsr223.GroovyScriptEngineFactory

import java.nio.file.Path

/**
 *
 */
class Application {

    static void main(String... args) {
        URL[] roots = [this.class.getResource("/script/")]

        GroovyScriptEngine scriptEngine = new GroovyScriptEngine(roots)

        Component component = new Component(version: "0.1", payload: "BLAH_BLAH")

        Binding binding = new Binding();
        binding.setVariable("component",component)

//        scriptEngine.loadScriptByName("com/company/meta/script/ScriptOne.groovy")
        scriptEngine.run("com/company/meta/script/ScriptOne.groovy", binding)
    }

}
