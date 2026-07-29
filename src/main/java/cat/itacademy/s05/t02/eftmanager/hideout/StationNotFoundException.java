package cat.itacademy.s05.t02.eftmanager.hideout;

public class StationNotFoundException extends RuntimeException {
    public StationNotFoundException(String message) {
        super(message);
    }
}