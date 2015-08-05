package com.other.package10

class OtherScript {

    def static String generate() {
        return " HI - from another package " + this.getClass().getCanonicalName()
    }

}
