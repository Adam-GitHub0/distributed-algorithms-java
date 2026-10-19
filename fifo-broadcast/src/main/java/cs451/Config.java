package cs451;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class Config {
    private int messageCount;
   // private int receiverId;

    public Config(String configPath) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(configPath))) {
            String line = reader.readLine();
            /*if (line != null) {
                String[] parts = line.split("\\s+");
                if (parts.length == 2) {
                    messageCount = Integer.parseInt(parts[0]);
                    receiverId = Integer.parseInt(parts[1]);
                } else {
                    throw new IllegalArgumentException("Invalid config file format. Expected: <messageCount> <receiverId>");
                }
            }
        }
    }*/
                if (line != null) {
                    if (line.matches("\\d+")) {
                        messageCount = Integer.parseInt(line);
                    } else {
                        throw new IllegalArgumentException("Invalid config file format. Expected: <messageCount>");
                    }
                } else {
                    throw new IllegalArgumentException("Config file is empty.");
                }
            }
            }

    public int getMessageCount() {
        return messageCount;
    }

    /*public int getReceiverId() {
        return receiverId;
    }*/
}
