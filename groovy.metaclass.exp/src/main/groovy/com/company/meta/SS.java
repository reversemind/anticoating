package com.company.meta;

import com.company.meta.inter.InterfaceSimple;

/**
 *
 */
public class SS implements InterfaceSimple {

    @Override
    public String getValue(String number, String code) {
        return "fffffffffffffffffffffffff:" + number + " = " + code;
    }
}