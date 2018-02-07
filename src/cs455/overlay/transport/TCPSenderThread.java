package cs455.overlay.transport;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class TCPSenderThread implements Runnable {
    private Socket socket;
    private DataOutputStream dOut;
    private byte[] msg;

    public TCPSenderThread(Socket socket, byte[] msg) throws IOException {
        this.socket = socket;
        dOut = new DataOutputStream(socket.getOutputStream());
        this.msg = msg;
    }

    public void sendData(byte[] dataToSend) throws IOException {
        // num bytes in the dataToSend byte arr
        int dataLength = dataToSend.length;
        // write the length of the msg to the buffer
        dOut.writeInt(dataLength);

        // write dataLength (all) bytes from dataToSend byte arr,
        // starting at first (0th) byte, to the dOut buffer
        dOut.write(dataToSend, 0, dataLength);

        // write whats in the buffer to the underlying stream
        dOut.flush();
    }

    public void run() {
        try {
            sendData(msg);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
