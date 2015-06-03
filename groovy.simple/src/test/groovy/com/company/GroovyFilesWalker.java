package com.company;

import bsh.StringUtil;
import org.apache.commons.io.DirectoryWalker;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.AndFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 *
 */
public class GroovyFilesWalker extends DirectoryWalker {

    private static IOFileFilter filter = new AndFileFilter(FileFilterUtils.fileFileFilter(),
            new SuffixFileFilter("groovy", IOCase.INSENSITIVE));

    private String rootPath;

    public GroovyFilesWalker(String rootPath, boolean isRecursive) {
        super(filter, isRecursive ? Integer.MAX_VALUE : 1);
        if (StringUtils.isBlank(rootPath)) {
            throw new IllegalArgumentException("Root path could not be an empty");
        }
        this.rootPath = rootPath;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void handleFile(final File file, final int depth, final Collection results) throws IOException {
        results.add(file);
    }

    public List<File> getFiles() {
        List<File> files = new ArrayList<File>();

        File directory = new File(this.rootPath);

        if (!directory.exists()) {
            throw new IllegalArgumentException("Path is not exist:" + this.rootPath);
        }

        try {
            walk(directory, files);
        } catch (IOException e) {
//            log.error("Problem finding configuration files!", e);
        }

        return files;
    }
}
