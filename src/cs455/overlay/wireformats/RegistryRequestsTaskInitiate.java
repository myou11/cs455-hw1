package cs455.overlay.wireformats;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class RegistryRequestsTaskInitiate implements Protocol, Event {
    private int type = REGISTRY_REQUESTS_TASK_INITIATE;
    private int numPacketsToSend;

    public RegistryRequestsTaskInitiate(int numPacketsToSend) {
        this.numPacketsToSend = numPacketsToSend;
    }

    public int getNumPacketsToSend() {
        return numPacketsToSend;
    }

    @Override
    public int getType() {
        return type;
    }

    @Override
    public byte[] getBytes() throws IOException {
        /*
            Msg outline:
                byte: msg type (REGISTRY_REQUESTS_TASK_INITIATE)
                int:  number of data packets to send
         */
        byte[] marshalledBytes = null;

        ByteArrayOutputStream baOutStream = new ByteArrayOutputStream();
        DataOutputStream dOut = new DataOutputStream(baOutStream);

        // msg type
        dOut.writeInt(type);

        // number of data packets to send
        dOut.writeInt(numPacketsToSend);

        dOut.flush();
        marshalledBytes = baOutStream.toByteArray();

        baOutStream.close();
        dOut.close();

        return marshalledBytes;
    }
}
