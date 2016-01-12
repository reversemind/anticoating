#!/bin/bash


# default parameters
#./mjit "java -jar build/libs/jvm-jit-parameters-0.1.jar"

#    ====================================================================================
#    JVM params:
#    java -jar build/libs/jvm-jit-parameters-0.1.jar
#
#    Compile:
#    - average:48.6000000000 ms
#    - standart deviation:1.3564659966 ms
#
#    Algo execution:
#    - average:4.2000000000 ms
#    - standart deviation:0.4000000000 ms
#
#    All time:
#    - average:54.7000000000 ms
#    - standart deviation:1.2688577540 ms
#    ====================================================================================


# server is ON
#./mjit "java -server -d64 -jar build/libs/jvm-jit-parameters-0.1.jar"

#    ====================================================================================
#    JVM params:
#    java -server -d64 -jar build/libs/jvm-jit-parameters-0.1.jar
#
#    Compile:
#    - average:48.2000000000 ms
#    - standart deviation:0.6000000000 ms
#
#    Algo execution:
#    - average:4.4000000000 ms
#    - standart deviation:0.4898979486 ms
#
#    All time:
#    - average:54.6000000000 ms
#    - standart deviation:0.4898979486 ms
#    ====================================================================================


# client is ON
#./mjit "java -client -jar build/libs/jvm-jit-parameters-0.1.jar"

#    ====================================================================================
#    JVM params:
#    java -client -jar build/libs/jvm-jit-parameters-0.1.jar
#
#    Compile:
#    - average:48.0000000000 ms
#    - standart deviation:1.0000000000 ms
#
#    Algo execution:
#    - average:4.3000000000 ms
#    - standart deviation:0.4582575695 ms
#
#    All time:
#    - average:54.5000000000 ms
#    - standart deviation:1.0246950766 ms
#    ====================================================================================




# fast start & slow execution
#./mjit "java -server -d64 -jar -XX:+TieredCompilation build/libs/jvm-jit-parameters-0.1.jar"

#    =====================================================================================================
#    JVM params:
#    java -server -d64 -jar -XX:+TieredCompilation build/libs/jvm-jit-parameters-0.1.jar
#
#    Compile:
#    - average:48.636 ms
#    - standart deviation:0.881 ms
#
#    Algo execution:
#    - average:4.182 ms
#    - standart deviation:0.386 ms
#
#    All time:
#    - average:54.727 ms
#    - standart deviation:0.862 ms
#    =====================================================================================================






# slow start compilation & fast execution
#./mjit "java -server -d64 -jar -Xcomp -XX:-TieredCompilation -XX:CICompilerCount=1 build/libs/jvm-jit-parameters-0.1.jar"

#    =====================================================================================================
#    JVM params:
#    java -server -d64 -jar -Xcomp -XX:-TieredCompilation -XX:CICompilerCount=1 build/libs/jvm-jit-parameters-0.1.jar
#
#    Compile:
#    - average:1021.273 ms
#    - standart deviation:2.700 ms
#
#    Algo execution:
#    - average:0.455 ms
#    - standart deviation:0.498 ms
#
#    All time:
#    - average:1083.364 ms
#    - standart deviation:2.869 ms
#    =======================================================================================================





# slow start compilation with N threads & fast execution
#./mjit "java -server -d64 -jar -Xcomp -XX:-TieredCompilation -XX:CICompilerCount=10 build/libs/jvm-jit-parameters-0.1.jar"


#    =====================================================================================================
#    JVM params:
#    java -server -d64 -jar -Xcomp -XX:-TieredCompilation -XX:CICompilerCount=10 build/libs/jvm-jit-parameters-0.1.jar
#
#    Compile:
#    - average:1085.909 ms
#    - standart deviation:2.999 ms
#
#    Algo execution:
#    - average:0.455 ms
#    - standart deviation:0.498 ms
#
#    All time:
#    - average:1151.545 ms
#    - standart deviation:2.808 ms
#    =====================================================================================================



# other fatures
#./mjit "java -server -d64 -jar -Xcomp -XX:CICompilerCount=2 -XX:CompileThreshold=100 build/libs/jvm-jit-parameters-0.1.jar"

#    =====================================================================================================
#    JVM params:
#    java -server -d64 -jar -Xcomp -XX:CICompilerCount=2 -XX:CompileThreshold=100 build/libs/jvm-jit-parameters-0.1.jar
#
#    Compile:
#    - average:1061.909 ms
#    - standart deviation:2.539 ms
#
#    Algo execution:
#    - average:2.273 ms
#    - standart deviation:0.445 ms
#
#    All time:
#    - average:1141.727 ms
#    - standart deviation:2.733 ms
#    =====================================================================================================


# 10.000
#./mjit "java -server -d64 -jar -Xcomp -XX:CICompilerCount=2 -XX:CompileThreshold=10000 build/libs/jvm-jit-parameters-0.1.jar"

#    =====================================================================================================
#    JVM params:
#    java -server -d64 -jar -Xcomp -XX:CICompilerCount=2 -XX:CompileThreshold=10000 build/libs/jvm-jit-parameters-0.1.jar
#
#    Compile:
#    - average:1065.000 ms
#    - standart deviation:4.285 ms
#
#    Algo execution:
#    - average:2.364 ms
#    - standart deviation:0.481 ms
#
#    All time:
#    - average:1145.091 ms
#    - standart deviation:4.399 ms
#    =====================================================================================================





# 100.000
#./mjit "java -server -d64 -jar -Xcomp -XX:-TieredCompilation  -XX:CICompilerCount=2 -XX:CompileThreshold=100000 build/libs/jvm-jit-parameters-0.1.jar"

#=====================================================================================================
#JVM params:
#java -server -d64 -jar -Xcomp -XX:-TieredCompilation  -XX:CICompilerCount=2 -XX:CompileThreshold=100000 build/libs/jvm-jit-parameters-0.1.jar
#
#Compile:
#- average:1079.909 ms
#- standart deviation:1.564 ms
#
#Algo execution:
#- average:0.455 ms
#- standart deviation:0.498 ms
#
#All time:
#- average:1145.545 ms
#- standart deviation:1.437 ms
#=====================================================================================================


# 100
./mjit "java -server -d64 -jar -Xcomp -XX:-TieredCompilation -XX:CICompilerCount=1 -XX:CompileThreshold=100 build/libs/jvm-jit-parameters-0.1.jar"

#    =====================================================================================================
#    JVM params:
#    java -server -d64 -jar -Xcomp -XX:-TieredCompilation -XX:CICompilerCount=2 -XX:CompileThreshold=100 build/libs/jvm-jit-parameters-0.1.jar
#
#    Compile:
#    - average:1090.000 ms
#    - standart deviation:23.332 ms
#
#    Algo execution:
#    - average:0.364 ms
#    - standart deviation:0.481 ms
#
#    All time:
#    - average:1156.727 ms
#    - standart deviation:25.930 ms
#    =====================================================================================================
