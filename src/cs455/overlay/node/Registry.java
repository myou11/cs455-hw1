package cs455.overlay.node;

import cs455.overlay.transport.TCPConnection;
import cs455.overlay.transport.TCPConnectionsCache;
import cs455.overlay.transport.TCPSenderThread;
import cs455.overlay.transport.TCPServerThread;
import cs455.overlay.util.InteractiveCommandParser;
import cs455.overlay.wireformats.*;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Registry implements Protocol, Node {

    // might not be thread-safe
    // https://stackoverflow.com/questions/40471/differences-between-hashmap-and-hashtable
    // using hashmap bc of below answer; basically hashtable is outdated and rarely used in modern Java code
    // https://stackoverflow.com/questions/9536077/when-should-i-use-a-hashtable-versus-a-hashmap
    private int portNum;
    // TODO: should synchronize this hash map later; made static bc there is only 1 registry, so the registeredNodes map will be the same
    // Integer will be IDs of the registered nodes and String will be a string representation of IP and portNum of registered node
    // TODO: IS IT OKAY TO MAKE THIS STATIC??? There's only one registry so why not? ASK
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
    public void onEvent(Event event, Socket socket) throws IOException {
        switch(event.getType()) {
            case (OVERLAY_NODE_SENDS_REGISTRATION):
                registerNode((OverlayNodeSendsRegistration)event, socket);
                break;
            case (OVERLAY_NODE_SENDS_DEREGISTRATION):
                deregisterNode((OverlayNodeSendsDeregistration)event, socket);
                break;
            case (NODE_REPORTS_OVERLAY_SETUP_STATUS):
                processOverlaySetupStatusResponse((NodeReportsOverlaySetupStatus)event);
        }
    }

    // TODO: remember to make the necessary functions synchronized if they modify the connectionsCache or registeredNodes
    private synchronized void registerNode(OverlayNodeSendsRegistration event, Socket socket) throws IOException {
        // TODO: abstract out assigning ID to an assignID maybe?
        Random r = new Random();
        int ID = r.nextInt(128); // chooses ID between 0-127 (inclusive)
        String infoStr;
        // create a string to represent the IP and portNum of msging node. use this as key into HashMap
        String IPportNumStr = event.getIP() + ':' + event.getPortNum();

        // TODO: ADD TCPCONNECTION COMMENTS IF WORK
        TCPConnection connection = new TCPConnection(socket, this);
        // TODO: CREATE METHOD TO CHECK IF VALID REGISTRATION
        if (registeredNodes.get(ID) == null && !registeredNodes.containsValue(IPportNumStr)) {
            // ID is not a duplicate and node's IP and portNum haven't been previously registered
            // TODO: still need to check if it came from the correct IP addr

            registeredNodes.put(ID, IPportNumStr);
            System.out.printf("Registered node from %s, ID is %d\n", registeredNodes.get(ID), ID);

            infoStr = "Registration request successful. The number of messaging nodes currently constituting " +
                    "the overlay is (" + registeredNodes.size() + ")";

            // add the connection into the registry's connection cache, so we can use it for communication later
            this.getConnectionsCache().addConnection(IPportNumStr, connection);
        } else { // invalid registration request
            // ID is a duplicate or node's IP and portNum have been previously registered
            // or IP addr in msg didnt match IP addr of Socket it was sent from
            // TODO: TEST IF IT WILL FAIL IF WE TRY TO REGISTER A NODE MORE THAN ONCE. JUST CODE SEND A REG REQ TWICE IN THE MSG NODE
            infoStr = "Registration request failed. There is either (1) no more room in the Registry, " +
                    "(2) this node has already been registered, or (3) the IP address in the request did not" +
                    "match the IP address of the origin";
        }

        RegistryReportsRegistrationStatus registrationStatus = new RegistryReportsRegistrationStatus(ID, infoStr);
        // TODO: ADD TCPCONNECTION COMMENTS IF WORK
        connection.sendMsg(registrationStatus.getBytes());
        /*try {
            System.out.printf("Socket we are using to communicate with %s: %s\n", IPportNumStr, connectionsCache.getConnection(IPportNumStr));
            (new Thread(new TCPSenderThread(connectionsCache.getConnection(IPportNumStr), response.getBytes()))).start();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }*/
    }

    private synchronized void deregisterNode(OverlayNodeSendsDeregistration event, Socket socket) throws IOException {
        int idToRemove = event.getAssignedID();
        String infoStr;

        // if getting the node with idToRemove returns null, node doesn't exist in registry anymore, so can't deregister
        // TODO: NEED TO CHECK IF IP OF MSG NODE'S END OF SOCKET IS SAME AS IP IN MSG
        if (socket.getInetAddress().getHostAddress().equals(event.getIP()) || registeredNodes.containsKey(idToRemove)) { // valid deregistration req
            String removed = registeredNodes.remove(idToRemove);
            System.out.printf("Removed node with ID [%d] and value [%s] from registeredNodes\n", idToRemove, removed);

            infoStr = "Deregistration request successful. The number of messaging nodes currently constituting " +
                    "the overlay is (" + registeredNodes.size() + ")";

        } else { // invalid deregistration request
            infoStr = "Deregistration request failed. The node was (1) not registered in the system or (i.e. deregistered already or was never registered) " +
                    "(2) the IP address in the request did not match the IP address of the origin";
        }

        RegistryReportsDeregistrationStatus deregistrationStatus = new RegistryReportsDeregistrationStatus(idToRemove, infoStr);
        // TODO: ADD TCPCONNECTION COMMENTS IF WORK
        // remove the registry's connection (socket) from the connectionsCache
        // TODO: check if this actually gets the msging nodes IP and port
        String IPportNumStr = socket.getInetAddress().getHostAddress() + ':' + socket.getPort();
        TCPConnection connection = connectionsCache.removeConnection(IPportNumStr);
        connection.sendMsg(deregistrationStatus.getBytes());
        /*try {
            String IPportNumStr = event.getIP() + ':' + event.getPortNum();
            (new Thread(new TCPSenderThread(connectionsCache.getConnection(IPportNumStr), response.getBytes()))).start();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }*/
    }

    // TODO: should probably sync this method since registry could get many responses from the nodes at once and don't want to let multiple rcvr threads modify this var at the same time
    private synchronized void processOverlaySetupStatusResponse(NodeReportsOverlaySetupStatus event) {
        if (event.getStatus() > -1) {
            System.out.println(event.getInfoStr());
            ++numNodesEstablishedConnections;
        }
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
                // TODO: SHOULD WE BE ABLE TO HANDLE THE CASE WHERE size < 3 IS CHOSEN? OR IS 3 THE MIN NUM OF NODES THAT MUST BE IN ROUTING TABLE? ASK
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
            // TODO: might need to get rid of 1 of these prints?
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
