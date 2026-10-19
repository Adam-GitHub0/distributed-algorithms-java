package cs451;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {

    private static PerfectLink perfectLink;
    private static Receiver receiver;
    private static Logger logger;
    private static ScheduledExecutorService scheduledTasksExecutor;
    private static ExecutorService mainTaskExecutor;
    private static Host myHost ;
    private static BestEffortBroadcast bestEffortBroadcast;

    private static void initSignalHandlers() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                handleSignal();
            }
        });
    }

    private static void handleSignal() {
       if (perfectLink != null) {
            perfectLink.shutdown();
        }
        if (logger != null) {
            logger.shutdown();
        }
        // Shutdown both executors
        shutdownExecutor(mainTaskExecutor);
        shutdownExecutor(scheduledTasksExecutor);
    }

    private static void shutdownExecutor(ExecutorService executor) {
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow(); // Force shutdown
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    public static void main(String[] args) throws InterruptedException, IOException {
        Parser parser = new Parser(args);
        parser.parse();
        initSignalHandlers();

        int myId = parser.myId();
        List<Host> hostsList = parser.hosts();
        String configPath = parser.config();
        String outputPath = parser.output();

        // We won't read all proposals up front, but we'll open the config in lazy mode
        Config config = new Config(configPath);
        int p = config.getP();
        int ds = config.getDs();

        // Start executors , before for submission2 it was 5 2 , now im trying with 6 1 
        mainTaskExecutor = Executors.newFixedThreadPool(6);
        scheduledTasksExecutor = Executors.newScheduledThreadPool(1);

        logger = new Logger(outputPath);

        // Separate out myHost vs. other hosts
        List<Host> receivers = new ArrayList<>();
        for (Host h : hostsList) {
            if (h.getId() == myId) {
                myHost = h;
            } else {
                receivers.add(h);
            }
        }

        // Build communication modules
        receiver = new Receiver(myHost, mainTaskExecutor);
        perfectLink = new PerfectLink(myId, receivers, mainTaskExecutor, scheduledTasksExecutor, receiver);
        bestEffortBroadcast = new BestEffortBroadcast(myId, perfectLink);
        receiver.setBestEffortBroadcast(bestEffortBroadcast);

        perfectLink.start();

        // n & f
        int n = hostsList.size();
        int f = (n - 1) / 2;

        // Create multi-shot LA with p slots
        MultiShotLA multiShotLA = new MultiShotLA(myId, n, f, p, bestEffortBroadcast, logger, ds);
        bestEffortBroadcast.setMultiShotLA(multiShotLA);
        
        // Instead of reading all p proposals up front, we read line-by-line:
        for (int slotIndex = 0; slotIndex < p; slotIndex++) {

            Set<Integer> proposal = config.readNextProposal();
            multiShotLA.proposeToSlot(slotIndex, proposal);
        }

        config.close();

        multiShotLA.waitForAllDecisions();

        //System.out.println("Done with streaming proposals. Press Ctrl+C to stop.");
        while (true) {
            Thread.sleep(60000);
        }
    }
}
