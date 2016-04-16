package com.company.meta

/**
 *
 */
class Component {
    String version
    Object payload

    String process() {
        return version + "|" + payload
    }
}
