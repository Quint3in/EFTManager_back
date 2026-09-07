package cat.itacademy.s05.t02.eftmanager.task;

public class UserNotFoundForComparisonException extends RuntimeException {
    public UserNotFoundForComparisonException(String message) {
        super(message);
    }
}