package cs455.overlay.node;

public class RegistryEntry {
    private String IP;
    private int portNum;

    public RegistryEntry(String IP, int portNum) {
        this.IP = IP;
        this.portNum = portNum;
    }

    // https://www.mkyong.com/java/java-how-to-overrides-equals-and-hashcode/
    public int hashCode() {
        int result = 17;
        result = 31 * result + IP.hashCode();
        result = 31 * result + portNum;
        return result;
    }

    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof RegistryEntry))
            return false;
        RegistryEntry regEntry = (RegistryEntry)obj;
        return regEntry.IP.equals(this.IP) && regEntry.portNum == this.portNum;
    }

    public String toString() {
        return String.format("IP address: %s Port: %d", IP, portNum);
    }
}
