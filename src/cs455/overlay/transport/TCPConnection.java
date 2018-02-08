package cs455.overlay.transport;

import cs455.overlay.wireformats.Node;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

public class TCPConnection {
    private Socket socket;
    private Node node;
    private ArrayList<byte[]> msgQueue;

    public TCPConnection(Socket socket, Node node) throws IOException {
        this.socket = socket;
        this.node = node;
        this.msgQueue = new ArrayList<>();
        //startSenderAndReceiverThreads();
    }

    public Socket getSocket() {
        return socket;
    }

    public void startSenderAndReceiverThreads() {
        try {
            (new Thread(new TCPSenderThread(this, msgQueue))).start();
            (new Thread(new TCPReceiverThread(this, node))).start();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public void sendMsg(byte[] msg) {
        msgQueue.add(msg);
    }
}
