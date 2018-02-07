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
        // Registry and MsgingNode each have their own serverSockets, need to typecast the node to be able to call getServerSocket()
        if (node instanceof Registry)
            this.serverSocket = ((Registry) node).getServerSocket();
        else
            this.serverSocket = ((MessengerNode) node).getServerSocket();
        this.node = node;
    }

    public void run() {
        while(true) {   // true so we can continue to listen for connections
            try {
                System.out.println("Starting TCPServerThread...");
                /*  create a new socket with the incoming connection so we can pass it to a TCPReceiverThread
                    to handle the communications between the nodes
                 */
                Socket commSocket = serverSocket.accept();

                /*  pass the socket to the thread so it can retrieve the communications being sent to it
                    while the server continues to listen for incoming connections.
                    pass the registry so we can call registry's onEvent method to modify the registeredNodes
                    in the receiver thread
                 */
                //(new Thread(new TCPReceiverThread(commSocket, node))).start();
                TCPConnection connection = new TCPConnection(commSocket, node);
                connection.startSenderAndReceiverThreads();
            } catch(IOException ioe) {
                System.err.println("Unable to create Socket for communication");
                ioe.printStackTrace();
            }
        }
    }
}
