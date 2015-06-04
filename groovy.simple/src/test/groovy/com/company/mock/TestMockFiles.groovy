package com.company.mock

import com.company.FileUtils
import groovy.mock.interceptor.MockFor
import spock.lang.Specification

/**
 *
 */
class TestMockFiles extends Specification {


    def TEMP_SCRIPT_DIRECTORY = System.getProperty("java.io.tmpdir")

    def "getGroovyFilesInDirectories"() {
        setup:
        def subDirectory = "subDirectory"
        def groovyFileName01 = "groovy.script.001.groovy"
        def groovyFileName02 = "groovy.script.002.groovy"

        def mockFile = new MockFor(File)

        mockFile.demand.eachFile { file ->
            [new File(TEMP_SCRIPT_DIRECTORY + File.separator + subDirectory + File.separator + groovyFileName01),
             new File(TEMP_SCRIPT_DIRECTORY + File.separator + subDirectory + File.separator + groovyFileName02)].each { item ->
                file(item)
            }
        }

        def mockFileProxy = mockFile.proxyInstance(TEMP_SCRIPT_DIRECTORY + File.separator + subDirectory)

        String[] directories = [TEMP_SCRIPT_DIRECTORY + File.separator + subDirectory]

        mockFileProxy.eachFile { file ->
            println "$file"
        }

        when:
        println "GO"

        List<File> groovyFiles = FileUtils.getGroovyFilesInDirectories(directories, false)


        then:
        println "files:" + groovyFiles
    }

}
