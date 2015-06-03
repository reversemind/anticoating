package com.company;

import org.apache.commons.lang3.StringUtils;

import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * JDK7+ Features
 */
public class FileVisitorByExtension extends SimpleFileVisitor<Path> {

    private List<Path> pathList;
    private String suffix;

    public FileVisitorByExtension(String suffix) {
        this.pathList = new ArrayList<Path>();

        this.suffix = "";
        if (StringUtils.isNotBlank(suffix)) {
            this.suffix = suffix.toLowerCase();
        }
    }

    public List<Path> getPathList() {
        return pathList;
    }

    /**
     * @param file
     * @param attr
     * @return
     */
    @Override
    public FileVisitResult visitFile(Path file,
                                     BasicFileAttributes attr) {

        System.out.println("1111111111111");
        if (!attr.isDirectory() && file.toString().toLowerCase().endsWith(this.suffix)) {
            this.pathList.add(file);
        }
        return FileVisitResult.CONTINUE;
    }
}
