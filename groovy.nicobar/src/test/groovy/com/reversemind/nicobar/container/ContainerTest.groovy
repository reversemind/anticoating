package com.reversemind.nicobar.container

import com.netflix.nicobar.core.archive.ModuleId
import groovy.util.logging.Slf4j
import spock.lang.Ignore
import spock.lang.Specification

import java.nio.file.Path
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 *
 */
@Slf4j
class ContainerTest extends Specification {

    def setup() {
        backToInitialState();
        backToInitialStateGroovyScript();
        Thread.sleep(1000);
    }

    def cleanup() {
        backToInitialState();
        backToInitialStateGroovyScript();
        Thread.sleep(1000);
    }

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

    @Ignore
    def 'by Hand - auto rebuild scripts and reload'() {
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

    def 'auto rebuild scripts and reload multithreaded invoke method for class'() {
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

                changeByString("ScriptHelper2.groovy", "return \"ScriptHelper2|\" + string", "return \"!!! CHANGED !!!|\" + string")

                Thread.sleep(1500);

                backToInitialState();

                Thread.sleep(1500);
            }
        }, 1, 3, TimeUnit.SECONDS);

        2000.times { idx ->
            containerCaller.execute(new ContainerPusher(container, moduleId, idx, false));
            Thread.sleep(10);
        }

        containerCaller.shutdown()
        containerCaller.awaitTermination(5, TimeUnit.SECONDS);

        scheduledThreadPool.shutdown();
        scheduledThreadPool.awaitTermination(5, TimeUnit.SECONDS);

        then:
        log.info "then:"

    }

    def 'auto rebuild scripts and reload multithreaded run script'() {
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

                changeByString("script.groovy",
                        "println \"Date 1:|\"",
                        "println \" !!!! CHANGED !!!! :|\"")

                Thread.sleep(1500);

                backToInitialStateGroovyScript();

                Thread.sleep(1500);
            }
        }, 1, 3, TimeUnit.SECONDS);

        200.times { idx ->
            containerCaller.execute(new ContainerPusher(container, moduleId, idx, true));
            Thread.sleep(10);
        }

        containerCaller.shutdown()
        containerCaller.awaitTermination(5, TimeUnit.SECONDS);

        scheduledThreadPool.shutdown();
        scheduledThreadPool.awaitTermination(5, TimeUnit.SECONDS);

        then:
        log.info "then:"

    }

    public class ContainerPusher implements Runnable {

        private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss.SSS");

        private Container container;
        private ModuleId moduleId;
        private long index;
        private boolean isGroovyScript;

        ContainerPusher(Container container, ModuleId moduleId, long index, boolean isGroovyScript) {
            this.container = container
            this.moduleId = moduleId
            this.index = index;
            this.isGroovyScript = isGroovyScript;
        }

        @Override
        public void run() {
            if (this.isGroovyScript) {
                this.container.executeScript(this.moduleId, "com.company.script")
                Date date = new Date();
                println "index:" + index + "|" + Thread.currentThread().getName() + "|time:" + dateFormat.format(date) + "/stamp:" + date.getTime() + "\n";
            } else {
                Class clazz = this.container.findClass(this.moduleId, "com.company.ScriptHelper2");
                Object object = clazz.newInstance()
                Date date = new Date();
                String threadName = "index:" + index + "|" + Thread.currentThread().getName() + "|time:" + dateFormat.format(date) + "/stamp:" + date.getTime();
                String result = (String) object.invokeMethod("getResponse", [threadName])
                println "result:" + result
            }
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

        TestHelper.replaceContentInFile(filePath.toAbsolutePath().toString(),
                "return \"ScriptHelper2|\" + string",
                "return \"ScriptHelper2|thread:${Thread.currentThread().getId()}|${Thread.currentThread().getName()}|\" + string")

        then:
        log.info "then:"
    }

    private static void changeByString(String scriptName, String whatReplace, String byString) {
        final String BASE_PATH = "src/test/resources/base-path/modules";
        Path filePath = Paths.get(BASE_PATH, "src", "moduleName.moduleVersion", "com", "company", scriptName).toAbsolutePath();

        TestHelper.replaceContentInFile(filePath.toAbsolutePath().toString(),
                whatReplace,
                byString)
    }

    private static void backToInitialState() {
        final String BASE_PATH = "src/test/resources/base-path/modules";
        Path filePath = Paths.get(BASE_PATH, "src", "moduleName.moduleVersion", "com", "company", "ScriptHelper2.groovy").toAbsolutePath();
        TestHelper.replaceContentInFile(filePath.toAbsolutePath().toString(), initialScriptHelper2groovy);
    }

    private static void backToInitialStateGroovyScript() {
        final String BASE_PATH = "src/test/resources/base-path/modules";
        Path filePath = Paths.get(BASE_PATH, "src", "moduleName.moduleVersion", "com", "company", "script.groovy").toAbsolutePath();
        TestHelper.replaceContentInFile(filePath.toAbsolutePath().toString(), initialScriptGroovy);
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

    static String initialScriptGroovy = """package com.company

import com.company.subpackage1.*

println "Date 1:|" + ScriptHelper2.getTime() + "| sublevel1:" + Subpackage1Class.method1() + "|" + Thread.currentThread().getName() + "|" + new Date().getTime()
"""

}
