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
        // process msgs below

        // num bytes in msg
        int msgLength;
        // TODO: unsure of purpose of socket != null. Seems its intention is to keep reading from the socket
        // why do this if the registry will just spawn a thread for each receipt of a msg? TODO: ASK!!!!!
        // wouldnt the thread just process the msg and then die?
        while(socket != null) {
            try {
                // num bytes in msg is the first 4 bytes (int) in msg
                msgLength = dIn.readInt();
                // create byte[] to hold the reading of msg
                byte[] msg = new byte[msgLength];
                // read whole msg into the created byte[]
                dIn.readFully(msg, 0, msgLength);

                // create Event Factory to handle the processing of msgs
                EventFactory eventFactory = new EventFactory();
                /*  processMsg will unmarshall the msg based on the msg type
                    and return an appropriate Event obj
                    Have to pass in the socket being used to communicate with msging node
                    because we don't want to create another socket for sending a response msg
                    The node is passed in because in the case that it is a registration request,
                    the socket can be cached in the registry's connection cache
                 */
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