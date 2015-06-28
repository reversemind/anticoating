package com.reversemind.nicobar

import com.netflix.nicobar.core.archive.ModuleId
import spock.lang.Specification

import java.nio.file.Paths

/**
 *
 */
class ModuleIdExperimentsTest extends Specification{

    def 'module id from string'(){
        setup:
        println "moduleId:" + ModuleId.fromString('module-name.V-SNAPSHOT')
        println "moduleId:" + ModuleId.create('name','1_0_0-SNAPSHOT')
        println "moduleId:" + ModuleId.create('name','1.0.0-SNAPSHOT'.replaceAll('\\.', '_'))

        println "paths:" + Paths.get('src/main/resources/module/sublevel/ScriptHelper.groovy').toAbsolutePath()
    }
}
