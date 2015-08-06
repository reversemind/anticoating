package com.reversemind.nicobar.container.mix.plugin;

import com.netflix.nicobar.core.compile.ScriptArchiveCompiler;
import com.netflix.nicobar.core.plugin.ScriptCompilerPlugin;
import com.reversemind.nicobar.container.mix.compiler.MixBytecodeLoader;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class MixBytecodeLoadingPlugin implements ScriptCompilerPlugin {

    public static final String PLUGIN_ID = "mix.bytecode";

    @Override
    public Set<? extends ScriptArchiveCompiler> getCompilers(Map<String, Object> compilerParams) {
        return Collections.singleton(new MixBytecodeLoader());
    }
}
