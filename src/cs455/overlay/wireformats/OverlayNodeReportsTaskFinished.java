package cs455.overlay.wireformats;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class OverlayNodeReportsTaskFinished implements Protocol, Event {
    private int type = OVERLAY_NODE_REPORTS_TASK_FINISHED;
    private String IP;
    private int portNum;
    private int nodeID;

    public OverlayNodeReportsTaskFinished(String IP, int portNum, int nodeID) {
        this.IP = IP;
        this.portNum = portNum;
        this.nodeID = nodeID;
    }

    @Override
    public int getType() {
        return type;
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
    public byte[] getBytes() throws IOException {
        /*  Msg outline:
            int:    OVERLAY_NODE_REPORTS_TASK_FINISHED
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

        dOut.flush();
        marshalledBytes = baOutStream.toByteArray();

        baOutStream.close();
        dOut.close();

        return marshalledBytes;
    }
}
