package cs451;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Logger {
    private final BufferedWriter writer;
    private final List<String> logBatch = new ArrayList<>();
    private static final int MAX_BATCH_SIZE = 5000; 
    

    public Logger(String logFilePath) throws IOException {
        this.writer = new BufferedWriter(new FileWriter(logFilePath, true));
    }

    public synchronized void log(String logEntry) {
        logBatch.add(logEntry);
        
        if (logBatch.size() >= MAX_BATCH_SIZE) {
            flush(); 
        }
    }

    private synchronized void flush() {
        try {
            if (!logBatch.isEmpty()) { 
                for (String logEntry : logBatch) {
                    writer.write(logEntry + "\n");
                }
                logBatch.clear();
                writer.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void shutdown() {
        flush(); 
        try {
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
