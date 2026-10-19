package cs451;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
    private static UniformReliableBroadcast  urb;
    private static FIFOBroadcast fifoBroadcast;


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
                    executor.shutdownNow(); // Force shutdown if tasks don’t complete
                    
             }} catch (InterruptedException e) {
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
        int messageCount = new Config(configPath).getMessageCount();

        
        // Initialize two separate executors
        mainTaskExecutor = Executors.newFixedThreadPool(5);
        scheduledTasksExecutor = Executors.newScheduledThreadPool(2);

        logger = new Logger(outputPath);
        
        List<Host> receivers = new ArrayList<>();

        for (Host host : hostsList) {
            if (host.getId() == myId) {
                myHost = host;
            } else {
                receivers.add(host);
            }
        }

        receiver = new Receiver(myHost, mainTaskExecutor);
        perfectLink = new PerfectLink(myId, receivers, mainTaskExecutor, scheduledTasksExecutor,receiver);
        

        // Initialize BestEffortBroadcast
        bestEffortBroadcast = new BestEffortBroadcast(perfectLink);
        // Set BEB in Receiver
        receiver.setBestEffortBroadcast(bestEffortBroadcast);

        // Initialize UniformReliableBroadcast
        urb = new UniformReliableBroadcast(bestEffortBroadcast, myId, hostsList.size(), logger);
        // Set URB in BEB
        bestEffortBroadcast.setUniformReliableBroadcast(urb);

        // Initialize FIFOBroadcast
        fifoBroadcast = new FIFOBroadcast(urb, hostsList.size(), logger);
        // Set FIFOBroadcast in URB
        urb.setFIFOBroadcast(fifoBroadcast);


        // we start only here and not before because we need to set the URB in BEB first ( and BEB in Receiver) so I can beb.deliver() in the receiver and urb.deliver() in the URB
        perfectLink.start();

        
         for (int i = 1; i <= messageCount; i++) {
            fifoBroadcast.broadcast(String.valueOf(i)); 

        }
       
       
            
        // Allow for shutdown signal handling
        System.out.println("Process running. Press Ctrl+C to terminate.");
        while (true) {
            Thread.sleep(60 * 60 * 1000); // Sleep to keep process alive
        }
    }
}