import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.Comparator;
import java.util.concurrent.atomic.AtomicBoolean;


public class Player
{
    private int id;

    private int index;
    private String name;
    private int coordX = -1;
    private int coordY = -1;
    private Location location = null;
    private String figureId = "";
    private boolean isBot = false;
    public Player(Integer id, Integer index, String name)
    {
        this.id = id;
        this.index = index;
        this.name = name;
        this.coordX = 0;
        this.coordY = 0;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getName() {
        return name;
    }

    public void setCoordX(int coordX) {
        this.coordX = coordX;
    }

    public void setCoordY(int coordY) {
        this.coordY = coordY;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public boolean isBot() {
        return isBot;
    }

    public void setBot(boolean bot) {
        isBot = bot;
    }

    public int getCoordX() {
        return coordX;
    }

    public int getCoordY() {
        return coordY;
    }

    public String getFigureId() {
        return figureId;
    }

    public void setFigureId(String figureId) {
        this.figureId = figureId;
    }
}