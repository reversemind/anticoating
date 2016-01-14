#!/bin/bash

# default parameters
./mjit "build/libs/jvm-jit-parameters-0.1.jar"

#    =====================================================================================================
#    JVM params:
#    java -jar build/libs/jvm-jit-parameters-0.1.jar
#
#    Compile:
#    - average:53.364 ms
#    - standart deviation:3.675 ms
#
#    Algo execution:
#    - average:4.364 ms
#    - standart deviation:0.643 ms
#
#    Algo+stdout execution:
#    - average:6.818 ms
#    - standart deviation:0.716 ms
#
#    All time:
#    - average:60.182 ms
#    - standart deviation:3.688 ms
#    =====================================================================================================
#    ;53.364;4.364;6.818;60.182;



# server is ON
#./mjit "-server -d64" "build/libs/jvm-jit-parameters-0.1.jar"
#
#    =====================================================================================================
#    JVM params:
#    -server -d64
#
#    Compile:
#    - average:50.455 ms
#    - standart deviation:1.924 ms
#
#    Algo execution:
#    - average:4.455 ms
#    - standart deviation:0.498 ms
#
#    Algo+stdout execution:
#    - average:6.636 ms
#    - standart deviation:0.881 ms
#
#    All time:
#    - average:57.091 ms
#    - standart deviation:1.975 ms
#    =====================================================================================================
#    -server -d64;50.455;4.455;6.636;57.091;




# client is ON
#./mjit "-client" "build/libs/jvm-jit-parameters-0.1.jar"

#    =====================================================================================================
#    JVM params:
#    -client
#
#    Compile:
#    - average:49.727 ms
#    - standart deviation:1.052 ms
#
#    Algo execution:
#    - average:4.364 ms
#    - standart deviation:0.643 ms
#
#    Algo+stdout execution:
#    - average:6.455 ms
#    - standart deviation:0.498 ms
#
#    All time:
#    - average:56.182 ms
#    - standart deviation:0.936 ms
#    =====================================================================================================
#    -client;49.727;4.364;6.455;56.182;



# fast start & slow execution
#./mjit "-server -d64 -XX:+TieredCompilation" "build/libs/jvm-jit-parameters-0.1.jar"

#    =====================================================================================================
#    JVM params:
#    -server -d64 -XX:+TieredCompilation
#
#    Compile:
#    - average:48.182 ms
#    - standart deviation:1.192 ms
#
#    Algo execution:
#    - average:4.000 ms
#    - standart deviation:0.000 ms
#
#    Algo+stdout execution:
#    - average:6.455 ms
#    - standart deviation:0.498 ms
#
#    All time:
#    - average:54.636 ms
#    - standart deviation:1.367 ms
#    =====================================================================================================
#    -server -d64 -XX:+TieredCompilation;48.182;4.000;6.455;54.636;




# slow start compilation & fast execution
#./mjit "-server -d64 -Xcomp -XX:+TieredCompilation" "build/libs/jvm-jit-parameters-0.1.jar"

#    =====================================================================================================
#    JVM params:
#    -server -d64 -Xcomp -XX:+TieredCompilation
#
#    Compile:
#    - average:1139.909 ms
#    - standart deviation:28.640 ms
#
#    Algo execution:
#    - average:2.727 ms
#    - standart deviation:0.617 ms
#
#    Algo+stdout execution:
#    - average:85.364 ms
#    - standart deviation:2.772 ms
#
#    All time:
#    - average:1225.273 ms
#    - standart deviation:29.949 ms
#    =====================================================================================================
#    -server -d64 -Xcomp -XX:+TieredCompilation;1139.909;2.727;85.364;1225.273;



#
#./mjit " -server -d64 -Xcomp -XX:-TieredCompilation" "build/libs/jvm-jit-parameters-0.1.jar"

#    =====================================================================================================
#    JVM params:
#     -server -d64 -Xcomp -XX:-TieredCompilation
#
#    Compile:
#    - average:1099.273 ms
#    - standart deviation:22.255 ms
#
#    Algo execution:
#    - average:0.364 ms
#    - standart deviation:0.481 ms
#
#    Algo+stdout execution:
#    - average:67.273 ms
#    - standart deviation:1.863 ms
#
#    All time:
#    - average:1166.545 ms
#    - standart deviation:22.661 ms
#    =====================================================================================================
#     -server -d64 -Xcomp -XX:-TieredCompilation;1099.273;0.364;67.273;1166.545;




#./mjit " -server -d64 -Xcomp -XX:+TieredCompilation -XX:CICompilerCount=10" "build/libs/jvm-jit-parameters-0.1.jar"

#    =====================================================================================================
#    JVM params:
#     -server -d64 -Xcomp -XX:+TieredCompilation -XX:CICompilerCount=10
#
#    Compile:
#    - average:1101.818 ms
#    - standart deviation:12.755 ms
#
#    Algo execution:
#    - average:2.636 ms
#    - standart deviation:0.481 ms
#
#    Algo+stdout execution:
#    - average:83.091 ms
#    - standart deviation:1.676 ms
#
#    All time:
#    - average:1184.909 ms
#    - standart deviation:13.083 ms
#    =====================================================================================================
#     -server -d64 -Xcomp -XX:+TieredCompilation -XX:CICompilerCount=10;1101.818;2.636;83.091;1184.909;










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
#./mjit "java -server -d64 -jar -Xcomp -XX:-TieredCompilation -XX:CICompilerCount=1 -XX:CompileThreshold=100 build/libs/jvm-jit-parameters-0.1.jar"

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




#;52.000;4.455;6.455;51.909;
#-client;51.909;4.364;6.455;51.909;
#-server -d64;51.909;4.273;6.727;52.455;
#-server -d64 -XX:+TieredCompilation;52.455;4.455;6.727;1106.545;
#-server -d64 -Xcomp -XX:+TieredCompilation;1106.545;2.818;82.727;1088.545;
#-server -d64 -Xcomp -XX:-TieredCompilation;1088.545;0.455;65.364;1132.909;
#-server -d64 -Xcomp -XX:+TieredCompilation -XX:CICompilerCount=10;1132.909;2.636;85.364;1087.545;
#-server -d64 -jar -Xcomp -XX:CICompilerCount=2 -XX:CompileThreshold=100;1087.545;2.636;81.364;1168.909;