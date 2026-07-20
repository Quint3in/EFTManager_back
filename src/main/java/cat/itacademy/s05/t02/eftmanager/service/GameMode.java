package cat.itacademy.s05.t02.eftmanager.service;

public enum GameMode {
    PVP("regular"),
    PVE("pve");

    private final String externalPath;

    GameMode(String externalPath) {
        this.externalPath = externalPath;
    }

    public String getExternalPath() {
        return externalPath;
    }
}