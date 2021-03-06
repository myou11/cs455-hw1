package cs455.overlay.transport;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

public class TCPSenderThread implements Runnable {
    private Socket socket;
    private DataOutputStream dOut;

    /*  Access to this must be synchronized b/c while only the main thread
        of a msging node can send msgs, multiple rcvr threads of a msging node
        can be relaying msgs since a msging node can rcv msgs from each of its
        connections. Therefore, we can only allow one rcvr thead at a time to
        add msgs to the msgQueue.  */
    //private ArrayList<byte[]> msgQueue;
    private ConcurrentLinkedQueue<byte[]> msgQueue;

    private boolean DEBUG = false;

    public TCPSenderThread(Socket socket) throws IOException {
        this.socket = socket;
        this.dOut = new DataOutputStream(socket.getOutputStream());
        this.msgQueue = new ConcurrentLinkedQueue<>();
    }

    public int getMsgQueueSize() {
        return msgQueue.size();
    }

    /*public synchronized void addMessage(byte[] msg) {
        msgQueue.add(msg);
        // let any thread know to check the msgQueue again
        notifyAll();
    }*/

    public void addMessage(byte[] msg) {
        msgQueue.add(msg);
    }

    private void sendData(byte[] msg) throws IOException {
        // num bytes in the msg byte arr
        int dataLength = msg.length;
        // write the length of the msg to the buffer
        dOut.writeInt(dataLength);

        // write dataLength (all) bytes from msg byte arr,
        // starting at first (0th) byte, to the dOut buffer
        dOut.write(msg, 0, dataLength);

        // write whats in the buffer to the underlying stream
        dOut.flush();

        // remove the msg from the queue now that it has been sent
        msgQueue.remove(msg);
    }

    // TODO: MIGHT NEED TO SNYC RUN OR ADDMSG
    public void run() {
        if (DEBUG)
            System.out.println("Starting TCPSenderThread...");

        /*try {
            while(true) {
                if (msgQueue.isEmpty()) {
                    try {
                        // if nothing to send, wait until we are notified to check the condition again
                        wait();
                    } catch (InterruptedException ie) {
                        // do nothing if interrupted
                    }
                } else {
                    sendData(msgQueue.get(0));
                    notifyAll();

                    if (DEBUG)
                        System.out.println("Sending msg");
                }
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }*/

        while (socket != null) {    // while the socket is still connected
            byte[] msgToSend = msgQueue.poll();
            try {
                if (msgToSend != null) {
                    sendData(msgToSend);
                }
            } catch (IOException ioe) {
                System.out.println("TCPSenderThread: Message failed to send");
                ioe.printStackTrace();
            }
        }
    }
}
