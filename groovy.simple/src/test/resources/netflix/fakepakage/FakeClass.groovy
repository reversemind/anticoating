package fakepackage;

/**
 * FakeClass for dependency
 */
class FakeClass {

    private String fakeValue

    FakeClass(String fakeValue) {
        this.fakeValue = fakeValue
    }

    String getFakeValue() {
        return fakeValue
    }

    void setFakeValue(String fakeValue) {
        this.fakeValue = fakeValue
    }

    def
    static processValues(String string, FakeClass fakeClass) {
        return "processed:" + string + " other:" + (fakeClass != null ? fakeClass.getFakeValue() : "null");
    }
}
