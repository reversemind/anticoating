package com.reversemind.nicobar;

import com.netflix.nicobar.core.archive.ModuleId;

import java.nio.file.Path;

/**
 * Listen for subpath for any changes and fire an event
 */
public class PathWatcher implements IPathWatcher {

    private Path watchPath;
    private IScriptContainerListener scriptContainerListener;
    private ModuleId moduleId;

    public PathWatcher(ModuleId moduleId, Path watchPath, IScriptContainerListener scriptContainerListener){
        this.watchPath = watchPath;
        this.scriptContainerListener = scriptContainerListener;
        this.moduleId = moduleId;
    }

    @Override
    public void pathChanged() {
        System.out.println("Path changed");
        scriptContainerListener.changed(moduleId);
    }

    @Override
    public void setPath(Path watchPath) {

    }

}
