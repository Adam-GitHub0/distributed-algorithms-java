package cs451;

import java.io.IOException;

public class BestEffortBroadcast {

    private final PerfectLink perfectLink;
    private UniformReliableBroadcast urb;

    public BestEffortBroadcast(PerfectLink perfectLink) {
        this.perfectLink = perfectLink;
    }

    public void setUniformReliableBroadcast(UniformReliableBroadcast urb) {
        this.urb = urb;
    }

    public void broadcast(Message message) throws IOException {
        perfectLink.sendToAll(message);
    }

    public void deliver(int senderId, Message message) {
        try {
            if (urb != null) {
                urb.onBEBDeliver(senderId, message);
            }
        } catch (Exception e) {
            //logger.log("Error in BEB deliver: " + e.getMessage());
            throw new RuntimeException("Critical error in BEB delivery", e); // Rethrow if necessary
        }
    }
}
