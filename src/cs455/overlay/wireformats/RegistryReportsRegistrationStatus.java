package cs455.overlay.wireformats;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import cs455.overlay.node.Registry;

public class RegistryReportsRegistrationStatus implements Protocol, Event {
    private int type = REGISTRY_REPORTS_REGISTRATION_STATUS;
    private int ID;
    private String infoStr;

    public RegistryReportsRegistrationStatus(int ID, String infoStr) {
        this.ID = ID;
        this.infoStr = infoStr;
    }

    public int getID() { return ID; }

    public String getInfoStr() { return infoStr; }

    @Override
    public int getType() {
        return type;
    }

    // marshall this msg into a byte arr
    @Override
    public byte[] getBytes() throws IOException {
        /*
            Msg outline:
                byte: msg type (REGISTRY_REPORTS_REGISTRATION_STATUS)
                int:  success status; ID if success, -1 if failure
                byte: length of following infoStr field
                byte: info string
         */
        byte[] marshalledBytes = null;

        ByteArrayOutputStream baOutStream = new ByteArrayOutputStream();
        DataOutputStream dOut = new DataOutputStream(new BufferedOutputStream(baOutStream));

        // msg type
        dOut.writeInt(type);

        // success status
        dOut.writeInt(ID);

        // info string
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
