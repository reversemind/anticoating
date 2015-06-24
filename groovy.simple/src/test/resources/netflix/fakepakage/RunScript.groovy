package fakepackage;

/**
 *
 */
class RunScript {
    public static void main(String... args) {
        FakeClass f1 = new FakeClass("v1");
        println "go:" + FakeClass.processValues("v0", f1);
    }
}
