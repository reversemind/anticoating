package com.reversemind.nicobar


import groovy.util.logging.Slf4j
import spock.lang.Specification

import java.nio.file.Path
import java.nio.file.Paths
import java.util.jar.JarEntry
import java.util.jar.JarFile

/**
 *
 */
@Slf4j
class ScriptInvokerHelperTest extends Specification {

    def 'create groovy script from compiled class inside jar'() {
        setup:
        log.info "\nsetup:\n"

        def canonicalClassName = "precompiled.mainScript"
        def Path preCompiledJarPath = Paths.get("src/test/resources/libs/precompiled.jar").toAbsolutePath()

        when:
        log.info "\nwhen:\n"

        Class clazz = findClassInJar(canonicalClassName, preCompiledJarPath);
        Script script = ScriptInvokerHelper.createGroovyScript(clazz, new Binding());
        ScriptInvokerHelper.runGroovyScript(clazz)

        then:
        log.info "\nthen:\n"

        clazz != null
        script != null
        ScriptInvokerHelper.runGroovyScript(clazz) == null
    }

    private Class findClassInJar(String canonicalClassName, Path pathToJarFile) {
        Enumeration entries = new JarFile(pathToJarFile.toString()).entries();

        while (entries.hasMoreElements()) {

            JarEntry entry = (JarEntry) entries.nextElement();
            if (entry.isDirectory() || !entry.getName().endsWith(".class")) {
                continue;
            }

            log.info "class inside jar:${entry.getName()}"

            if (canonicalClassName.equals(
                    entry.getName()
                    // remove ".class"
                            .substring(0, entry.getName().length() - 6)
                            .replace('/', '.')
            )
            ) {
                URL[] urls = [pathToJarFile.toFile().toURL()];
                return new URLClassLoader(urls, getClass().getClassLoader()).loadClass(canonicalClassName);
            }
        }

        return null;
    }
}
