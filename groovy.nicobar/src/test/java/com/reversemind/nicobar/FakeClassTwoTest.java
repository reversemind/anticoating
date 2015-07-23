package com.reversemind.nicobar;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Jar;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.*;

/**
 *
 */
public class FakeClassTwoTest {

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testJar(){

        String targetName = Paths.get("src/main/resources").toAbsolutePath().toString() + File.separator + "module_result.jar";
        String compiledClassesPath = Paths.get("src/main/resources/compileTo").toAbsolutePath().toString();

        Jar jar = new Jar();
        jar.setDestFile(new File(targetName));
        jar.setBasedir(new File(compiledClassesPath));
        jar.setProject(new Project());
        jar.setIncludes("package2/*");
        jar.execute();
    }
}