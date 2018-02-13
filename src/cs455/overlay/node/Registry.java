package cs455.overlay.node;

import cs455.overlay.transport.TCPConnection;
import cs455.overlay.transport.TCPConnectionsCache;
import cs455.overlay.transport.TCPServerThread;
import cs455.overlay.util.InteractiveCommandParser;
import cs455.overlay.wireformats.*;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Registry implements Protocol, Node {
    private boolean DEBUG = true;

    private int portNum;
    /*  Integer will be IDs of the registered nodes and String will be a string representation of IP and portNum of registered node.
        Access to map will be syncd since registry could rcv many registration/deregistration requests at once  */
    private TreeMap<Integer, String> registeredNodes = new TreeMap<>();
    // store sockets used to communicate with other nodes so we dont have to create a new socket for each communication (snd/rcv)
    private TCPConnectionsCache connectionsCache;
    private ServerSocket serverSocket;

    private int numNodesRegistered; // set when setup-overlay is initiated
    private int numNodesEstablishedConnections = 0;

    // keep track of nodes that have finished sending packets
    private int numNodesFinishedSending = 0;

    // Counters for the traffic summaries nodes will send back
    private int numTrafficSummariesRcvd = 0;
    private int packetsSnt = 0;
    private int packetsRcvd = 0;
    private int packetsRelayed = 0;
    private long packetsSntSummation = 0;
    private long packetsRcvdSummation = 0;

    public Registry(int portNum, ServerSocket serverSocket) {
        this.portNum = portNum;
        this.serverSocket = serverSocket;
        this.connectionsCache = new TCPConnectionsCache();
    }

    public TreeMap<Integer, String> getRegisteredNodes() { return registeredNodes; }

    public TCPConnectionsCache getConnectionsCache() {
        return connectionsCache;
    }

    public ServerSocket getServerSocket() {
        return serverSocket;
    }

    public int getNumNodesRegistered() {
        return numNodesRegistered;
    }

    public void setNumNodesRegistered(int numNodesRegistered) {
        this.numNodesRegistered = numNodesRegistered;
    }

    public int getNumNodesEstablishedConnections() {
        return numNodesEstablishedConnections;
    }

    @Override
    public void onEvent(Event event, TCPConnection connection) throws IOException {
        switch(event.getType()) {
            case (OVERLAY_NODE_SENDS_REGISTRATION):
                registerNode((OverlayNodeSendsRegistration)event, connection);
                break;
            case (OVERLAY_NODE_SENDS_DEREGISTRATION):
                deregisterNode((OverlayNodeSendsDeregistration)event, connection);
                break;
            case (NODE_REPORTS_OVERLAY_SETUP_STATUS):
                processOverlaySetupStatusResponse((NodeReportsOverlaySetupStatus)event);
                break;
            case (OVERLAY_NODE_REPORTS_TASK_FINISHED):
                processTaskFinished((OverlayNodeReportsTaskFinished)event);
                break;
            case (OVERLAY_NODE_REPORTS_TRAFFIC_SUMMARY):
                processTrafficSummary((OverlayNodeReportsTrafficSummary)event);
                break;
        }
    }

    // Selects a unique ID for the msging node btwn 0-127
    private int assignID() {
        Random r = new Random();
        int ID = r.nextInt(128);
        while (registeredNodes.containsKey(ID)) {
            ID = r.nextInt(128);
        }
        return ID;
    }

    /*  Valid registration if node has not been previously registered (i.e. same IP and port)
        and IP in packet matches IP of the connection  */
    private boolean validRegistration(String IP, int portNum, TCPConnection connection) {
        String IPportNumStr = IP + ':' + portNum;
        String connectionIP = connection.getSocket().getInetAddress().getHostAddress();
        return !registeredNodes.containsValue(IPportNumStr) && connectionIP.equals(IP);
    }

    private synchronized void registerNode(OverlayNodeSendsRegistration event, TCPConnection connection) throws IOException {
        int ID = assignID();
        // create a string to represent the IP and portNum of msging node. use this as key into HashMap
        String IPportNumStr = event.getIP() + ':' + event.getPortNum();

        // Info on outcome of registration (i.e. success or failure)
        String infoStr;

        if (validRegistration(event.getIP(), event.getPortNum(), connection)) {
            // register the msging node
            registeredNodes.put(ID, IPportNumStr);

            infoStr = "Registration request successful. The number of messaging nodes currently constituting " +
                    "the overlay is (" + registeredNodes.size() + ")";

            // add the connection into the registry's connection cache, so we can use it for communication later
            connectionsCache.addConnection(IPportNumStr, connection);

            if (DEBUG)
                System.out.printf("Registered node from %s, ID is %d\n", registeredNodes.get(ID), ID);
        } else { // invalid registration request
            // TODO: TEST IF IT WILL FAIL IF WE TRY TO REGISTER A NODE MORE THAN ONCE. JUST CODE SEND A REG REQ TWICE IN THE MSG NODE
            ID = -1; // failure ID
            infoStr = "Registration request failed. There is either (1) no more room in the Registry, " +
                    "(2) this node has already been registered, or (3) the IP address in the request did not" +
                    "match the IP address of the origin";
        }

        RegistryReportsRegistrationStatus registrationStatus = new RegistryReportsRegistrationStatus(ID, infoStr);
        connection.getSenderThread().addMessage(registrationStatus.getBytes());
    }

    /*  Valid deregistration if node ID is in the registry and IP in packet matches IP of the connection  */
    private boolean validDeregistration(String IP, int IDtoRemove, TCPConnection connection) {
        String connectionIP = connection.getSocket().getInetAddress().getHostAddress();
        return registeredNodes.containsKey(IDtoRemove) && connectionIP.equals(IP);
    }

    private synchronized void deregisterNode(OverlayNodeSendsDeregistration event, TCPConnection connection) throws IOException {
        int idToRemove = event.getNodeID();
        String infoStr;
        Socket connectionSocket = connection.getSocket();

        // if getting the node with idToRemove returns null, node doesn't exist in registry anymore, so can't deregister
        if (validDeregistration(event.getIP(), idToRemove, connection)) {
            String removed = registeredNodes.remove(idToRemove);

            if (DEBUG)
                System.out.printf("Removed node with ID [%d] and value [%s] from registeredNodes\n", idToRemove, removed);

            infoStr = "Deregistration request successful. The number of messaging nodes currently constituting " +
                    "the overlay is (" + registeredNodes.size() + ")";

        } else { // invalid deregistration request
            infoStr = "Deregistration request failed. The node was (1) not registered in the system or (i.e. deregistered already or was never registered) " +
                    "(2) the IP address in the request did not match the IP address of the origin";
        }

        // remove the registry's connection (socket) from the connectionsCache
        String IPportNumStr = connectionSocket.getInetAddress().getHostAddress() + ':' + connectionSocket.getPort();
        TCPConnection removedConnection = connectionsCache.removeConnection(IPportNumStr);

        RegistryReportsDeregistrationStatus deregistrationStatus = new RegistryReportsDeregistrationStatus(idToRemove, infoStr);
        removedConnection.getSenderThread().addMessage(deregistrationStatus.getBytes());
    }

    // Syncd b/c registry could get many responses from the msging nodes at once
    private synchronized void processOverlaySetupStatusResponse(NodeReportsOverlaySetupStatus event) {
        if (event.getStatus() > -1) {   // succeeded in setting up overlay
            System.out.println(event.getInfoStr());
            ++numNodesEstablishedConnections;

            if (numNodesEstablishedConnections == numNodesRegistered)
                System.out.println("All nodes in the overlay were successful in establishing connections to nodes that comprised their routing table");
        }
        else
            System.out.println(event.getInfoStr());
    }

    // Syncd b/c registry could get many reports of finished sending at once
    private synchronized void processTaskFinished(OverlayNodeReportsTaskFinished event) throws IOException {
        String IP = event.getIP();
        int portNum = event.getPortNum();
        int nodeID = event.getNodeID();

        if (DEBUG)
            System.out.printf("Node (%d) [%s:%d] has finished sending messages\n", nodeID, IP, portNum);

        ++numNodesFinishedSending;

        if (numNodesFinishedSending == numNodesRegistered) {
            System.out.printf("All messaging nodes have finished sending messages...\n" +
                    "Waiting 15 seconds before retrieving traffic summaries from messaging nodes...\n");
            try {
                Thread.sleep(15000);
            } catch (InterruptedException ie) {
                System.out.println("Thread was interrupted");
                ie.printStackTrace();
                System.exit(1);
            }

            RegistryRequestsTrafficSummary trafficSummary = new RegistryRequestsTrafficSummary();
            for (Map.Entry<Integer, String> entry : registeredNodes.entrySet()) {
                /*  Get the IP and portNum for the registered node so we can retrieve the
                    connection associated with that node. Send the message through that connection  */
                String IPportNumStr = entry.getValue();
                TCPConnection connection = connectionsCache.getConnection(IPportNumStr);
                connection.getSenderThread().addMessage(trafficSummary.getBytes());
            }
        }
    }

    private synchronized void processTrafficSummary(OverlayNodeReportsTrafficSummary event) {
        int nodeID = event.getID();
        int packetsSnt = event.getTotalPacketsSent();
        int packetsRcvd = event.getTotalPacketsRcvd();
        int packetsRelayed = event.getTotalPacketsRelayed();
        long packetsSntSummation = event.getSendSummation();
        long packetsRcvdSummation = event.getRcvSummation();

        // If this is the first of the traffic summaries, print the table header
        if (numTrafficSummariesRcvd == 0)
            System.out.printf("\t| Packets Sent\t| Packets Received\t| Packets Relayed\t| Sum Values Sent\t| Sum Values Received\n");

        System.out.printf("Node %d\t| %d\t| %d\t| %d\t| %d\t| %d\n",
                            nodeID, packetsSnt, packetsRcvd, packetsRelayed, packetsSntSummation, packetsRcvdSummation);

        this.packetsSnt += packetsSnt;
        this.packetsRcvd += packetsRcvd;
        this.packetsRelayed += packetsRelayed;
        this.packetsSntSummation += packetsSntSummation;
        this.packetsRcvdSummation += packetsRcvdSummation;

        ++numTrafficSummariesRcvd;

        if (numTrafficSummariesRcvd == numNodesRegistered) {
            System.out.printf("Sum\t| %d\t|%d \t|%d \t|%d \t|%d \n",
                              this.packetsSnt, this.packetsRcvd, this.packetsRelayed,
                              this.packetsSntSummation, this.packetsRcvdSummation);
        }

    }

    private void processCommand(String[] command, InteractiveCommandParser commandParser) {
        // carry out different tasks based on the command specified
        switch (command[0]) {
            case ("list-messaging-nodes"):
                commandParser.listMessagingNodes();
                break;
            case ("setup-overlay"):
                if (command.length == 1) {
                    // default routing table size is 3 if not specified
                    commandParser.setupOverlay(3);
                    break;
                }
                commandParser.setupOverlay(Integer.parseInt(command[1]));
                break;
            case ("list-routing-tables"):
                commandParser.listRoutingTables();
                break;
            case ("start"):
                if (command.length == 1) {
                    System.out.println("Please specify the number of packets to send");
                    break;
                }
                commandParser.start(Integer.parseInt(command[1]));
                break;
            default:
                System.out.println("Please enter a valid command listed above");
        }
        System.out.print("Please enter a command for the registry to execute (e.g. list-messaging-nodes, setup-overlay number-of-routing-table-entries, list-routing-tables, start number-of-messages):\n");
    }

    public static void main(String[] args) {
        ServerSocket registryServerSocket = null;
        try {
            registryServerSocket = new ServerSocket(Integer.parseInt(args[0]));
            System.out.println("Registry is listening on (IP:port): " + registryServerSocket.getInetAddress().getHostAddress() + ':' + args[0] + '\n');
        } catch (IOException e) {
            System.err.println("Unable to create ServerSocket");
        }

        // only proceed if the registryServerSocket is created
        if (registryServerSocket != null) {
            Registry registry = new Registry(Integer.parseInt(args[0]), registryServerSocket);
            // have a thread run the server portion of the registry
            (new Thread(new TCPServerThread(registry))).start();

            /* Interactive Command Parser */
            // Allow user to enter commands to control the registry
            InteractiveCommandParser commandParser = new InteractiveCommandParser(registry);
            Scanner sc = new Scanner(System.in);
            System.out.print("Please enter a command for the registry to execute (e.g. list-messaging-nodes, setup-overlay number-of-routing-table-entries, list-routing-tables, start number-of-messages, CTRL-D to exit):\n");
            while (sc.hasNextLine()) {
                // some commands have arguments that accompany them (e.g. setup-overlay 3)
                // therefore, [0] will be the command and any successive indices will be the parameters to that command
                String[] command = sc.nextLine().split(" ");
                registry.processCommand(command, commandParser);
            }
        } else {
            System.out.println("Registry was unable to be created");
        }
    }
}
