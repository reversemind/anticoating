package com.company

import groovy.util.logging.Slf4j
import spock.lang.Specification

import static groovy.io.FileType.FILES

/**
 *
 */
@Slf4j
class GroovyScriptEnginePreCompileTest extends Specification {

    def fileNameList = []
    def tempScriptDirectory = System.getProperty("java.io.tmpdir")

    def String zeroNumber(int signCount, int number) {
        def _tmp = "" + number
        (signCount - _tmp.length()).times {
            _tmp = "0" + _tmp
        }
        return _tmp
    }

    def "call script with the same name that lies deeper"() {
        setup:

        fileNameList = []

        log.info "temp path:" + System.getProperty("java.io.tmpdir");
        tempScriptDirectory = System.getProperty("java.io.tmpdir") + File.separator + "" + new Date().getTime()

        log.info "Temp script path:" + tempScriptDirectory
        log.info "Create tmp directory:" + new File(tempScriptDirectory).mkdirs()


        def subDirectory = "subDirectory"
        log.info "Create subDirectory directory:" + new File(tempScriptDirectory + File.separator + "subDirectory").mkdirs()


        def scriptName = "script.${zeroNumber(9, 1)}.groovy"
        def scriptName2 = "script2.${zeroNumber(9, 1)}.groovy"
        def scriptFileName = tempScriptDirectory + File.separator + scriptName
        def scriptFileNameInSubDirectory = tempScriptDirectory + File.separator + subDirectory + File.separator + scriptName2


        if (new File(scriptFileName).createNewFile()) {
            fileNameList << scriptName
            new File(scriptFileName) << "println \"From script: \${this.getClass().getCanonicalName()} time:\" + new Date() + \" / \" + new Date().getTime() + \" ms \""
        }

        if (new File(scriptFileNameInSubDirectory).createNewFile()) {
            fileNameList << scriptName
            new File(scriptFileNameInSubDirectory) << "println \"ACTUALLY it's other script - from script: \${this.getClass().getCanonicalName()} time:\" + new Date() + \" / \" + new Date().getTime() + \" ms \""
        }

        new File(tempScriptDirectory).eachFileRecurse(FILES) { file ->
            if (file.name.endsWith('.groovy')) {
                log.info "name ${file.name} path:${file.absolutePath}"
            }
        }

        when:
        GroovyScriptEngine groovyScriptEngine = new GroovyScriptEngine(tempScriptDirectory);
        groovyScriptEngine.run(scriptName2, "")


        then: "'Cause GroovyScriptEngine look inside ONLY one directory - without any recursivity"
        thrown(groovy.util.ResourceException)
    }


    def "generate a bunch of scripts"() {
        setup:

        // number of scripts
        def N = 10

        log.info "temp path:" + System.getProperty("java.io.tmpdir");
        tempScriptDirectory = System.getProperty("java.io.tmpdir") + File.separator + "" + new Date().getTime()

        log.info "Temp script path:" + tempScriptDirectory
        log.info "Create tmp directory:" + new File(tempScriptDirectory).mkdirs()

        N.times { number ->
            def scriptName = "script.${zeroNumber(9, number)}.groovy"
            def scriptFileName = tempScriptDirectory + File.separator + scriptName

            if (new File(scriptFileName).createNewFile()) {
                fileNameList << scriptName
                new File(scriptFileName) << "println \"From script: \${this.getClass().getCanonicalName()} time:\" + new Date() + \" / \" + new Date().getTime() + \" ms \""
            }
        }



        when:

        GroovyScriptEngine groovyScriptEngine = new GroovyScriptEngine(tempScriptDirectory);

        double firstRunTime = 0.0
        double secondRunTime = 0.0
        double thirdRunTime = 0.0

        N.times { number ->
//            log.info "" + fileNameList.get(number) + " modified:" + groovyScriptEngine.getLastModified(fileNameList.get(number) as String)
            Class clazz = groovyScriptEngine.loadScriptByName(fileNameList.get(number) as String);
            log.info "" + clazz
        }

        // run two times each script and estimate and compare a compilation time
        3.times { runStep ->
            log.info "step to run ${runStep}"
            long beginTime = System.currentTimeMillis()
            N.times { number ->
                log.info "run script:" + groovyScriptEngine.run(fileNameList.get(number) as String, "")
            }
            long endTime = System.currentTimeMillis()
            log.info "Computation time:" + (endTime - beginTime) + " ms  " + (endTime - beginTime) / N * 1.0 + " ms spend to compile & run script"

            if (runStep == 0) {
                firstRunTime = (endTime - beginTime) / N * 1.0
            }

            if (runStep == 1) {
                secondRunTime = (endTime - beginTime) / N * 1.0
            }

            if (runStep == 2) {
                thirdRunTime = (endTime - beginTime) / N * 1.0
            }
        }


        then:
        log.info "DONE"
        log.info "Speed up in ${firstRunTime / secondRunTime} times |  ${secondRunTime / thirdRunTime} times"
    }


    def "extracts all script names"() {
        setup:

        // number of scripts
        def N = 20

        log.info "temp path:" + System.getProperty("java.io.tmpdir");
        tempScriptDirectory = System.getProperty("java.io.tmpdir") + File.separator + "" + new Date().getTime()

        log.info "Temp script path:" + tempScriptDirectory
        log.info "Create tmp directory:" + new File(tempScriptDirectory).mkdirs()

        N.times { number ->
            def scriptName = "script.${zeroNumber(9, number)}.groovy"
            def scriptFileName = tempScriptDirectory + File.separator + scriptName

            if (new File(scriptFileName).createNewFile()) {
                fileNameList << scriptName
                new File(scriptFileName) << "println \"From script: \${this.getClass().getCanonicalName()} time:\" + new Date() + \" / \" + new Date().getTime() + \" ms \""
            }
        }



        when:

        GroovyScriptEngine groovyScriptEngine = new GroovyScriptEngine(tempScriptDirectory);
        groovyScriptEngine.run(fileNameList.get(0) as String, "")

        then: ""
        log.info "DONE"

    }

}