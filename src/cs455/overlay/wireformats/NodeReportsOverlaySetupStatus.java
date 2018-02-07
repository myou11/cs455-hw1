package cs455.overlay.wireformats;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class NodeReportsOverlaySetupStatus implements Protocol, Event {
    private int type = NODE_REPORTS_OVERLAY_SETUP_STATUS;
    private int status;
    private String infoStr;

    public NodeReportsOverlaySetupStatus(int status, String infoStr) {
        this.status = status;
        this.infoStr = infoStr;
    }

    public int getStatus() {
        return status;
    }

    public String getInfoStr() {
        return infoStr;
    }

    @Override
    public int getType() {
        return type;
    }

    @Override
    public byte[] getBytes() throws IOException {
        /*
            Msg outline:
                byte: msg type (NODE_REPORTS_OVERLAY_SETUP_STATUS)
                int:  success status; ID if success, -1 if failure
                int:  length of following info str field
                byte: info str
         */
        byte[] marshalledBytes = null;

        ByteArrayOutputStream baOutStream = new ByteArrayOutputStream();
        DataOutputStream dOut = new DataOutputStream(baOutStream);

        // msg type
        dOut.writeInt(type);

        // status; ID if success, -1 if failure
        dOut.writeInt(status);

        // infoStr
        byte[] infoStrBytes = infoStr.getBytes();
        int infoStrLength = infoStrBytes.length;
        dOut.writeInt(infoStrLength);
        dOut.write(infoStrBytes);

        dOut.flush();
        marshalledBytes = baOutStream.toByteArray();

        baOutStream.close();
        dOut.close();

        return marshalledBytes;
    }
}
