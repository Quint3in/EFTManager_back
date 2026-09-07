package cat.itacademy.s05.t02.eftmanager.common;

public enum GameMode {
    PVP("regular"),
    PVE("pve"),
    SEASON("pvp-season");

    private final String externalPath;

    GameMode(String externalPath) {
        this.externalPath = externalPath;
    }

    public String getExternalPath() {
        return externalPath;
    }
}