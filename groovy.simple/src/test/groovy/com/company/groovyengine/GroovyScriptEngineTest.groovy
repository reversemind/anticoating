package com.company.groovyengine

import groovy.util.logging.Slf4j
import spock.lang.Specification

/**
 *
 */
@Slf4j
class GroovyScriptEngineTest extends Specification{


    def TEMP_SCRIPT_DIRECTORY = System.getProperty("java.io.tmpdir")

    def SUB_DIRECTORY = "subDirectory"
    def PATH_TO_SCRIPTS = TEMP_SCRIPT_DIRECTORY + File.separator + SUB_DIRECTORY


    def 'setup'() {
        log.info "Create directories and files"
        new File(PATH_TO_SCRIPTS).mkdirs()
        new File(PATH_TO_SCRIPTS + File.separator + SUB_DIRECTORY).mkdirs()
    }

    def 'cleanup'() {
        log.info "Cleanup all directories & files"

        new File(PATH_TO_SCRIPTS + File.separator + SUB_DIRECTORY).deleteDir()
        new File(PATH_TO_SCRIPTS).deleteDir()
    }

    def 'auto reload updated scripts'(){
        setup: 'SETUP'

        def scriptName1 = "script_with_on_line_modification.groovy"
        def script1 = """
            println "Date:" + new Date()
        """

        if (new File(PATH_TO_SCRIPTS + File.separator + scriptName1).createNewFile())
            new File(PATH_TO_SCRIPTS + File.separator + scriptName1) << script1

        String[] roots = [PATH_TO_SCRIPTS]

        ////////////////////////////////////////////////////////
        //
        when: 'WHEN'

        long beginTime = System.currentTimeMillis()
        GroovyScriptEngine groovyScriptEngine = new GroovyScriptEngine(roots);

        long endTime = System.currentTimeMillis()
        log.info "GroovyScripts loaded for ${(endTime - beginTime)} ms"


        groovyScriptEngine.loadScriptByName(scriptName1)
        groovyScriptEngine.createScript(scriptName1, new Binding()).run()
        Thread.sleep(1000)

        log.info("Adding a new string to script")
        new File(PATH_TO_SCRIPTS + File.separator + scriptName1) << "println \"Added string\""
        Thread.sleep(1000)

        groovyScriptEngine.loadScriptByName(scriptName1)
        groovyScriptEngine.createScript(scriptName1, new Binding()).run()

        ////////////////////////////////////////////////////////////
        //
        then: 'THEN'

        ////////////////////////////////////////////////////////////
        //
        cleanup: 'LOCAL CLEANUP'
        log.info "Delete scripts: ${scriptName1}"
    }
}
