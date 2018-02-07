package cs455.overlay.wireformats;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class RegistryReportsDeregistrationStatus implements Protocol, Event {
    private int type = REGISTRY_REPORTS_DEREGISTRATION_STATUS;
    private int deregisteredID;
    private String infoStr;

    public RegistryReportsDeregistrationStatus(int deregisteredID, String infoStr) {
        this.deregisteredID = deregisteredID;
        this.infoStr = infoStr;
    }

    public int getDeregisteredID() {
        return deregisteredID;
    }

    public String getInfoStr() {
        return infoStr;
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
                byte: msg type (REGISTRY_REPORTS_DEREGISTRATION_STATUS)
                int:  success status; deregistered ID if success, -1 if failure
                byte: length of following info str field
                byte: info str
         */
        byte[] marshalledBytes = null;

        ByteArrayOutputStream baOutStream = new ByteArrayOutputStream();
        DataOutputStream dOut = new DataOutputStream(baOutStream);

        // REGISTRY_REPORTS_DEREGISTRATION_STATUS
        dOut.writeInt(type);

        // success status
        dOut.writeInt(deregisteredID);

        // len of info str
        byte[] infoStrBytes = infoStr.getBytes();
        int infoStrLength = infoStrBytes.length;
        dOut.writeInt(infoStrLength);
        dOut.write(infoStrBytes);

        // write buffer to the stream
        dOut.flush();
        marshalledBytes = baOutStream.toByteArray();

        // close streams
        baOutStream.close();
        dOut.close();

        return marshalledBytes;
    }
}
