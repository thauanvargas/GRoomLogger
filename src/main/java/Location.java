public class Location
{
    private int coordX;
    private int coordY;
    private String customName = "";

    public Location(int coordX, int coordY, String customName)
    {
        this.coordX = coordX;
        this.coordY = coordY;
        this.customName = customName;
    }
    public int getCoordX() {
        return coordX;
    }

    public int getCoordY() {
        return coordY;
    }

    public String getCustomName() {
        return customName;
    }

    public void setCustomName(String customName) {
        this.customName = customName;
    }

}