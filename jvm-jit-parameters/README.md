# Measurement of time for different compilation parameters

1. >./build.sh
2. >./run.sh
на выходе result.csv файл

Тестируемое приложение
src/main/java/com/test/loop/SimpleLoop.java

Измерение времени выполнения SimpleLoop.java 
src/main/c++/mjit.cpp


### mjit args

1. path to jar file
2. path to jvm parameters - look for example jvm.parameters.txt
2. path to result.csv file


### result.csv description 

T1 - время компиляции JVM
T2 - время выполнения ТОЛЬКО алгоритма - inside JVM
T3 - время выполнения алгоритма + затраты на stdout
T4 - общее время - компиляция + выполнение алгоритма + затраты на stdout

T1 + T3 = T4

Проводилось в 11 измерений по каждому .
Для каждого T вычыслялось среднеее + СКО в мс.

Первый столбец - использованные JVM параметры. Если строка с параметрами пустая, значит все значения дефолтные.
 
Измерения проводились на системе:

Architecture:          x86_64
CPU op-mode(s):        32-bit, 64-bit
Byte Order:            Little Endian
CPU(s):                4
On-line CPU(s) list:   0-3
Thread(s) per core:    1
Core(s) per socket:    4
Socket(s):             1
NUMA node(s):          1
Vendor ID:             GenuineIntel
CPU family:            6
Model:                 60
Model name:            Intel(R) Core(TM) i5-4590 CPU @ 3.30GHz
Stepping:              3
CPU MHz:               3364.710
CPU max MHz:           3700.0000
CPU min MHz:           800.0000
BogoMIPS:              6599.90
Virtualization:        VT-x
L1d cache:             32K
L1i cache:             32K
L2 cache:              256K
L3 cache:              6144K
NUMA node0 CPU(s):     0-3


### JVM parameters


#### Tiered Compilation / -XX:+TieredCompilation

В Java 8 (-XX:+TieredCompilation) дефолтный параметр, вперые появился в Java 7.

-XX:+TieredCompilation - дает JVM время старта равное при опции -client.

{quote}
http://docs.oracle.com/javase/8/docs/technotes/guides/vm/performance-enhancements-7.html#tieredcompilation.

Tiered compilation, introduced in Java SE 7, brings client startup speeds to the server VM. Normally, a server VM uses the interpreter to collect profiling information about methods that is fed into the compiler. In the tiered scheme, in addition to the interpreter, the client compiler is used to generate compiled versions of methods that collect profiling information about themselves. Since the compiled code is substantially faster than the interpreter, the program executes with greater performance during the profiling phase. In many cases, a startup that is even faster than with the client VM can be achieved because the final code produced by the server compiler may be already available during the early stages of application initialization. The tiered scheme can also achieve better peak performance than a regular server VM because the faster profiling phase allows a longer period of profiling, which may yield better optimization.

Tiered compilation is the default mode for the server VM. Both 32 and 64 bit modes are supported, as well as compressed oops (see the next section). Use the -XX:-TieredCompilation flag with the java command to disable tiered compilation.
{quote}



#### Some usefull JIT info:
http://docs.oracle.com/javase/8/docs/technotes/tools/unix/java.html#BABCBGHF
http://www.slideshare.net/CharlesNutter/redev-2011-jvm-jit-for-dummies-what-the-jvm-does-with-your-bytecode-when-youre-not-looking
http://www.beyondjava.net/blog/a-close-look-at-javas-jit-dont-waste-your-time-on-local-optimizations/
http://docs.oracle.com/cd/E15289_01/doc.40/e15058/underst_jit.htm
http://hg.openjdk.java.net/jdk8/jdk8/hotspot/file/104743074675/src/share/vm/runtime/advancedThresholdPolicy.hpp
http://java-performance.com/
https://github.com/lampepfl/dotty/issues/222
http://ispyker.blogspot.ru/2015/07/java-8-tiered-compilation-big-pro-and.html
http://stackoverflow.com/questions/3779339/java-jvm-hotspot-is-there-a-way-to-save-jit-performance-gains-at-compile-time
http://www.infoq.com/presentations/self-heal-scalable-system
http://www.slideshare.net/maddocig/tiered
http://middlewaresnippets.blogspot.ru/2014/11/java-virtual-machine-code-generation.html
http://jpbempel.blogspot.ru/2013/04/compilethreshold-is-relative.html