package com.company.netflix.compile

import com.netflix.nicobar.core.archive.PathScriptArchive
import com.netflix.nicobar.groovy2.internal.compile.Groovy2CompilerHelper
import org.codehaus.groovy.runtime.InvokerHelper
import org.codehaus.groovy.tools.GroovyClass
import spock.lang.Specification

import java.nio.file.Path
import java.nio.file.Paths

/**
 *
 */
class GroovyHelperTest extends Specification{

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
//            Class klass = groovyScriptEngine.getGroovyClassLoader().defineClass(groovyClass.getName(), groovyClass.getBytes());
            Class klass = getClass().getClassLoader().defineClass(groovyClass.getName(), groovyClass.getBytes(), 0, groovyClass.getBytes().length);
            println "klass:" + klass
            if(klass.getCanonicalName().equals("fakepackage.RunScript")){
                println "go!"
                Script script = InvokerHelper.createScript(klass, new Binding());
                script.run()
            }
        }






        System.out.println("\n");

    }
}
