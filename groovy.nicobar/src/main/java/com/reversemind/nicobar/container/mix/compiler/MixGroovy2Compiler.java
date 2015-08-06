package com.reversemind.nicobar.container.mix.compiler;

import com.netflix.nicobar.core.archive.ScriptArchive;
import com.netflix.nicobar.core.compile.ScriptArchiveCompiler;
import com.netflix.nicobar.core.compile.ScriptCompilationException;
import com.netflix.nicobar.core.module.jboss.JBossModuleClassLoader;
import com.reversemind.nicobar.container.mix.MixGroovy2CompilerHelper;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.CompilationCustomizer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/**
 * Mix - 'cause it is possible to put dependency of .jar & .class at level of .groovy scripts
 */
public class MixGroovy2Compiler implements ScriptArchiveCompiler {

    public final static String GROOVY2_COMPILER_ID = "mix.groovy2";
    public final static String GROOVY2_COMPILER_PARAMS_CUSTOMIZERS = "customizerClassNames";

    private List<String> customizerClassNames = new LinkedList<String>();

    public MixGroovy2Compiler(Map<String, Object> compilerParams) {
        this.processCompilerParams(compilerParams);
    }

    private void processCompilerParams(Map<String, Object> compilerParams) {

        // filtering compilation customizers class names
        if (compilerParams.containsKey(GROOVY2_COMPILER_PARAMS_CUSTOMIZERS)) {
            Object customizers = compilerParams.get(GROOVY2_COMPILER_PARAMS_CUSTOMIZERS);

            if (customizers instanceof List) {
                for (Object customizerClassName: (List<?>) customizers) {
                    if (customizerClassName instanceof String) {
                        this.customizerClassNames.add((String)customizerClassName);
                    }
                }
            }
        }
    }

    private CompilationCustomizer getCustomizerInstanceFromString(String className, JBossModuleClassLoader moduleClassLoader) {
        CompilationCustomizer instance = null;

        try {
            // TODO: replace JBossModuleClassLoader with generic class loader
            ClassLoader classLoader = moduleClassLoader != null ?
                    moduleClassLoader : Thread.currentThread().getContextClassLoader();

            Class<?> klass = classLoader.loadClass(className);
            instance = (CompilationCustomizer)klass.newInstance();
        }
        catch (InstantiationException | IllegalAccessException | ClassNotFoundException | ClassCastException e) {
            e.printStackTrace();
            // TODO: add logger support for compiler (due to a separate class loader logger is not visible)
        }
        return instance;
    }

    @Override
    public boolean shouldCompile(ScriptArchive archive) {
        return archive.getModuleSpec().getCompilerPluginIds().contains(GROOVY2_COMPILER_ID);
    }

    @Override
    public Set<Class<?>> compile(ScriptArchive archive, JBossModuleClassLoader moduleClassLoader, Path compilationRootDir)
            throws ScriptCompilationException, IOException {

        List<CompilationCustomizer> customizers = new LinkedList<CompilationCustomizer>();

        for (String klassName: this.customizerClassNames) {
            CompilationCustomizer instance = this.getCustomizerInstanceFromString(klassName, moduleClassLoader);
            if (instance != null ) {
                customizers.add(instance);
            }
        }

        CompilerConfiguration config = new CompilerConfiguration(CompilerConfiguration.DEFAULT);
        config.getScriptExtensions().add("class");
        config.getScriptExtensions().add("jar");

        config.addCompilationCustomizers(customizers.toArray(new CompilationCustomizer[0]));

        new MixGroovy2CompilerHelper(compilationRootDir)
                .addScriptArchive(archive)
                .withParentClassloader(moduleClassLoader) // TODO: replace JBossModuleClassLoader with generic class loader
                .withConfiguration(config)
                .compile();
        return Collections.emptySet();
    }
}
