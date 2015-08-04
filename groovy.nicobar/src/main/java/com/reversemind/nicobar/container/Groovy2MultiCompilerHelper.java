package com.reversemind.nicobar.container;

import com.netflix.nicobar.core.archive.ScriptArchive;
import com.netflix.nicobar.core.compile.ScriptCompilationException;
import groovy.lang.GroovyClassLoader;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.Phases;
import org.codehaus.groovy.tools.GroovyClass;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Helper class for compiling Groovy files into classes. This class takes as it's input a collection
 * of {@link ScriptArchive}s and outputs a {@link GroovyClassLoader} with the classes pre-loaded into it.
 *
 * If a parent {@link ClassLoader} is not provided, the current thread context classloader is used.
 *
 * @author James Kojo
 * @author Vasanth Asokan
 */
public class Groovy2MultiCompilerHelper {
    private final Path targetDir;
    private final List<Path> sourceFiles = new LinkedList<Path>();
    private final List<ScriptArchive> scriptArchives = new LinkedList<ScriptArchive>();
    private ClassLoader parentClassLoader;
    private CompilerConfiguration compileConfig;

    public Groovy2MultiCompilerHelper(Path targetDir) {
        Objects.requireNonNull(targetDir, "targetDir");
        this.targetDir = targetDir;
    }

    public Groovy2MultiCompilerHelper withParentClassloader(ClassLoader parentClassLoader) {
        this.parentClassLoader = parentClassLoader;
        return this;
    }

    public Groovy2MultiCompilerHelper addSourceFile(Path groovyFile) {
        if (groovyFile != null) {
            sourceFiles.add(groovyFile);
        }
        return this;
    }

    public Groovy2MultiCompilerHelper addScriptArchive(ScriptArchive archive) {
        if (archive != null) {
            scriptArchives.add(archive);
        }
        return this;
    }

    public Groovy2MultiCompilerHelper withConfiguration(CompilerConfiguration compilerConfig) {
        if (compilerConfig != null) {
            this.compileConfig = compilerConfig;
        }
        return this;
    }

    /**
     * Compile the given source and load the resultant classes into a new {@link ClassNotFoundException}
     * @return initialized and laoded classes
     * @throws ScriptCompilationException
     */
    @SuppressWarnings("unchecked")
    public Set<GroovyClass> compile() throws ScriptCompilationException {
        final CompilerConfiguration conf = compileConfig != null ? compileConfig: CompilerConfiguration.DEFAULT;
        conf.setTolerance(0);
        conf.setVerbose(true);
        conf.setTargetDirectory(targetDir.toFile());
        final ClassLoader buildParentClassloader = parentClassLoader != null ?
                parentClassLoader : Thread.currentThread().getContextClassLoader();
        GroovyClassLoader groovyClassLoader = AccessController.doPrivileged(new PrivilegedAction<GroovyClassLoader>() {
            public GroovyClassLoader run() {
                return new GroovyClassLoader(buildParentClassloader, conf, false);
            }
        });

        CompilationUnit unit = new CompilationUnit(conf, null, groovyClassLoader);
        Set<String> scriptExtensions = conf.getScriptExtensions();
        scriptExtensions.add("class");
        try {
            for (ScriptArchive scriptArchive : scriptArchives) {
                Set<String> entryNames = scriptArchive.getArchiveEntryNames();
                for (String entryName : entryNames) {
                    for (String extension : scriptExtensions) {
                        if (entryName.endsWith(extension)) {
                            // identified groovy file

                            if ("groovy".equals(extension)) {
                                unit.addSource(scriptArchive.getEntry(entryName));
                            }

                            if ("class".equals(extension)) {
                                URL url = new URL("jar:file://opt/dev/github/reversemind/anticoating/groovy.nicobar/src/test/resources/base-path-build-module-src-plus-jar/src/moduleName.moduleVersion/external.v1.jar!/com.company2.packageother.OtherHelper.class");
//                                URL url = new URL("file://opt/dev/github/reversemind/anticoating/groovy.nicobar/src/test/resources/base-path-build-module-src-plus-jar/src/moduleName.moduleVersion/external.v1.jar");
//                                groovyClassLoader.addURL(new URL("jar:" + scriptArchive.getEntry(entryName).toString()));
                                groovyClassLoader.addURL(url);

//                                URL url = new URL("jar:file://opt/dev/github/reversemind/anticoating/groovy.nicobar/src/test/resources/base-path-build-module-src-plus-jar/src/moduleName.moduleVersion/external.v1.jar!/com.company2.packageother.OtherHelper.class");URL url = new URL("jar:file://opt/dev/github/reversemind/anticoating/groovy.nicobar/src/test/resources/base-path-build-module-src-plus-jar/src/moduleName.moduleVersion/external.v1.jar!/com.company2.packageother.OtherHelper.class");

//                                URL url = new URL("file:/opt/dev/github/reversemind/anticoating/groovy.nicobar/src/test/resources/base-path-build-module-src-plus-jar/classes/moduleName.moduleVersion/com/company2/packageother/OtherHelper.class");
//                                unit.addSource(url);
//                                unit.addSource()

//                                try {
//                                    String _tmp = Paths.get(scriptArchive.getRootUrl().toURI()).toAbsolutePath().toString();
//                                    Class clazz = findClassInJar(groovyClassLoader, entryName.replaceAll("/", "."), Paths.get(_tmp, "external.v1.jar"));
//                                    if (clazz != null) {
//                                        unit.addClassNode(new ClassNode(clazz));
////                                        unit.addSource()
////                                        unit.addClassNode();
////                                        unit.addSource()
//                                    }
//                                } catch (ClassNotFoundException | URISyntaxException e) {
//                                    e.printStackTrace();
//                                }

                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new ScriptCompilationException("Exception loading source files", e);
        }
        for (Path sourceFile : sourceFiles) {
            unit.addSource(sourceFile.toFile());
        }
        try {
            unit.compile(Phases.OUTPUT);
        } catch (CompilationFailedException e) {
            throw new ScriptCompilationException("Exception during script compilation", e);
        }
        return new HashSet<GroovyClass>(unit.getClasses());
    }

    private Class findClassInJar(ClassLoader classLoader, String canonicalClassName, Path pathToJarFile) throws IOException, ClassNotFoundException {
        Enumeration entries = new JarFile(pathToJarFile.toString()).entries();

        while (entries.hasMoreElements()) {

            JarEntry entry = (JarEntry) entries.nextElement();
            if (entry.isDirectory() || !entry.getName().endsWith(".class")) {
                continue;
            }

            System.out.println(" class inside jar: " + canonicalClassName);

            if (canonicalClassName.equals(
                    entry.getName().replace('/', '.')
            )
                    ) {
                URL[] urls = {pathToJarFile.toFile().toURL()};
                return new URLClassLoader(urls, classLoader).loadClass(canonicalClassName.replace(".class", ""));
            }
        }

        return null;
    }
}

