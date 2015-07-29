package com.reversemind.nicobar.container;

import com.netflix.nicobar.core.archive.ModuleId;

/**
 *
 */
public interface IScriptContainerListener {
    void changed(ModuleId moduleId);
}
