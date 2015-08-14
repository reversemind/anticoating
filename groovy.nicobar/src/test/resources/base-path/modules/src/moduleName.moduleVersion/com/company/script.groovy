package com.company

import com.company.subpackage1.*

println "Date 1:|" + ScriptHelper2.getTime() + "| sublevel1:" + Subpackage1Class.method1() + "|" + Thread.currentThread().getName() + "|" + new Date().getTime()
