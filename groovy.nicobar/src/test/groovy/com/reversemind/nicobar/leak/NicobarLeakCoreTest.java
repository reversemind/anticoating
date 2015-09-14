package com.reversemind.nicobar.leak;

import com.netflix.nicobar.core.archive.JarScriptArchive;
import com.netflix.nicobar.core.archive.ModuleId;
import com.netflix.nicobar.core.archive.ScriptArchive;
import com.netflix.nicobar.core.archive.ScriptModuleSpec;
import com.netflix.nicobar.core.module.jboss.JBossModuleLoader;
import com.netflix.nicobar.core.module.jboss.JBossModuleUtils;
import org.jboss.modules.*;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *
 */
public class NicobarLeakCoreTest {

    @Test
    public void testJarResourcesMulti() throws Exception {
        Path jarPath = Paths.get("src/test/resources/libs/precompiled.jar").toAbsolutePath();

        ScriptArchive jarScriptArchive = new JarScriptArchive.Builder(jarPath)
                .setModuleSpec(new ScriptModuleSpec.Builder(ModuleId.create("precompiled"))
                        .build())
                .build();

        ExecutorService containerCaller = Executors.newFixedThreadPool(20);
        while (true) {
            containerCaller.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        ModuleIdentifier revisionId = JBossModuleUtils.createRevisionId(ModuleId.create("precompiled"), 1);
                        ModuleSpec.Builder moduleSpecBuilder = ModuleSpec.build(revisionId);
                        JBossModuleLoader moduleLoader = new JBossModuleLoader();
                        JBossModuleUtils.populateModuleSpecWithCoreDependencies(moduleSpecBuilder, jarScriptArchive);
                        JBossModuleUtils.populateModuleSpecWithResources(moduleSpecBuilder, jarScriptArchive);

                        moduleLoader.addModuleSpec(moduleSpecBuilder.create());
                        Module module = moduleLoader.loadModule(revisionId);
                        ModuleClassLoader moduleClassLoader = module.getClassLoader();

                        Set<String> actualPaths = getResourcePaths(moduleClassLoader);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });
            Thread.sleep(10);
        }

    }

    private static Set<String> getResourcePaths(ModuleClassLoader moduleClassLoader) {
        Set<String> result = new HashSet<String>();
        Iterator<Resource> resources = moduleClassLoader.iterateResources("", true);
        while (resources.hasNext()) {
            Resource resource = resources.next();
            result.add(resource.getName());
        }
        return result;
    }

}
