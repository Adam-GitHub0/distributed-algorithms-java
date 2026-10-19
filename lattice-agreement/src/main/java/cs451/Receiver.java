package cs451;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;

public class Receiver {

    private final int receiverPort;
    private final int receiverId;
    private final DatagramSocket socket;
    private final ExecutorService mainTaskExecutor;
    private boolean isRunning;
    private BestEffortBroadcast beb;


    public Receiver(Host receiverHost, ExecutorService mainTaskExecutor) throws SocketException {
        this.receiverPort = receiverHost.getPort();
        this.receiverId = receiverHost.getId();
        this.socket = new DatagramSocket(receiverPort);
        this.mainTaskExecutor = mainTaskExecutor;
        this.isRunning = true;
    }

    
    public void setBestEffortBroadcast(BestEffortBroadcast beb) {
        this.beb = beb;
    }

    public void receiveMessages() {
        mainTaskExecutor.submit(() -> {
            byte[] buffer = new byte[32768];//before it was 512 but now it's more with the set of integers 
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            while (isRunning) {
            
                try {
                    socket.receive(packet);
                    handlePacket(packet);
                } catch (IOException e) {
                    if (isRunning) e.printStackTrace();
                }
            }
        });
    }

    private void handlePacket(DatagramPacket packet) throws IOException {
    ByteBuffer byteBuffer = ByteBuffer.wrap(packet.getData(), 0, packet.getLength());
    int senderId = byteBuffer.getInt();
    int packetSeqNr = byteBuffer.getInt();
    byte messageCount = byteBuffer.get();      // 1 byte
    
    for (int i = 0; i < messageCount; i++) {
        Message message = Message.fromBytes(byteBuffer);
        if (beb != null) {
            beb.deliver(senderId, message);
        }
    }

    sendAck(packet.getAddress(), packet.getPort(), packetSeqNr);
}
    
    

    private void sendAck(InetAddress senderAddress, int senderPort, int packetSeqNr) {
        try {
            ByteBuffer ackBuffer = ByteBuffer.allocate(Integer.BYTES * 2);
            ackBuffer.putInt(receiverId);
            ackBuffer.putInt(packetSeqNr);
            byte[] ackData = ackBuffer.array();

            DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length, senderAddress, senderPort);
            socket.send(ackPacket);
        } catch (IOException e) {
            if (isRunning) e.printStackTrace();
        }
    }
   

    public void shutdown() {
        isRunning = false;
        socket.close();
    }


    
}
