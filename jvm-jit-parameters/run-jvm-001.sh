#!/bin/bash

# disable JIT
#java -client -Xint -d64 -jar -XX:+TieredCompilation build/libs/jvm-jit-parameters-0.1.jar

#java -client -d64 -jar -XX:+TieredCompilation build/libs/jvm-jit-parameters-0.1.jar

#java -client -d64 -jar -XX:+TieredCompilation build/libs/jvm-jit-parameters-0.1.jar



#java -client -d64 -jar -Xcomp -XX:+TieredCompilation build/libs/jvm-jit-parameters-0.1.jar
#java -client -d64 -jar -Xcomp -XX:-TieredCompilation -XX:+UnlockDiagnosticVMOptions -XX:+LogCompilation build/libs/jvm-jit-parameters-0.1.jar
#java -client -d64 -jar -Xcomp -XX:-TieredCompilation -XX:+UnlockDiagnosticVMOptions -XX:+PrintCompilation build/libs/jvm-jit-parameters-0.1.jar


#java -client -d64 -jar -Xcomp -XX:-TieredCompilation -XX:CompileThreshold=120000 build/libs/jvm-jit-parameters-0.1.jar
#java -client -d64 -jar -XX:CompileThreshold=1 build/libs/jvm-jit-parameters-0.1.jar

#java -client -d64 -jar -showversion -Xcomp -XX:+TieredCompilation -XX:TieredStopAtLevel=9 build/libs/jvm-jit-parameters-0.1.jar

# minimize a start time
# good result
#java -server -d64 -jar -showversion -Xcomp -XX:-TieredCompilation build/libs/jvm-jit-parameters-0.1.jar

# slower
#java -server -d64 -jar -showversion -Xmixed -XX:-TieredCompilation build/libs/jvm-jit-parameters-0.1.jar



# steps two
startTime=$(date +%s.%N)
#java -server -d64 -jar -showversion -Xcomp -XX:-TieredCompilation -XX:CICompilerCount=1 -XX:CompileThreshold=120000 build/libs/jvm-jit-parameters-0.1.jar
nnn=5
for (( i=0; i<$nnn; i++ ))
#do java -server -d64 -jar -showversion -Xcomp -XX:+UseParallelGC -XX:+AggressiveOpts -XX:-BackgroundCompilation -XX:-TieredCompilation -XX:CICompilerCount=1 build/libs/jvm-jit-parameters-0.1.jar;
#do java -server -d64 -jar -Xcomp -XX:+UseParallelGC -XX:+AggressiveOpts -XX:-BackgroundCompilation -XX:-TieredCompilation -XX:CICompilerCount=1 build/libs/jvm-jit-parameters-0.1.jar;
do java -server -d64 -jar -Xcomp -XX:+UseParallelGC -XX:CICompilerCount=2 build/libs/jvm-jit-parameters-0.1.jar;
#do ./mjit
done
duration=$(echo "$(date +%s.%N) - $startTime" | bc)
result=$(bc <<< "scale=6;$duration/$nnn")
printf "\nCompile and run time is %.6f seconds\n" $result

