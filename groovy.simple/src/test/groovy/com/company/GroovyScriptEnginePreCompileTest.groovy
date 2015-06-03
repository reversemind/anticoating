package com.company

import groovy.util.logging.Slf4j
import org.apache.commons.io.DirectoryWalker
import org.apache.commons.io.filefilter.FileFilterUtils
import org.apache.commons.lang3.StringUtils
import org.codehaus.groovy.control.CompilerConfiguration
import spock.lang.Specification

import java.nio.file.FileVisitOption
import java.nio.file.Files
import java.nio.file.Paths

import static groovy.io.FileType.FILES

/**
 * ??? GroovyCompiledScript
 */
@Slf4j
class GroovyScriptEnginePreCompileTest extends Specification {

    def fileNameList = []
    def tempScriptDirectory = System.getProperty("java.io.tmpdir")

    public static List<File> getFileListByDirectory(String directoryPath) {
        if (StringUtils.isBlank(directoryPath)) {
            return new ArrayList<File>(0);
        }

        List<File> _list = new ArrayList<File>()

        return _list;
    }

    def "get all files for array of directories"() {
        setup:

        log.info "temp path:" + System.getProperty("java.io.tmpdir");
        tempScriptDirectory = System.getProperty("java.io.tmpdir") + File.separator + new Date().getTime()

        log.info "Temp script path:" + tempScriptDirectory
        log.info "Create tmp directory:" + new File(tempScriptDirectory).mkdirs()


        def subDirectory = "subDirectory"
        def tempSubScriptDirectory = tempScriptDirectory + File.separator + subDirectory
        log.info "Create subDirectory directory:" + new File(tempSubScriptDirectory).mkdirs()

        // Important order of directories
        String[] directories = new String[2];
        directories[0] = tempScriptDirectory
        directories[1] = tempSubScriptDirectory


        def fileNames = ["file1.groovy", "file2.groovy", "file2.groovy"];

        // add three files
        if (new File(tempScriptDirectory + File.separator + fileNames[0]).createNewFile()) {
            new File(tempScriptDirectory + File.separator + fileNames[0]) << "println \"from script: \${this.getClass().getCanonicalName()} time:\" + new Date() + \" / \" + new Date().getTime() + \" ms \""
        }

        if (new File(tempScriptDirectory + File.separator + fileNames[1]).createNewFile()) {
            new File(tempScriptDirectory + File.separator + fileNames[1]) << "println \"from script: \${this.getClass().getCanonicalName()} time:\" + new Date() + \" / \" + new Date().getTime() + \" ms \""
        }

        if (new File(tempSubScriptDirectory + File.separator + fileNames[2]).createNewFile()) {
            new File(tempSubScriptDirectory + File.separator + fileNames[2]) << "println \"SUB DIRECTORY with the same name - from script: \${this.getClass().getCanonicalName()} time:\" + new Date() + \" / \" + new Date().getTime() + \" ms \""
        }

        // 'Cause GroovyScriptEngine find only the first presents of file name
        def fileList = getGroovyFilesInDirectories(directories, false)
        log.info "fileList" + fileList

        Set<String> pureFileNames = new TreeSet<String>(getFileName(fileList));
        log.info "pureFileNames:" + pureFileNames


        GroovyScriptEngine groovyScriptEngine = new GroovyScriptEngine(directories);
        for (String scriptName : pureFileNames) {
            Class clazz = groovyScriptEngine.loadScriptByName(scriptName);
        }

        when:
        println ""

        for (String scriptName : pureFileNames) {
            groovyScriptEngine.run(scriptName, "");
        }

        println "files:" + FileUtils.getFileListInDirectoryByExtension(directories[0], false, ".groovy")

        println "files:" + new GroovyFilesWalker(directories[0], false).getFiles()

        then:
        println ""

    }

    def String zeroNumber(int signCount, int number) {
        def _tmp = "" + number
        (signCount - _tmp.length()).times {
            _tmp = "0" + _tmp
        }
        return _tmp
    }

    def List<String> getFileName(List<File> fileList) {
        if (!fileList) {
            return new ArrayList<String>(0);
        }
        List<String> _list = new ArrayList<String>(fileList.size())
        fileList.each { file ->
            _list.add file.name
        }
        return _list
    }

    def List<File> getGroovyFilesInDirectories(String[] directories, boolean isRecursively) {
        return getFilesByExtensionInDirectory(directories, '.groovy', isRecursively);
    }

    def List<File> getFilesByExtensionInDirectory(String[] directories, String extension, boolean isRecursively) {
        if (!directories) {
            return new ArrayList<File>(0);
        }

        List<File> _list = new ArrayList<File>();
        directories.each { directory ->
            getFilesByExtensionInDirectory(directory, extension, isRecursively).each { file ->
                _list.add(file)
            }
        }

        return _list;
    }

    def List<File> getFilesByExtensionInDirectory(String directoryPath, String extension, boolean isRecursively) {
        List<File> fileList = new ArrayList<File>();

        if (isRecursively) {
            new File(directoryPath).eachFileRecurse(FILES) { file ->
                if (file.name.endsWith(extension)) {
                    fileList.add file
                }
            }
        } else {
            new File(directoryPath).eachFile(FILES) { file ->
                if (file.name.endsWith(extension)) {
                    fileList.add file
                }
            }
        }
        return fileList;
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

        def fileList = getFilesByExtensionInDirectory(tempScriptDirectory, '.groovy', false)
        log.info "files:" + fileList

        log.info "names:" + getFileName(fileList)

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
        CompilerConfiguration compilerConfiguration = groovyScriptEngine.getConfig()
        compilerConfiguration.setRecompileGroovySource(true);
        compilerConfiguration.setVerbose(true)
        groovyScriptEngine.setConfig(compilerConfiguration);


        double firstRunTime = 0.0
        double secondRunTime = 0.0
        double thirdRunTime = 0.0

        N.times { number ->
            log.info "" + fileNameList.get(number) + " modified:" + groovyScriptEngine.getLastModified(fileNameList.get(number) as String)
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

        compilerConfiguration = groovyScriptEngine.getConfig()
        File file = compilerConfiguration.getTargetDirectory()
        log.info "Target directory:" + file?.absolutePath
        log.info "" + compilerConfiguration.getVerbose()


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