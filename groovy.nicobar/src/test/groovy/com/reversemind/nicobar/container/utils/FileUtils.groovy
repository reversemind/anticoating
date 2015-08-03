package com.reversemind.nicobar.container.utils

import java.nio.file.FileSystem
import java.nio.file.FileSystems

/**
 * Utils for files
 */
public class FileUtils {

    private static FileSystem fileSystem = FileSystems.getDefault()

    private static AntBuilder antBuilder = new AntBuilder()

    def
    static void replaceContentInFile(String fileName, String whatReplace, String replaceBy) {
        if (new File(fileName).exists()) {
            antBuilder.replace(file: fileName, token: whatReplace, value: replaceBy)
        }
    }

    def
    static void replaceContentInFile(String fileName, String replaceByContent) {
        if (!new File(fileName).exists()) {
            new File(fileName).createNewFile()
        }

        if (new File(fileName).exists()) {
            RandomAccessFile randomAccessFile = new RandomAccessFile(new File(fileName), "rw");
            randomAccessFile.setLength(0);
            randomAccessFile.close();

            new File(fileName) << replaceByContent
        }
    }
}
