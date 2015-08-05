package com.reversemind.nicobar.container.plugin;

import com.netflix.nicobar.core.archive.ScriptArchive;
import com.netflix.nicobar.core.compile.ScriptArchiveCompiler;
import com.netflix.nicobar.core.compile.ScriptCompilationException;
import com.netflix.nicobar.core.module.jboss.JBossModuleClassLoader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 *
 */
public class BytecodeLoaderMulti implements ScriptArchiveCompiler {

    // external.v1.jar!/com.company2.packageother.OtherHelper.class
    public static final String JAR_ARCHIVE_SEPARATOR = "!/";

    /**
     * Compile (load from) an archive, if it contains any .class files.
     */
    @Override
    public boolean shouldCompile(ScriptArchive archive) {

        Set<String> entries = archive.getArchiveEntryNames();
        boolean shouldCompile = false;
        for (String entry: entries) {
            if (entry.endsWith(".class")) {
                shouldCompile = true;
            }
        }

        return shouldCompile;
    }

    @Override
    public Set<Class<?>> compile(ScriptArchive archive, JBossModuleClassLoader moduleClassLoader, Path targetDir)
            throws ScriptCompilationException, IOException {
        HashSet<Class<?>> addedClasses = new HashSet<Class<?>>(archive.getArchiveEntryNames().size());
        for (String entry : archive.getArchiveEntryNames()) {
            if (!entry.endsWith(".class")) {
                continue;
            }

            // jar:file://opt/dev/github/reversemind/anticoating/groovy.nicobar/src/test/resources/base-path-build-module-src-plus-jar/src/moduleName.moduleVersion/external.v1.jar!/com.company2.packageother.OtherHelper.class
//            if(entry.contains(JAR_ARCHIVE_SEPARATOR)){
//                continue;
//            }

            String entryName = entry;
            // in case of MultiScriptArchive
            if(entry.contains(JAR_ARCHIVE_SEPARATOR)){
                //entryName = entry.substring(entry.indexOf(JAR_ARCHIVE_SEPARATOR) + JAR_ARCHIVE_SEPARATOR.length(), entry.length());
//                entryName = "jar:file://opt/dev/github/reversemind/anticoating/groovy.nicobar/src/test/resources/base-path-build-module-src-plus-jar/src/moduleName.moduleVersion/external.v1.jar!/com.company2.packageother.OtherHelper.class";
                entryName = "com.company2.packageother.OtherHelper";



                System.out.println("\n\n!!!!!!!!!!!!!!!!!!!!!!!!\n\n" + entry);


                entryName = entryName.replace(".class", "").replace("/", ".");

                try {
                    Class<?> addedClass = moduleClassLoader.loadClassLocal(entryName, true);
                    addedClasses.add(addedClass);
                } catch (Exception e) {
                    throw new ScriptCompilationException("Unable to load class: " + entryName, e);
                }
                moduleClassLoader.addClasses(addedClasses);

//                try {
//                    byte[] bytes = getBytesOfClassFromJar(entryName, Paths.get("/opt/dev/github/reversemind/anticoating/groovy.nicobar/src/test/resources/base-path-build-module-src-plus-jar/src/moduleName.moduleVersion/external.v1.jar"));
//                    if(bytes != null){
////                        Class<?> addedClass = moduleClassLoader.addClassBytes(entryName, bytes);
////                        addedClasses.add(addedClass);
////                        moduleClassLoader.addClasses(addedClasses);
//                    }
//
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }

            }else{

                entryName = entryName.replace(".class", "").replace("/", ".");

                try {
                    Class<?> addedClass = moduleClassLoader.loadClassLocal(entryName, true);
                    addedClasses.add(addedClass);
                } catch (Exception e) {
                    throw new ScriptCompilationException("Unable to load class: " + entryName, e);
                }
                moduleClassLoader.addClasses(addedClasses);
            }
            // Load from the underlying archive class resource

        }

        return Collections.unmodifiableSet(addedClasses);
    }

    private byte[] getBytesOfClassFromJar(String canonicalClassName, Path pathToJarFile) throws IOException, ClassNotFoundException {
        JarFile file = new JarFile(pathToJarFile.toString());
        JarEntry entry = (JarEntry) file.getEntry(canonicalClassName.replace('.', '/') + ".class");

        if (entry != null) {
            InputStream input = file.getInputStream(entry);
            ByteArrayOutputStream output = new ByteArrayOutputStream();

            byte[] buffer = new byte[4096];
            long count = 0L;

            int n1;
            for (boolean n = false; -1 != (n1 = input.read(buffer)); count += (long) n1) {
                output.write(buffer, 0, n1);
            }

            return output.toByteArray();
//            return IOUtils.toByteArray(in);
        }

//        Enumeration entries = new JarFile(pathToJarFile.toString()).entries();
//
//        while (entries.hasMoreElements()) {
//
//            JarEntry entry = (JarEntry) entries.nextElement();
//            if (entry.isDirectory() || !entry.getName().endsWith(".class")) {
//                continue;
//            }
//
//            System.out.println(" class inside jar: " + canonicalClassName);
//
//            if (canonicalClassName.equals(
//                    entry.getName().replace('/', '.')
//            )
//                    ) {
//
//                    InputStream in = file.getInputStream(entry);
//                    return IOUtils.toByteArray(in);
//            }
//        }
//
//        ByteArrayOutputStream oStream = new ByteArrayOutputStream();
//        Streams.copy(in, oStream);
        return null;
    }

//    protected Class findClass(File file, String name) throws ClassNotFoundException {
//        ZipEntry entry = this.file.getEntry(name.replace('.', '/') + ".class");
//        if (entry == null) {
//            throw new ClassNotFoundException(name);
//        }
//        try {
//            byte[] array = new byte[1024];
//            InputStream in = this.file.getInputStream(entry);
//            ByteArrayOutputStream out = new ByteArrayOutputStream(array.length);
//            int length = in.read(array);
//            while (length > 0) {
//                out.write(array, 0, length);
//                length = in.read(array);
//            }
//            return defineClass(name, out.toByteArray(), 0, out.size());
//        }
//        catch (IOException exception) {
//            throw new ClassNotFoundException(name, exception);
//        }
//    }

}
