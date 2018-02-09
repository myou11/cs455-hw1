package cs455.overlay.transport;

import cs455.overlay.node.MessengerNode;
import cs455.overlay.node.Registry;
import cs455.overlay.wireformats.Node;

import java.net.ServerSocket;
import java.io.IOException;
import java.net.Socket;

public class TCPServerThread implements Runnable {
    private ServerSocket serverSocket;
    private Node node;

    public TCPServerThread(Node node) {
        // Registry and MsgingNode each have their own serverSockets, typecast the node to call getServerSocket()
        if (node instanceof Registry)
            this.serverSocket = ((Registry) node).getServerSocket();
        else
            this.serverSocket = ((MessengerNode) node).getServerSocket();
        this.node = node;
    }

    public void run() {
        System.out.println("Starting TCPServerThread...");
        while(true) { // true so we can continue to listen for connections
            try {
                /*
                    create a new socket with the incoming connection so we can pass it to
                    a TCPConnection to handle the communications between the nodes
                 */
                Socket commSocket = serverSocket.accept();
                TCPConnection connection = new TCPConnection(commSocket, node);
                connection.startSenderAndReceiverThreads();
            } catch(IOException ioe) {
                System.err.println("Unable to create Socket for communication");
                ioe.printStackTrace();
            }
        }
    }
}
