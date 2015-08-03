package com.reversemind.nicobar.container;

import com.netflix.nicobar.core.archive.ModuleId;

import java.util.Set;

/**
 *
 */
public interface IContainerListener {
    void changed(Set<ModuleId> moduleIdSet);
}
