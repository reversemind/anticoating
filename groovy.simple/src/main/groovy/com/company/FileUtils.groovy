package com.company

import groovy.io.FileType

/**
 * Util files
 */
public class FileUtils {

    def
    static List<File> getGroovyFilesInDirectories(String[] directories, boolean isRecursively) {
        return getFilesByExtensionInDirectories(directories, '.groovy', isRecursively);
    }

    def
    static List<File> getFilesByExtensionInDirectories(String[] directories, String extension, boolean isRecursively) {
        if (!directories) {
            return new ArrayList<File>(0);
        }
        List<File> _list = new ArrayList<File>();
        directories.each { directory ->
            getFilesByExtensionInDirectory(directory, extension, isRecursively).each { file ->
                _list.add(file)
            }
        }
        return _list;
    }

    def
    static List<File> getFilesByExtensionInDirectory(String directoryPath, String extension, boolean isRecursively) {
        List<File> fileList = new ArrayList<File>();
        if (isRecursively) {
            new File(directoryPath).eachFileRecurse(FileType.FILES) { file ->
                if (file.name.endsWith(extension)) {
                    fileList.add file
                }
            }
        } else {
            new File(directoryPath).eachFile(FileType.FILES) { file ->
                if (file.name.endsWith(extension)) {
                    fileList.add file
                }
            }
        }
        return fileList;
    }

}
