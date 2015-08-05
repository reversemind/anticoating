package com.reversemind.nicobar.container.experiments

import com.netflix.nicobar.core.archive.ModuleId
import com.reversemind.nicobar.container.Container
import com.reversemind.nicobar.container.utils.ContainerUtils
import groovy.util.logging.Slf4j
import org.apache.commons.lang3.StringUtils
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

/**
 *
 */
@Slf4j
class BuildJarFromSrc extends Specification {

    def 'build jar from src'() {
        setup:
        log.info "setup:"

        final String BASE_PATH = "src/test/resources/build-jar-from-src";

        Path srcPath = Paths.get(BASE_PATH, "src").toAbsolutePath();
        Path classesPath = Paths.get(BASE_PATH, "classes").toAbsolutePath();
        Path libPath = Paths.get(BASE_PATH, "libs").toAbsolutePath();

        Set<Path> runtimeJars = new HashSet<>();
        runtimeJars.add(Paths.get("src/test/resources/libs/spock-core-0.7-groovy-2.0.jar").toAbsolutePath())


        when:
        log.info "when:"


        new Container.Builder(srcPath, classesPath, libPath)
                .setRuntimeJarLibs(runtimeJars)
                .build()

        Container container = Container.getInstance();

        ModuleId moduleId = ModuleId.create("external", "v1")

        container.addModule(moduleId, false)


        ContainerUtils.packToJar(ContainerUtils.getModulePath(classesPath, moduleId),
                Paths.get(libPath.toAbsolutePath().toString(), "external.v1.jar")
                , moduleId)


        then:
        log.info "then:"
    }

    def 'build jar from src another'() {
        setup:
        log.info "setup:"

        final String BASE_PATH = "src/test/resources/build-jar-from-src-another";

        Path srcPath = Paths.get(BASE_PATH, "src").toAbsolutePath();
        Path classesPath = Paths.get(BASE_PATH, "classes").toAbsolutePath();
        Path libPath = Paths.get(BASE_PATH, "libs").toAbsolutePath();

        Set<Path> runtimeJars = new HashSet<>();
        runtimeJars.add(Paths.get("src/test/resources/libs/spock-core-0.7-groovy-2.0.jar").toAbsolutePath())


        when:
        log.info "when:"


        new Container.Builder(srcPath, classesPath, libPath)
                .setRuntimeJarLibs(runtimeJars)
                .build()

        Container container = Container.getInstance();

        ModuleId moduleId = ModuleId.create("other", "v02")

        container.addModule(moduleId, false)


        ContainerUtils.packToJar(ContainerUtils.getModulePath(classesPath, moduleId),
                Paths.get(libPath.toAbsolutePath().toString(), "other.v02.jar")
                , moduleId)


        then:
        log.info "then:"
    }

    def 'unjar into path'() {
        setup:
        log.info "setup:"

        Path jarPath = Paths.get("src/test/resources/libs/spock-core-0.7-groovy-2.0.jar").toAbsolutePath();
        Path targetPath = Paths.get("src/test/resources/unzip/spock-core").toAbsolutePath()

        when:
        log.info "when:"

        ContainerUtils.unJar(jarPath.toFile(), targetPath.toFile(), true)

        then:
        log.info "then:"
    }

    def 'copy class file to target'(){
        setup:
        log.info "setup:"

        Path sourceClassPath = Paths.get("src/test/resources/base-path-build-module-src-plus-jar/src/moduleName.moduleVersion/com/other/package10/OtherScript.class").toAbsolutePath();
        Path targetPath = Paths.get("src/test/resources/base-path-build-module-src-plus-jar/classes/moduleName.moduleVersion").toAbsolutePath()


        def _basePath = "/opt/dev/github/reversemind/anticoating/groovy.nicobar/src/test/resources/base-path-build-module-src-plus-jar/src/moduleName.moduleVersion"
        def _classSubPath = "com/other/package10/OtherScript.class"


        Path basePath = Paths.get(_basePath);
        Path classPath = basePath.resolve(_classSubPath);

        println "classPath" + classPath

        println "dirs:" + Paths.get(_classSubPath).getNameCount()
        println "packages to dir:" + Paths.get(_classSubPath).subpath(0,Paths.get(_classSubPath).getNameCount()-1);


        Path packagesPath = Paths.get(_classSubPath).subpath(0,Paths.get(_classSubPath).getNameCount()-1)
        Path targetPackagesPath = Paths.get(targetPath.toString(), packagesPath.toString());


        when:
        log.info "when:"

        Path _targetPackagesPath = getTargetPackagesPath(targetPath, "com.other.package10.OtherScript")

        if(_targetPackagesPath != null){
            try {
                Files.createDirectories(targetPackagesPath);
            } catch (Exception ignore) {
                ignore.printStackTrace();
            }
        }

        Path targetClassPath = Files.copy(sourceClassPath, _targetPackagesPath.resolve("OtherScript.class"), StandardCopyOption.REPLACE_EXISTING)

        then:
        log.info "then:"

        _targetPackagesPath.toFile().exists()
        targetClassPath.toFile().exists()

    }

    def 'copy class file to target for the one hop'(){
        setup:
        log.info "setup:"

        Path sourceClassPath = Paths.get("src/test/resources/base-path-build-module-src-plus-jar/src/moduleName.moduleVersion/com/other/package10/OtherScript.class").toAbsolutePath();
        Path targetPath = Paths.get("src/test/resources/base-path-build-module-src-plus-jar/classes/moduleName.moduleVersion").toAbsolutePath()
        String canonicalClassName = "com.other.package10.OtherScript"
        String className = "OtherScript"

        when:
        log.info "when:"

        Path targetClassPath = copyClassRelativelyAt(targetPath, sourceClassPath, canonicalClassName, className);

        then:
        log.info "then:"
        targetClassPath.toFile().exists()

    }

    /**
     *
     * @param targetPath
     * @param sourceClassPath
     * @param canonicalClassName
     * @return - if null - file was not copy
     */
    private static Path copyClassRelativelyAt(
                                                final Path targetPath,
                                                final Path sourceClassPath,
                                                final String canonicalClassName,
                                                final String className) {
        Path _targetPackagesPath = getTargetPackagesPath(targetPath, canonicalClassName)

        if (_targetPackagesPath == null) {
            return null;
        }

        Files.createDirectories(_targetPackagesPath);
        return Files.copy(sourceClassPath, _targetPackagesPath.resolve(className + ".class"), StandardCopyOption.REPLACE_EXISTING)
    }

    /**
     *
     * @param target - target path
     * @param canonicalClassName - com.other.package10.OtherScript
     * @return - if null - no need to create sub paths
     */
    private static Path getTargetPackagesPath(final Path targetPath, final String canonicalClassName){
        if(targetPath == null){
            return null;
        }

        if(StringUtils.isBlank(canonicalClassName)){
            return null;
        }

        String _classSubPath = canonicalClassName.replaceAll("\\.", File.separator) + ".class";
        int nameCount = Paths.get(_classSubPath).getNameCount()

        if(nameCount == 0){
            return null;
        }

        Paths.get(_classSubPath).subpath(0,Paths.get(_classSubPath).getNameCount()-1)

        Path packagesPath = Paths.get(_classSubPath).subpath(0,Paths.get(_classSubPath).getNameCount()-1)
        Path targetPackagesPath = Paths.get(targetPath.toString(), packagesPath.toString());

        return targetPackagesPath;
    }
}
