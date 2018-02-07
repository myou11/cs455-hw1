package cs455.overlay.wireformats;

import java.io.IOException;
import java.net.Socket;

public interface Node {
    void onEvent(Event event, Socket socket) throws IOException;
}
