public class Location
{
    private int coordX;
    private int coordY;
    private int furniId;
    private String customName = "";
    private boolean isSitted;

    public Location(int coordX, int coordY, int furniId, boolean isSitted)
    {
        this.coordX = coordX;
        this.coordY = coordY;
        this.furniId = furniId;
        this.isSitted = isSitted;
    }
    public int getCoordX() {
        return coordX;
    }

    public int getCoordY() {
        return coordY;
    }

    public int getFurniId() {
        return furniId;
    }

    public String getCustomName() {
        return customName;
    }

    public void setCustomName(String customName) {
        this.customName = customName;
    }

    public boolean isSitted() {
        return isSitted;
    }
}