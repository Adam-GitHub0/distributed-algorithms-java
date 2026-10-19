package cs451;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

public class PerfectLink {
    private final int myId;
    private final Map<Integer, UnicastSender> unicastSenders = new HashMap<>();
    private final ExecutorService mainTaskExecutor;
    private final Receiver receiver; 
    private final DatagramSocket sendingSocket;



    public PerfectLink(int myId, List<Host> hosts,ExecutorService mainTaskExecutor,ScheduledExecutorService scheduledTasksExecutor,Receiver receiver) throws IOException {
        this.myId = myId;
        this.receiver = receiver;
        this.mainTaskExecutor = mainTaskExecutor;
        this.sendingSocket = new DatagramSocket();
        // Initialize a UnicastSender for each host in the system
        for (Host host : hosts) {
            unicastSenders.put(host.getId(), new UnicastSender(myId, host,sendingSocket, scheduledTasksExecutor));
        }
         // Start a single ACK receiver thread
         startAckReceiver();
    }

    public void start() {
        receiver.receiveMessages(); // Start handling incoming messages
    }

    private void startAckReceiver() {
        mainTaskExecutor.submit(() -> {
            try {
                byte[] buffer = new byte[8]; //  ACK includes senderId and packetSeqNr
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                
                while (!sendingSocket.isClosed()) {
                    
                    sendingSocket.receive(packet);

                    ByteBuffer byteBuffer = ByteBuffer.wrap(packet.getData(), 0, packet.getLength());
                    int senderId = byteBuffer.getInt();
                    int ackPacketSeqNr = byteBuffer.getInt();

                    // Dispatch the ACK to the appropriate UnicastSender responsible for this sender ( who sent the ACK )
                    UnicastSender sender = unicastSenders.get(senderId);
                    if (sender != null) {
                        sender.handleAck(ackPacketSeqNr);
                    } 
                }
            } catch (IOException e) {
                if (!sendingSocket.isClosed()) {
                    e.printStackTrace();
                }
            }
        });
    }



  public void sendToAll(Message message) {
        unicastSenders.values().forEach(sender -> {
            try {
                sender.sendMessage(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void sendToHost(int hostId, Message message) throws IOException {
        UnicastSender sender = unicastSenders.get(hostId);
        if (sender != null) {
            sender.sendMessage(message);
        } else {
            throw new IOException("No UnicastSender found for hostId: " + hostId);
        }
    }

    public void flush() {
        unicastSenders.values().forEach(sender -> {
            try {
                sender.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

   
    public void shutdown() {
        // Flush all pending messages
        flush();
        // Shutdown all UnicastSenders
        unicastSenders.values().forEach(UnicastSender::shutdown);
        // Shutdown the receiver
        receiver.shutdown();
        sendingSocket.close();
    }
}
