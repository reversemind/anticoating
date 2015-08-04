package com.reversemind.nicobar.container;

import com.netflix.nicobar.core.compile.ScriptArchiveCompiler;
import com.netflix.nicobar.core.plugin.ScriptCompilerPlugin;
import com.netflix.nicobar.groovy2.internal.compile.Groovy2Compiler;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class Groovy2MultiCompilerPlugin implements ScriptCompilerPlugin {

    public static final String PLUGIN_ID = "groovy2multi";

    public Groovy2MultiCompilerPlugin() {
    }

    @Override
    public Set<? extends ScriptArchiveCompiler> getCompilers(Map<String, Object> compilerParams) {
        return Collections.singleton(new Groovy2MultiCompiler(compilerParams));
    }
}
