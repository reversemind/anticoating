package com.reversemind.nicobar.container.plugin;

import com.netflix.nicobar.core.compile.ScriptArchiveCompiler;
import com.netflix.nicobar.core.internal.compile.BytecodeLoader;
import com.netflix.nicobar.core.plugin.ScriptCompilerPlugin;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class BytecodeMultiLoadingPlugin implements ScriptCompilerPlugin {

    public static final String PLUGIN_ID = "bytecodemulti";

    @Override
    public Set<? extends ScriptArchiveCompiler> getCompilers(Map<String, Object> compilerParams) {
        return Collections.singleton(new BytecodeLoaderMulti());
    }
}
