package cs451;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Sender {
    private final int senderId;
    private int messageSequenceNumber = 1;
    private final AtomicInteger packetSequenceNumber = new AtomicInteger(1);
    private final ConcurrentHashMap<Integer, List<String>> pendingPackets = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduledExecutorService;
    private final Logger logger;
    private final DatagramSocket socket;
    private final int receiverPort;
    private final InetAddress receiverAddress;
    private static final int INITIAL_RETRY_DELAY_MS = 100;
    private static final int MAX_RETRY_DELAY_MS = 1000;
    private static final int MAX_MESSAGES_PER_PACKET = 8;
    private static final int MAX_RETRIES = 5;
    //private static final int PACKET_CHECK_INTERVAL_MS = 5000; 
    //private final int totalMessages; 


    public Sender(int senderId, Host receiverHost, Logger logger, ScheduledExecutorService scheduledExecutorService) throws IOException {
        this.senderId = senderId;
        this.logger = logger;
        this.scheduledExecutorService = scheduledExecutorService;
        this.socket = new DatagramSocket(senderId);
        this.receiverAddress = InetAddress.getByName(receiverHost.getIp());
        this.receiverPort = receiverHost.getPort();
        //this.totalMessages = totalMessages;
        receiveAck();
        //startPacketCheckTask();
    }

    public void sendMessage(String message) throws IOException {
        int msgSeqNr = messageSequenceNumber++;
        logger.log("b " + msgSeqNr);

        List<String> packetMessages = pendingPackets.computeIfAbsent(packetSequenceNumber.get(), k -> new ArrayList<>());
        packetMessages.add(msgSeqNr + ":" + message);

        if (packetMessages.size() >= MAX_MESSAGES_PER_PACKET) {
            sendPacket(packetSequenceNumber.get(), packetMessages);
            scheduleRetryWithBackoff(packetSequenceNumber.get(), INITIAL_RETRY_DELAY_MS, 0);
            packetSequenceNumber.incrementAndGet();
        }

        
    }

    private void sendPacket(int packetSeqNr, List<String> messages) throws IOException {
        StringBuilder messageData = new StringBuilder();
        for (String msg : messages) {
            messageData.append(msg).append("|"); // Delimit messages with "|"
        }

        byte[] messageBytes = messageData.toString().getBytes(StandardCharsets.UTF_8);
        int bufferSize = Integer.BYTES * 2 + messageBytes.length; // senderId, packetSeqNr, and message data

        ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
        buffer.putInt(senderId);
        buffer.putInt(packetSeqNr);
        buffer.put(messageBytes);

        byte[] packetData = buffer.array();
        DatagramPacket packet = new DatagramPacket(packetData, packetData.length, receiverAddress, receiverPort);

        socket.send(packet);
    }

    private void scheduleRetryWithBackoff(int packetSeqNr, int delay, int retryCount) {
        if (retryCount >= MAX_RETRIES) return;
        scheduledExecutorService.schedule(() -> {
            if (pendingPackets.containsKey(packetSeqNr)) {
                try {
                    List<String> messages = pendingPackets.get(packetSeqNr);
                    sendPacket(packetSeqNr, messages);
                    int nextDelay = Math.min(delay * 2, MAX_RETRY_DELAY_MS);
                    scheduleRetryWithBackoff(packetSeqNr, nextDelay, retryCount + 1);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    private void receiveAck() {
        scheduledExecutorService.submit(() -> {
            try {
                while (!socket.isClosed()) {
                    byte[] buffer = new byte[4]; // Only expecting an integer (packet sequence number)
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);

                    int ackPacketSeqNr = ByteBuffer.wrap(buffer).getInt();
                    if (pendingPackets.remove(ackPacketSeqNr) != null) {
                    }
                }
            } catch (IOException e) {
                if (!socket.isClosed()) {
                    e.printStackTrace();
                } else {
                }
            }
        });
    }

    public void flush() throws IOException {
        List<String> packetMessages = pendingPackets.get(packetSequenceNumber.get());
        if (packetMessages != null && !packetMessages.isEmpty()) {
            sendPacket(packetSequenceNumber.get(), packetMessages);
            scheduleRetryWithBackoff(packetSequenceNumber.get(), INITIAL_RETRY_DELAY_MS, 0);
        }
    }
    
    public void shutdown() {
        try {
            List<String> packetMessages = pendingPackets.get(packetSequenceNumber.get());
            if (packetMessages != null && !packetMessages.isEmpty()) {
                sendPacket(packetSequenceNumber.get(), packetMessages);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        socket.close();
    }
}
