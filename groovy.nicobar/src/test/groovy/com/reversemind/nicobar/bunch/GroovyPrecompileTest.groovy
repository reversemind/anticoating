package com.reversemind.nicobar.bunch

import com.netflix.nicobar.core.archive.PathScriptArchive
import com.netflix.nicobar.core.archive.ScriptArchive
import com.netflix.nicobar.groovy2.internal.compile.Groovy2CompilerHelper
import groovy.util.logging.Slf4j
import org.codehaus.groovy.tools.GroovyClass
import spock.lang.Specification

import java.nio.file.Path
import java.nio.file.Paths

/**
 *
 */
@Slf4j
class GroovyPrecompileTest extends Specification {

    def 'groovy precompile'() {
        setup:
        println 'go'

        ScriptArchive scriptArchivePackage1 = new PathScriptArchive.Builder(path('src/test/resources/bunch/source/package1'))
                .setRecurseRoot(true)
                .build();

        ScriptArchive scriptArchivePackage2 = new PathScriptArchive.Builder(path('src/test/resources/bunch/source/package2'))
                .setRecurseRoot(true)
                .build();

        ScriptArchive scriptArchiveRoot = new PathScriptArchive.Builder(path('src/test/resources/bunch/source'))
                .setRecurseRoot(false)
                .addFile(path('src/test/resources/bunch/source/rootScript1.groovy'))
                .addFile(path('src/test/resources/bunch/source/rootScript2.groovy'))
                .addFile(path('src/test/resources/bunch/source/moduleSpec.json'))
                .build();


        Path scriptCompilePath = Paths.get('src/test/resources/bunch/compiled').toAbsolutePath()
        Set<GroovyClass> compiledClasses = new Groovy2CompilerHelper(scriptCompilePath)
                .addScriptArchive(scriptArchivePackage1)
                .addScriptArchive(scriptArchivePackage2)
                .addScriptArchive(scriptArchiveRoot)
                .compile();

        compiledClasses.each { klass ->
            log.info "" + klass.getName()
        }

    }

    def
    static Path path(String relativePath) {
        return Paths.get(relativePath).toAbsolutePath()
    }
}
