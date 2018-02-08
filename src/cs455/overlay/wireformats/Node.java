package cs455.overlay.wireformats;

import cs455.overlay.transport.TCPConnection;

import java.io.IOException;

public interface Node {
    void onEvent(Event event, TCPConnection connection) throws IOException;
}
