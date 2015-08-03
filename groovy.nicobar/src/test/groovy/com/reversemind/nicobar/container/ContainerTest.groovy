package com.reversemind.nicobar.container

import com.netflix.nicobar.core.archive.ModuleId
import com.reversemind.nicobar.container.utils.FileUtils
import groovy.util.logging.Slf4j
import spock.lang.Specification

import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit


/**
 *
 */
@Slf4j
class ContainerTest extends Specification {

    def 'container was not initialized'() {
        setup:
        log.info "setup:"

        when:
        log.info "when:"

        Container container = Container.getInstance();

        then:
        log.info "then:"
        thrown IllegalStateException
    }

    def 'call builder twice is wrong'() {
        setup:
        log.info "setup:"
        final String BASE_PATH = "src/test/resources/base-path/modules";

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


        new Container.Builder(srcPath, classesPath, libPath)
                .setRuntimeJarLibs(runtimeJars)
                .build()

        then:
        log.info "then:"
        thrown IllegalStateException
    }

    def 'build and init container'() {
        setup:
        log.info "setup:"

        final String BASE_PATH = "src/test/resources/base-path/modules";

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

        ModuleId moduleId = ModuleId.create("moduleName", "moduleVersion")

        container.addModule(moduleId, false)
        container.executeScript(moduleId, "com.company.script")

        then:
        log.info "then:"
    }

    def 'auto rebuild scripts and reload'() {
        setup:
        log.info "setup:"

        final String BASE_PATH = "src/test/resources/base-path/modules";

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

        ModuleId moduleId = ModuleId.create("moduleName", "moduleVersion")

        container.addModule(moduleId, true)

        10000.times() {
            container.executeScript(moduleId, "com.company.script")
            Thread.sleep(2000);
        }

        then:
        log.info "then:"
    }

    def 'auto rebuild scripts and reload multithreaded'() {
        setup:
        log.info "setup:"

        final String BASE_PATH = "src/test/resources/base-path/modules";

        Path srcPath = Paths.get(BASE_PATH, "src").toAbsolutePath();
        Path classesPath = Paths.get(BASE_PATH, "classes").toAbsolutePath();
        Path libPath = Paths.get(BASE_PATH, "libs").toAbsolutePath();

        Set<Path> runtimeJars = new HashSet<>();
        runtimeJars.add(Paths.get("src/test/resources/libs/spock-core-0.7-groovy-2.0.jar").toAbsolutePath())


        ExecutorService containerCaller = Executors.newFixedThreadPool(10)
        ScheduledExecutorService scheduledThreadPool = Executors.newScheduledThreadPool(1);

        when:
        log.info "when:"

        new Container.Builder(srcPath, classesPath, libPath)
                .setWatchPeriod(100)
                .setNotifyPeriod(1000)
                .setRuntimeJarLibs(runtimeJars)
                .build()

        Container container = Container.getInstance();

        ModuleId moduleId = ModuleId.create("moduleName", "moduleVersion")

        container.addModule(moduleId, true)


        scheduledThreadPool.scheduleAtFixedRate(new Runnable() {
            @Override
            void run() {

                changeByString("return \"ScriptHelper2|\" + string", "return \"!!! CHANGED !!!|\" + string")

                Thread.sleep(500);

                backToInitialState();

                Thread.sleep(500);
            }
        }, 3, 2, TimeUnit.SECONDS);

        100.times { idx ->
            println "index:${idx}"
            containerCaller.execute(new ContainerPusher(container, moduleId, idx));
        }


        Thread.sleep(10000);

        containerCaller.shutdown()
        while (!containerCaller.isShutdown()) {
            println "Still running"
            Thread.sleep(1000);
        }



        then:
        log.info "then:"

    }

    public class ContainerPusher implements Runnable {
        private Container container;
        private ModuleId moduleId;
        private long index;

        ContainerPusher(Container container, ModuleId moduleId, long index) {
            this.container = container
            this.moduleId = moduleId
            this.index = index;
        }

        @Override
        public void run() {
            Class clazz = this.container.findClass(this.moduleId, "com.company.ScriptHelper2");
            Object object = clazz.newInstance()
            String threadName = "index:" + index + "|" + Thread.currentThread().getName();
            String result = (String) object.invokeMethod("getResponse", [threadName])
            println "result:" + result
            Thread.sleep(100);
        }
    }

    def 'rewrite script into initial state'() {
        setup:
        log.info "info:"

        final String BASE_PATH = "src/test/resources/base-path/modules";
        Path filePath = Paths.get(BASE_PATH, "src", "moduleName.moduleVersion", "ScriptHelper2.groovy").toAbsolutePath();

        when:
        log.info "when:"

        backToInitialState();

        then:
        log.info "then:"
    }

    def 'change in script a single string'() {
        setup:
        log.info "info:"

        final String BASE_PATH = "src/test/resources/base-path/modules";
        Path filePath = Paths.get(BASE_PATH, "src", "moduleName.moduleVersion", "com", "company", "ScriptHelper2.groovy").toAbsolutePath();

        when:
        log.info "when:"

        FileUtils.replaceContentInFile(filePath.toAbsolutePath().toString(),
                "return \"ScriptHelper2|\" + string",
                "return \"ScriptHelper2|thread:${Thread.currentThread().getId()}|${Thread.currentThread().getName()}|\" + string")

        then:
        log.info "then:"
    }

    private static void changeByString(String whatReplace, String byString) {
        final String BASE_PATH = "src/test/resources/base-path/modules";
        Path filePath = Paths.get(BASE_PATH, "src", "moduleName.moduleVersion", "com", "company", "ScriptHelper2.groovy").toAbsolutePath();

        FileUtils.replaceContentInFile(filePath.toAbsolutePath().toString(),
                whatReplace,
                byString)
    }

    private static void backToInitialState() {
        final String BASE_PATH = "src/test/resources/base-path/modules";
        Path filePath = Paths.get(BASE_PATH, "src", "moduleName.moduleVersion", "com", "company", "ScriptHelper2.groovy").toAbsolutePath();
        FileUtils.replaceContentInFile(filePath.toAbsolutePath().toString(), initialScriptHelper2groovy);
    }

    static String initialScriptHelper2groovy = """package com.company

class ScriptHelper2 {

    def static String getTime() {
        return " script helper 2:" + new Date()
    }

    def static String getResponse(String string) {
        return "ScriptHelper2|" + string
    }
}
"""
}
