#include <iostream>
#include <math.h>

#include <stdio.h>
#include <stdlib.h>
#include <malloc.h>

#include <time.h>
#include <vector>
#include <fstream>
#include <algorithm>
#include <string>

using namespace std;

struct ExtDuration{
	long startedAt;
	long spendMs;
	long spendMicro;
	long spendNano;
};

ExtDuration extractDuration(string _string){
//ExtDuration extractDuration(char *_string){

	size_t pos = _string.find("Enter to main method at ms:") + string("Enter to main method at ms:").length();

	string mainMethodStartTime = _string.substr(pos);

	ExtDuration duration;
	duration.startedAt = std::stol(mainMethodStartTime);

	return duration;
}
int main(int argc, char *argv[]) {

	cout << "Start measurement\n" << argv[1];

	int nLoop = 5;

	cout.setf(ios_base::fixed,ios_base::floatfield);
	cout.precision(10);

	struct timespec tps, tpe, globalStart, globalEnd;

	clock_gettime(CLOCK_REALTIME, &globalStart);

	for(int i=0; i<nLoop; i++ ){

			FILE* fp;

    		char __result [2000];

		clock_gettime(CLOCK_REALTIME, &tps);
		long currentTime = tps.tv_sec*1000 + (tps.tv_nsec / 1000000.0);
		long processStartTime = currentTime;
		cout << "\n\n-------------------------------------------------\nStart JVM at " << currentTime << " ms\n\n";

		// system("java -server -d64 -jar -Xcomp -XX:-TieredCompilation -XX:CICompilerCount=1 -XX:CompileThreshold=120000 build/libs/jvm-jit-parameters-0.1.jar");
		// system(argv[1]);

    	fp = popen(argv[1],"r");

		clock_gettime(CLOCK_REALTIME, &tpe);

		currentTime = tpe.tv_sec*1000 + (tpe.tv_nsec / 1000000.0);
		long processEndTime = currentTime;
		cout << "\nEnd JVM at " << currentTime << " ms\n-------------------------------------------------\n";

		    	fread(__result,1,sizeof(__result),fp);

				cout << "size:" << sizeof(__result) << "\n";

				string _str = string(__result);
				cout << "string length is:" << _str.length() << "\n";

		    	fclose (fp);
            	cout << "string:'" << _str << "'\n";

				ExtDuration extDuration = extractDuration(_str);
            	cout << " position:\n" << extDuration.startedAt << "\n";

		cout << "\n---deltas:\n";
		cout << (extDuration.startedAt - processStartTime) << " ms compilation time\n";
		cout << (processEndTime - processStartTime) << " ms compilation+execution time\n";

	}
	clock_gettime(CLOCK_REALTIME, &globalEnd);

	double startResult = globalStart.tv_sec*1000 + (globalStart.tv_nsec / 1000000.0);
	double endResult = globalEnd.tv_sec*1000 + (globalEnd.tv_nsec / 1000000.0);

	cout << "\n\nAverage spent time:" << (endResult - startResult) / 5.0  << " ms \n";

	cout << "OK!" << "\n";

	return 0;
}





