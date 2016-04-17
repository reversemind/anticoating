package com.company.meta

import com.company.meta.inter.InterfaceSimple

/**
 *
 */
class Component {
    String version
    Object payload
    private Class<?>[] classes

    String process() {
        return version + "|" + payload
    }

    void register(Class<?>[] classes) {
        this.classes = classes
    }

    public static <T> T of(Class<T> clazz) {
        return (T) GenericProxyFactory.getProxy(clazz, (T)new SS())
    }

}
