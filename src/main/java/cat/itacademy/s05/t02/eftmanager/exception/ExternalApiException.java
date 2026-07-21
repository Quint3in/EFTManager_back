package cat.itacademy.s05.t02.eftmanager.exception;

public class ExternalApiException extends RuntimeException {
    public ExternalApiException(String message, Throwable throwable) {
        super(message);
    }
}
