package com.company.netflix.compile

import com.netflix.nicobar.core.archive.*
import com.netflix.nicobar.core.internal.compile.BytecodeLoader
import com.netflix.nicobar.core.module.ScriptModule
import com.netflix.nicobar.core.module.ScriptModuleLoader
import com.netflix.nicobar.core.persistence.ArchiveRepository
import com.netflix.nicobar.core.persistence.JarArchiveRepository
import com.netflix.nicobar.core.plugin.BytecodeLoadingPlugin
import com.netflix.nicobar.core.plugin.ScriptCompilerPluginSpec
import com.netflix.nicobar.core.utils.ClassPathUtils
import com.netflix.nicobar.example.groovy2.ExampleResourceLocator
import com.netflix.nicobar.groovy2.internal.compile.Groovy2Compiler
import com.netflix.nicobar.groovy2.internal.compile.Groovy2CompilerHelper
import com.netflix.nicobar.groovy2.plugin.Groovy2CompilerPlugin
import org.codehaus.groovy.runtime.InvokerHelper
import org.codehaus.groovy.tools.GroovyClass
import spock.lang.Specification

import java.nio.file.Path
import java.nio.file.Paths

import static org.testng.Assert.assertNotNull
import static org.testng.Assert.fail

/**
 *
 */
class GroovyHelperTest extends Specification{

    private static final String GROOVY2_COMPILER_PLUGIN = Groovy2CompilerPlugin.class.getName();

    def 'load scriptModule'(){
        setup:

        def rootArchiveDirectory = Paths.get("/opt/script/modules/repository");

        ScriptModuleLoader moduleLoader = createGroovyModuleLoader().build();


        ArchiveRepository archiveRepository = new JarArchiveRepository.Builder(rootArchiveDirectory).build();

        String[] paths = [
                '/opt/script/modules/lib/p90jvm-common.jar',
                '/opt/script/modules/lib/p90jvm-component.jar',
                '/opt/script/modules/lib/p90sdk-api.jar',
                '/opt/script/modules/lib/spock-core.jar'
        ]

        ScriptModuleSpec _moduleSpec = new ScriptModuleSpec.Builder(ModuleId.create("p90jvm-common"))
                .addCompilerPluginId(Groovy2Compiler.GROOVY2_COMPILER_ID)
                .build()
        ScriptArchive scriptArchive = new JarScriptArchive.Builder(Paths.get(paths[0]))
                .setCreateTime(new Date().getTime())
                .setModuleSpec(_moduleSpec)
                .build();
        archiveRepository.insertArchive(scriptArchive);
        moduleLoader.updateScriptArchives(new LinkedHashSet<ScriptArchive>(Arrays.asList(scriptArchive)));


        _moduleSpec = new ScriptModuleSpec.Builder(ModuleId.create("p90jvm-component"))
                .addCompilerPluginId(Groovy2Compiler.GROOVY2_COMPILER_ID)
                .build()
        scriptArchive = new JarScriptArchive.Builder(Paths.get(paths[1]))
                .setCreateTime(new Date().getTime())
                .setModuleSpec(_moduleSpec)
                .build();
        archiveRepository.insertArchive(scriptArchive);
        moduleLoader.updateScriptArchives(new LinkedHashSet<ScriptArchive>(Arrays.asList(scriptArchive)));


        _moduleSpec = new ScriptModuleSpec.Builder(ModuleId.create("p90sdk-api"))
                .addCompilerPluginId(Groovy2Compiler.GROOVY2_COMPILER_ID)
                .build()
        scriptArchive = new JarScriptArchive.Builder(Paths.get(paths[2]))
                .setCreateTime(new Date().getTime())
                .setModuleSpec(_moduleSpec)
                .build();
        archiveRepository.insertArchive(scriptArchive);
        moduleLoader.updateScriptArchives(new LinkedHashSet<ScriptArchive>(Arrays.asList(scriptArchive)));

        _moduleSpec = new ScriptModuleSpec.Builder(ModuleId.create("spock-core"))
                .addCompilerPluginId(Groovy2Compiler.GROOVY2_COMPILER_ID)
                .build()
        scriptArchive = new JarScriptArchive.Builder(Paths.get(paths[3]))
                .setCreateTime(new Date().getTime())
                .setModuleSpec(_moduleSpec)
                .build();
        archiveRepository.insertArchive(scriptArchive);
        moduleLoader.updateScriptArchives(new LinkedHashSet<ScriptArchive>(Arrays.asList(scriptArchive)));


        ScriptModuleSpec moduleSpec = new ScriptModuleSpec.Builder(ModuleId.create("p90test-module"))
                .addModuleDependency("p90jvm-common")
                .addModuleDependency("p90jvm-component")
                .addModuleDependency("p90sdk-api")
                .addModuleDependency("spock-core")
                .build();

        def scriptRootPath = Paths.get('/opt/script/modules/script')
        scriptArchive = new PathScriptArchive.Builder(scriptRootPath)
                .setRecurseRoot(true)
                .setModuleSpec(moduleSpec)
                .build();

        moduleLoader.updateScriptArchives(new LinkedHashSet<ScriptArchive>(Arrays.asList(scriptArchive)));

        ScriptModule scriptModule = moduleLoader.getScriptModule("p90test-module")
        Class<?> clazz = findClassByName(scriptModule, "p90script");



        println "go!"
        Script script = InvokerHelper.createScript(clazz, new Binding());
        script.run()
    }


    public static Path getCoberturaJar(ClassLoader classLoader) {
        return ClassPathUtils.findRootPathForResource("net/sourceforge/cobertura/coveragedata/HasBeenInstrumented.class", classLoader);
    }

    private ScriptModuleLoader.Builder createGroovyModuleLoader() throws Exception {

        Path groovyRuntimePath = ClassPathUtils.findRootPathForResource("META-INF/groovy-release-info.properties", GroovyHelperTest.class.getClassLoader());
//        Path groovyRuntimePath = ClassPathUtils.findRootPathForClass(GroovyHelperTest.class);
        assertNotNull(groovyRuntimePath, "coudln't find groovy-all jar");

        Path groovyPluginLocationPath = ClassPathUtils.findRootPathForClass(Groovy2CompilerPlugin.class);
        assertNotNull(groovyPluginLocationPath, "nicobar-groovy2 project on classpath.");

        // create the groovy plugin spec. this plugin specified a new module and classloader called "Groovy2Runtime"
        // which contains the groovy-all-2.1.6.jar and the nicobar-groovy2 project.
        ScriptCompilerPluginSpec pluginSpec = new ScriptCompilerPluginSpec.Builder(Groovy2Compiler.GROOVY2_COMPILER_ID)
//                .addRuntimeResource(groovyRuntimePath)
//                .addRuntimeResource(groovyPluginLocationPath)
                .addRuntimeResource(ExampleResourceLocator.getGroovyRuntime())
                .addRuntimeResource(ExampleResourceLocator.getGroovyPluginLocation())

        // hack to make the gradle build work. still doesn't seem to properly instrument the code
        // should probably add a classloader dependency on the system classloader instead
                .addRuntimeResource(getCoberturaJar(getClass().getClassLoader()))
                .withPluginClassName(GROOVY2_COMPILER_PLUGIN)
                .withPluginClassName(BytecodeLoadingPlugin.class.getName())
                .build();

        // create and start the builder with the plugin
        return new ScriptModuleLoader.Builder().addPluginSpec(pluginSpec);
    }













    def 'load jar into ArchiveRepository'(){
        setup:

        def rootArchiveDirectory = Paths.get("/opt/script/modules/repository");
        ArchiveRepository archiveRepository = new JarArchiveRepository.Builder(rootArchiveDirectory).build();

        String[] paths = [
                '/opt/script/modules/lib/p90jvm-common.jar',
                '/opt/script/modules/lib/p90jvm-component.jar',
                '/opt/script/modules/lib/p90sdk-api.jar'
        ]

        paths.each { path ->
            JarScriptArchive jarScriptArchive = new JarScriptArchive.Builder(Paths.get(path))
                    .setCreateTime(new Date().getTime())
                    .build();
            archiveRepository.insertArchive(jarScriptArchive);
        }


        ScriptModuleSpec moduleSpec = new ScriptModuleSpec.Builder(ModuleId.create("p90test-module"))
                .addModuleDependency("p90jvm-common")
                .addModuleDependency("p90jvm-component")
                .addModuleDependency("p90sdk-api")
                .build();

        def scriptRootPath = Paths.get('/opt/script/modules/script')
        PathScriptArchive scriptArchive = new PathScriptArchive.Builder(scriptRootPath)
                .setRecurseRoot(true)
                .setModuleSpec(moduleSpec)
                .build();

        def whereToCompile = Paths.get('/opt/script/modules/whereToCompile')
        Groovy2CompilerHelper groovy2CompilerHelper = new Groovy2CompilerHelper(whereToCompile);



        paths.each { path ->
            JarScriptArchive jarScriptArchive = new JarScriptArchive.Builder(Paths.get(path))
                    .setCreateTime(new Date().getTime())
                    .build();
            groovy2CompilerHelper.addScriptArchive(jarScriptArchive);
        }
        groovy2CompilerHelper.addScriptArchive(scriptArchive);

        Set<GroovyClass> compiledClasses = groovy2CompilerHelper.compile()

        println "\n\n--------------"
        println "archiveRepository:" + archiveRepository
    }

    def 'compile files'(){
        setup:
        println "directory:" + this.getClass().getClassLoader().getResource("").getPath()

        // /build/resources/test/netflix
        // /build/classes/test/fakepakage


        String currentDirectory = this.getClass().getClassLoader().getResource("").getPath()

        def compiledDirectory = currentDirectory + "../../resources/test/compiled"
        Path scriptRootPath = Paths.get(currentDirectory + "../../resources/test/netflix");

        println "scriptRootPath:${scriptRootPath}"

        PathScriptArchive scriptArchive = new PathScriptArchive.Builder(scriptRootPath)
                .setRecurseRoot(true)
                .build();

        Set<GroovyClass> compiledClasses = new Groovy2CompilerHelper(Paths.get(compiledDirectory))
                .addScriptArchive(scriptArchive)
                .compile();

        System.out.println(compiledClasses);


        GroovyScriptEngine groovyScriptEngine = new GroovyScriptEngine("");
//        groovyScriptEngine.getGroovyClassLoader().setResourceLoader();
//        groovyScriptEngine.run("","")

//        scriptArchive.getClassLoader()

        for(GroovyClass groovyClass: compiledClasses){
            System.out.println("groovyClass:" + groovyClass.getName());
            Class klass = getClass().getClassLoader().defineClass(groovyClass.getName(), groovyClass.getBytes(), 0, groovyClass.getBytes().length);
            println "klass:" + klass
            println "short name:" + klass.getName()
            println "simple name:" + klass.getSimpleName()

            if(klass.getCanonicalName().equals("fakepackage.RunScript")){
                println "go!"
                Script script = InvokerHelper.createScript(klass, new Binding());
                script.run()
            }
        }

        System.out.println("\n");
    }

    private Class<?> findClassByName(ScriptModule scriptModule, String className) {
        Set<Class<?>> classes = scriptModule.getLoadedClasses();
        for (Class<?> clazz : classes) {
            if (clazz.getName().equals(className)) {
                return clazz;
            }
        }
        fail("couldn't find class " + className);
        return null;
    }
}


/*

ScriptModule



 */