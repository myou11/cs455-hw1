package cs455.overlay.wireformats;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class RegistryRequestsTrafficSummary implements Protocol, Event {
    private int type = REGISTRY_REQUESTS_TRAFFIC_SUMMARY;

    @Override
    public int getType() {
        return type;
    }

    @Override
    public byte[] getBytes() throws IOException {
        /*  Msg outline:
            int: type  */
        byte[] marshalledBytes = null;

        ByteArrayOutputStream baOutStream = new ByteArrayOutputStream();
        DataOutputStream dOut = new DataOutputStream(baOutStream);

        dOut.writeInt(type);

        dOut.flush();
        marshalledBytes = baOutStream.toByteArray();

        baOutStream.close();
        dOut.close();

        return marshalledBytes;
    }
}
