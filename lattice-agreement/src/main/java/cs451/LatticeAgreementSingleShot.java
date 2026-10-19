package cs451;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

public class LatticeAgreementSingleShot {

    private final int myId;
    private final int n;
    private final int f;
    private final int slotId;
    private final BestEffortBroadcast beb;
    private final int ds;

    // Proposer state
    private  boolean active;
    private int ackCount = 1; 
    private int nackCount = 0; 
    private int activeProposalNumber = 0;

    private final Set<Integer> proposedValue ;

    // For blocking the local thread
    private final CountDownLatch decideLatch = new CountDownLatch(1);
    private Set<Integer> decidedValue = null;

    public LatticeAgreementSingleShot(int myId, int n, int f, int slotId,
                                      BestEffortBroadcast beb, int ds) {
        this.myId = myId;
        this.n = n;
        this.f = f;
        this.slotId = slotId;
        this.beb = beb;
        this.ds = ds;
        this.proposedValue = new HashSet<>(ds);
    }

    public synchronized void propose(Set<Integer> proposal) {
        if (active) {
            return;
        }
        active = true;
        proposedValue.addAll(proposal);
        activeProposalNumber++;

        broadcastProposal();
    }

    public Set<Integer> waitForDecision() {
        try {
            decideLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return decidedValue;
    }

    // ------------------------------------------------------------
    // Acceptors
    // ------------------------------------------------------------
    public synchronized void onProposalReceived(int senderId, int proposalNumber, Set<Integer> incomingSet) {
        if (incomingSet.containsAll(proposedValue)) {
            // acceptedValue ⊆ incomingSet => ack
            proposedValue.clear();
            proposedValue.addAll(incomingSet);
            sendAck(senderId, proposalNumber);
        } else {
            // Merge
            proposedValue.addAll(incomingSet);
            sendNack(senderId, proposalNumber, proposedValue);
        }
    }

    // ------------------------------------------------------------
    // Proposer
    // ------------------------------------------------------------
    public synchronized void onAckReceived(int senderId, int proposalNumber) {
        if (!active) return;
        if (proposalNumber != activeProposalNumber) return;

        ackCount++;
       

        // if some nacks are present and total >= f+1 => re-broadcast
        if (nackCount > 0 && (ackCount + nackCount) >= (f + 1)) {
            reBroadcastProposal();
            return;
        }

        if (ackCount >= (f + 1)) {
            decide();
        }
    }

    public synchronized void onNackReceived(int senderId, int proposalNumber, Set<Integer> merged) {
        if (!active) return;
        if (proposalNumber != activeProposalNumber) return;

        proposedValue.addAll(merged);
        
        nackCount++;


        if ((nackCount + ackCount) >= (f + 1)) {
            reBroadcastProposal();
        }
    }

    // ------------------------------------------------------------
    // Helper: re-broadcast
    // ------------------------------------------------------------
    private void reBroadcastProposal() {
        // check ds 
        if (proposedValue.size() >= ds) {
            decide();
            return;
        }

        activeProposalNumber++;
        ackCount = 1;
        nackCount = 0;
        broadcastProposal();
    }

    // ------------------------------------------------------------
    // Decide
    // ------------------------------------------------------------
    private synchronized void decide() {
        decidedValue = new HashSet<>(proposedValue);
        active = false;
        decideLatch.countDown();
    }

    // ------------------------------------------------------------
    // Sending Methods
    // ------------------------------------------------------------
    private void broadcastProposal() {
       

        // Build LAMessageBinary
        LAMessageBinary laMsg = new LAMessageBinary(
            LAMessageBinary.Type.PROPOSAL,
            slotId,
            activeProposalNumber,
           proposedValue
        );
        // Convert to bytes
        byte[] raw = laMsg.serialize();
        beb.broadcast(raw);


    }

    private void sendAck(int toId, int proposalNumber) {
        LAMessageBinary laMsg = new LAMessageBinary(
            LAMessageBinary.Type.ACK,
            slotId,
            proposalNumber,
            null
        );
        byte[] raw = laMsg.serialize();
        Message m = new Message(myId, raw);
        sendDirect(toId, m);

    }

    private void sendNack(int toId, int proposalNumber, Set<Integer> merged) {
        LAMessageBinary laMsg = new LAMessageBinary(
            LAMessageBinary.Type.NACK,
            slotId,
            proposalNumber,
            merged
        );
        byte[] raw = laMsg.serialize();
        Message m = new Message(myId, raw);
        sendDirect(toId, m);
    }

    private void sendDirect(int toId, Message msg) {
        try {
            beb.getPerfectLink().sendToHost(toId, msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}