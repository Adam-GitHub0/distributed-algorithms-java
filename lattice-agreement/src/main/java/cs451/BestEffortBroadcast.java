package cs451;

public class BestEffortBroadcast {

    private final PerfectLink perfectLink;
    private final int myId;

    private MultiShotLA multiShotLA;

    public BestEffortBroadcast(int myId, PerfectLink perfectLink) {
        this.myId = myId;
        this.perfectLink = perfectLink;
    }

    public void setMultiShotLA(MultiShotLA multiShotLA) {
        this.multiShotLA = multiShotLA;
    }

 
   
    public void broadcast(byte[] rawBytes) {
        Message m = new Message(myId, rawBytes);
        perfectLink.sendToAll(m);
    }

    public void deliver(int senderId, Message message) {
        LAMessageBinary laMsg = LAMessageBinary.deserialize(message.getContentBytes());
    
        // 2. Dispatch to single-shot instance
        LatticeAgreementSingleShot laInstance =
            multiShotLA.getSingleShotInstance(laMsg.getSlotId());
    
        switch (laMsg.getType()) {
            case PROPOSAL:
                laInstance.onProposalReceived(
                    senderId,
                    laMsg.getProposalNumber(),
                    laMsg.getValue() 
                );
                break;
            case ACK:
                laInstance.onAckReceived(senderId, laMsg.getProposalNumber());
                break;
            case NACK:
                laInstance.onNackReceived(
                    senderId,
                    laMsg.getProposalNumber(),
                    laMsg.getValue() 
                );
                break;
        }
    }
    
    

    public PerfectLink getPerfectLink() {
        return this.perfectLink;
    }
}
