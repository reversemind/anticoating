package com.company.mock

import com.company.FileUtils
import groovy.mock.interceptor.MockFor
import groovy.util.logging.Slf4j
import spock.lang.Specification

import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Paths

/**
 *
 */
@Slf4j
class TestMockFiles extends Specification {


    def TEMP_SCRIPT_DIRECTORY = System.getProperty("java.io.tmpdir")

    def SUB_DIRECTORY = "subDirectory"
    def PATH_TO_SCRIPTS = TEMP_SCRIPT_DIRECTORY + File.separator + SUB_DIRECTORY

    def groovyFileName01 = "groovy.script.001.groovy"
    def groovyFileName02 = "groovy.script.002.groovy"

    def 'setup'() {
        log.info "Create directories and files"
        new File(TEMP_SCRIPT_DIRECTORY + File.separator + SUB_DIRECTORY).mkdirs()
        new File(TEMP_SCRIPT_DIRECTORY + File.separator + SUB_DIRECTORY + File.separator + SUB_DIRECTORY).mkdirs()

        new File(TEMP_SCRIPT_DIRECTORY + File.separator + SUB_DIRECTORY + File.separator + groovyFileName01).createNewFile()
        new File(TEMP_SCRIPT_DIRECTORY + File.separator + SUB_DIRECTORY + File.separator + groovyFileName02).createNewFile()

        new File(TEMP_SCRIPT_DIRECTORY + File.separator + SUB_DIRECTORY + File.separator + SUB_DIRECTORY + File.separator + groovyFileName01).createNewFile()
    }

    def 'cleanup'() {
        log.info "Cleanup all files"

        new File(TEMP_SCRIPT_DIRECTORY + File.separator + SUB_DIRECTORY + File.separator + SUB_DIRECTORY + File.separator + groovyFileName01).delete()

        new File(TEMP_SCRIPT_DIRECTORY + File.separator + SUB_DIRECTORY + File.separator + groovyFileName01).delete()
        new File(TEMP_SCRIPT_DIRECTORY + File.separator + SUB_DIRECTORY + File.separator + groovyFileName02).delete()

        new File(TEMP_SCRIPT_DIRECTORY + File.separator + SUB_DIRECTORY).deleteDir()
        new File(TEMP_SCRIPT_DIRECTORY + File.separator + SUB_DIRECTORY + File.separator + SUB_DIRECTORY).deleteDir()
    }

    def 'getGroovyFilesInDirectories'() {
        setup:

        given:
        String[] directoriesTheOne = [TEMP_SCRIPT_DIRECTORY + File.separator + SUB_DIRECTORY]
        String[] directoriesMultiple = [TEMP_SCRIPT_DIRECTORY + File.separator + SUB_DIRECTORY,
                                        TEMP_SCRIPT_DIRECTORY + File.separator + SUB_DIRECTORY + File.separator + SUB_DIRECTORY]
        String[] directoriesMultipleReverseOrder = [TEMP_SCRIPT_DIRECTORY + File.separator + SUB_DIRECTORY + File.separator + SUB_DIRECTORY,
                                                    TEMP_SCRIPT_DIRECTORY + File.separator + SUB_DIRECTORY]

        String[] nonExistDirectories = ['./104a7011-0eec-47a5-a918-b97dd692f13f/80d74f3e-205e-4866-8ce9-ebdaf09be0e3', './0d911330-8a74-4ea8-964e-ee04827d5ab6/0d911330-8a74-4ea8-964e-ee04827d5ab6']

        when:
        List<File> groovyFilesOneLevel = FileUtils.getGroovyFilesInDirectories(directoriesTheOne, false)
        List<File> groovyFilesRecursively = FileUtils.getGroovyFilesInDirectories(directoriesTheOne, true)
        List<File> groovyFilesMultiple = FileUtils.getGroovyFilesInDirectories(directoriesMultiple, false)
        List<File> groovyFilesMultipleReverseOrder = FileUtils.getGroovyFilesInDirectories(directoriesMultipleReverseOrder, false)

        List<File> filesForEmptyMask1 = FileUtils.getFilesByExtensionInDirectories(directoriesMultipleReverseOrder, null, false);
        List<File> filesForEmptyMask2 = FileUtils.getFilesByExtensionInDirectories(directoriesMultipleReverseOrder, "", false);

        List<File> filesForEmptyNonExistDirectories = FileUtils.getFilesByExtensionInDirectories(nonExistDirectories, "", false);

        then:
        log.info "Extra checks"
        2 == groovyFilesOneLevel.size()
        groovyFilesOneLevel.get(0).getAbsolutePath() == "" + TEMP_SCRIPT_DIRECTORY + File.separator + SUB_DIRECTORY + File.separator + groovyFileName01
        groovyFilesOneLevel.get(1).getAbsolutePath() == "" + TEMP_SCRIPT_DIRECTORY + File.separator + SUB_DIRECTORY + File.separator + groovyFileName02

        3 == groovyFilesRecursively.size()

        3 == groovyFilesMultiple.size()
        groovyFilesMultiple.get(0).getAbsolutePath() == "" + TEMP_SCRIPT_DIRECTORY + File.separator + SUB_DIRECTORY + File.separator + groovyFileName01
        groovyFilesMultiple.get(1).getAbsolutePath() == "" + TEMP_SCRIPT_DIRECTORY + File.separator + SUB_DIRECTORY + File.separator + groovyFileName02
        groovyFilesMultiple.get(2).getAbsolutePath() == "" + TEMP_SCRIPT_DIRECTORY + File.separator + SUB_DIRECTORY + File.separator + SUB_DIRECTORY + File.separator + groovyFileName01

        3 == groovyFilesMultipleReverseOrder.size()
        groovyFilesMultipleReverseOrder.get(0).getAbsolutePath() == "" + TEMP_SCRIPT_DIRECTORY + File.separator + SUB_DIRECTORY + File.separator + SUB_DIRECTORY + File.separator + groovyFileName01
        groovyFilesMultipleReverseOrder.get(1).getAbsolutePath() == "" + TEMP_SCRIPT_DIRECTORY + File.separator + SUB_DIRECTORY + File.separator + groovyFileName01
        groovyFilesMultipleReverseOrder.get(2).getAbsolutePath() == "" + TEMP_SCRIPT_DIRECTORY + File.separator + SUB_DIRECTORY + File.separator + groovyFileName02

        3 == filesForEmptyMask1.size()
        3 == filesForEmptyMask2.size()

        0 == filesForEmptyNonExistDirectories.size()
    }

    def 'relativize path'() {
        setup:
        String localPath = Paths.get(".").toAbsolutePath().toString();


        when:
        log.info ""

        then:
        'special.file'              == FileUtils.relativizePath('/something/', '/something/special.file')
        'something/special.file'    == FileUtils.relativizePath(localPath, localPath + '/something/special.file')
        'something/special.file'    == FileUtils.relativizePath('.', './something/special.file')
        'something/special.file'    != FileUtils.relativizePath('/tmp', './something/special.file')


        when:
        FileUtils.relativizePath('', './something/special.file')
        then:
        thrown(IllegalArgumentException.class);

        when:
        FileUtils.relativizePath('/something', '')
        then:
        thrown(IllegalArgumentException.class);

        when:
        FileUtils.relativizePath(null, './something/special.file')
        then:
        thrown(NullPointerException.class)

        when:
        FileUtils.relativizePath('/something', null)
        then:
        thrown(NullPointerException.class)
    }

    def 'script path cases'(){
        setup:
        log.info "Go"

        log.info "path:" + new File(new File("/opt/p90"), "appl_scripts")
        log.info "path:" + new File(new File("/opt/p90"), "/appl_scripts")
        log.info "path:" + new File(new File("/opt/p90"), "./appl_scripts")


        log.info "\n\nUse Absolute Path\n"

        FileSystem fileSystem = FileSystems.getDefault()
        log.info "path:" + fileSystem.getPath("appl_scripts").toAbsolutePath()
        log.info "path:" + fileSystem.getPath("/appl_scripts").toAbsolutePath()
        log.info "path:" + fileSystem.getPath("./appl_scripts").toAbsolutePath()
        log.info "path:" + fileSystem.getPath("../appl_scripts").toAbsolutePath()

        log.info "isAbsolute:" + fileSystem.getPath("appl_scripts").isAbsolute()
        log.info "isAbsolute:" + fileSystem.getPath("/appl_scripts").isAbsolute()
        log.info "isAbsolute:" + fileSystem.getPath("./appl_scripts").isAbsolute()
        log.info "isAbsolute:" + fileSystem.getPath("../appl_scripts").isAbsolute()
        log.info "isAbsolute:" + fileSystem.getPath("../appl_scripts").isAbsolute()

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
