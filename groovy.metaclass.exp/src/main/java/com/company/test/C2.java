package com.company.test;

/**
 *
 */
public class C2 {
    public <T> T of(Class<T> clazz) throws IllegalAccessException, InstantiationException {
        return clazz.newInstance();
    }
}
