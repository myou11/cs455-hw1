package cs455.overlay.util;

import cs455.overlay.node.MessengerNode;
import cs455.overlay.node.Registry;
import cs455.overlay.routing.RoutingTable;
import cs455.overlay.transport.TCPConnection;
import cs455.overlay.transport.TCPSenderThread;
import cs455.overlay.wireformats.Node;
import cs455.overlay.wireformats.OverlayNodeSendsDeregistration;
import cs455.overlay.wireformats.RegistryRequestsTaskInitiate;
import cs455.overlay.wireformats.RegistrySendsNodeManifest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

public class InteractiveCommandParser {
    Node node;

    // node allows the command parser to know whether it is parsing
    // commands for the registry or the msging node
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
                // below is for seeing the IDs in each routing tbl. easier to see which nodes in which routing tbls and to spot if a node is in its own tbl.
                //routingTable[entry] = registeredNodesList.get(indexAtHopsAway).getKey().toString();
            }
            System.out.printf("Routing table for node %d is:\n%s", registeredNodesList.get(nodeIndex).getKey(), routingTable);

            // get the IPportNumStr associated with this node and use that to retrieve the socket connected to this node
            // TODO: DEL BELOW LINE IF TCPCONNECTION WORKS
            // Socket socket = (registry.getConnectionsCache().getConnection(registeredNodesList.get(nodeIndex).getValue()));
            // TODO: UPDATE WITH NEW TCPCONNECTION COMMENTS
            try {
                String IPportNumStr = registeredNodesList.get(nodeIndex).getValue();
                TCPConnection connection = registry.getConnectionsCache().getConnection(IPportNumStr);
                ArrayList<Integer> registeredNodeIds = new ArrayList<>(registry.getRegisteredNodes().keySet());
                RegistrySendsNodeManifest nodeManifest = new RegistrySendsNodeManifest(routingTable, registry.getRegisteredNodes().size(), registeredNodeIds);
                // TODO: DEL BELOW TRY CATCH IF TCP CONNECTION WORKS
            /*try {
                (new Thread(new TCPSenderThread(socket, nodeManifest.getBytes()))).start();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }*/
                connection.sendMsg(nodeManifest.getBytes());
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

    // list-routing-tables
    public void listRoutingTables() {
        System.out.printf("Executing list-routing-tables...\n");
    }

    // start number-of-messages (e.g. start 25000)
    public void start(int numMessages) {
        System.out.printf("Executing start %d...\n", numMessages);

        Registry registry = (Registry) node;

        if (registry.getNumNodesEstablishedConnections() == registry.getRegisteredNodes().size()) {
            RegistryRequestsTaskInitiate taskInitiate = new RegistryRequestsTaskInitiate(numMessages);
            for (Map.Entry<Integer, String> entry : registry.getRegisteredNodes().entrySet()) {
                try {
                    // TODO: UPDATE COMMENTS IF TCPCONNECTION WORKS
                    // get the socket associated with the IPportNumStr of the current registered node
                    String IPportNumStr = entry.getValue();
                    TCPConnection connection = registry.getConnectionsCache().getConnection(IPportNumStr);
                    connection.sendMsg(taskInitiate.getBytes());
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
                // TODO: DEL BELOW LINES IF TCPCONNECTION WORKS
                /*Socket socket = registry.getConnectionsCache().getConnection(entry.getValue());
                try {
                    // send the task initiate request to the registered node
                    (new Thread(new TCPSenderThread(socket, taskInitiate.getBytes()))).start();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }*/
            }
        } else {
            System.out.println("Not all nodes have successfully established connections with messaging nodes in their routing tables yet");
        }
    }
    /* END Registry COMMANDS */

    /* BEGIN MessengerNode COMMANDS */

    // print-counters-and-diagnostics
    public void printCountersAndDiagnostics() {
        System.out.printf("Executing print-counters-and-diagnostics...\n");

        MessengerNode msgNode = (MessengerNode) node;

        System.out.printf("-- Trackers and Summations --\nsndTracker: %d\nrcvTracker: %d\nrelayTracker: %d\nsndSummation: %d\nrcvSummation: %d\n",
                            msgNode.getSndTracker(), msgNode.getRcvTracker(), msgNode.getRelayTracker(), msgNode.getSndSummation(), msgNode.getRcvSummation());
    }

    // exit-overlay
    public void exitOverlay() {
        System.out.printf("Executing exit-overlay...\n");
        // look in msging node's TCPConnectionCache for the socket that is connected to the registry
        // use this socket to send a deregistration req
        MessengerNode msgNode = ((MessengerNode) node);
        OverlayNodeSendsDeregistration nodeDeregistration = new OverlayNodeSendsDeregistration(msgNode.getIP(), msgNode.getPortNum(), msgNode.getID());
        // TODO: how can I send this without creating another thread? should i just create another thread (and incur the overhead, shouldn't be too much)
        // TODO: and find the socket connected to registry and just use that thread to send a msg? probably what has to happen to send this msg
        // TODO: pros: wont have to create another socket, new thread overhead is low (dies after it sends msg) cons: thread overhead?
        // TODO: UPDATE COMMENTS ABOUT NEW TCPCONNECTION HERE IF WORK
        try {
            String registryIPportNumStr = msgNode.getRegistryIPportNumStr();
            TCPConnection registryConnection = msgNode.getConnectionsCache().getConnection(registryIPportNumStr);
            registryConnection.sendMsg(nodeDeregistration.getBytes());
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        // TODO: DEL LINES BELOW THIS IF TCPCONNECTION WORKS
        /*try {
            // first argument is the socket by which we will send the msg. we get the socket connected to the registry
            // second argument, we get the msg that we are sending, the node deregistration request
            (new Thread(new TCPSenderThread(msgNode.getConnectionsCache().getConnection(msgNode.getRegistryIPportNumStr()), nodeDeregistration.getBytes()))).start();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }*/
    }

    /* END MessengerNode COMMANDS */
}
