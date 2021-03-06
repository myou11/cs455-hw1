package cs455.overlay.util;

import cs455.overlay.node.MessagingNode;
import cs455.overlay.node.Registry;
import cs455.overlay.routing.RoutingTable;
import cs455.overlay.transport.TCPConnection;
import cs455.overlay.transport.TCPConnectionsCache;
import cs455.overlay.wireformats.Node;
import cs455.overlay.wireformats.OverlayNodeSendsDeregistration;
import cs455.overlay.wireformats.RegistryRequestsTaskInitiate;
import cs455.overlay.wireformats.RegistrySendsNodeManifest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

public class InteractiveCommandParser {
    private boolean DEBUG = false;

    // node allows the command parser to know whether it is parsing commands for the registry or the msging node
    private Node node;

    // If false, can't run the list-routing-tables and start number-of-messages commands
    private boolean overlayWasSetup = false;

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

        if (DEBUG)
            System.out.printf("Executing list-messenging-nodes...\n");

        System.out.printf("There are currently (%d) messaging nodes registered:\n", registry.getRegisteredNodes().size());

        for (Map.Entry<Integer, String> registeredNode : registry.getRegisteredNodes().entrySet()) {
            // [0]: IP addr, [1]: portNum
            String[] IPportNum = registeredNode.getValue().split(":");
            System.out.printf("ID: %d\tIP: %s\tPort number: %s\n", registeredNode.getKey(), IPportNum[0], IPportNum[1]);
        }
    }

    // setup-overlay number-of-routing-table-entries (e.g. setup-overlay 3)
    public void setupOverlay(int routingTableSize) {
        Registry registry = (Registry) node;

        if (registry.getNumNodesRegistered() == 0) {
            System.out.println("Cannot setup overlay with 0 nodes. Please register some nodes");
            return;
        }

        overlayWasSetup = true;


        /*  Routing of msgs will deal only with the nodes that are registered at the
            time of setup-overlay being called  */
        registry.setNumNodesRegistered(registry.getRegisteredNodes().size());
        System.out.printf("Executing setup-overlay with (%d) registered nodes and routing table size (%d)...\n", registry.getNumNodesRegistered(), routingTableSize);

        // Transfer the entries from the HashMap into an ArrayList for faster iteration
        ArrayList<Map.Entry<Integer, String>> registeredNodesList = new ArrayList<>(registry.getRegisteredNodes().entrySet());

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
            }

            // Store the routing table so it's easy to display them for the user on list-routing-tables command
            int nodeID = registeredNodesList.get(nodeIndex).getKey();
            registry.getNodeRoutingTables().put(nodeID, routingTable);

            // Look at IDs in each routing tbl. Easier to see which nodes in which routing tbls and to spot if a node is in its own tbl.
            if (DEBUG)
                System.out.printf("Routing table for node %d is:\n%s", nodeID, routingTable);

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
        if (!overlayWasSetup) {
            System.out.println("No routing tables because the overlay has not been setup yet. Please run setup-overlay first");
            return;
        }

        if (DEBUG)
            System.out.printf("Executing list-routing-tables...\n");

        Registry registry = (Registry) node;
        System.out.println("Routing Tables:\n");

        for (Map.Entry<Integer, RoutingTable> entry : registry.getNodeRoutingTables().entrySet()) {
            int nodeID = entry.getKey();
            RoutingTable routingTable = entry.getValue();
            System.out.printf("Node %d:\n%s\n\n\n", nodeID, routingTable.toString());
        }
    }

    // start number-of-messages (e.g. start 25000)
    // Send all nodes a request to start sending number-of-messages to random nodes in the system
    public void start(int numMessages) {
        if (!overlayWasSetup) {
            System.out.println("Overlay has not been setup yet. Please run setup-overlay first");
            return;
        }

        if (DEBUG)
            System.out.printf("Executing start %d...\n", numMessages);

        Registry registry = (Registry) node;

        if (registry.getNumNodesEstablishedConnections() == registry.getNumNodesRegistered()) {
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

    /* BEGIN MessagingNode COMMANDS */

    // print-counters-and-diagnostics
    // Info about the counters and trackers of a msging node
    public void printCountersAndDiagnostics() {
        if (DEBUG)
            System.out.printf("Executing print-counters-and-diagnostics...\n");

        MessagingNode msgNode = (MessagingNode) node;

        System.out.printf("-- Trackers and Summations --\nsndTracker: %d\nrcvTracker: %d\nrelayTracker: %d\nsndSummation: %d\nrcvSummation: %d\n",
                            msgNode.getSndTracker(), msgNode.getRcvTracker(), msgNode.getRelayTracker(), msgNode.getSndSummation(), msgNode.getRcvSummation());
    }

    // Was used to debug the wait-notify msg queue implementation
    public void printMsgQueueSize() {
        MessagingNode msgNode = (MessagingNode)node;
        TCPConnectionsCache cc = msgNode.getConnectionsCache();
        for (Map.Entry<String, TCPConnection> entry : cc.getEntrySet()) {
            System.out.printf("Msg queue size: %d\n", entry.getValue().getSenderThread().getMsgQueueSize());
        }
    }

    // exit-overlay
    public void exitOverlay() {
        if (DEBUG)
            System.out.printf("Executing exit-overlay...\n");

        MessagingNode msgNode = ((MessagingNode) node);
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

    /* END MessagingNode COMMANDS */
}
