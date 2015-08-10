package com.reversemind.nicobar.container.utils

import com.google.common.collect.Sets
import com.netflix.nicobar.core.archive.ModuleId
import com.reversemind.nicobar.container.TestHelper
import groovy.util.logging.Slf4j
import spock.lang.Specification

import java.nio.file.Path
import java.nio.file.Paths

/**
 *
 */
@Slf4j
class DetectWhatToLoadWhatToRecompileTest extends Specification{

    def 'detect what to load what to recompile'(){
        setup:
        log.info "setup:"

        TestHelper.mixCompilationOfModule()

        Path BASE_PATH = Paths.get("src/test/resources/base-path-build-module-src-plus-jar").toAbsolutePath();

        Path srcAtPath = Paths.get(BASE_PATH.toString(), "src" ).toAbsolutePath()
        Path classesAtPath = Paths.get(BASE_PATH.toString(), "classes" ).toAbsolutePath()




        when:
        log.info "when:"

        Set<ModuleId> whatToLoad = new HashSet<>();
        Set<ModuleId> whatToCompile = new HashSet<>();



        Set<ModuleId> allClassesModulesSet = ContainerUtils.getModuleIdListAtPath(classesAtPath);
        Set<ModuleId> allSrcModulesSet = ContainerUtils.getModuleIdListAtPath(srcAtPath);


        if(allClassesModulesSet.isEmpty()){
            whatToCompile = allSrcModulesSet;
        }else{

            whatToLoad = allClassesModulesSet;
            whatToCompile = allSrcModulesSet;


            Map<ModuleId, Date> allMapFromClasses = getRecentlyModificationDate(allClassesModulesSet, classesAtPath);
            Map<ModuleId, Date> allMapFromSrc = getRecentlyModificationDate(allSrcModulesSet, srcAtPath);
            log.info "classes map:" + allMapFromClasses
            log.info "src map:" + allMapFromSrc




            // #1 detect difference in classes and src
            Set<ModuleId> whatNeedToRemoveForLoadingFromClasses = new HashSet<>(Sets.difference(allClassesModulesSet, allSrcModulesSet));
            Set<ModuleId> whatNeedToCompileInAnyWay = new HashSet<>(Sets.difference(allSrcModulesSet, allClassesModulesSet));

            log.info "whatNeedToRemoveForLoadingFromClasses:" + whatNeedToRemoveForLoadingFromClasses
            log.info "whatNeedToCompileInAnyWay:" + whatNeedToCompileInAnyWay


            if (!whatNeedToRemoveForLoadingFromClasses.isEmpty()) {
                for (ModuleId _moduleId : whatNeedToRemoveForLoadingFromClasses) {
                    whatToLoad.remove(_moduleId);
                }
            }

            if(!whatNeedToCompileInAnyWay.isEmpty()){
                whatToCompile = new HashSet<>(whatNeedToCompileInAnyWay);
            }

            // #2 detect what is more early
            for (ModuleId _moduleId : whatToLoad) {
                Date classDate = allMapFromClasses.get(_moduleId);
                Date srcDate = allMapFromSrc.get(_moduleId);

                if (classDate == null) {
                    whatToLoad.remove(_moduleId)
                } else {
                    if (srcDate != null) {
                        if (classDate < srcDate) {
                            whatToLoad.remove(_moduleId)
                        }
                    } else {
                        // it means that allMapFromSrc does not contains a _moduleId
                        // so means that we should not LOAD it FROM CLASSES
                        whatToLoad.remove(_moduleId);
                    }
                }
            }


            // #3 let's have a look - what need to load & what need to recompile

            log.info "FINALLY whatToLoad:" + whatToLoad
            log.info "FINALLY whatToCompile:" + whatToCompile

        }

        then:
        log.info "then:"

        log.info "src modules:" + allClassesModulesSet
        log.info "classes modules:" + allSrcModulesSet

        whatToLoad.size() == 1
        whatToCompile.size() == 1

        whatToLoad.contains(ModuleId.fromString("moduleName.moduleVersion"))
        whatToCompile.contains(ModuleId.fromString("moduleNameNoInClasses.moduleVersionNoInClasses"))

    }

    def 'detect what to load what to recompile for the one hop'(){
        setup:
        log.info "setup:"

        TestHelper.mixCompilationOfModule()

        Path BASE_PATH = Paths.get("src/test/resources/base-path-build-module-src-plus-jar").toAbsolutePath();

        Path srcAtPath = Paths.get(BASE_PATH.toString(), "src" ).toAbsolutePath()
        Path classesAtPath = Paths.get(BASE_PATH.toString(), "classes" ).toAbsolutePath()


        when:
        log.info "when:"

        Pair<Set<ModuleId>, Set<ModuleId>> pair = getModuleToLoadAndCompile(classesAtPath, srcAtPath);

        Set<ModuleId> whatToLoad = pair.getT1();
        Set<ModuleId> whatToCompile = pair.getT2();

        then:
        log.info "then:"

        log.info "classes modules:" + whatToLoad
        log.info "src modules:" + whatToCompile

        whatToLoad.size() == 1
        whatToCompile.size() == 1

        whatToLoad.contains(ModuleId.fromString("moduleName.moduleVersion"))
        whatToCompile.contains(ModuleId.fromString("moduleNameNoInClasses.moduleVersionNoInClasses"))

    }

    /**
     * Need to make a description for business logic - how to process compiled and src script directories
     *
     * @param classesAtPath -
     * @param sourcesAtPath -
     * @return - Pair<Set<ModuleId>, Set<ModuleId>>
     */
    public static Pair<Set<ModuleId>, Set<ModuleId>> getModuleToLoadAndCompile(Path classesAtPath, Path sourcesAtPath){
        if(sourcesAtPath == null){
            return Pair.create(new HashSet<ModuleId>(), new HashSet<ModuleId>());
        }

        if(!sourcesAtPath.toFile().exists()){
            return Pair.create(new HashSet<ModuleId>(), new HashSet<ModuleId>());
        }

        Set<ModuleId> whatToLoad = new HashSet<>();
        Set<ModuleId> whatToCompile = new HashSet<>();

        Set<ModuleId> allClassesModulesSet = ContainerUtils.getModuleIdListAtPath(classesAtPath);
        Set<ModuleId> allSrcModulesSet = ContainerUtils.getModuleIdListAtPath(sourcesAtPath);


        if(allClassesModulesSet.isEmpty()){
            whatToCompile = allSrcModulesSet;
        }else {

            whatToLoad = allClassesModulesSet;
            whatToCompile = allSrcModulesSet;

            Map<ModuleId, Date> allMapFromClasses = getRecentlyModificationDate(allClassesModulesSet, classesAtPath);
            Map<ModuleId, Date> allMapFromSrc = getRecentlyModificationDate(allSrcModulesSet, sourcesAtPath);
            log.info "classes map:" + allMapFromClasses
            log.info "src map:" + allMapFromSrc

            // #1 detect difference in classes and src
            Set<ModuleId> whatNeedToRemoveForLoadingFromClasses = new HashSet<>(Sets.difference(allClassesModulesSet, allSrcModulesSet));
            Set<ModuleId> whatNeedToCompileInAnyWay = new HashSet<>(Sets.difference(allSrcModulesSet, allClassesModulesSet));

            log.info "whatNeedToRemoveForLoadingFromClasses:" + whatNeedToRemoveForLoadingFromClasses
            log.info "whatNeedToCompileInAnyWay:" + whatNeedToCompileInAnyWay


            if (!whatNeedToRemoveForLoadingFromClasses.isEmpty()) {
                for (ModuleId _moduleId : whatNeedToRemoveForLoadingFromClasses) {
                    whatToLoad.remove(_moduleId);
                }
            }

            if (!whatNeedToCompileInAnyWay.isEmpty()) {
                whatToCompile = whatNeedToCompileInAnyWay;
            }

            // #2 detect what is more early
            for (ModuleId _moduleId : whatToLoad) {
                Date classDate = allMapFromClasses.get(_moduleId);
                Date srcDate = allMapFromSrc.get(_moduleId);

                if (classDate == null) {
                    whatToLoad.remove(_moduleId)
                } else {
                    if (srcDate != null) {
                        if (classDate < srcDate) {
                            whatToLoad.remove(_moduleId)
                        }
                    } else {
                        // it means that allMapFromSrc does not contains a _moduleId
                        // so means that we should not LOAD it FROM CLASSES
                        whatToLoad.remove(_moduleId);
                    }
                }
            }
        }

        return Pair.create(whatToLoad, whatToCompile);
    }

    private static Map<ModuleId, Date> getRecentlyModificationDate(Set<ModuleId> moduleIdSet, Path path) {
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
                map.put(_pair.getT1(), _pair.getT2())
            }
        }

        return map;
    }

    private static Pair<ModuleId, Date> getRecentlyModificationDate(ModuleId moduleId, Path path) {
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

        return Pair.create(moduleId, new Date(timestamp))
    }

}
