package com.company.meta;

import com.company.meta.inter.InterfaceSimple;
import groovy.lang.Binding;
import groovy.util.GroovyScriptEngine;

import java.net.URL;

/**
 *
 */
public class Application2 {

    public static void main(String... args) {
        InterfaceSimple simple = Component.of(InterfaceSimple.class);
        System.out.println(simple.getValue("1", "2"));
    }
}
