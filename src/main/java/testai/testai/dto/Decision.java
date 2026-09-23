package testai.testai.dto;

public record Decision(
        Action action,
        String existingTestMethodName,
        String testCode,
        String reasoning) {

    public enum Action {
        ADD_TEST_METHOD, UPDATE_TEST_METHOD, CREATE_TEST_CLASS, NO_CHANGE
    }
}
