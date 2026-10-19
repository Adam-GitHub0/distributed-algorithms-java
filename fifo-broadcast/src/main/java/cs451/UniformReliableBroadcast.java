package cs451;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class UniformReliableBroadcast {

    private final BestEffortBroadcast beb;
    private final int myId;
    private final int totalProcesses;
    private final Logger logger;
    private final int majorityThreshold;

    private int sequenceNumber = 1; 
    private FIFOBroadcast fifoBroadcast;

    // One lock per sender
    private final Object[] senderLocks;

    // For each sender, track delivered and pending messages
    // Because we lock per sender, we can use plain HashSet (not thread-safe)
    private final Set<Message>[] deliveredPerSender;
    private final Set<Message>[] pendingPerSender;

    // For each sender, map seqNr -> AcksState to track who acknowledged
    private final Map<Integer, AcksState>[] ackPerSender;

    public UniformReliableBroadcast(BestEffortBroadcast beb, int myId, int totalProcesses, Logger logger) {
        this.beb = beb;
        this.myId = myId;
        this.totalProcesses = totalProcesses;
        this.logger = logger;
        this.majorityThreshold = totalProcesses / 2 + 1;

        // Initialize per-sender structures
        senderLocks = new Object[totalProcesses + 1];
        deliveredPerSender = (Set<Message>[]) new Set[totalProcesses + 1];
        pendingPerSender = (Set<Message>[]) new Set[totalProcesses + 1];
        ackPerSender = (Map<Integer, AcksState>[]) new Map[totalProcesses + 1];

        for (int i = 1; i <= totalProcesses; i++) {
            senderLocks[i] = new Object();
            deliveredPerSender[i] = new HashSet<>();
            pendingPerSender[i] = new HashSet<>();
            ackPerSender[i] = new HashMap<>();
        }
    }

    public void setFIFOBroadcast(FIFOBroadcast fifoBroadcast) {
        this.fifoBroadcast = fifoBroadcast;
    }

    /**
     * Broadcast a message to all processes.
     */
    public void broadcast(String messageContent) throws IOException {
        int msgSeqNr = sequenceNumber++;
        logger.log("b " + msgSeqNr);
        Message message = new Message(myId, msgSeqNr, messageContent);

        int senderId = myId;
        synchronized (senderLocks[senderId]) {
            // Add to pending for this sender
            pendingPerSender[senderId].add(message);
            // Initialize its AcksState
            AcksState state = new AcksState(totalProcesses);
            state.acked[myId] = true;
            state.ackCount = 1;
            ackPerSender[senderId].put(msgSeqNr, state);
        }

        beb.broadcast(message);
    }

    /**
     * Handle delivery of a message from BEB.
     */
    public void onBEBDeliver(int senderId, Message message) throws IOException {
        int sourceId = message.getSourceId();
        int seqNr = message.getSeqNr();

        // Acquire lock for the sender of the message since we will modify its data structures
        synchronized (senderLocks[sourceId]) {
            // Update ACK state for this message
            AcksState state = ackPerSender[sourceId].get(seqNr);
            if (state == null) {
                state = new AcksState(totalProcesses);
                ackPerSender[sourceId].put(seqNr, state);
            }

            if (!state.acked[senderId]) {
                state.acked[senderId] = true;
                state.ackCount++;
            }

            // If message not yet in pending or delivered, add to pending and rebroadcast
            if (!pendingPerSender[sourceId].contains(message) && !deliveredPerSender[sourceId].contains(message)) {
                pendingPerSender[sourceId].add(message);

                // Add our own acknowledgment if not already present
                if (!state.acked[myId]) {
                    state.acked[myId] = true;
                    state.ackCount++;
                }

                beb.broadcast(message);
            }

            // Try to deliver the message if conditions are met
            tryDeliver(sourceId, message, seqNr, state);
        }
    }

    /**
     * Attempt to deliver a message if conditions are met.
     */
    private void tryDeliver(int sourceId, Message message, int seqNr, AcksState state) throws IOException {
        // Check majority and not delivered
        if (!deliveredPerSender[sourceId].contains(message) 
                && state.ackCount >= majorityThreshold
                && pendingPerSender[sourceId].contains(message)) {

            deliveredPerSender[sourceId].add(message);
            pendingPerSender[sourceId].remove(message);

            // If FIFO broadcast is set, deliver FIFO
            if (fifoBroadcast != null) {
                fifoBroadcast.deliver(message);
            }
        }
    }

    // A small class to track acknowledgments for a given message
    private static class AcksState {
        final boolean[] acked;
        int ackCount;

        AcksState(int totalProcesses) {
            this.acked = new boolean[totalProcesses + 1]; // indexing from 1..N
            this.ackCount = 0;
        }
    }

}
