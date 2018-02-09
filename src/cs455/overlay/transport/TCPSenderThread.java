package cs455.overlay.transport;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

public class TCPSenderThread implements Runnable {
    private DataOutputStream dOut;
    private ArrayList<byte[]> msgQueue;

    public TCPSenderThread(Socket socket) throws IOException {
        this.dOut = new DataOutputStream(socket.getOutputStream());
        this.msgQueue = new ArrayList<>();
    }

    public synchronized void addMessage(byte[] msg) {
        msgQueue.add(msg);
        // let any thread know to check the msgQueue again
        notifyAll();
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

    public synchronized void run() {
        System.out.println("Starting TCPSenderThread...");
        try {
            while(true) {
                if (msgQueue.isEmpty()) {
                    try {
                        // if nothing to send, wait until we are notified to check the condition again
                        wait();
                    } catch (InterruptedException ie) {
                        // do nothing if interrupted
                    }
                }
                System.out.println("Sending msg");
                sendData(msgQueue.get(0));
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
