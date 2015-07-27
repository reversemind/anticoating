package com.reversemind.nicobar;

import java.nio.file.Path;

/**
 *
 */
public interface IPathWatcher {
    void pathChanged();
    void setPath(Path watchPath);
}
