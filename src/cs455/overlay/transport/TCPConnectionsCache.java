package cs455.overlay.transport;

import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TCPConnectionsCache {
    // the key will be a string that is a concatenation of the IP addr and portNum of a node
    // ex.) IP-addr:portNum -> 127.0.0.1:58390
    private HashMap<String, TCPConnection> connections;

    public TCPConnectionsCache() {
        connections = new HashMap<>();
    }

    public void addConnection(String IPportNumKey, TCPConnection connection) {
        connections.put(IPportNumKey, connection);
    }

    public TCPConnection removeConnection(String IPportNumKey) {
        return connections.remove(IPportNumKey);
    }

    public TCPConnection getConnection(String IPportNumKey) {
        return connections.get(IPportNumKey);
    }

    public Set<Map.Entry<String, TCPConnection>> getEntrySet() {
        return connections.entrySet();
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        for (Map.Entry<String, TCPConnection> entry : connections.entrySet()) {
            stringBuilder.append(entry.getKey());
            stringBuilder.append(" -> ");
            stringBuilder.append(entry.getValue());
            stringBuilder.append('\n');
        }
        return stringBuilder.toString();
    }
}
