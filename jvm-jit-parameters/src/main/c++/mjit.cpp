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
	// look at com.test.loop.SimpleLoop.java
	size_t pos = _string.find("Enter to main method at ms:") + string("Enter to main method at ms:").length();
	string mainMethodStartTime = _string.substr(pos);

	//
	pos = _string.find("Time ms:") + string("Time ms:").length();
	string executionMs = _string.substr(pos);

	ExtDuration duration;
	duration.startedAt = std::stol(mainMethodStartTime);
	duration.spendMs = std::stol(executionMs);
	return duration;
}

double sko(long *arr, int n){
	double average = 0.0;

	for(int i=0; i<n; i++ ){
		average += (double)arr[i];
	}

	average = average / (n * 1.0);

	double _sko = 0.0;
	for(int i=0; i<n; i++ ){
		_sko += ((double)arr[i] - average) * ((double)arr[i] - average);
	}

	return sqrt(_sko / ( n * 1.0));
}

double average(long *arr, int n){
	double average = 0.0;

	for(int i=0; i<n; i++ ){
		average += (double)arr[i];
	}

	return average / (n * 1.0);
}

int main(int argc, char *argv[]) {

	cout << "Start measurement\n" << argv[1];

	int nLoop = 11;

	long arrayT1[nLoop]; // системное время старта JVM
	long arrayT2[nLoop]; // системное время попадания в метод main Java приложения
	long arrayT3[nLoop]; // системное время окончания работы JVM
	long arrayT3_[nLoop]; // mks выполнение алгоритма

	cout.setf(ios_base::fixed,ios_base::floatfield);
	cout.precision(3);

	struct timespec tps, tpe;

	for(int i=0; i<nLoop; i++ ){

			FILE* fp;

    	char __result [2000];

		clock_gettime(CLOCK_REALTIME, &tps);
		long currentTime = tps.tv_sec*1000 + (tps.tv_nsec / 1000000.0);
		long processStartTime = currentTime;

		arrayT1[i] = currentTime;
		cout << "\n\n-------------------------------------------------\nStart JVM at " << currentTime << " ms\n\n";

		// system("java -server -d64 -jar -Xcomp -XX:-TieredCompilation -XX:CICompilerCount=1 -XX:CompileThreshold=120000 build/libs/jvm-jit-parameters-0.1.jar");
		// system(argv[1]);

    	fp = popen(argv[1],"r");

//		char buffer[2000];
//		while (fgets(buffer, 2000, fp) != NULL)
//		  {
//			printf(buffer);
//		  }

		fread(__result,1,sizeof(__result),fp);


		clock_gettime(CLOCK_REALTIME, &tpe);

		currentTime = tpe.tv_sec*1000 + (tpe.tv_nsec / 1000000.0);
		long processEndTime = currentTime;
		arrayT3[i] = currentTime;
		cout << "\nEnd JVM at " << currentTime << " ms\n-------------------------------------------------\n";





		string _str = string(__result);
		fclose (fp);
		cout << "string:'" << _str << "'\n";

		ExtDuration extDuration = extractDuration(_str);
		cout << " duration startedAt:\n" << extDuration.startedAt << "\n";
		cout << " duration spendMs:\n" << extDuration.spendMs << "\n";
		arrayT2[i] = extDuration.startedAt;
		arrayT3_[i] = extDuration.spendMs;

		cout << "\n---deltas:\n";
		cout << (extDuration.startedAt - processStartTime) << " ms compilation time\n";
		cout << (processEndTime - processStartTime) << " ms compilation+execution time\n";


	}// nLoop


	cout << "\n\n\n\n\n\n=============================\n";

	double averageT1 = 0.0;
	double averageT2 = 0.0;
	double averageT3 = 0.0;
	double averageT3_ = 0.0;

	for(int i=0; i<nLoop; i++ ){
		averageT1 += arrayT1[i];
		averageT2 += arrayT2[i];
		averageT3 += arrayT3[i];
		averageT3_ += arrayT3_[i];
	}

	averageT1 = averageT1 / (nLoop * 1.0);
	averageT2 = averageT2 / (nLoop * 1.0);
	averageT3 = averageT3 / (nLoop * 1.0);
	averageT3_ = averageT3_ / (nLoop * 1.0);





	double skoT1 = 0.0;
	double skoT2 = 0.0;
	double skoT3 = 0.0;
	double skoT3_ = 0.0;

	for(int i=0; i<nLoop; i++ ){
		skoT1 += (arrayT1[i] - averageT1) * (arrayT1[i] - averageT1);
		skoT2 += (arrayT2[i] - averageT2) * (arrayT2[i] - averageT2);
		skoT3 += (arrayT3[i] - averageT3) * (arrayT3[i] - averageT3);
		skoT3_ += (arrayT3_[i] - averageT3_) * (arrayT3_[i] - averageT3_);
	}

	skoT1 = sqrt(skoT1 / (nLoop * 1.0));
	skoT2 = sqrt(skoT2 / (nLoop * 1.0));
	skoT3 = sqrt(skoT3 / (nLoop * 1.0));
	skoT3_ = sqrt(skoT3_ / (nLoop * 1.0));




	long arraydT2T1[nLoop];
	long arraydT3T2[nLoop];
	long arraydT3T1[nLoop];
	long arraydT3_T2[nLoop];

	for(int i=0; i<nLoop; i++ ){
		long dT2T1; // время компиляции JVM
    	long dT3T2; // время выполнения алгоритма + stdout
    	long dT3_T2; // время выполнения ТОЛЬКО алгоритма - inside JVM
    	long dT3T1; // общее время - компиляция + выполнение алгоритма

    	dT2T1 = arrayT2[i] - arrayT1[i];
    	dT3T2 = arrayT3[i] - arrayT2[i];
    	dT3T1 = arrayT3[i] - arrayT1[i];

		dT3_T2 = arrayT3_[i];

		arraydT2T1[i] = dT2T1;
		arraydT3T2[i] = dT3T2;
		arraydT3T1[i] = dT3T1;
		arraydT3_T2[i] = dT3_T2;

    	cout << "/compile:" << dT2T1 << " /algo+stdout:" << dT3T2 << " /algo.exec:"<< dT3_T2 << "ms /all:" << dT3T1 << " ms\n";
	}

	cout << "\n\n";



	cout << "=====================================================================================================\n";

	cout << "JVM params:"
	<< "\n" << argv[1] << "\n\n";

	cout << "Compile:"
	<< "\n- average:" << average(arraydT2T1, nLoop) << " ms"
	<< "\n- standart deviation:" << sko(arraydT2T1, nLoop) << " ms"

	<< "\n\nAlgo execution:"
	<< "\n- average:" << average(arraydT3_T2, nLoop) << " ms"
	<< "\n- standart deviation:" << sko(arraydT3_T2, nLoop) << " ms"

	<< "\n\nAll time:"
    	<< "\n- average:" << average(arraydT3T1, nLoop) << " ms"
    	<< "\n- standart deviation:" << sko(arraydT3T1, nLoop) << " ms\n";


	cout << "=====================================================================================================\n";

	return 0;
}





