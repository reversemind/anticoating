package com.company.netflix.compile

import com.netflix.nicobar.core.archive.PathScriptArchive
import com.netflix.nicobar.groovy2.internal.compile.Groovy2CompilerHelper
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


        for(GroovyClass groovyClass: compiledClasses){
            System.out.println("groovyClass:" + groovyClass.getName());
        }


        GroovyScriptEngine groovyScriptEngine = new GroovyScriptEngine("");
//        groovyScriptEngine.getGroovyClassLoader().setResourceLoader();

        System.out.println("\n");

    }
}
