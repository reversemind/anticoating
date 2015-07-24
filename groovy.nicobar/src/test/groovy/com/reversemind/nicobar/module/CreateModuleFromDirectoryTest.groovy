package com.reversemind.nicobar.module

import com.google.common.hash.HashCode
import com.google.common.hash.HashFunction
import com.google.common.hash.Hasher
import com.google.common.hash.Hashing
import com.netflix.nicobar.core.archive.JarScriptArchive
import com.netflix.nicobar.core.archive.ModuleId
import com.netflix.nicobar.core.archive.PathScriptArchive
import com.netflix.nicobar.core.archive.ScriptArchive
import com.netflix.nicobar.core.archive.ScriptModuleSpec
import com.netflix.nicobar.core.plugin.BytecodeLoadingPlugin
import com.netflix.nicobar.groovy2.internal.compile.Groovy2CompilerHelper
import com.netflix.nicobar.groovy2.plugin.Groovy2CompilerPlugin
import com.reversemind.nicobar.BuildJarModule
import com.reversemind.nicobar.ScriptContainer
import groovy.util.logging.Slf4j
import org.apache.commons.lang3.StringUtils
import org.codehaus.groovy.tools.GroovyClass
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

/**
 *
 */
@Slf4j
class CreateModuleFromDirectoryTest extends Specification {

    def 'create module form directory'() {
        setup:
        log.info "GO"


        // #1 Watch directory

        // #2 Create module for directory
        // 2.1 - select directory to compile - all structure
        // 2.2 Compile it - if compilation is succesfull that ready to create jar file


        // #3 is it really need to compile fileda


        /*
            http://stackoverflow.com/questions/7225313/how-does-git-compute-file-hashes
            How does GIT compute its commit hashes
            Commit Hash (SHA1) = SHA1("blob" + <size_of_file> + "\0" + <contents_of_file>)
        */
        // Guava sha1 - git file hash
        HashFunction hashFunction = Hashing.sha1();
        Hasher hasher = hashFunction.newHasher()
        hasher.putBytes("".getBytes("UTF-8"))

        HashCode hashCode = hasher.hash()
        println "sha1:" + hashCode.toString()

    }

    def 'compile and pack to module jar'(){
        setup:

        String moduleName = "precompiled"
        String moduleVersion = "v0_1-SNAPSHOT"

        ScriptModuleSpec moduleSpec = new ScriptModuleSpec.Builder(ModuleId.create(moduleName, moduleVersion))
                .addCompilerPluginId(BytecodeLoadingPlugin.PLUGIN_ID)
                .addCompilerPluginId(Groovy2CompilerPlugin.PLUGIN_ID)
                .build();

        Path scriptRootPath = Paths.get('src/test/resources/auto/source').toAbsolutePath()

        PathScriptArchive scriptArchive = new PathScriptArchive.Builder(scriptRootPath)
                .setRecurseRoot(true)
                .setModuleSpec(moduleSpec)
                .build();

        Set<GroovyClass> compiledClasses = new Groovy2CompilerHelper(Paths.get('src/test/resources/auto/target').toAbsolutePath())
                .addScriptArchive(scriptArchive)
                .compile();

        for(GroovyClass groovyClass: compiledClasses){
            println "class:" + groovyClass.getName()
        }

        when:
        BuildJarModule buildJarModule = new BuildJarModule(moduleName,
                moduleVersion,
                Paths.get('src/test/resources/auto/target').toAbsolutePath().toString(), true);

        buildJarModule.toJar(Paths.get('src/test/resources/auto').toAbsolutePath());

        then:
        println "PATH:" + Paths.get('src/test/resources/auto').toAbsolutePath();
        true == Paths.get("src/test/resources/auto/" + getModuleNameForJarFile(moduleName, moduleVersion) + ".jar").toAbsolutePath().toFile().exists()


        // copy to runnable dir
        Path sourceJarPath = Paths.get("src/test/resources/auto/" + getModuleNameForJarFile(moduleName, moduleVersion) + ".jar").toAbsolutePath();
        Path targetJarPath = Paths.get("src/test/resources/auto/runnable/" + getModuleNameForJarFile(moduleName, moduleVersion) + ".jar").toAbsolutePath();
        Files.copy(sourceJarPath, targetJarPath, StandardCopyOption.REPLACE_EXISTING);



        // load compiled module and run main script

        Path modulePath = Paths.get("src/test/resources/auto/runnable/" + getModuleNameForJarFile(moduleName, moduleVersion) + ".jar").toAbsolutePath()
        ScriptArchive jarModule = new JarScriptArchive.Builder(modulePath).build();

        ScriptContainer scriptContainer = ScriptContainer.getInstance()
        scriptContainer.updateScriptArchive(jarModule)

        ModuleId moduleId = scriptArchive.getModuleSpec().getModuleId()

//        scriptContainer.executeModule(moduleId)

        scriptContainer.executeScript(moduleId, "com.company.script");

    }



}
