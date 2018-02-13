package cs455.overlay.wireformats;

import cs455.overlay.routing.RoutingTable;


import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;

public class EventFactory implements Protocol {

    // registry will use this
    private Event getNodeRegistration(DataInputStream dIn) throws IOException {
        // IP addr
        // use the length of the IP addr to figure out how many bytes to read
        int IPlength = dIn.readInt();
        byte[] IPbytes = new byte[IPlength];
        dIn.readFully(IPbytes);
        String IP = new String(IPbytes);

        // port num
        int msgNodePort = dIn.readInt();

        return new OverlayNodeSendsRegistration(IP, msgNodePort);
    }

    // msgNodes will use this
    private Event getNodeRegistrationStatus(DataInputStream dIn) throws IOException {
        // node ID assigned by registry; ID > -1 if success, otherwise -1 for failure
        int ID = dIn.readInt();

        // info string
        int infoStrLength = dIn.readInt();
        byte[] infoStrBytes = new byte[infoStrLength];
        dIn.readFully(infoStrBytes);
        String infoStr = new String(infoStrBytes);

        return new RegistryReportsRegistrationStatus(ID, infoStr);
    }

    // registry will use this
    private Event getNodeDeregistration(DataInputStream dIn) throws IOException {
        // IP addr
        int IPlength = dIn.readInt();
        byte[] IPbytes = new byte[IPlength];
        dIn.readFully(IPbytes);
        String IP = new String(IPbytes);

        // portNum
        int portNum = dIn.readInt();

        // assigned ID
        int assignedID = dIn.readInt();

        return new OverlayNodeSendsDeregistration(IP, portNum, assignedID);
    }

    // msgNodes will use this
    private Event getNodeDeregistrationStatus(DataInputStream dIn) throws IOException {
        // node ID removed by registry; ID > -1 if success, otherwise -1 for failure
        int deregisteredID = dIn.readInt();

        // info string
        int infoStrLength = dIn.readInt();
        byte[] infoStrBytes = new byte[infoStrLength];
        dIn.readFully(infoStrBytes);
        String infoStr = new String(infoStrBytes);

        return new RegistryReportsDeregistrationStatus(deregisteredID, infoStr);
    }

    // msgNodes will use this
    private Event getNodeManifest(DataInputStream dIn) throws IOException {
        // routing table size
        int routingTableSize = dIn.readInt();

        RoutingTable routingTable = new RoutingTable();

        for (int i = 0; i < routingTableSize; ++i) {
            // ID of node 2^i hops away
            int ID = dIn.readInt();

            // IP of node 2^i hops away
            int IPlength = dIn.readInt();
            byte[] IPbytes = new byte[IPlength];
            dIn.readFully(IPbytes);
            String IP = new String(IPbytes);

            // portNum of node 2^i hops away
            int portNum = dIn.readInt();

            // add an entry to routingTable with this info
            String IPportNumStr = IP + ':' + portNum;
            routingTable.addRoutingEntry(ID, IPportNumStr);
        }

        // num nodes in the system
        int numNodes = dIn.readInt();

        // get each ID of the nodes in the system
        ArrayList<Integer> registeredNodeIDs = new ArrayList<>();
        for (int i = 0; i < numNodes; ++i) {
            registeredNodeIDs.add(dIn.readInt());
        }

        return new RegistrySendsNodeManifest(routingTable, numNodes, registeredNodeIDs);
    }

    private Event getOverlaySetupStatus(DataInputStream dIn) throws IOException {
        // status; node ID if success, -1 if failure
        int status = dIn.readInt();

        // info str
        int infoStrLength = dIn.readInt();
        byte[] infoStrBytes = new byte[infoStrLength];
        dIn.readFully(infoStrBytes);
        String infoStr = new String(infoStrBytes);

        return new NodeReportsOverlaySetupStatus(status, infoStr);
    }

    private Event getTaskInitiate(DataInputStream dIn) throws IOException {
        // number of packets to send
        int numPacketsToSend = dIn.readInt();

        return new RegistryRequestsTaskInitiate(numPacketsToSend);
    }

    private Event getNodeSendsData(DataInputStream dIn) throws IOException {
        // dst ID
        int dstID = dIn.readInt();

        // src ID
        int srcID = dIn.readInt();

        // payload
        int payload = dIn.readInt();

        // routing trace
        int routingTraceLength = dIn.readInt();
        ArrayList<Integer> routingTrace = new ArrayList<>();
        for (int i = 0; i < routingTraceLength; ++i)
            routingTrace.add(dIn.readInt());

        return new OverlayNodeSendsData(dstID, srcID, payload, routingTrace);
    }

    // registry will use this
    private Event getTaskFinished(DataInputStream dIn) throws IOException {
        // IP addr
        int IPlength = dIn.readInt();
        byte[] IPbytes = new byte[IPlength];
        dIn.readFully(IPbytes);
        String IP = new String(IPbytes);

        // portNum
        int portNum = dIn.readInt();

        // nodeID
        int nodeID = dIn.readInt();

        return new OverlayNodeReportsTaskFinished(IP, portNum, nodeID);
    }

    private Event getTrafficSummary(DataInputStream dIn) throws IOException {
        return new RegistryRequestsTrafficSummary();
    }

    private Event getTrafficSummaryReport(DataInputStream dIn) throws IOException {
        // nodeID
        int nodeID = dIn.readInt();

        // total num packets sent
        int totalPacketsSent = dIn.readInt();

        // total num packets relayed
        int totalPacketsRelayed = dIn.readInt();

        // sum of packet data sent
        long sendSummation = dIn.readLong();

        // total num packets received
        int totalPacketsRcvd = dIn.readInt();

        // sum of packet data received
        long rcvSummation = dIn.readLong();

        return new OverlayNodeReportsTrafficSummary(nodeID, totalPacketsSent, totalPacketsRelayed, sendSummation, totalPacketsRcvd, rcvSummation);
    }

    // registry will use this
    public Event processMsg(byte[] msg) throws IOException {
        ByteArrayInputStream baInStream = new ByteArrayInputStream(msg);
        DataInputStream dIn = new DataInputStream(new BufferedInputStream(baInStream));

        int type = dIn.readInt();   // read msg type to decide what kind of msg to unmarshall

        // TODO: might need breaks after each return since case statements execute all that match?
        // Don't think so though bc the return should end the execution of this method...
        switch(type) {
            case (OVERLAY_NODE_SENDS_REGISTRATION):
                return getNodeRegistration(dIn);
            case (REGISTRY_REPORTS_REGISTRATION_STATUS):
                return getNodeRegistrationStatus(dIn);
            case (OVERLAY_NODE_SENDS_DEREGISTRATION):
                return getNodeDeregistration(dIn);
            case (REGISTRY_REPORTS_DEREGISTRATION_STATUS):
                return getNodeDeregistrationStatus(dIn);
            case (REGISTRY_SENDS_NODE_MANIFEST):
                return getNodeManifest(dIn);
            case (NODE_REPORTS_OVERLAY_SETUP_STATUS):
                return getOverlaySetupStatus(dIn);
            case (REGISTRY_REQUESTS_TASK_INITIATE):
                return getTaskInitiate(dIn);
            case (OVERLAY_NODE_SENDS_DATA):
                return getNodeSendsData(dIn);
            case (OVERLAY_NODE_REPORTS_TASK_FINISHED):
                return getTaskFinished(dIn);
            case (REGISTRY_REQUESTS_TRAFFIC_SUMMARY):
                return getTrafficSummary(dIn);
            case (OVERLAY_NODE_REPORTS_TRAFFIC_SUMMARY):
                return getTrafficSummaryReport(dIn);
        }

        // close the streams
        baInStream.close();
        dIn.close();

        return null;
    }
}
