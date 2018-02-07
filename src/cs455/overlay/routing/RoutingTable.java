package cs455.overlay.routing;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class RoutingTable {
    /* private ArrayList<RoutingEntry> routingTable;

    public RoutingTable() {
        this.routingTable = new ArrayList<>();
    }

    public void addRoutingEntry(RoutingEntry entry) {
        routingTable.add(entry);
    }

    public RoutingEntry getEntry(int i) {
        return routingTable.get(i);
    }

    public boolean containsID(int ID) {
        return routingTable.contains(ID);
    }

    public int size() {
        return routingTable.size();
    }

    public String toString() {
        return routingTable.toString();
    }
    */

    // used LinkedHashMap because I want the routing entries to be stored in the order they are inserted
    // entries are inserted by hop count; 1st: 1 hop, 2nd: 2 hops, 3rd: 4 hops, nth: 2^(n-1) hops
    private LinkedHashMap<Integer, String> routingTable;

    public RoutingTable() {
        this.routingTable = new LinkedHashMap<>();
    }

    public void addRoutingEntry(int ID, String IPportNumStr) {
        routingTable.put(ID, IPportNumStr);
    }

    public String getEntry(int ID) {
        return routingTable.get(ID);
    }

    public Set<Map.Entry<Integer,String>> getEntrySet() {
        return routingTable.entrySet();
    }

    public ArrayList<Integer> getKeys() {
        return new ArrayList<>(routingTable.keySet());
    }

    public ArrayList<String> getValues() {
        return new ArrayList<>(routingTable.values());
    }

    public boolean contains(int ID) {
        return routingTable.containsKey(ID);
    }

    public int size() {
        return routingTable.size();
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        for (Map.Entry<Integer, String> entry : routingTable.entrySet()) {
            stringBuilder.append(entry.getKey());
            stringBuilder.append(": ");
            stringBuilder.append(entry.getValue());
            stringBuilder.append('\n');
        }
        return stringBuilder.toString();
    }
}
