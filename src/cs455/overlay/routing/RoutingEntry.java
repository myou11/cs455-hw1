package cs455.overlay.routing;

public class RoutingEntry {
    private int ID;
    private String IP;
    private int portNum;

    public RoutingEntry(int ID, String IP, int portNum) {
        this.ID = ID;
        this.IP = IP;
        this.portNum = portNum;
    }

    public int getID() {
        return ID;
    }

    public String getIP() {
        return IP;
    }

    public int getPortNum() {
        return portNum;
    }

    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof Integer))
            return false;
        int ID = (int) obj;
        // Compare routing entries by ID
        return this.ID == ID;
    }

    public String toString() {
        // TODO: only prints the ID of each routing entry right now. did that so i could see the routing table entries easier
        // seems to work now so change it to print ID, IP, and portNum
        return Integer.toString(ID);
    }
}
