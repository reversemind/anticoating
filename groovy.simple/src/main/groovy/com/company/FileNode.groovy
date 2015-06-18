package com.company

import java.nio.file.FileSystems
import java.nio.file.Path

/**
 * // TODO maybe this competence of class that much closer to Script
 */
class FileNode extends File{

    private Path basePath;
    private String relativePathFileName;

    FileNode(Path basePath, Path relativePathFileName){
        super(basePath?.toString(), relativePathFileName.toString())
    }

    FileNode(String parent, String child) {
        super(FileSystems.getDefault().getPath(parent).toString(), child)
        this.basePath = FileSystems.getDefault().getPath(parent)
        this.relativePathFileName = child
    }

    FileNode(File parent, String child) {
        super(parent, child)
        this.basePath = FileSystems.getDefault().getPath(parent.getAbsolutePath())
        this.relativePathFileName = child
    }

    Path getBasePath() {
        return basePath
    }

    String getRelativePathFileName() {
        return relativePathFileName
    }
}
