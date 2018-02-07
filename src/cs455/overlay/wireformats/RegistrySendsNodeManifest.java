package cs455.overlay.wireformats;

import cs455.overlay.routing.RoutingTable;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class RegistrySendsNodeManifest implements Protocol, Event {
    private int type = REGISTRY_SENDS_NODE_MANIFEST;
    private RoutingTable routingTable;
    private int numNodes;
    private ArrayList<Integer> registeredNodeIDs;

    public RegistrySendsNodeManifest(RoutingTable routingTable, int numNodes, ArrayList<Integer> registeredNodeIDs) {
        this.routingTable = routingTable;
        this.numNodes = numNodes;
        this.registeredNodeIDs = registeredNodeIDs;
    }

    @Override
    public int getType() {
        return type;
    }

    public RoutingTable getRoutingTable() {
        return routingTable;
    }

    public int getNumNodes() {
        return numNodes;
    }

    public ArrayList<Integer> getRegisteredNodeIDs() {
        return registeredNodeIDs;
    }

    // marshall this msg into a byte arr
    @Override
    public byte[] getBytes() throws IOException {
        /*
            Msg outline:
                byte: msg type (REGISTRY_SENDS_NODE_MANIFEST)
                int:  routing table size (Nr)
                int:  Node ID of node 1 hop away
                int:  length of following IP field
                byte: IP of node 1 hop away
                int:  portNum of node 1 hop away
                int:  Node ID of node 2 hops away
                int:  length of following IP field
                byte: IP of node 2 hops away
                int:  portNum of node 2 hops away
                                .
                                .
                                .
                int:  Node ID of node 2^(Nr-1) hops away
                int:  length of following IP field
                byte: IP of node 2^(Nr-1) hops away
                int:  portNum of node 2^(Nr-1) hops away
         */
        byte[] marshalledBytes = null;

        ByteArrayOutputStream baOutStream = new ByteArrayOutputStream();
        DataOutputStream dOut = new DataOutputStream(baOutStream);

        // type
        dOut.writeInt(type);

        // routing table size
        dOut.writeInt(routingTable.size());

        /* Routing table entries */

        // Node ID of node 1 hop away
        // TODO: these might not be in the right order because it is retrieving from a HashMap. Though it should be the same order as it would get printed, so might be okay
        ArrayList<Integer> routingTableKeys = routingTable.getKeys();
        ArrayList<String> routingTableVals = routingTable.getValues();
        for (int i = 0; i < routingTable.size(); ++i) {
            // ID of node 2^i hops away
            dOut.writeInt(routingTableKeys.get(i));

            // the IP and portNum are stored as a single string delimited with a colon
            String[] IPportNumArr = routingTableVals.get(i).split(":");
            String IP = IPportNumArr[0];

            // IP of node 2^i hops away
            byte[] IPbytes = IP.getBytes();
            int IPlength = IPbytes.length;
            dOut.writeInt(IPlength);
            dOut.write(IPbytes);

            // portNum of node 2^i hops away
            int portNum = Integer.parseInt(IPportNumArr[1]);
            dOut.writeInt(portNum);
        }

        // num node IDs in the system
        dOut.writeInt(numNodes);

        // write each ID to the buffer
        for (int i = 0; i < registeredNodeIDs.size(); ++i) {
            dOut.writeInt(registeredNodeIDs.get(i));
        }

        dOut.flush();
        marshalledBytes = baOutStream.toByteArray();

        baOutStream.close();
        dOut.close();

        return marshalledBytes;
    }
}
