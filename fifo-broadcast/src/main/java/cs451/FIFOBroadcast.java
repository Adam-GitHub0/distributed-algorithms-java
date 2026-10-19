package cs451;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class FIFOBroadcast {

    private final UniformReliableBroadcast urb;
    private final Logger logger;
    // Lock array: one lock per sender
    private final Object[] senderLocks;
    // nextExpected[senderId] = the next expected sequence number from senderId
    private final int[] nextExpected;
    private final Map<Integer, Message>[] pendingPerSender;

    
    public FIFOBroadcast(UniformReliableBroadcast urb, int totalProcesses, Logger logger) {
        this.urb = urb;
        this.logger = logger;

        this.nextExpected = new int[totalProcesses + 1];  // assuming process IDs start from 1
        for (int i = 1; i <= totalProcesses; i++) {
            nextExpected[i] = 1;
        }
        

         // Initialize the array of Maps for each sender
        pendingPerSender = (Map<Integer, Message>[]) new Map[totalProcesses + 1];
        for (int i = 1; i <= totalProcesses; i++) {
            pendingPerSender[i] = new HashMap<>();
        }

        // Initialize locks for each sender
        senderLocks = new Object[totalProcesses + 1];
        for (int i = 1; i <= totalProcesses; i++) {
            senderLocks[i] = new Object();
        }
}

    /**
     * Broadcast a message with FIFO ordering.
     */
    public void broadcast(String messageContent) throws IOException {
        // URB already handles sequence numbering per sender
        urb.broadcast(messageContent);
    }

    
    public void deliver(Message message) throws IOException {
        int senderId = message.getSourceId();
        int seqNr = message.getSeqNr();
    
        synchronized (senderLocks[senderId]) {
            Map<Integer, Message> senderBuffer = pendingPerSender[senderId];
            senderBuffer.put(seqNr, message);
    
            int expected = nextExpected[senderId];
            Message toDeliver;
            while ((toDeliver = senderBuffer.remove(expected)) != null) {
                logger.log("d " + toDeliver.getSourceId() + " " + toDeliver.getSeqNr());
                expected++;
            }
            nextExpected[senderId] = expected;
        }
    }
    
}
