package com.company.mock

import com.company.FileUtils
import groovy.mock.interceptor.MockFor
import groovy.util.logging.Slf4j
import spock.lang.Specification

/**
 *
 */
@Slf4j
class TestMockFiles extends Specification {

    def TEMP_SCRIPT_DIRECTORY = System.getProperty("java.io.tmpdir")

    def subDirectory = "subDirectory"
    def groovyFileName01 = "groovy.script.001.groovy"
    def groovyFileName02 = "groovy.script.002.groovy"

    def 'setup'() {
        new File(TEMP_SCRIPT_DIRECTORY + File.separator + subDirectory).mkdirs()
        new File(TEMP_SCRIPT_DIRECTORY + File.separator + subDirectory + File.separator + subDirectory).mkdirs()

        new File(TEMP_SCRIPT_DIRECTORY + File.separator + subDirectory + File.separator + groovyFileName01).createNewFile()
        new File(TEMP_SCRIPT_DIRECTORY + File.separator + subDirectory + File.separator + groovyFileName02).createNewFile()

        new File(TEMP_SCRIPT_DIRECTORY + File.separator + subDirectory + File.separator + subDirectory + File.separator + groovyFileName01).createNewFile()
    }

    def 'cleanup'() {
        println "Cleanup all files"

        new File(TEMP_SCRIPT_DIRECTORY + File.separator + subDirectory + File.separator + subDirectory + File.separator + groovyFileName01).delete()

        new File(TEMP_SCRIPT_DIRECTORY + File.separator + subDirectory + File.separator + groovyFileName01).delete()
        new File(TEMP_SCRIPT_DIRECTORY + File.separator + subDirectory + File.separator + groovyFileName02).delete()

        new File(TEMP_SCRIPT_DIRECTORY + File.separator + subDirectory).deleteDir()
        new File(TEMP_SCRIPT_DIRECTORY + File.separator + subDirectory + File.separator + subDirectory).deleteDir()
    }

    def "getGroovyFilesInDirectories"() {
        setup:

        given:
        String[] directoriesTheOne = [TEMP_SCRIPT_DIRECTORY + File.separator + subDirectory]
        String[] directoriesMultiple = [TEMP_SCRIPT_DIRECTORY + File.separator + subDirectory,
                                      TEMP_SCRIPT_DIRECTORY + File.separator + subDirectory + File.separator + subDirectory]
        String[] directoriesMultipleReverseOrder = [TEMP_SCRIPT_DIRECTORY + File.separator + subDirectory + File.separator + subDirectory,
                                        TEMP_SCRIPT_DIRECTORY + File.separator + subDirectory]

        when:
        List<File> groovyFilesOneLevel = FileUtils.getGroovyFilesInDirectories(directoriesTheOne, false)
        List<File> groovyFilesRecursively = FileUtils.getGroovyFilesInDirectories(directoriesTheOne, true)
        List<File> groovyFilesMultiple = FileUtils.getGroovyFilesInDirectories(directoriesMultiple, false)
        List<File> groovyFilesMultipleReverseOrder = FileUtils.getGroovyFilesInDirectories(directoriesMultipleReverseOrder, false)

        then:
        2 == groovyFilesOneLevel.size()
        groovyFilesOneLevel.get(0).getAbsolutePath() == "" + TEMP_SCRIPT_DIRECTORY + File.separator + subDirectory + File.separator + groovyFileName01
        groovyFilesOneLevel.get(1).getAbsolutePath() == "" + TEMP_SCRIPT_DIRECTORY + File.separator + subDirectory + File.separator + groovyFileName02

        3 == groovyFilesRecursively.size()

        3 == groovyFilesMultiple.size()
        groovyFilesMultiple.get(0).getAbsolutePath() == "" + TEMP_SCRIPT_DIRECTORY + File.separator + subDirectory + File.separator + groovyFileName01
        groovyFilesMultiple.get(1).getAbsolutePath() == "" + TEMP_SCRIPT_DIRECTORY + File.separator + subDirectory + File.separator + groovyFileName02
        groovyFilesMultiple.get(2).getAbsolutePath() == "" + TEMP_SCRIPT_DIRECTORY + File.separator + subDirectory + File.separator + subDirectory + File.separator + groovyFileName01

        3 == groovyFilesMultipleReverseOrder.size()
        groovyFilesMultipleReverseOrder.get(0).getAbsolutePath() == "" + TEMP_SCRIPT_DIRECTORY + File.separator + subDirectory + File.separator + subDirectory + File.separator + groovyFileName01
        groovyFilesMultipleReverseOrder.get(1).getAbsolutePath() == "" + TEMP_SCRIPT_DIRECTORY + File.separator + subDirectory + File.separator + groovyFileName01
        groovyFilesMultipleReverseOrder.get(2).getAbsolutePath() == "" + TEMP_SCRIPT_DIRECTORY + File.separator + subDirectory + File.separator + groovyFileName02

    }

    def "getGroovyFilesInDirectoriesMock"() {
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
