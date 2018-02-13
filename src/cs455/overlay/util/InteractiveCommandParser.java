package cs455.overlay.util;

import cs455.overlay.node.MessengerNode;
import cs455.overlay.node.Registry;
import cs455.overlay.routing.RoutingTable;
import cs455.overlay.transport.TCPConnection;
import cs455.overlay.wireformats.Node;
import cs455.overlay.wireformats.OverlayNodeSendsDeregistration;
import cs455.overlay.wireformats.RegistryRequestsTaskInitiate;
import cs455.overlay.wireformats.RegistrySendsNodeManifest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

public class InteractiveCommandParser {
    private boolean DEBUG = true;

    // node allows the command parser to know whether it is parsing commands for the registry or the msging node
    private Node node;

    public InteractiveCommandParser(Node node) {
        this.node = node;
    }

    /* BEGIN Registry COMMANDS */

    // list-messaging-nodes
    public void listMessagingNodes() {
        Registry registry = (Registry) node;

        if (registry.getRegisteredNodes().isEmpty()) {
            System.out.println("No messaging nodes currently registered\n");
            return;
        }

        System.out.printf("Executing list-messenging-nodes...\n");

        for (Map.Entry<Integer, String> registeredNode : registry.getRegisteredNodes().entrySet()) {
            // [0]: IP addr, [1]: portNum
            String[] IPportNum = registeredNode.getValue().split(":");
            System.out.printf("ID: %d\tIP: %s\tPort number: %s\n", registeredNode.getKey(), IPportNum[0], IPportNum[1]);
        }
    }

    // setup-overlay number-of-routing-table-entries (e.g. setup-overlay 3)
    public void setupOverlay(int routingTableSize) {
        System.out.printf("Executing setup-overlay %d...\n", routingTableSize);

        Registry registry = (Registry) node;

        /*  Routing of msgs will deal only with the nodes that are registered at the
            time of setup-overlay being called  */
        registry.setNumNodesRegistered(registry.getRegisteredNodes().size());

        // Transfer the entries from the HashMap into an ArrayList for faster iteration
        ArrayList<Map.Entry<Integer, String>> registeredNodesList = new ArrayList<>(registry.getRegisteredNodes().entrySet());

        // TODO: CHECK PIAZZA FOR HOW TO HANDLE CASE WHERE NODE COULD BE INCLUDED IN ITS OWN ROUTING TABLE
        // TLDR: If routingTableSize >  2 * Nr, it will work
        //       If routingTableSize <= 2 * Nr, it could end up in its own routing tbl, report an error to user? you decide how to handle
        for (int nodeIndex = 0; nodeIndex < registeredNodesList.size(); ++nodeIndex) {
            RoutingTable routingTable = new RoutingTable();
            for (int entry = 0; entry < routingTableSize; ++entry) {
                // ID space wraps around, so have to mod the hopsAway by the num of registered nodes
                // number of hops to get to the next node
                int hopsAway = ((int) Math.pow(2, entry));
                // add the number of hops to the current node's index to get the index of the node it should add for this entry
                int indexAtHopsAway = (nodeIndex + hopsAway) % registeredNodesList.size();

                int ID = registeredNodesList.get(indexAtHopsAway).getKey();
                String IPportNumStr = registeredNodesList.get(indexAtHopsAway).getValue();
                routingTable.addRoutingEntry(ID, IPportNumStr);
                //routingTable[entry] = registeredNodesList.get(indexAtHopsAway).getKey().toString();
            }

            // Look at IDs in each routing tbl. Easier to see which nodes in which routing tbls and to spot if a node is in its own tbl.
            if (DEBUG)
                System.out.printf("Routing table for node %d is:\n%s", registeredNodesList.get(nodeIndex).getKey(), routingTable);

            /*  Retrieve the connection to the current node and send it its routing table and info about all nodes in the system  */
            try {
                String IPportNumStr = registeredNodesList.get(nodeIndex).getValue();
                TCPConnection connection = registry.getConnectionsCache().getConnection(IPportNumStr);
                ArrayList<Integer> registeredNodeIds = new ArrayList<>(registry.getRegisteredNodes().keySet());
                RegistrySendsNodeManifest nodeManifest = new RegistrySendsNodeManifest(routingTable, registry.getRegisteredNodes().size(), registeredNodeIds);
                connection.getSenderThread().addMessage(nodeManifest.getBytes());
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

    // list-routing-tables
    // Info about routing tbls of each node
    public void listRoutingTables() {
        System.out.printf("Executing list-routing-tables...\n");
    }

    // start number-of-messages (e.g. start 25000)
    // Send all nodes a request to start sending number-of-messages to random nodes in the system
    public void start(int numMessages) {
        System.out.printf("Executing start %d...\n", numMessages);

        Registry registry = (Registry) node;

        if (registry.getNumNodesEstablishedConnections() == registry.getRegisteredNodes().size()) {
            RegistryRequestsTaskInitiate taskInitiate = new RegistryRequestsTaskInitiate(numMessages);
            for (Map.Entry<Integer, String> entry : registry.getRegisteredNodes().entrySet()) {
                try {
                    // get the connection associated with the IPportNumStr of the current registered node
                    String IPportNumStr = entry.getValue();
                    TCPConnection connection = registry.getConnectionsCache().getConnection(IPportNumStr);
                    connection.getSenderThread().addMessage(taskInitiate.getBytes());
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        } else {
            System.out.println("Not all nodes have successfully established connections with messaging nodes in their routing tables yet");
        }
    }
    /* END Registry COMMANDS */

    /* BEGIN MessengerNode COMMANDS */

    // print-counters-and-diagnostics
    // Info about the counters and trackers of a msging node
    public void printCountersAndDiagnostics() {
        System.out.printf("Executing print-counters-and-diagnostics...\n");

        MessengerNode msgNode = (MessengerNode) node;

        System.out.printf("-- Trackers and Summations --\nsndTracker: %d\nrcvTracker: %d\nrelayTracker: %d\nsndSummation: %d\nrcvSummation: %d\n",
                            msgNode.getSndTracker(), msgNode.getRcvTracker(), msgNode.getRelayTracker(), msgNode.getSndSummation(), msgNode.getRcvSummation());
    }

    // exit-overlay
    public void exitOverlay() {
        System.out.printf("Executing exit-overlay...\n");

        MessengerNode msgNode = ((MessengerNode) node);
        OverlayNodeSendsDeregistration nodeDeregistration = new OverlayNodeSendsDeregistration(msgNode.getIP(), msgNode.getPortNum(), msgNode.getID());

        // Retrieve connection to registry and send a deregistration request
        try {
            String registryIPportNumStr = msgNode.getRegistryIPportNumStr();
            TCPConnection registryConnection = msgNode.getConnectionsCache().getConnection(registryIPportNumStr);
            registryConnection.getSenderThread().addMessage(nodeDeregistration.getBytes());
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    /* END MessengerNode COMMANDS */
}
