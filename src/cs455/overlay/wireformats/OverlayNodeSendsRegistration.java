package cs455.overlay.wireformats;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class OverlayNodeSendsRegistration implements Protocol, Event {
    private int type = OVERLAY_NODE_SENDS_REGISTRATION;
    private String IP;
    private int portNum;

    public OverlayNodeSendsRegistration(String IP, int portNum) {
        this.IP = IP;
        this.portNum = portNum;
    }

    public String getIP() { return IP; }

    public int getPortNum() { return portNum; }

    @Override
    public int getType() {
        return type;
    }

    // marshall this msg into a byte arr
    @Override
    public byte[] getBytes() throws IOException {
        byte[] marshalledBytes = null;

        // creates a byte array output stream to write data to
        ByteArrayOutputStream baOutStream = new ByteArrayOutputStream();
        // use data ostream to write data to the underlying output stream (in this case, a byte array output stream)
        // (easier interface to write to byte array stream using data output stream)
        // buffered ostream is for buffering the output so we dont continually write to the stream
        // until we are ready; faster than writing after every byte
        // TLDR: dOut is used to write data to the baOutStream
        DataOutputStream dOut = new DataOutputStream(new BufferedOutputStream(baOutStream));

        // start writing data to the stream
        // msg type
        dOut.writeInt(type);

        byte[] IPbytes = IP.getBytes();
        int IPlength = IPbytes.length;
        // len of IP addr
        dOut.writeInt(IPlength);
        // IP addr, in bytes
        dOut.write(IPbytes);
        // port num
        dOut.writeInt(portNum);

        // write the data to the byte array output stream
        // flush makes it so we dont constantly write to the stream
        // that would be inefficient. Instead, we only write to the
        // stream when we are all ready with the msg fields
        dOut.flush();
        // now that we wrote all the data to the baOutStream, we can turn the data
        // in the stream into a byte array, which will be our final marshalled bytes
        marshalledBytes = baOutStream.toByteArray();

        baOutStream.close();
        dOut.close();
        return marshalledBytes;
    }
}
