package cs455.overlay.transport;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

public class TCPSenderThread implements Runnable {
    private Socket socket;
    private DataOutputStream dOut;
    private ArrayList<byte[]> msgQueue;

    public TCPSenderThread(TCPConnection connection, ArrayList<byte[]> msgQueue) throws IOException {
        this.socket = socket;
        this.dOut = new DataOutputStream(socket.getOutputStream());
        this.msgQueue = msgQueue;
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

    public void run() {
        System.out.println("Starting TCPSenderThread...");
        try {
            while(true) {
                if (!msgQueue.isEmpty()) {
                    sendData(msgQueue.get(0));
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
