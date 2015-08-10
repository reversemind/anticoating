package com.reversemind.nicobar.container.utils;

import com.google.common.collect.Sets;
import com.netflix.nicobar.core.archive.*;
import com.netflix.nicobar.core.plugin.BytecodeLoadingPlugin;
import com.netflix.nicobar.core.plugin.ScriptCompilerPluginSpec;
import com.netflix.nicobar.core.utils.ClassPathUtils;
import com.netflix.nicobar.example.groovy2.ExampleResourceLocator;
import com.netflix.nicobar.groovy2.internal.compile.Groovy2Compiler;
import com.netflix.nicobar.groovy2.plugin.Groovy2CompilerPlugin;
import com.reversemind.nicobar.container.ContainerModuleLoader;
import com.reversemind.nicobar.container.mix.compiler.MixGroovy2Compiler;
import com.reversemind.nicobar.container.mix.plugin.MixBytecodeLoadingPlugin;
import com.reversemind.nicobar.container.mix.plugin.MixGroovy2CompilerPlugin;
import org.apache.commons.lang3.StringUtils;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Expand;
import org.apache.tools.ant.taskdefs.Jar;
import org.jboss.modules.ModuleLoadException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

import static java.nio.file.FileVisitOption.FOLLOW_LINKS;

/**
 * // TODO need to refactor it and remove some methods
 */
public class ContainerUtils {

    private final
    static ScriptModuleSpecSerializer DEFAULT_MODULE_SPEC_SERIALIZER = new GsonScriptModuleSpecSerializer();

    public
    static ContainerModuleLoader.Builder createMixContainerModuleLoaderBuilder(Set<Path> externalLibs) {

        ScriptCompilerPluginSpec.Builder builder = new ScriptCompilerPluginSpec.Builder(MixGroovy2Compiler.GROOVY2_COMPILER_ID)
                .addRuntimeResource(ExampleResourceLocator.getGroovyRuntime())
                .addRuntimeResource(getMixGroovy2PluginLocation(ExampleResourceLocator.class.getClassLoader()))
                .addRuntimeResource(getMixByteCodeLoadingPluginPath())

                        // hack to make the gradle build work. still doesn't seem to properly instrument the code
                        // should probably add a classloader dependency on the system classloader instead
                .addRuntimeResource(getCoberturaJar(ContainerUtils.class.getClassLoader()))
                .withPluginClassName(MixGroovy2CompilerPlugin.class.getName());

        // in version higher 0.2.6 of Nicobar should be added some useful methods, but now needs to iterate
        // add run time .jar libs
        if (!externalLibs.isEmpty()) {
            for (Path path : externalLibs) {
                builder.addRuntimeResource(path.toAbsolutePath());
            }
        }

        ScriptCompilerPluginSpec mixGroovy2CompilerPluginSpec = builder.build();
        ScriptCompilerPluginSpec mixByteCodeCompilerPluginSpec = buildByteCodeCompilerPluginSpec2(externalLibs);

        ScriptCompilerPluginSpec groovy2CompilerPluginSpec = buildGroovy2CompilerPluginSpec(externalLibs);
        ScriptCompilerPluginSpec byteCodeCompilerPluginSpec = buildByteCodeCompilerPluginSpec(externalLibs);

        // create and start the builder with the plugin
        return new ContainerModuleLoader.Builder()
                .addPluginSpec(mixGroovy2CompilerPluginSpec)
                .addPluginSpec(mixByteCodeCompilerPluginSpec)
                .addPluginSpec(groovy2CompilerPluginSpec)
                .addPluginSpec(byteCodeCompilerPluginSpec);
    }
//
//    public
//    static ContainerModuleLoader.Builder createContainerModuleLoaderBuilder2(Set<Path> externalLibs) {
//
//        ScriptCompilerPluginSpec.Builder builder = new ScriptCompilerPluginSpec.Builder(MixGroovy2Compiler.GROOVY2_COMPILER_ID)
//                .addRuntimeResource(ExampleResourceLocator.getGroovyRuntime())
//                .addRuntimeResource(getMixGroovy2PluginLocation(ExampleResourceLocator.class.getClassLoader()))
//                .addRuntimeResource(getMixByteCodeLoadingPluginPath())
//
//                        // hack to make the gradle build work. still doesn't seem to properly instrument the code
//                        // should probably add a classloader dependency on the system classloader instead
//                .addRuntimeResource(getCoberturaJar(ContainerUtils.class.getClassLoader()))
//                .withPluginClassName(MixGroovy2CompilerPlugin.class.getName());
//
//        // in version higher 0.2.6 of Nicobar should be added some useful methods, but now needs to iterate
//        // add run time .jar libs
//        if (!externalLibs.isEmpty()) {
//            for (Path path : externalLibs) {
//                builder.addRuntimeResource(path.toAbsolutePath());
//            }
//        }
//
//        ScriptCompilerPluginSpec mixGroovy2CompilerPluginSpec = builder.build();
//        ScriptCompilerPluginSpec mixByteCodeCompilerPluginSpec = buildByteCodeCompilerPluginSpec2(externalLibs);
//
//        // create and start the builder with the plugin
//        return new ContainerModuleLoader.Builder()
//                .addPluginSpec(mixGroovy2CompilerPluginSpec)
//                .addPluginSpec(mixByteCodeCompilerPluginSpec);
//    }

    public static Path getMixGroovy2PluginLocation(ClassLoader classLoader) {
        String resourceName = ClassPathUtils.classNameToResourceName("com.reversemind.nicobar.container.mix.compiler.MixGroovy2Compiler");
        Path path = ClassPathUtils.findRootPathForResource(resourceName, classLoader);
        if (path == null) {
            throw new IllegalStateException("coudln't find groovy2 plugin jar in the classpath.");
        }
        return path;
    }


    //
    // spock-core-0.7-groovy-2.0.jar needs to run inside Spock tests
    // .addRuntimeResource(Paths.get("src/test/resources/libs/spock-core-0.7-groovy-2.0.jar").toAbsolutePath())

    public
    static ContainerModuleLoader.Builder createContainerModuleLoaderBuilder(Set<Path> externalLibs) {

        ScriptCompilerPluginSpec groovy2CompilerPluginSpec = buildGroovy2CompilerPluginSpec(externalLibs);
        ScriptCompilerPluginSpec byteCodeCompilerPluginSpec = buildByteCodeCompilerPluginSpec(externalLibs);

        // create and start the builder with the plugin
        return new ContainerModuleLoader.Builder()
                .addPluginSpec(groovy2CompilerPluginSpec)
                .addPluginSpec(byteCodeCompilerPluginSpec);
    }

    public static ContainerModuleLoader createContainerModuleLoader(Set<Path> externalLibs) throws IOException, ModuleLoadException {

        ScriptCompilerPluginSpec groovy2CompilerPluginSpec = buildGroovy2CompilerPluginSpec(externalLibs);
        ScriptCompilerPluginSpec byteCodeCompilerPluginSpec = buildByteCodeCompilerPluginSpec(externalLibs);

        // create and start the builder with the plugin
        return new ContainerModuleLoader.Builder()
                .addPluginSpec(groovy2CompilerPluginSpec)
                .addPluginSpec(byteCodeCompilerPluginSpec)
                .build();
    }

    /**
     * Pack compiled class into .jar
     *
     * @param compiledClassesPath - path to compiled classes
     * @param pathToJar           - where to put a packet .jar file - path should exis
     * @param moduleId            - will be taken to construct a file name for jar file
     * @throws IOException
     * @throws BuildException
     */
    public
    static void packToJar(Path compiledClassesPath, Path pathToJar, ModuleId moduleId) throws IOException, BuildException {

        // use Ant jar builder
        Jar jar = new Jar();
        jar.setDestFile(pathToJar.toAbsolutePath().toFile());
        jar.setBasedir(compiledClassesPath.toAbsolutePath().toFile());

        // write default moduleSpec.json
        addDefaultModuleSpec(compiledClassesPath, moduleId);

        jar.setProject(new Project());
        jar.execute();
    }

    /**
     * Add default moduleSpec.json into compiled classes directory
     *
     * @param path
     * @param moduleId
     * @throws IOException
     */
    private static void addDefaultModuleSpec(Path path, ModuleId moduleId) throws IOException {
        final String json = DEFAULT_MODULE_SPEC_SERIALIZER.serialize(getDefaultScriptModuleSpec(moduleId));

        if (StringUtils.isBlank(json)) {
            return;
        }

        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(
                Paths.get(path.toAbsolutePath().toString(),
                        GsonScriptModuleSpecSerializer.DEFAULT_MODULE_SPEC_FILE_NAME).toAbsolutePath()
                        .toFile()
        )
        );

        bufferedWriter.write(json);
        bufferedWriter.flush();
        bufferedWriter.close();
    }

    protected static ScriptModuleSpec getDefaultScriptModuleSpec(ModuleId moduleId) {
        return new ScriptModuleSpec.Builder(moduleId)
                .addCompilerPluginId(MixBytecodeLoadingPlugin.PLUGIN_ID) // in this case we should not compile a content of jar file
                .build();
    }

    /**
     * ByteCode CompilerPluginSpec
     *
     * @param runTimeResourcesJar - set of paths to runtime .jar lib
     * @return
     */
    public static ScriptCompilerPluginSpec buildByteCodeCompilerPluginSpec(Set<Path> runTimeResourcesJar) {

        ScriptCompilerPluginSpec.Builder builder = new ScriptCompilerPluginSpec.Builder(BytecodeLoadingPlugin.PLUGIN_ID)

                .addRuntimeResource(ExampleResourceLocator.getGroovyRuntime())
                .addRuntimeResource(ExampleResourceLocator.getGroovyPluginLocation())
                .addRuntimeResource(getCoberturaJar(ContainerUtils.class.getClassLoader()))

                .addRuntimeResource(getByteCodeLoadingPluginPath())
                .withPluginClassName(BytecodeLoadingPlugin.class.getName());

        // in version higher 0.2.6 of Nicobar should be added some useful methods, but now needs to iterate
        // add run time .jar libs
        if (!runTimeResourcesJar.isEmpty()) {
            for (Path path : runTimeResourcesJar) {
                builder.addRuntimeResource(path.toAbsolutePath());
            }
        }

        return builder.build();
    }

    public static ScriptCompilerPluginSpec buildByteCodeCompilerPluginSpec2(Set<Path> runTimeResourcesJar) {

        ScriptCompilerPluginSpec.Builder builder = new ScriptCompilerPluginSpec.Builder(MixBytecodeLoadingPlugin.PLUGIN_ID)

                .addRuntimeResource(ExampleResourceLocator.getGroovyRuntime())
                .addRuntimeResource(getMixGroovy2PluginLocation(ExampleResourceLocator.class.getClassLoader()))
                .addRuntimeResource(getCoberturaJar(ContainerUtils.class.getClassLoader()))

                .addRuntimeResource(getMixByteCodeLoadingPluginPath())
                .withPluginClassName(MixBytecodeLoadingPlugin.class.getName());

        // in version higher 0.2.6 of Nicobar should be added some useful methods, but now needs to iterate
        // add run time .jar libs
        if (!runTimeResourcesJar.isEmpty()) {
            for (Path path : runTimeResourcesJar) {
                builder.addRuntimeResource(path.toAbsolutePath());
            }
        }

        return builder.build();
    }


    /**
     * Groovy2 CompilerPluginSpec
     *
     * @param runTimeResourcesJar - set of paths to runtime .jar lib
     * @return
     */
    public static ScriptCompilerPluginSpec buildGroovy2CompilerPluginSpec(Set<Path> runTimeResourcesJar) {
        // Groovy2CompilerPlugin

        // create the groovy plugin spec. this plugin specified a new module and classloader called "Groovy2Runtime"
        // which contains the groovy-all-n.n.n.jar
        ScriptCompilerPluginSpec.Builder builder = new ScriptCompilerPluginSpec.Builder(Groovy2Compiler.GROOVY2_COMPILER_ID)
                .addRuntimeResource(ExampleResourceLocator.getGroovyRuntime())
                .addRuntimeResource(ExampleResourceLocator.getGroovyPluginLocation())
                .addRuntimeResource(getByteCodeLoadingPluginPath())

                        // hack to make the gradle build work. still doesn't seem to properly instrument the code
                        // should probably add a classloader dependency on the system classloader instead
                .addRuntimeResource(getCoberturaJar(ContainerUtils.class.getClassLoader()))
                .withPluginClassName(Groovy2CompilerPlugin.class.getName());

        // in version higher 0.2.6 of Nicobar should be added some useful methods, but now needs to iterate
        // add run time .jar libs
        if (!runTimeResourcesJar.isEmpty()) {
            for (Path path : runTimeResourcesJar) {
                builder.addRuntimeResource(path.toAbsolutePath());
            }
        }

        return builder.build();
    }

    private static Path getByteCodeLoadingPluginPath() {
        String resourceName = ClassPathUtils.classNameToResourceName("com.netflix.nicobar.core.plugin.BytecodeLoadingPlugin");
        Path path = ClassPathUtils.findRootPathForResource(resourceName, ContainerUtils.class.getClassLoader());
        if (path == null) {
            throw new IllegalStateException("coudln't find BytecodeLoadingPlugin plugin jar in the classpath.");
        }
        return path;
    }

    private static Path getMixByteCodeLoadingPluginPath() {
        String resourceName = ClassPathUtils.classNameToResourceName("com.reversemind.nicobar.container.mix.plugin.MixBytecodeLoadingPlugin");
        Path path = ClassPathUtils.findRootPathForResource(resourceName, ContainerUtils.class.getClassLoader());
        if (path == null) {
            throw new IllegalStateException("coudln't find MixBytecodeLoadingPlugin plugin jar in the classpath.");
        }
        return path;
    }

    private static Path getCoberturaJar(ClassLoader classLoader) {
        return ClassPathUtils.findRootPathForResource("net/sourceforge/cobertura/coveragedata/HasBeenInstrumented.class", classLoader);
    }

    public static ScriptArchive getMixScriptArchiveAtPath(Path basePath, ModuleId moduleId) throws IOException {
        ScriptModuleSpec moduleSpec = new ScriptModuleSpec.Builder(moduleId)
                .addCompilerPluginId(MixBytecodeLoadingPlugin.PLUGIN_ID)
                .addCompilerPluginId(MixGroovy2CompilerPlugin.PLUGIN_ID)
                .build();

        ScriptArchive scriptArchive = new PathScriptArchive.Builder(getModulePath(basePath, moduleId).toAbsolutePath())
                .setRecurseRoot(true)
                .setModuleSpec(moduleSpec)
                .build();

        return scriptArchive;
    }

    public static ScriptArchive getScriptArchiveAtPath(Path basePath, ModuleId moduleId) throws IOException {
        ScriptModuleSpec moduleSpec = new ScriptModuleSpec.Builder(moduleId)
                .addCompilerPluginId(BytecodeLoadingPlugin.PLUGIN_ID)
                .addCompilerPluginId(Groovy2CompilerPlugin.PLUGIN_ID)
                .build();

        ScriptArchive scriptArchive = new PathScriptArchive.Builder(getModulePath(basePath, moduleId).toAbsolutePath())
                .setRecurseRoot(true)
                .setModuleSpec(moduleSpec)
                .build();

        return scriptArchive;
    }

    public static Path getModulePath(Path basePath, ModuleId moduleId) {
        if (!basePath.isAbsolute()) {
            throw new IllegalArgumentException("Base path should be absolute");
        }
        return basePath.resolve(moduleId.toString());
    }

    /**
     * Find all valid names of directories at basePath for ModuleId
     *
     * @param basePath
     * @return
     */
    public static Set<ModuleId> getModuleIdListAtPath(Path basePath) throws IOException {

        final int maxDepth = 1;

        final Set<String> directories = new HashSet<String>();

        Files.walkFileTree(basePath,
                EnumSet.of(FOLLOW_LINKS),
                maxDepth,
                new SimpleFileVisitor<Path>() {

                    private void addPath(Path directoryPath) {
                        if (directoryPath != null && directoryPath.toFile().isDirectory()) {
                            int index = directoryPath.getNameCount();
                            if (index > 0) {
                                Path name = directoryPath.getName(index - 1);
                                directories.add(name.toString());
                            }
                        }
                    }

                    @Override
                    public FileVisitResult visitFile(Path filePath, BasicFileAttributes attrs) throws IOException {
                        this.addPath(filePath);
                        return FileVisitResult.CONTINUE;
                    }

                }
        );

        if (!directories.isEmpty()) {

            Set<ModuleId> moduleIds = new HashSet<ModuleId>();

            for (String name : directories) {
                ModuleId _moduleId = null;
                try {
                    _moduleId = ModuleId.fromString(name);
                } catch (RuntimeException ex) {
                    ex.printStackTrace();
                }

                if (_moduleId != null) {
                    moduleIds.add(_moduleId);
                }
            }

            return moduleIds;
        }

        return new HashSet<ModuleId>();
    }

    public static void unJar(File source, File target, boolean overwrite) throws BuildException {
        Expand expand = new Expand();
        expand.setProject(new Project());
        expand.setDest(target);
        expand.setSrc(source);
        expand.setOverwrite(overwrite);
        expand.execute();
    }

    /**
     * Look inside a directory and find date(timestamp) of recently modified dir or file
     *
     * @param rootPath - path where to find any dir of a file date that was recently modified
     * @return - a time stamp
     * @throws IOException -
     */
    public static Long recentlyModifyDate(Path rootPath) throws IOException {
        if (rootPath == null) {
            return null;
        }

        if (!rootPath.toFile().exists()) {
            // TODO logging
            System.out.println("File is not exist:" + rootPath.toString());
            return null;
        }

        ComparePathDate compareDate = new ComparePathDate();
        Files.walkFileTree(rootPath.toAbsolutePath(), compareDate);

        return compareDate.getCompareDate();
    }

    /**
     * Helper class
     */
    private static class ComparePathDate extends SimpleFileVisitor<Path> {

        Long compareDate = 0L;

        private void process(Path path) {
            File file = path.toFile();
            if (compareDate <= file.lastModified()) {
                compareDate = file.lastModified();
            }
        }

        public Long getCompareDate() {
            return compareDate;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path directory, IOException exc) throws IOException {
            process(directory);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            process(file);
            return FileVisitResult.CONTINUE;
        }
    }

    /**
     * Need to make a description for business logic - how to process compiled and src script directories
     *
     * @param classesAtPath -
     * @param sourcesAtPath -
     * @return - Pair<Set<ModuleId>, Set<ModuleId>>
     */
    @SuppressWarnings("unchecked")
    public static Pair<Set<ModuleId>, Set<ModuleId>> getModuleToLoadAndCompile(Path classesAtPath, Path sourcesAtPath) throws IOException {
        final Set<ModuleId> EMPTY = new HashSet<>();

        if(sourcesAtPath == null){
            return Pair.create(EMPTY, EMPTY);
        }

        if(!sourcesAtPath.toFile().exists()){
            return Pair.create(EMPTY, EMPTY);
        }

        Set<ModuleId> whatToLoad = new HashSet<>();
        Set<ModuleId> whatToCompile = new HashSet<>();

        Set<ModuleId> allClassesModulesSet = getModuleIdListAtPath(classesAtPath);
        Set<ModuleId> allSrcModulesSet = getModuleIdListAtPath(sourcesAtPath);


        if(allClassesModulesSet.isEmpty()){
            whatToCompile = allSrcModulesSet;
        }else {

            whatToLoad = allClassesModulesSet;
            whatToCompile = allSrcModulesSet;

            Map<ModuleId, Date> allMapFromClasses = getRecentlyModificationDate(allClassesModulesSet, classesAtPath);
            Map<ModuleId, Date> allMapFromSrc = getRecentlyModificationDate(allSrcModulesSet, sourcesAtPath);

            // #1 detect difference in classes and src
            Set<ModuleId> whatNeedToRemoveForLoadingFromClasses = new HashSet<>(Sets.difference(allClassesModulesSet, allSrcModulesSet));
            Set<ModuleId> whatNeedToCompileInAnyWay = new HashSet<>(Sets.difference(allSrcModulesSet, allClassesModulesSet));

            if (!whatNeedToRemoveForLoadingFromClasses.isEmpty()) {
                for (ModuleId _moduleId : whatNeedToRemoveForLoadingFromClasses) {
                    whatToLoad.remove(_moduleId);
                }
            }

            if (!whatNeedToCompileInAnyWay.isEmpty()) {
                whatToCompile = whatNeedToCompileInAnyWay;
            }

            Iterator<ModuleId> iterator = whatToLoad.iterator();
            while(iterator.hasNext()){
                ModuleId _moduleId = iterator.next();

                Date classDate = allMapFromClasses.get(_moduleId);
                Date srcDate = allMapFromSrc.get(_moduleId);

                if (classDate == null) {
                    iterator.remove();
                } else {
                    if (srcDate != null) {
                        if (classDate.getTime() < srcDate.getTime()) {
                            iterator.remove();
                        }
                    } else {
                        // it means that allMapFromSrc does not contains a _moduleId
                        // so means that we should not LOAD it FROM CLASSES
                        iterator.remove();
                    }
                }
            }
        }

        return Pair.create(whatToLoad, whatToCompile);
    }

    public static Map<ModuleId, Date> getRecentlyModificationDate(Set<ModuleId> moduleIdSet, Path path) throws IOException {
        if (path == null) {
            return new HashMap<ModuleId, Date>();
        }

        if (!path.toFile().exists()) {
            return new HashMap<ModuleId, Date>();
        }

        if (moduleIdSet.isEmpty()) {
            return new HashMap<ModuleId, Date>();
        }

        Map<ModuleId, Date> map = new HashMap<ModuleId, Date>();

        for (ModuleId moduleId : moduleIdSet) {
            Pair<ModuleId, Date> _pair = getRecentlyModificationDate(moduleId, path);

            if (_pair != null && _pair.getT1() != null && _pair.getT2() != null) {
                // t1 - moduleId, t2 - date
                map.put(_pair.getT1(), _pair.getT2());
            }
        }

        return map;
    }

    public static Pair<ModuleId, Date> getRecentlyModificationDate(ModuleId moduleId, Path path) throws IOException {
        if (path == null) {
            return Pair.create(null, null);
        }

        if (!path.toFile().exists()) {
            return Pair.create(null, null);
        }

        if (moduleId == null) {
            return Pair.create(null, null);
        }

        Long timestamp = ContainerUtils.recentlyModifyDate(path.resolve(moduleId.toString()));

        if (timestamp == null) {
            return Pair.create(null, null);
        }

        return Pair.create(moduleId, new Date(timestamp));
    }

    public static class Pair<T1, T2> {

        private final T1 t1;
        private final T2 t2;

        public Pair(T1 t1, T2 t2) {
            this.t1 = t1;
            this.t2 = t2;
        }

        public T1 getT1() {
            return t1;
        }

        public T2 getT2() {
            return t2;
        }

        public static <T1, T2> Pair<T1, T2> create(T1 t1, T2 t2) {
            return new Pair<T1, T2>(t1, t2);
        }

        @Override
        public String toString() {
            return "Pair{" +
                    "t1=" + t1 +
                    ", t2=" + t2 +
                    '}';
        }
    }

}
