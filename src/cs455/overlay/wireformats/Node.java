package cs455.overlay.wireformats;

import cs455.overlay.transport.TCPConnection;

import java.io.IOException;

public interface Node {
    /*  Performs different actions depending on the Event (msg) type.
        The connection allows the nodes (registry considered a node too)
        to access their sender threads and other fields inside TCPConnection  */
    void onEvent(Event event, TCPConnection connection) throws IOException;
}
