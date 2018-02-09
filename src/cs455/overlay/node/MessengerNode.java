package cs455.overlay.node;

import cs455.overlay.routing.RoutingTable;
import cs455.overlay.transport.*;
import cs455.overlay.util.InteractiveCommandParser;
import cs455.overlay.wireformats.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;

public class MessengerNode implements Protocol, Node {
    private boolean DEBUG = false;

    private int ID;
    private String IP;
    private int portNum;
    private ServerSocket serverSocket;
    private TCPConnectionsCache connectionsCache;
    private String registryIPportNumStr;
    private RoutingTable routingTable;
    private ArrayList<Integer> registeredNodeIDs;

    // trackers and counters
    private int sndTracker = 0;
    private int rcvTracker = 0;
    private int relayTracker = 0;
    private long sndSummation = 0;
    private long rcvSummation = 0;

    // lock to update the trackers
    private final Object trackersLock = new Object();

    public MessengerNode(String IP, int portNum, ServerSocket serverSocket, String registryIPportNumStr) {
        this.IP = IP;
        this.portNum = portNum;
        this.serverSocket = serverSocket;
        this.connectionsCache = new TCPConnectionsCache();
        this.registryIPportNumStr = registryIPportNumStr;
    }

    public int getID() {
        return ID;

    }

    public String getIP() {
        return IP;
    }

    public int getPortNum() {
        return portNum;
    }

    public ServerSocket getServerSocket() {
        return serverSocket;
    }

    public TCPConnectionsCache getConnectionsCache() {
        return connectionsCache;
    }

    public String getRegistryIPportNumStr() {
        return registryIPportNumStr;
    }

    public int getSndTracker() {
        return sndTracker;
    }

    public int getRcvTracker() {
        return rcvTracker;
    }

    public int getRelayTracker() {
        return relayTracker;
    }

    public long getSndSummation() {
        return sndSummation;
    }

    public long getRcvSummation() {
        return rcvSummation;
    }

    @Override
    public void onEvent(Event event, TCPConnection connection) throws IOException {
        switch(event.getType()) {
            case (REGISTRY_REPORTS_REGISTRATION_STATUS):
                processRegistrationStatusResponse((RegistryReportsRegistrationStatus)event, connection);
                break;
            case (REGISTRY_REPORTS_DEREGISTRATION_STATUS):
                processDeregistrationStatusResponse((RegistryReportsDeregistrationStatus)event, connection);
                break;
            case (REGISTRY_SENDS_NODE_MANIFEST):
                processNodeManifest((RegistrySendsNodeManifest)event);
                break;
            case (REGISTRY_REQUESTS_TASK_INITIATE):
                processTaskInitiate((RegistryRequestsTaskInitiate)event);
                break;
            case (OVERLAY_NODE_SENDS_DATA):
                processNodeSendsData((OverlayNodeSendsData)event);
                break;
        }
    }

    private void processRegistrationStatusResponse(RegistryReportsRegistrationStatus event, TCPConnection connection) throws IOException {
        if (event.getID() > -1) {
            this.ID = event.getID();

            // Cache the socket connected to the registry
            // Cached this only if successfully registered, otherwise no need to cache connection to registry
            // TODO: UPDATE THIS COMMENT TO REFLECT THE NEW TCP CONNECTION CHANGES
            // TCPConnection connection = connectionsCache.getConnection(registryIPportNumStr);
            // TODO: DIDN'T START THE SENDER AND RECEIVER THREADS HERE BECAUSE I DID IT IN THE TCPCONNECTION CTOR; IF IT DOESN'T WORK, START IT HERE
            // connectionsCache.addConnection(registryIPportNumStr, connection);
            // TODO: didnt cache connection here bc it is already done when this msging node initiates a connec w registry.
            // TODO: also dont need to start sndr and rcvr threads for this connec since this msging node does that when it
            // TODO: first creates this connection to register itself to the registry (REFINE THESE COMMENTS)
            System.out.printf("Cached connection with %s using socket: %s\n", registryIPportNumStr, connection.getSocket());
            System.out.println(event.getInfoStr());
            System.out.println("My assigned ID is: " + this.ID);
        } else { // registration failure
            System.out.println(event.getInfoStr());
        }
    }

    // TODO: might need to change TCPConnection to socket once I get to removing the connection from the cache
    private void processDeregistrationStatusResponse(RegistryReportsDeregistrationStatus event, TCPConnection connection) {
        int deregisteredID = event.getDeregisteredID();
        System.out.println(event.getInfoStr());
        System.out.printf("The deregistered ID was %d\n", deregisteredID);
        // TODO: remove the connection to registry from connectionsCache
        // Do I need to do this?
        // ALSO: is there a way to not have a connectionsCache for each msging node and only have it for the registry
        // the msging nodes are only snding msgs forward and never backwards, so technically they only need to
        // remember their sockets that they used to connect to the other msging node
        // Idk though, as of 2:38pm on 2/2/18, I'm thinking that having a connections cache is good
        System.out.println("Uncached connection to Registry");
    }

    private void processNodeManifest(RegistrySendsNodeManifest event) throws IOException {
        routingTable = event.getRoutingTable();

        // Print information about the routing table received
        System.out.println("Received node manifest from registry");
        System.out.printf("Routing table size: %d\nRouting table:\n", routingTable.size());
        ArrayList<Integer> routingTableIDs = routingTable.getKeys();
        for (int i = 0; i < routingTable.size(); ++i) {
            System.out.printf("Entry %d: %s\n", i+1, routingTableIDs.get(i));
        }

        registeredNodeIDs = event.getRegisteredNodeIDs();
        System.out.printf("Nodes in the system: %s\n", Arrays.toString(registeredNodeIDs.toArray()));

        // if this ever becomes false, it means this node was not able to establish connections to all nodes in its routing table
        boolean connectionsEstablished = true;

        // Establish connections with every node in the rcvd routing table. Cache the connections also
        for(Map.Entry<Integer, String> entry : routingTable.getEntrySet()) {
            try {
                String[] IPportNumArr = entry.getValue().split(":");
                Socket socket = new Socket(IPportNumArr[0], Integer.parseInt(IPportNumArr[1]));
                // TODO: COMMENT ABOUT THE NEW TCPCONNECTION HERE; UNCOMMENT THE startSenderAndReceiverThreads() BELOW IF CTOR CALL DOESN'T WORK
                // connection to a node in the routing table
                TCPConnection connection = new TCPConnection(socket, this);
                connectionsCache.addConnection(entry.getValue(), connection);
                // TODO: refine comments here
                // the rcvr and sndr thread for this node's side of the connection (pipe)
                // when the other node receives the connection request from the socket above,
                // it will start its own sndr and rcvr threads for its end of the pipe
                connection.startSenderAndReceiverThreads();
            } catch(IOException ioe) {
                connectionsEstablished = false;
                break;
            }
        }

        int status;
        String infoStr;
        if (connectionsEstablished) {
            status = this.ID;
            infoStr = String.format("Messaging Node (%d) established connections to messaging nodes in its routing table successfully", this.ID);
            System.out.println("Successfully established connections. connectionsCache:");
            System.out.println(connectionsCache);
        } else {
            status = -1;
            infoStr = String.format("Messaging Node (%d) failed to establish connections to messaging nodes in its routing table", this.ID);
        }
        // TODO: MAKE SURE THIS IS COMMENTED THOROUGHLY WITH NEW TCP CONNECTION CHANGES; UPDATE COMMENTS WITH NEW CTOR START THREADS
        // registryConnection's sender and receiver threads should have been started when the registry confirmed
        // this node's registration. after confirming registration, this node should have cached the connection
        // and started the sender and receiver threads in the processNodeRegistrationStatusResponse() above
        TCPConnection registryConnection = connectionsCache.getConnection(registryIPportNumStr);
        NodeReportsOverlaySetupStatus overlaySetupStatus = new NodeReportsOverlaySetupStatus(status, infoStr);
        registryConnection.getSenderThread().addMessage(overlaySetupStatus.getBytes());
    }

    private int selectRandomDstID() {
        // choose a random ID from the list of all registered nodes
        Random r = new Random();
        // random index between 0 and registeredNodeIDs.size() - 1
        int randIndex = r.nextInt(registeredNodeIDs.size());
        // use randIndex to retrieve a node to send a packet to
        int dstID = registeredNodeIDs.get(randIndex);

        // ensure a node does not choose to send msgs to itself
        while (dstID == this.ID) {
            randIndex = r.nextInt(registeredNodeIDs.size());
            dstID = registeredNodeIDs.get(randIndex);
        }
        return dstID;
    }

    // finds the closest node to the given dst ID and returns the connection to it
    // TODO: NEED TO REDO THIS TO NOT USE THE LIST OF ALL NODES IN THE SYSTEM
    private TCPConnection findClosestNode(int dstID) {
        // store connection to closest node
        TCPConnection routingConnection = null;

        int numHopsToDst = 0;
        // to find hops to dst, have to start at src ID (this node's) and count clockwise
        int currIdx = registeredNodeIDs.indexOf(this.ID);
        while(registeredNodeIDs.get(currIdx) != dstID) {
            ++numHopsToDst;
            currIdx = (currIdx + 1) % registeredNodeIDs.size();
        }
        // after this loop, we should have the correct num hops to the dst

        ArrayList<Map.Entry<Integer, String>> routingTableList = new ArrayList<>(routingTable.getEntrySet());

        /*
            should always get overwritten in the loop since if the dstID is not in the routing table,
            then with the way the tbl is set up, at least 2 entries must be before the dstID
            (b/c if dstID is not in table, then it is at least 3 or more hops away and 1st & 2nd entries are only 1 & 2 hops away)
         */
        int closestID = -1;
        for(int entry = 0; entry < routingTableList.size(); ++entry) {
            if(Math.pow(2, entry) < numHopsToDst) {
                closestID = routingTableList.get(entry).getKey();
                // get the connection to the current closest node
                routingConnection = connectionsCache.getConnection(routingTableList.get(entry).getValue());
            }
        }
        // after this loop, we should have the connection to the closest node w/o overshooting

        if (DEBUG)
            System.out.printf("Node (%d) is not in my routing table. Routing data to closest node: node (%d)\n", dstID, closestID);

        return routingConnection;
    }

    private void processTaskInitiate(RegistryRequestsTaskInitiate event) throws IOException {
        System.out.printf("Task initiate received. Starting to send %d packets\n", event.getNumPacketsToSend());

        // Begin sending msgs
        int numRounds = event.getNumPacketsToSend();
        for (int round = 0; round < numRounds; ++round) {
            // Choose a random node to send data to
            int dstID = selectRandomDstID();

            TCPConnection routingConnection;
            if (routingTable.contains(dstID)) { // dst is in routing table
                String IPportNumStr = routingTable.getEntry(dstID);

                if (DEBUG)
                    System.out.printf("Node (%d) is in my routing table. Sending data to %s\n", dstID, IPportNumStr);

                // Retrieve the connection to the dst node
                routingConnection = connectionsCache.getConnection(IPportNumStr);
            } else { // dst is not in routing table
                // find closest node to route data to
                routingConnection = findClosestNode(dstID);
            }

            // Send packet to the next node with a random int (-2mil to 2mil) as the payload
            Random r = new Random();
            int payload = r.nextInt();

            // update the send trackers and summations for this node
            ++this.sndTracker;
            this.sndSummation += payload;

            OverlayNodeSendsData nodeSendsData = new OverlayNodeSendsData(dstID, this.ID, payload, new ArrayList<>());
            routingConnection.getSenderThread().addMessage(nodeSendsData.getBytes());
        }
    }

    private void processNodeSendsData(OverlayNodeSendsData event) throws IOException {
        int dstID = event.getDstID();
        int srcID = event.getSrcID();
        int payload = event.getPayload();
        ArrayList<Integer> routingTrace = event.getRoutingTrace();

        // will hold connection that we should route packet to
        TCPConnection routingConnection;

        if (dstID == this.ID) {
            // this is the dst
            // update trackers and summations
            synchronized(trackersLock) {
                ++this.rcvTracker;
                this.rcvSummation += payload;
            }
        } else if (routingTable.contains(dstID)){
            // dst is in routing table
            routingConnection = connectionsCache.getConnection(routingTable.getEntry(dstID));

            synchronized(trackersLock) {
                ++this.relayTracker;
            }

            OverlayNodeSendsData nodeSendsData = new OverlayNodeSendsData(dstID, srcID, payload, routingTrace);
            routingConnection.getSenderThread().addMessage(nodeSendsData.getBytes());
        } else {
            // dst is not in routing table; choose closest node
            routingConnection = findClosestNode(dstID);

            // don't have to check if this is the src node b/c once a msg gets sent, it should never arrv back at the src
            // and since this is also not the sink for the msg, this node is relaying a msg, so add this ID to the routing trace
            // also update the relayTracker
            routingTrace.add(this.ID);

            synchronized(trackersLock) {
                ++this.relayTracker;
            }

            OverlayNodeSendsData nodeSendsData = new OverlayNodeSendsData(dstID, srcID, payload, routingTrace);
            routingConnection.getSenderThread().addMessage(nodeSendsData.getBytes());
        }
    }

    public static void main(String[] args) {
        // args[0]: registry IP
        // args[1]: registry portNum

        // Get the InetAddress obj for this node
        InetAddress inetInfo = null;
        try {
            inetInfo = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            System.err.println("Unable to find local host " + e);
            System.exit(1);
        }

        if (inetInfo != null) {
            // retrieve the hostname
            String hostname = inetInfo.getHostName();
            System.out.println("hostname: " + hostname);

            // retrieve the IP addr
            String IP = inetInfo.getHostAddress();
            System.out.println("IP addr: " + IP);

            ServerSocket serverSocket;
            try {
                // Create the server socket and have it listen on any port
                serverSocket = new ServerSocket(0);

                // IP and portNum of registry converted to a string
                String registryIPportNumStr = args[0] + ':' + args[1];

                // create messenger node
                MessengerNode msgNode = new MessengerNode(IP, serverSocket.getLocalPort(), serverSocket, registryIPportNumStr);

                // start the server of the msging node in a different thread so it can do other tasks while listening for connections
                (new Thread(new TCPServerThread(msgNode))).start();
                System.out.println("Node listening on port: " + serverSocket.getLocalPort());

                // Initiate a connection to registry to send a registration request
                Socket commSocket = new Socket(args[0], Integer.parseInt(args[1]));

                /*
                    Create a TCPConnection to store the info about the connection
                    This allows reuse of the created socket for subsequent communication
                    The node is passed in b/c the TCPReceiverThread will use it to call
                    the node's onEvent() function after reconstructing a received msg
                 */
                TCPConnection registryConnection = new TCPConnection(commSocket, msgNode);

                /*
                    Start the sender and receiver threads for this connection so the user
                    can enter commands while this node is sending and receiving msgs
                */
                registryConnection.startSenderAndReceiverThreads();

                /*
                    Add the connection to the registry into this node's connection cache
                    This will allow us to fetch the connection later when we need to talk
                    to the registry again
                 */
                msgNode.getConnectionsCache().addConnection(registryIPportNumStr, registryConnection);

                // create registration request msg
                OverlayNodeSendsRegistration nodeRegistration = new OverlayNodeSendsRegistration(msgNode.IP, msgNode.portNum);

                // Retrieve the sender thread of this connection and queue a msg to be sent
                registryConnection.getSenderThread().addMessage(nodeRegistration.getBytes());
                System.out.printf("Sending reg req to regsitry on socket: %s\n", commSocket);

                InteractiveCommandParser commandParser = new InteractiveCommandParser(msgNode);
                Scanner sc = new Scanner(System.in);
                System.out.print("Please enter a command for the registry to execute (e.g. 'print-counters-and-diagnostics', 'exit-overlay', 'quit' to exit):\n");
                while (sc.hasNextLine()) {
                    String command = sc.nextLine();

                    switch (command) {
                        case ("print-counters-and-diagnostics"):
                            commandParser.printCountersAndDiagnostics();
                            break;
                        case ("exit-overlay"):
                            commandParser.exitOverlay();
                            break;
                        default:
                            System.out.println("Please enter a valid command listed above");
                    }
                    System.out.print("Please enter a command for the registry to execute (e.g. print-counters-and-diagnostics, exit-overlay, CTRL-D to exit):\n");
                }
            } catch(IOException e) {
                System.err.println("Unable to create socket: " + e);
                // TODO: check if we should exit on failed socket creation or not
                System.exit(1);
            }
        }
    }
}