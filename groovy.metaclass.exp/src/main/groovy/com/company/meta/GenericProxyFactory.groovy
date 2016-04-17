package com.company.meta


import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 *
 */
public class GenericProxyFactory {
    public static <T> T getProxy(Class<T> interfaces,
                                 final T object) {
        return (T)
        Proxy.newProxyInstance(object.getClass().getClassLoader(),
//                new Class({interfaces}),
                {interfaces},
                new InvocationHandler() {
                    public Object invoke(Object proxy, Method method,
                                         Object[] args) throws Throwable {
                        println "method:" + method.getName()

                        return method.invoke(object, args);
                    }
                });
    }
}