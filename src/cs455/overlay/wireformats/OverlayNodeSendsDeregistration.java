package cs455.overlay.wireformats;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class OverlayNodeSendsDeregistration implements Protocol, Event {
    private int type = OVERLAY_NODE_SENDS_DEREGISTRATION;
    private String IP;
    private int portNum;
    private int assignedID;

    public OverlayNodeSendsDeregistration(String IP, int portNum, int assignedID) {
        this.IP = IP;
        this.portNum = portNum;
        this.assignedID = assignedID;
    }

    public String getIP() {
        return IP;
    }

    public int getPortNum() {
        return portNum;
    }

    public int getAssignedID() {
        return assignedID;
    }

    @Override
    public int getType() {
        return type;
    }

    // marshall this msg into a byte arr
    @Override
    public byte[] getBytes() throws IOException {
        /*
            Msg outline:
            byte: msg type (OVERLAY_NODE_SENDS_DEREGISTRATION)
            byte: length of following IP field
            byte: IP addr
            int:  portNum
            int:  assigned node ID
         */
        byte[] marshalledBytes = null;

        ByteArrayOutputStream baOutStream = new ByteArrayOutputStream();
        DataOutputStream dOut = new DataOutputStream(baOutStream);

        // msg type
        dOut.writeInt(type);

        byte[] IPbytes = IP.getBytes();
        int IPlength = IPbytes.length;
        // len of IP addr
        dOut.writeInt(IPlength);
        // IP addr in bytes
        dOut.write(IPbytes);

        // portNum
        dOut.writeInt(portNum);

        // assigned ID
        dOut.writeInt(assignedID);

        // write buffer to the stream
        dOut.flush();
        marshalledBytes = baOutStream.toByteArray();

        // close streams
        baOutStream.close();
        dOut.close();

        return marshalledBytes;
    }
}
