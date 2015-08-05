package com.reversemind.nicobar.container.plugin;

import com.netflix.nicobar.core.archive.ScriptArchive;
import com.netflix.nicobar.core.compile.ScriptArchiveCompiler;
import com.netflix.nicobar.core.compile.ScriptCompilationException;
import com.netflix.nicobar.core.module.jboss.JBossModuleClassLoader;
import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 *
 */
public class BytecodeLoaderMulti implements ScriptArchiveCompiler {

    /**
     * Compile (load from) an archive, if it contains any .class files.
     */
    @Override
    public boolean shouldCompile(ScriptArchive archive) {

        Set<String> entries = archive.getArchiveEntryNames();
        boolean shouldCompile = false;
        for (String entry: entries) {
            if (entry.endsWith(".class") || entry.endsWith(".jar")) {
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
            if (!(entry.endsWith(".class") || entry.endsWith(".jar"))) {
                continue;
            }

            if(entry.endsWith(".class")){
                // Load from the underlying archive class resource

                String entryName = entry.replace(".class", "").replace("/", ".");
                try {

                    Path sourceBasePath = Paths.get(archive.getRootUrl().toURI()).toAbsolutePath();
                    Path pathToClass = sourceBasePath.resolve(entry);


                    Class<?> addedClass = moduleClassLoader.loadClassLocal(entryName, true);
                    addedClasses.add(addedClass);


                    Path p = copyClassRelativelyAt(targetDir, pathToClass, addedClass.getCanonicalName(), "OtherScript");

                } catch (Exception e) {
                    throw new ScriptCompilationException("Unable to load class: " + entryName, e);
                }



            } else {

                try {
                    String jarFile = entry;
                    String _path = Paths.get(archive.getRootUrl().toURI()).toAbsolutePath().toString();
                    Path path = Paths.get(_path, jarFile);

                    Set<String> _set = getEntriesFromJar(path);
                    System.out.println("set:" + _set);

                    for(String className: _set){
                        Class<?> addedClass = moduleClassLoader.loadClassLocal(className.replace(".class", ""), true);
//                        addedClasses.add(addedClass);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }


            }

            moduleClassLoader.addClasses(addedClasses);



            // TODO copy all entryNames into targetDir
            if(targetDir != null){
//                Path sourcePath
//                Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
//
//            // jar:file://opt/dev/github/reversemind/anticoating/groovy.nicobar/src/test/resources/base-path-build-module-src-plus-jar/src/moduleName.moduleVersion/external.v1.jar!/com.company2.packageother.OtherHelper.class
////            if(entry.contains(JAR_ARCHIVE_SEPARATOR)){
////                continue;
////            }
//
//            String entryName = entry;
//            // in case of MultiScriptArchive
//            if(entry.contains(JAR_ARCHIVE_SEPARATOR)){
//                //entryName = entry.substring(entry.indexOf(JAR_ARCHIVE_SEPARATOR) + JAR_ARCHIVE_SEPARATOR.length(), entry.length());
////                entryName = "jar:file://opt/dev/github/reversemind/anticoating/groovy.nicobar/src/test/resources/base-path-build-module-src-plus-jar/src/moduleName.moduleVersion/external.v1.jar!/com.company2.packageother.OtherHelper.class";
//                entryName = "com.company2.packageother.OtherHelper";
//
//
//
//                System.out.println("\n\n!!!!!!!!!!!!!!!!!!!!!!!!\n\n" + entry);
//
//
//                entryName = entryName.replace(".class", "").replace("/", ".");
//
//                try {
//                    Class<?> addedClass = moduleClassLoader.loadClassLocal(entryName, true);
//                    addedClasses.add(addedClass);
//                } catch (Exception e) {
//                    throw new ScriptCompilationException("Unable to load class: " + entryName, e);
//                }
//                moduleClassLoader.addClasses(addedClasses);
//
////                try {
////                    byte[] bytes = getBytesOfClassFromJar(entryName, Paths.get("/opt/dev/github/reversemind/anticoating/groovy.nicobar/src/test/resources/base-path-build-module-src-plus-jar/src/moduleName.moduleVersion/external.v1.jar"));
////                    if(bytes != null){
//////                        Class<?> addedClass = moduleClassLoader.addClassBytes(entryName, bytes);
//////                        addedClasses.add(addedClass);
//////                        moduleClassLoader.addClasses(addedClasses);
////                    }
////
////                } catch (Exception e) {
////                    e.printStackTrace();
////                }
//
//            }else{
//
//                entryName = entryName.replace(".class", "").replace("/", ".");
//
//                try {
//                    Class<?> addedClass = moduleClassLoader.loadClassLocal(entryName, true);
//                    addedClasses.add(addedClass);
//                } catch (Exception e) {
//                    throw new ScriptCompilationException("Unable to load class: " + entryName, e);
//                }
//                moduleClassLoader.addClasses(addedClasses);
//            }
//            // Load from the underlying archive class resource

        }


        return Collections.unmodifiableSet(addedClasses);
    }


    /**
     *
     * @param targetPath
     * @param sourceClassPath
     * @param canonicalClassName
     * @return - if null - file was not copy
     */
    private static Path copyClassRelativelyAt(
            final Path targetPath,
            final Path sourceClassPath,
            final String canonicalClassName,
            final String className) throws IOException {
        Path _targetPackagesPath = getTargetPackagesPath(targetPath, canonicalClassName);

        if (_targetPackagesPath == null) {
            return null;
        }

        Files.createDirectories(_targetPackagesPath);
        return Files.copy(sourceClassPath, _targetPackagesPath.resolve(className + ".class"), StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     *
     * @param targetPath
     * @param canonicalClassName - com.other.package10.OtherScript
     * @return - if null - no need to create sub paths
     */
    private static Path getTargetPackagesPath(final Path targetPath, final String canonicalClassName){
        if(targetPath == null){
            return null;
        }

        if(canonicalClassName == null){
            return null;
        }

        if(canonicalClassName.trim().length() == 0){
            return null;
        }

        String _classSubPath = canonicalClassName.replaceAll("\\.", File.separator) + ".class";
        int nameCount = Paths.get(_classSubPath).getNameCount();

        if(nameCount == 0){
            return null;
        }

        Paths.get(_classSubPath).subpath(0,Paths.get(_classSubPath).getNameCount()-1);

        Path packagesPath = Paths.get(_classSubPath).subpath(0,Paths.get(_classSubPath).getNameCount()-1);
        Path targetPackagesPath = Paths.get(targetPath.toString(), packagesPath.toString());

        return targetPackagesPath;
    }

    private Set<String> getEntriesFromJar(Path jarPath) throws IOException {
        // initialize the index
        JarFile jarFile = new JarFile(jarPath.toFile());
        Set<String> indexBuilder = new HashSet<>();
        try {
            Enumeration<JarEntry> jarEntries = jarFile.entries();
            indexBuilder = new HashSet<String>();
            while (jarEntries.hasMoreElements()) {
                JarEntry jarEntry = jarEntries.nextElement();

                if (!jarEntry.isDirectory()) {
                    if(jarEntry.getName().endsWith(".class")){
                        // add it as a URL like name
//                        indexBuilder.add(jarPath.toFile().getName() + "!/" + jarEntry.getName().replaceAll("/", "."));
//                        indexBuilder.add(jarEntry.getName());//.replaceAll("/","."));
                        indexBuilder.add(jarEntry.getName());
                    }
                }
            }
        } finally {
            jarFile.close();
        }

//        Set<String> indexBuilder = new HashSet<>();
//        indexBuilder.add(jarPath.toFile().getName());

        return indexBuilder;
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
