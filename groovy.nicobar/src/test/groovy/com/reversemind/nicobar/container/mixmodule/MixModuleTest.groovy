package com.reversemind.nicobar.container.mixmodule

import com.netflix.nicobar.core.archive.ModuleId
import com.reversemind.nicobar.container.Container
import com.reversemind.nicobar.container.utils.ContainerUtils
import groovy.util.logging.Slf4j
import spock.lang.Specification

import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes

/**
 *
 */
@Slf4j
class MixModuleTest extends Specification {

    def 'mix compilation of module'(){
        setup:
        log.info "setup:"

        ModuleId moduleId = ModuleId.create("moduleName", "moduleVersion")

        final String BASE_PATH = "src/test/resources/base-path-build-module-src-plus-jar"
        delete(Paths.get(BASE_PATH, "classes", moduleId.toString()))

        prepareJarAndClass(BASE_PATH)

        Path srcPath = Paths.get(BASE_PATH, "src").toAbsolutePath();
        Path classesPath = Paths.get(BASE_PATH, "classes").toAbsolutePath();
        Path libPath = Paths.get(BASE_PATH, "libs").toAbsolutePath();

        Set<Path> runtimeJars = new HashSet<>();
        // because of AntBuilder inside MixBytecodeLoader
        runtimeJars.add(Paths.get("src/test/resources/libs/ant-1.9.6.jar").toAbsolutePath())
        // 'cause it's run under spock tests
        runtimeJars.add(Paths.get("src/test/resources/libs/spock-core-0.7-groovy-2.0.jar").toAbsolutePath())


        when:
        log.info "when"


        new Container.Builder(srcPath, classesPath, libPath)
                .setModuleLoader(
                ContainerUtils.createMixContainerModuleLoaderBuilder(runtimeJars)
                        .withCompilationRootDir(classesPath)
                        .build()
        )
                .setRuntimeJarLibs(runtimeJars)
                .build()

        Container container = Container.getInstance();


        1.times(){
            container.addMixModule(moduleId, false)
            Thread.sleep(2000);
            log.info "\n\nmake it again";
        }


        then:
        log.info "then"

        container.executeScript(moduleId, "com.company.script")
        container.destroy();

    }

    def 'load mixed module'(){
        setup:
        log.info "setup:"

        final String BASE_PATH = "src/test/resources/base-path-build-module-src-plus-jar"
        prepareJarAndClass(BASE_PATH)

        Path srcPath = Paths.get(BASE_PATH, "src").toAbsolutePath();
        Path classesPath = Paths.get(BASE_PATH, "classes").toAbsolutePath();
        Path libPath = Paths.get(BASE_PATH, "libs").toAbsolutePath();

        Set<Path> runtimeJars = new HashSet<>();
        // because of AntBuilder inside MixBytecodeLoader
        runtimeJars.add(Paths.get("src/test/resources/libs/ant-1.9.6.jar").toAbsolutePath())
        // 'cause it's run under spock tests
        runtimeJars.add(Paths.get("src/test/resources/libs/spock-core-0.7-groovy-2.0.jar").toAbsolutePath())


        when:
        log.info "when"

        new Container.Builder(srcPath, classesPath, libPath)
                .setModuleLoader(
                ContainerUtils.createMixContainerModuleLoaderBuilder(runtimeJars)
                        .withCompilationRootDir(classesPath)
                        .build()
        )
                .setRuntimeJarLibs(runtimeJars)
                .build()

        Container container = Container.getInstance();


        ModuleId moduleId = ModuleId.create("moduleName", "moduleVersion")
        container.loadModules(true)
//        container.addModule(moduleId, false)




        then:
        log.info "then"

        container.executeScript(moduleId, "com.company.script")
        container.destroy();
    }

    def static prepareJarAndClass(String basePath){
        final String JAR_BASE_PATH =    "src/test/resources/build-jar-from-src";
        final String CLASS_BASE_PATH =  "src/test/resources/build-jar-from-src-another";


        // create directories
        Files.createDirectories(Paths.get(basePath, "classes"));
        Files.createDirectories(Paths.get(basePath, "libs"));
        Files.createDirectories(Paths.get(basePath, "src", "moduleName.moduleVersion", "com", "other", "package10"));


        // #1 build dependency jar
        buildDependencyJar();

        // copy dependency jar to the src/moduleName.moduleVersion
        Files.copy(
                Paths.get(JAR_BASE_PATH, "libs", "external.v1.jar"),
                Paths.get(basePath, "src", "moduleName.moduleVersion", "external.v1.jar"),
                StandardCopyOption.REPLACE_EXISTING)



        // #2 build dependency class
        buildDependencyClass();

        // copy dependency class to the src/moduleName.moduleVersion
        Files.copy(
                // src/test/resources/build-jar-from-src-another/classes/other.v02/com/other/package10
                Paths.get(CLASS_BASE_PATH, "classes", "other.v02", "com", "other", "package10", "OtherScript.class"),
                Paths.get(basePath, "src", "moduleName.moduleVersion", "com", "other", "package10", "OtherScript.class"),
                StandardCopyOption.REPLACE_EXISTING);
    }

    def static buildDependencyJar(){
        final String BASE_PATH = "src/test/resources/build-jar-from-src";

        Path srcPath = Paths.get(BASE_PATH, "src").toAbsolutePath();
        Path classesPath = Paths.get(BASE_PATH, "classes").toAbsolutePath();
        Path libPath = Paths.get(BASE_PATH, "libs").toAbsolutePath();

        Set<Path> runtimeJars = new HashSet<>();
        runtimeJars.add(Paths.get("src/test/resources/libs/spock-core-0.7-groovy-2.0.jar").toAbsolutePath())

        new Container.Builder(srcPath, classesPath, libPath)
                .setRuntimeJarLibs(runtimeJars)
                .build()

        Container container = Container.getInstance();

        ModuleId moduleId = ModuleId.create("external", "v1")

        container.addModule(moduleId, false)


        ContainerUtils.packToJar(ContainerUtils.getModulePath(classesPath, moduleId),
                Paths.get(libPath.toAbsolutePath().toString(), "external.v1.jar")
                , moduleId)

        container.destroy();
    }

    def static buildDependencyClass(){
        final String BASE_PATH = "src/test/resources/build-jar-from-src-another";

        Path srcPath = Paths.get(BASE_PATH, "src").toAbsolutePath();
        Path classesPath = Paths.get(BASE_PATH, "classes").toAbsolutePath();
        Path libPath = Paths.get(BASE_PATH, "libs").toAbsolutePath();

        Set<Path> runtimeJars = new HashSet<>();
        runtimeJars.add(Paths.get("src/test/resources/libs/spock-core-0.7-groovy-2.0.jar").toAbsolutePath())

        new Container.Builder(srcPath, classesPath, libPath)
                .setRuntimeJarLibs(runtimeJars)
                .build()

        Container container = Container.getInstance();

        ModuleId moduleId = ModuleId.create("other", "v02")

        container.addModule(moduleId, false)


        ContainerUtils.packToJar(ContainerUtils.getModulePath(classesPath, moduleId),
                Paths.get(libPath.toAbsolutePath().toString(), "other.v02.jar")
                , moduleId)

        Object obj;
        container.destroy();
    }

    def static delete(Path rootPath) {

        if (rootPath == null) {
            return;
        }

        if (!rootPath.toFile().exists()) {
            return;
        }

        Files.walkFileTree(rootPath.toAbsolutePath(), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult postVisitDirectory(Path directory, IOException exc) throws IOException {
                Files.delete(directory);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
