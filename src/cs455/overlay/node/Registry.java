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
        Access to this map will be syncd since registry could rcv many registration/deregistration requests at once  */
    private TreeMap<Integer, String> registeredNodes = new TreeMap<>();
    // store sockets used to communicate with other nodes so we dont have to create a new socket for each communication (snd/rcv)
    private TCPConnectionsCache connectionsCache;
    private ServerSocket serverSocket;
    private int numNodesEstablishedConnections = 0;

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
        return registeredNodes.containsValue(IPportNumStr) && connectionIP.equals(IP);
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
            this.getConnectionsCache().addConnection(IPportNumStr, connection);

            if (DEBUG)
                System.out.printf("Registered node from %s, ID is %d\n", registeredNodes.get(ID), ID);
        } else { // invalid registration request
            // TODO: TEST IF IT WILL FAIL IF WE TRY TO REGISTER A NODE MORE THAN ONCE. JUST CODE SEND A REG REQ TWICE IN THE MSG NODE
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
        int idToRemove = event.getAssignedID();
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
        } else if (numNodesEstablishedConnections == registeredNodes.size())
            System.out.println("All nodes in the overlay were successful in establishing connections to nodes that comprised their routing table");
        else
            System.out.println("Not all nodes in the overlay were successful in establishing connections to nodes that comprised their routing table");
    }

    private void processCommand(String[] command, InteractiveCommandParser commandParser) {
        // carry out different tasks based on the command specified
        switch (command[0]) {
            case ("list-messaging-nodes"):
                commandParser.listMessagingNodes();
                break;
            case ("setup-overlay"):
                if (command.length == 1) {
                    // default routing table size is 3 if not specific
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
