#!/bin/bash

g++ -O0 -g3 -Wall -c -fmessage-length=0 -MMD -MP -MF"src/main/c++/mjit.d" -MT"src/main/c++/mjit.d" -o"src/main/c++/mjit.o" "src/main/c++/mjit.cpp"

g++  -o"mjit"  ./src/main/c++/mjit.o
