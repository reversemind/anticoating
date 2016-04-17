package com.company.meta.script

import com.company.meta.inter.InterfaceSimple
import com.company.meta.Component

println "component result:" + component.process()

def v = Component.of(InterfaceSimple.class)
println "interface result:" + v.getValue("1", "2")
