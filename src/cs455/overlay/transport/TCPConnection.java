package cs455.overlay.transport;

import cs455.overlay.wireformats.Node;

import java.io.IOException;
import java.net.Socket;

public class TCPConnection {
    private Socket socket;
    private Node node;
    private TCPSenderThread senderThread;
    private TCPReceiverThread receiverThread;

    public TCPConnection(Socket socket, Node node) throws IOException {
        this.socket = socket;
        this.node = node;
        this.senderThread = new TCPSenderThread(socket);
        this.receiverThread = new TCPReceiverThread(this, node);
    }

    public Socket getSocket() {
        return socket;
    }

    public TCPSenderThread getSenderThread() {
        return senderThread;
    }

    public void startSenderAndReceiverThreads() {
        (new Thread(senderThread)).start();
        (new Thread(receiverThread)).start();
    }
}
