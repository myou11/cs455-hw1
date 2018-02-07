package cs455.overlay.transport;

import cs455.overlay.node.RegistryEntry;

import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class TCPConnectionsCache {
    // the key will be a string that is a concatenation of the IP addr and portNum of a node
    // ex.) IP-addr:portNum -> 127.0.0.1:58390
    private HashMap<String, Socket> connections;

    public TCPConnectionsCache() {
        connections = new HashMap<>();
    }

    public void addConnection(String IPportNumKey, Socket socket) {
        connections.put(IPportNumKey, socket);
    }

    public Socket removeConnection(String IPportNumKey) {
        return connections.remove(IPportNumKey);
    }

    public Socket getConnection(String IPportNumKey) {
        return connections.get(IPportNumKey);
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        for (Map.Entry<String, Socket> entry : connections.entrySet()) {
            stringBuilder.append(entry.getKey());
            stringBuilder.append(" -> ");
            stringBuilder.append(entry.getValue());
            stringBuilder.append('\n');
        }
        return stringBuilder.toString();
    }
}
