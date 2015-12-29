#include <iostream>
#include <math.h>

#include <stdio.h>
#include <stdlib.h>
#include <malloc.h>

#include <time.h>
#include <vector>
#include <fstream>
#include <algorithm>

using namespace std;

int main() {

	cout << "Start measurement\n";

	int nLoop = 5;


	cout.setf(ios_base::fixed,ios_base::floatfield);
	cout.precision(10);

	clock_t beginTime = clock();

	for(int i=0; i<nLoop; i++ ){
		system("java -server -d64 -jar -showversion -Xcomp -XX:-TieredCompilation -XX:CICompilerCount=1 -XX:CompileThreshold=120000 build/libs/jvm-jit-parameters-0.1.jar");
//		system("java -server -d64 -jar -showversion -XX:-TieredCompilation -XX:CICompilerCount=1 -XX:CompileThreshold=120000 build/libs/jvm-jit-parameters-0.1.jar");
//		system("java -server -d64 -jar -showversion build/libs/jvm-jit-parameters-0.1.jar");
	}

	clock_t endTime = clock();

	cout << "\n\nSpend time:" << ((endTime-beginTime)*(double)CLOCKS_PER_SEC/1000000.0)  << "\n";

	cout << "OK!" << "\n";

	return 0;
}