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
for i in {1..5};
do java -server -d64 -jar -showversion -Xcomp -XX:-TieredCompilation -XX:CICompilerCount=2 build/libs/jvm-jit-parameters-0.1.jar;
done
duration=$(echo "$(date +%s.%N) - $startTime" | bc)
ff=$(echo "$duration / 5")
printf "\nCompilation time: %.6f seconds\n" $ff

