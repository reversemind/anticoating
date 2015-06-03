package com.company;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/**
 *
 */
public class FileUtils {

    public static List<Path> getFileListInDirectoryByExtension(final String path, boolean isRecursively, final String extension) throws IOException {
        if (StringUtils.isBlank(path)) {
            return new ArrayList<Path>(0);
        }

        if (StringUtils.isBlank(extension)) {
            return new ArrayList<Path>(0);
        }

        int depths = 1;
        if (isRecursively) {
            depths = Integer.MIN_VALUE;
        }
        FileVisitorByExtension fileVisitorByExtension = new FileVisitorByExtension(extension);
        Files.walkFileTree(Paths.get(path), new HashSet<FileVisitOption>(Arrays.asList(FileVisitOption.FOLLOW_LINKS)), depths, fileVisitorByExtension);
        return fileVisitorByExtension.getPathList();
    }

}
