package cs451;

import java.util.Set;

/**
 * MultiShotLA runs single-shot Lattice Agreement for p slots in parallel.
 */
public class MultiShotLA {

    private final int myId;
    private final int n;      // number of processes
    private final int f;      // fault tolerance
    private final int p;      // number of slots

    private final BestEffortBroadcast beb;
    private final Logger logger;

    // We use an array instead of a List
    private final LatticeAgreementSingleShot[] singleShotInstances;
    private final int ds;

    @SuppressWarnings("unchecked")
    public MultiShotLA(int myId, int n, int f, int p, BestEffortBroadcast beb, Logger logger, int ds) {
        this.myId = myId;
        this.n = n;
        this.f = f;
        this.p = p;
        this.beb = beb;
        this.logger = logger;
        this.ds = ds;

        this.singleShotInstances = new LatticeAgreementSingleShot[p];
        

        // Create p single-shot instances
        for (int slot = 0; slot < p; slot++) {
            LatticeAgreementSingleShot instance = new LatticeAgreementSingleShot(
                myId, n, f, slot, beb, ds  
            );
            singleShotInstances[slot] = instance;
        }
    }

   
    /**
     * Called by the BestEffortBroadcast/Receiver to forward messages to the correct
     * single-shot instance. We'll parse the slotId from the message in BEB and call this.
     */
    public LatticeAgreementSingleShot getSingleShotInstance(int slotId) {
        return singleShotInstances[slotId];
    }

    /**
     * For code that *does* want to wait for decisions, we can
     * optionally expose a method to do so. 
     */
    
    public void waitForAllDecisions() {
        for (int slot = 0; slot < p; slot++) {
            Set<Integer> decided = singleShotInstances[slot].waitForDecision();
    
            // Build line manually
            StringBuilder sb = new StringBuilder();
            for (int x : decided) {
                sb.append(x).append(' ');
            }
            if (sb.length() > 0) {
                sb.setLength(sb.length() - 1); // remove trailing space
            }
            logger.log(sb.toString());
        }
    }

  
    public void proposeToSlot(int slotId, Set<Integer> proposal) {
        singleShotInstances[slotId].propose(proposal);
    }
    
    
}
