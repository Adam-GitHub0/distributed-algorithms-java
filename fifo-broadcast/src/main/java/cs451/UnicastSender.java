package cs451;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class UnicastSender {
    private final int senderId;
    private final AtomicInteger packetSequenceNumber = new AtomicInteger(1);
    private final Map<Integer, PacketBuffer> pendingPackets = new HashMap<>();
    private final Map<Integer, UnackedPacket> unackedPackets = new ConcurrentHashMap<>();

    private final ScheduledExecutorService scheduledTasksExecutor;
    private final DatagramSocket socket;
    private final int receiverPort;
    private final InetAddress receiverAddress;

    private static final int INITIAL_RETRY_DELAY_MS = 100;
    private static final int RETRANSMISSION_CHECK_INTERVAL_MS = 100;
    private static final int MAX_RETRIES = 5;

    private static final int MAX_MESSAGES_PER_PACKET = 8;
    private static final int PARTIAL_PACKET_FLUSH_TIMEOUT_MS = 5000;
    private static final int MAX_PACKET_SIZE = 65507;

    private final ByteBuffer sendBuffer = ByteBuffer.allocate(MAX_PACKET_SIZE);
    private volatile long lastPacketSentTime = System.currentTimeMillis();

    public UnicastSender(int senderId, Host receiverHost, DatagramSocket sendingSocket,
                         ScheduledExecutorService scheduledTasksExecutor) throws IOException {
        this.senderId = senderId;
        this.scheduledTasksExecutor = scheduledTasksExecutor;
        this.socket = sendingSocket;
        this.receiverAddress = InetAddress.getByName(receiverHost.getIp());
        this.receiverPort = receiverHost.getPort();
        startPeriodicFlush();
        startRetransmissionTask();

    }

    private void startPeriodicFlush() {
        scheduledTasksExecutor.scheduleAtFixedRate(() -> {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastPacketSentTime >= PARTIAL_PACKET_FLUSH_TIMEOUT_MS) {
                try {
                    flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, PARTIAL_PACKET_FLUSH_TIMEOUT_MS, PARTIAL_PACKET_FLUSH_TIMEOUT_MS, TimeUnit.MILLISECONDS);
    }




    private void startRetransmissionTask() {
        scheduledTasksExecutor.scheduleAtFixedRate(() -> {
            long now = System.currentTimeMillis();
            for (Map.Entry<Integer, UnackedPacket> entry : unackedPackets.entrySet()) {
                UnackedPacket up = entry.getValue();
                // Check if it's time to retransmit
                if (now >= up.nextSendTime && up.retryCount < MAX_RETRIES) {
                    try {
                        // Retransmit the packet
                        socket.send(new DatagramPacket(up.data, up.data.length, receiverAddress, receiverPort));
                        up.retryCount++;
                        // Calculate exponential backoff delay
                        int newDelay = INITIAL_RETRY_DELAY_MS << up.retryCount; // 2^(retryCount)*INITIAL_RETRY_DELAY_MS
                        up.nextSendTime = now + newDelay;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if (up.retryCount >= MAX_RETRIES) {
                    // If we've exceeded max retries, we could remove it or handle it as a lost cause.
                    // For now, let's just remove it to avoid infinite retransmissions.
                    unackedPackets.remove(entry.getKey());
                }
            }
        }, RETRANSMISSION_CHECK_INTERVAL_MS, RETRANSMISSION_CHECK_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }










    public synchronized void sendMessage(Message message) throws IOException {
        int currentPacket = packetSequenceNumber.get();
        PacketBuffer packetBuffer = pendingPackets.get(currentPacket);
        if (packetBuffer == null) {
            packetBuffer = new PacketBuffer(MAX_MESSAGES_PER_PACKET);
            pendingPackets.put(currentPacket, packetBuffer);
        }

        packetBuffer.addMessage(message);

        if (packetBuffer.isFull()) {
            int seq = packetSequenceNumber.getAndIncrement();
            byte[] packetData = buildPacket(seq, packetBuffer.messages, packetBuffer.count);
            sendPacket(seq, packetData);
            packetBuffer.reset();
        }
    }
    private byte[] buildPacket(int packetSeqNr, Message[] messages, int messageCount) {
        sendBuffer.clear();
        sendBuffer.putInt(senderId);
        sendBuffer.putInt(packetSeqNr);
        sendBuffer.putInt(messageCount);

        for (int i = 0; i < messageCount; i++) {
            Message m = messages[i];
            byte[] contentBytes = m.getContentBytes();
            sendBuffer.putInt(m.getSourceId());
            sendBuffer.putInt(m.getSeqNr());
            sendBuffer.putInt(contentBytes.length);
            sendBuffer.put(contentBytes);
        }

        int length = sendBuffer.position();
        byte[] packetData = new byte[length];
        System.arraycopy(sendBuffer.array(), 0, packetData, 0, length);
        return packetData;
    }


    private void sendPacket(int packetSeqNr, byte[] packetData) throws IOException {
        DatagramPacket packet = new DatagramPacket(packetData, packetData.length, receiverAddress, receiverPort);
        socket.send(packet);
        long now = System.currentTimeMillis();
        // Store packet in unackedPackets with initial retry delay
        UnackedPacket up = new UnackedPacket(packetData, now, 0);
        // The first retransmission wait is INITIAL_RETRY_DELAY_MS
        up.nextSendTime = now + INITIAL_RETRY_DELAY_MS;
        unackedPackets.put(packetSeqNr, up);
    }

    
    public synchronized void handleAck(int packetSeqNr) {
        unackedPackets.remove(packetSeqNr);
        }

    public synchronized void flush() throws IOException {
        int currentPacket = packetSequenceNumber.get();
        PacketBuffer pb = pendingPackets.get(currentPacket);
        if (pb != null && pb.count > 0) {
            int seq = packetSequenceNumber.getAndIncrement();
            byte[] packetData = buildPacket(seq, pb.messages, pb.count);
            sendPacket(seq, packetData);
            pb.reset();
        }
    }

    public synchronized void shutdown() {
        pendingPackets.clear();
        unackedPackets.clear();
    }


    private static class PacketBuffer {
        final Message[] messages;
        int count;
    
        PacketBuffer(int size) {
            messages = new Message[size];
            count = 0;
        }
    
        void addMessage(Message m) {
            messages[count++] = m;
        }
    
        boolean isFull() {
            return count == messages.length;
        }
    
        boolean isEmpty() {
            return count == 0;
        }
    
        void reset() {
            count = 0;
        }
    }


     // Tracks a sent packet that has not yet been ACKed
     private static class UnackedPacket {
        final byte[] data;
        long nextSendTime; // When the next retransmission should occur
        int retryCount;

        UnackedPacket(byte[] data, long lastSentTimestamp, int retryCount) {
            this.data = data;
            this.retryCount = retryCount;
            // nextSendTime is set after the first send
        }
    }
    
}
