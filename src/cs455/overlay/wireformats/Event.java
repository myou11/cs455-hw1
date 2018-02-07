package cs455.overlay.wireformats;

import java.io.IOException;

// implemented by all msg types
public interface Event {
    // returns msg type of the Event
    int getType();

    // marshalls the fields of the msg type
    byte[] getBytes() throws IOException;
}
