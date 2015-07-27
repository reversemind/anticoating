package com.reversemind.nicobar;

import com.netflix.nicobar.core.archive.ModuleId;

/**
 *
 */
public interface IScriptContainerListener {
    void changed(ModuleId moduleId);
}
