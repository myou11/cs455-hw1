package cs455.overlay.transport;

import cs455.overlay.wireformats.Event;
import cs455.overlay.wireformats.EventFactory;
import cs455.overlay.wireformats.Node;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

public class TCPReceiverThread implements Runnable {
    private TCPConnection connection;
    private Socket socket;
    private DataInputStream dIn;
    private Node node;

    private boolean DEBUG = false;

    public TCPReceiverThread(TCPConnection connection, Node node) throws IOException {
        this.connection = connection;
        this.socket = connection.getSocket();
        this.dIn = new DataInputStream(connection.getSocket().getInputStream());
        this.node = node;
    }

    public void run() {
        if (DEBUG)
            System.out.println("TCPReceiverThread running...");

        // Num bytes in msg
        int msgLength;

        // Rcv data from the socket until it is closed (i.e. NULL)
        while(socket != null) {
            try {
                // num bytes in msg is the first 4 bytes (int) in msg
                msgLength = dIn.readInt();

                // create byte[] to hold the reading of msg
                byte[] msg = new byte[msgLength];

                // read whole msg into the created byte[]
                dIn.readFully(msg, 0, msgLength);

                // Create Event Factory to handle the processing of msgs (processMsg)
                /*  processMsg will unmarshall the msg based on the msg type and return an appropriate Event obj.
                    Pass in the connection being used to communicate with the msging node b/c we don't want to
                    create a connection-per-message. The node is passed in so the methods, in onEvent(), can
                    modify the appropriate fields in the node.  */
                EventFactory eventFactory = new EventFactory();
                Event event = eventFactory.processMsg(msg);
                node.onEvent(event, connection);
            } catch(SocketException se) {
                System.out.println(se.getMessage());
                break;
            } catch(IOException ioe) {
                System.out.println(ioe.getMessage());
                break;
            }
        }
        System.out.println("------------------------------\nEXITING TCPReceiverThread!!!!!\n------------------------------");
    }
}