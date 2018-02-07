package cs455.overlay.wireformats;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class OverlayNodeSendsData implements Protocol, Event {
    private int type = OVERLAY_NODE_SENDS_DATA;
    private int dstID;
    private int srcID;
    private int payload;
    // includes the nodes (except the src and sink) that routed this packet
    private ArrayList<Integer> routingTrace;

    public OverlayNodeSendsData(int dstID, int srcID, int payload, ArrayList<Integer> routingTrace) {
        this.dstID = dstID;
        this.srcID = srcID;
        this.payload = payload;
        this.routingTrace = routingTrace;
    }

    public int getDstID() {
        return dstID;
    }

    public int getSrcID() {
        return srcID;
    }

    public int getPayload() {
        return payload;
    }

    public ArrayList<Integer> getRoutingTrace() {
        return routingTrace;
    }

    @Override
    public int getType() {
        return type;
    }

    @Override
    public byte[] getBytes() throws IOException {
        /*
            Msg outline:
            byte: msg type (OVERLAY_NODE_SENDS_DATA)
            int:  dst ID
            int:  src ID
            int:  payload
            int:  length of following routing trace field
            int[]: routing trace
         */
        byte[] marshalledBytes = null;

        ByteArrayOutputStream baOutStream = new ByteArrayOutputStream();
        DataOutputStream dOut = new DataOutputStream(baOutStream);

        // msg type
        dOut.writeInt(type);

        // dst ID
        dOut.writeInt(dstID);

        // src ID
        dOut.writeInt(srcID);

        // payload
        dOut.writeInt(payload);

        // routing trace
        dOut.writeInt(routingTrace.size());
        for (Integer trace : routingTrace)
            dOut.writeInt(trace);

        dOut.flush();
        marshalledBytes = baOutStream.toByteArray();

        baOutStream.close();
        dOut.flush();

        return marshalledBytes;
    }
}
