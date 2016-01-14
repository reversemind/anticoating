package com.test.loop;

import java.util.concurrent.TimeUnit;

public class SimpleLoop {

    public static void main(String... args) {
        System.out.println("\nEnter to main method at ms:" + System.currentTimeMillis());

        long start = System.currentTimeMillis();
        long startNano = System.nanoTime();
        fakeLoop();
        long endNano = System.nanoTime();
        long end = System.currentTimeMillis();

        System.out.println("Time ms:" + (end - start));
        System.out.println("Time micros:" + (TimeUnit.NANOSECONDS.toMicros(endNano) - TimeUnit.NANOSECONDS.toMicros(startNano)));
        System.out.println("Time nanos :" + (endNano - startNano) + "\n");
    }

    private static void fakeLoop() {
        for (int i = 0; i < 200000000; i++) {
            emptyLoop();
        }
    }

    private static void emptyLoop() {
        for (int i = 0; i < 200000000; i++) {
        }
    }
}
