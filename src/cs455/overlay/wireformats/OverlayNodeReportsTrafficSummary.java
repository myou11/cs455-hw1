package cs455.overlay.wireformats;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class OverlayNodeReportsTrafficSummary implements Protocol, Event {
    private int type = OVERLAY_NODE_REPORTS_TRAFFIC_SUMMARY;
    private int ID;
    private int totalPacketsSent;
    private int totalPacketsRelayed;
    private long sendSummation;
    private int totalPacketsRcvd;
    private long rcvSummation;

    public int getID() {
        return ID;
    }

    public int getTotalPacketsSent() {
        return totalPacketsSent;
    }

    public int getTotalPacketsRelayed() {
        return totalPacketsRelayed;
    }

    public long getSendSummation() {
        return sendSummation;
    }

    public int getTotalPacketsRcvd() {
        return totalPacketsRcvd;
    }

    public long getRcvSummation() {
        return rcvSummation;
    }

    public OverlayNodeReportsTrafficSummary(int ID, int totalPacketsSent, int totalPacketsRelayed, long sendSummation, int totalPacketsRcvd, long rcvSummation) {
        this.ID = ID;
        this.totalPacketsSent = totalPacketsSent;
        this.totalPacketsRelayed = totalPacketsRelayed;
        this.sendSummation = sendSummation;
        this.totalPacketsRcvd = totalPacketsRcvd;
        this.rcvSummation = rcvSummation;
    }

    @Override
    public int getType() {
        return type;
    }

    @Override
    public byte[] getBytes() throws IOException {
        /*  Msg Outline:
            int: type
            int: ID
            int: totalPacketsSent
            int: totalPacketsRelayed
            long: sendSummation
            int: totalPacketsRcvd
            long: rcvSummation
         */
        byte[] marshalledBytes = null;

        ByteArrayOutputStream baOutStream = new ByteArrayOutputStream();
        DataOutputStream dOut = new DataOutputStream(baOutStream);

        dOut.writeInt(type);

        dOut.writeInt(ID);

        dOut.writeInt(totalPacketsSent);

        dOut.writeInt(totalPacketsRelayed);

        dOut.writeLong(sendSummation);

        dOut.writeInt(totalPacketsRcvd);

        dOut.writeLong(rcvSummation);

        dOut.flush();
        marshalledBytes = baOutStream.toByteArray();

        baOutStream.close();
        dOut.close();

        return marshalledBytes;
    }
}
