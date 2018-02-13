package cs455.overlay.wireformats;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class OverlayNodeSendsDeregistration implements Protocol, Event {
    private int type = OVERLAY_NODE_SENDS_DEREGISTRATION;
    private String IP;
    private int portNum;
    private int nodeID;

    public OverlayNodeSendsDeregistration(String IP, int portNum, int nodeID) {
        this.IP = IP;
        this.portNum = portNum;
        this.nodeID = nodeID;
    }

    public String getIP() {
        return IP;
    }

    public int getPortNum() {
        return portNum;
    }

    public int getNodeID() {
        return nodeID;
    }

    @Override
    public int getType() {
        return type;
    }

    // marshall this msg into a byte arr
    @Override
    public byte[] getBytes() throws IOException {
        /*  Msg outline:
            int:    msg type (OVERLAY_NODE_SENDS_DEREGISTRATION)
            int:    length of following IP field
            byte[]: IP addr
            int:    portNum
            int:    nodeID  */
        byte[] marshalledBytes = null;

        ByteArrayOutputStream baOutStream = new ByteArrayOutputStream();
        DataOutputStream dOut = new DataOutputStream(baOutStream);

        dOut.writeInt(type);

        byte[] IPbytes = IP.getBytes();
        int IPlength = IPbytes.length;
        dOut.writeInt(IPlength);
        dOut.write(IPbytes);

        dOut.writeInt(portNum);

        dOut.writeInt(nodeID);

        // write buffer to the stream
        dOut.flush();
        marshalledBytes = baOutStream.toByteArray();

        // close streams
        baOutStream.close();
        dOut.close();

        return marshalledBytes;
    }
}
