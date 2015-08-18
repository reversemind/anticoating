package com.reversemind.nicobar.leak
import com.netflix.nicobar.core.archive.ModuleId
import com.netflix.nicobar.core.archive.PathScriptArchive
import com.netflix.nicobar.core.archive.ScriptArchive
import com.netflix.nicobar.core.archive.ScriptModuleSpec
import com.netflix.nicobar.core.module.ScriptModule
import com.netflix.nicobar.core.module.ScriptModuleUtils
import com.netflix.nicobar.core.plugin.BytecodeLoadingPlugin
import com.netflix.nicobar.core.plugin.ScriptCompilerPluginSpec
import com.netflix.nicobar.groovy2.plugin.Groovy2CompilerPlugin
import com.reversemind.nicobar.container.ContainerModuleLoader
import com.reversemind.nicobar.container.ContainerTest
import com.reversemind.nicobar.container.GroovyScriptInvokerHelper
import com.reversemind.nicobar.container.TestHelper
import com.reversemind.nicobar.container.utils.ContainerUtils
import groovy.util.logging.Slf4j

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
class NicobarLeakTest extends ContainerTest{

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss.SSS");

    def 'pure test - Nicobar libs - auto rebuild scripts and reload multithreaded run script'(){
        setup:
        log.info "setup:"

        final String BASE_PATH = "src/test/resources/base-path/modules";

        Path srcPath = Paths.get(BASE_PATH, "src").toAbsolutePath();
        Path classesPath = Paths.get(BASE_PATH, "classes").toAbsolutePath();

        Set<Path> runtimeJars = new HashSet<>();
        runtimeJars.add(Paths.get("src/test/resources/libs/spock-core-0.7-groovy-2.0.jar").toAbsolutePath())


        ExecutorService containerCaller = Executors.newFixedThreadPool(10)
        ScheduledExecutorService scheduledThreadPool = Executors.newScheduledThreadPool(1);


        ScriptCompilerPluginSpec groovy2CompilerPluginSpec = ContainerUtils.buildGroovy2CompilerPluginSpec(runtimeJars);
        ScriptCompilerPluginSpec byteCodeCompilerPluginSpec = ContainerUtils.buildByteCodeCompilerPluginSpec(runtimeJars);

        // create and start the builder with the plugin
//        ScriptModuleLoader scriptModuleLoader = new ScriptModuleLoader.Builder()
//                .addPluginSpec(groovy2CompilerPluginSpec)
//                .addPluginSpec(byteCodeCompilerPluginSpec)
//                .withCompilationRootDir(classesPath)
//                .build();

        ContainerModuleLoader scriptModuleLoader = new ContainerModuleLoader.Builder()
                .addPluginSpec(groovy2CompilerPluginSpec)
                .addPluginSpec(byteCodeCompilerPluginSpec)
                .withCompilationRootDir(classesPath)
                .build();


        when:
        log.info "when:"

        TestHelper.resetContainer()

        ModuleId moduleId = ModuleId.create("moduleName", "moduleVersion")



        ScriptModuleSpec moduleSpec = new ScriptModuleSpec.Builder(moduleId)
                .addCompilerPluginId(BytecodeLoadingPlugin.PLUGIN_ID)
                .addCompilerPluginId(Groovy2CompilerPlugin.PLUGIN_ID)
                .build();

        ScriptArchive scriptArchive = new PathScriptArchive.Builder(ContainerUtils.getModulePath(srcPath, moduleId).toAbsolutePath())
                .setRecurseRoot(true)
                .setModuleSpec(moduleSpec)
                .build();


        scheduledThreadPool.scheduleAtFixedRate(new Runnable() {
            @Override
            void run() {

                changeByString("script.groovy",
                        "println \"Date 1:|\"",
                        "println \" !!!! CHANGED !!!! :|\"")

                Thread.sleep(1500);
                scriptModuleLoader.updateScriptArchives(new LinkedHashSet<ScriptArchive>(Arrays.asList(scriptArchive)))

                backToInitialStateGroovyScript();
                Thread.sleep(1500);
                scriptModuleLoader.updateScriptArchives(new LinkedHashSet<ScriptArchive>(Arrays.asList(scriptArchive)))
            }
        }, 1, 3, TimeUnit.SECONDS);



//        1500.times { idx ->
        200000000.times { idx ->
            containerCaller.execute( new Runnable() {
                @Override
                public void run() {
                    println "before script execution"
                    final ScriptModule scriptModule = scriptModuleLoader.getScriptModule(moduleId);
                    if (scriptModule != null) {

                        Class clazz = ScriptModuleUtils.findClass(scriptModule, "com.company.script");
                        if (clazz != null) {
                            try {
                                GroovyScriptInvokerHelper.runGroovyScript(clazz);
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                    }

                    println "after script execution"
                    Date date = new Date();
                    println "index:" + idx + "|" + Thread.currentThread().getName() + "|time:" + dateFormat.format(date) + "/stamp:" + date.getTime() + "\n";

                    Thread.sleep(100);
                }
            });
            Thread.sleep(10);
        }


        /*

            No-threads

         */
//        changeByString("script.groovy",
//                "println \"Date 1:|\"",
//                "println \" !!!! CHANGED !!!! :|\"")
//
//        Thread.sleep(1500);
//        scriptModuleLoader.updateScriptArchives(new LinkedHashSet<ScriptArchive>(Arrays.asList(scriptArchive)))
//
//        backToInitialStateGroovyScript();
//        Thread.sleep(1500);
//        scriptModuleLoader.updateScriptArchives(new LinkedHashSet<ScriptArchive>(Arrays.asList(scriptArchive)))
//
//
//        ScriptModule scriptModule = scriptModuleLoader.getScriptModule(moduleId);
//        if (scriptModule != null) {
//
//            Class clazz = ScriptModuleUtils.findClass(scriptModule, "com.company.script");
//            if (clazz != null) {
//                try {
//                    GroovyScriptInvokerHelper.runGroovyScript(clazz);
//                } catch (Exception ex) {
//                    ex.printStackTrace();
//                }
//            }
//        }
//
//        println "after script execution"
//        Date date = new Date();
//        println "index:" + 1 + "|" + Thread.currentThread().getName() + "|time:" + dateFormat.format(date) + "/stamp:" + date.getTime() + "\n";
//
//        Thread.sleep(100);



        // call it again - for debugging purpose

        changeByString("script.groovy",
                "println \"Date 1:|\"",
                "println \" !!!! CHANGED !!!! :|\"")

        Thread.sleep(1500);
        scriptModuleLoader.updateScriptArchives(new LinkedHashSet<ScriptArchive>(Arrays.asList(scriptArchive)))

        backToInitialStateGroovyScript();
        Thread.sleep(1500);
        scriptModuleLoader.updateScriptArchives(new LinkedHashSet<ScriptArchive>(Arrays.asList(scriptArchive)))


        ScriptModule scriptModule = scriptModuleLoader.getScriptModule(moduleId);
        if (scriptModule != null) {

            Class clazz = ScriptModuleUtils.findClass(scriptModule, "com.company.script");
            if (clazz != null) {
                try {
                    GroovyScriptInvokerHelper.runGroovyScript(clazz);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

        println "after script execution"

        Thread.sleep(100);




        then:
        log.info "then:"
    }

}
