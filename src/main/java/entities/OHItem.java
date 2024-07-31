package entities;

import gearth.protocol.HPacket;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class OHItem {
    private int id;
    private String className;
    private String owner;
    private String type;


    public OHItem(String[] packetString) {
        this.id = Integer.parseInt(packetString[0]);
        this.className = packetString[1];
        this.owner = packetString[2];
        this.type = packetString[3];
    }

    public static OHItem[] parse(HPacket packet) {
        final byte[] dataRemainder = packet.readBytes(packet.getBytesLength() - packet.getReadIndex());
        String data = new String(dataRemainder, StandardCharsets.ISO_8859_1);
        String[] packets = data.split("\u0002");
        int i;
        OHItem[] entities = new OHItem[(packets.length)];

        for(i = 0; i < packets.length; i++) {
            String[] packetString = packets[i].split("\t");
            entities[i] = new OHItem(packetString);
        }

        return entities;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
