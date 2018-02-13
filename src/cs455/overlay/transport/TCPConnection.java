package cs455.overlay.transport;

import cs455.overlay.wireformats.Node;

import java.io.IOException;
import java.net.Socket;

public class TCPConnection {
    private Socket socket;
    private TCPSenderThread senderThread;
    private TCPReceiverThread receiverThread;

    /*  socket: used to retrieve the communications to a node
        node: allows TCPReceiver thread to call the node's onEvent()  */
    public TCPConnection(Socket socket, Node node) throws IOException {
        this.socket = socket;
        this.senderThread = new TCPSenderThread(socket);
        this.receiverThread = new TCPReceiverThread(this, node);
    }

    public Socket getSocket() {
        return socket;
    }

    /*  Allows the nodes to access the sender thread for this connection
        so msgs can be added to the sender thread's queue  */
    public TCPSenderThread getSenderThread() {
        return senderThread;
    }

    public void startSenderAndReceiverThreads() {
        (new Thread(senderThread)).start();
        (new Thread(receiverThread)).start();
    }

    public String toString() {
        return socket.toString();
    }
}
