package com.company

import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Path

import static groovy.io.FileType.FILES
/**
 * Utils for files
 */
public class FileUtils {

    private static FileSystem fileSystem = FileSystems.getDefault()

    private static AntBuilder antBuilder = new AntBuilder()

    def
    static FileSystem getFileSystem() {
        return fileSystem;
    }

    def
    static Set<String> getRelativePathFileNames(List<FileNode> fileList) {
        if (!fileList) {
            return new HashSet<String>(0);
        }
        return new HashSet<String>(fileList.each {}*.relativePathFileName);
    }

    def
    static Set<String> getFileNames(List<? extends File> fileList) {
        return new HashSet<String>(fileList.each {}*.name);
    }

    def
    static List<? extends File> sortByLastModified(List<? extends File> fileList) {
        return fileList.sort { f1, f2 -> f2.lastModified() <=> f1.lastModified() }
    }

    def
    static List<FileNode> getGroovyFilesInDirectories(String[] directories, boolean isRecursively) {
        return getFilesByExtensionInDirectories(directories, '.groovy', isRecursively);
    }

    def
    static List<FileNode> getFilesByExtensionInDirectories(String[] directories, String extension, boolean isRecursively) {
        if (!directories) {
            return new ArrayList<FileNode>(0);
        }

        List<FileNode> _list = new ArrayList<FileNode>();
        directories.each { directory ->
            getFilesByExtensionInDirectory(directory, extension, isRecursively).each { file ->
                _list << new FileNode(directory, relativizePath(directory, file.getAbsolutePath()))
            }
        }
        return _list;
    }

    def
    static List<FileNode> getFilesByExtensionInDirectory(String directoryPath, String extension, boolean isRecursively) {
        List<FileNode> fileList = new ArrayList<FileNode>();

        if (!directoryPath) {
            return fileList
        }

        if (!new File(directoryPath).exists()) {
            return fileList
        }

        String _extension = extension == null ? "" : extension;

        if (isRecursively) {
            new File(directoryPath).eachFileRecurse(FILES) { file ->
                if (file.name.endsWith(_extension)) {
                    fileList << new FileNode(directoryPath, relativizePath(directoryPath, file.getAbsolutePath()))
                }
            }
        } else {
            new File(directoryPath).eachFile(FILES) { file ->
                if (file.name.endsWith(_extension)) {
                    fileList << new FileNode(directoryPath, relativizePath(directoryPath, file.getAbsolutePath()))
                }
            }
        }
        return fileList;
    }

    def
    static String zeroNumber(final int signCount, final int number) {
        def _tmp = "" + number
        (signCount - _tmp.length()).times {
            _tmp = "0" + _tmp
        }
        return _tmp
    }

    /**
     *
     * @param parentAbsolutePath - Should be absolute /tmp/subdir
     * @param fileAbsolutePath - /tmp/subdir/otherdir/level01/file.txt
     * @return - otherdir/level01/file.txt
     */
    def
    static String relativizePath(String parentAbsolutePath, String fileAbsolutePath) {
        if (parentAbsolutePath == null || fileAbsolutePath == null) {
            throw new NullPointerException("Parent path or file path is null")
        }

        if (parentAbsolutePath.length() == 0 || fileAbsolutePath.length() == 0) {
            throw new IllegalArgumentException("Parent path or file path is empty")
        }

        Path parentPath = fileSystem.getPath(parentAbsolutePath).toAbsolutePath();
        Path filePath = fileSystem.getPath(fileAbsolutePath).toAbsolutePath();
        return parentPath.relativize(fileSystem.getPath(filePath.toString()))
    }

    def
    static boolean isAbsolute(String pathToFile){
        return fileSystem.getPath(pathToFile).isAbsolute();
    }

    def
    static void replaceContentInFile(String fileName, String whatReplace, String replaceBy) {
        if (new File(fileName).exists()) {
            antBuilder.replace(file: fileName, token: whatReplace, value: replaceBy)
        }
    }

    def
    static void replaceContentInFile(String fileName, String replaceByContent) {
        if (new File(fileName).exists()) {
            RandomAccessFile randomAccessFile = new RandomAccessFile(new File(fileName), "rw");
            randomAccessFile.setLength(0);
            randomAccessFile.close();

            new File(fileName) << replaceByContent
        }
    }
}
