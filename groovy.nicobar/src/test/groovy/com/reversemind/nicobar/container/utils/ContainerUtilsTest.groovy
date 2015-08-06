package com.reversemind.nicobar.container.utils

import com.netflix.nicobar.core.archive.ModuleId
import groovy.util.logging.Slf4j
import spock.lang.Specification

import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

import static java.nio.file.FileVisitOption.FOLLOW_LINKS;

/**
 *
 */
@Slf4j
class ContainerUtilsTest extends Specification{

    def 'ged module id list for path'(){
        setup:
        log.info "setup:"

        Path BASE_PATH = Paths.get("/opt/dev/github/reversemind/anticoating/groovy.nicobar/src/test/resources/base-path-build-module-src-plus-jar/classes").toAbsolutePath();

        println "set:" + getModuleIdListAtPath(BASE_PATH)

        when:
        log.info "when:"

        then:
        log.info "then:"
    }

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
                        addPath(filePath);
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

}
