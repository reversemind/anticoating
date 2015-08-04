package com.reversemind.nicobar.container;

import com.netflix.nicobar.core.archive.ScriptArchive;
import com.netflix.nicobar.core.archive.ScriptModuleSpec;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 *
 */
public class MixScriptArchive implements ScriptArchive {

    private final Set<String> entryNames;
    private final Path rootDirPath;
    private final URL rootUrl;
    private final long createTime;
    private ScriptModuleSpec moduleSpec;

    public MixScriptArchive(Path path, boolean recurseRoot) throws IOException {

        this.rootDirPath = path;
        this.rootUrl = this.rootDirPath.toUri().toURL();
        this.createTime = new Date().getTime();

        final LinkedHashSet<String> buildEntries = new LinkedHashSet<String>();
        if (recurseRoot) {
            Files.walkFileTree(this.rootDirPath, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {

                    if(file.toFile().getName().endsWith(".jar")){
                        Set<String> _set = getEntriesFromJar(file);
                        if(!_set.isEmpty()){
                            for(String _str: _set){
                                buildEntries.add(_str);
                            }
                        }
                    }else{
                        Path relativePath = rootDirPath.relativize(file);
                        buildEntries.add(relativePath.toString());
                    }
                    return FileVisitResult.CONTINUE;
                }
                ;
            });
        }

        entryNames = Collections.unmodifiableSet(buildEntries);
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
                        indexBuilder.add(jarPath.toFile().getName() + "!/" + jarEntry.getName().replaceAll("/","."));
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

    @Override
    public ScriptModuleSpec getModuleSpec() {
        return moduleSpec;
    }

    @Override
    public void setModuleSpec(ScriptModuleSpec spec) {
        this.moduleSpec = spec;
    }

    @Nullable
    @Override
    public URL getRootUrl() {
        return rootUrl;
    }

    @Override
    public Set<String> getArchiveEntryNames() {
        return entryNames;
    }

    @Nullable
    @Override
    public URL getEntry(String entryName) throws IOException {
        if (!entryNames.contains(entryName)) {
            return null;
        }
        return rootDirPath.resolve(entryName).toUri().toURL();
    }

    @Override
    public long getCreateTime() {
        return createTime;
    }
}
