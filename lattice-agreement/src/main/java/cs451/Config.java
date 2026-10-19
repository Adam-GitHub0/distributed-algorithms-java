package cs451;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Lazy-reading config file:
 *   - First line: "p vs ds"
 *   - Next p lines: each line is a set of integers.
 */
public class Config {
    private final int p;      // number of proposals
    private final int vs;     // max elements in a proposal
    private final int ds;     // max distinct elements overall

    private final BufferedReader reader;
    private int proposalsReadCount = 0; // how many proposals we have read so far

    public Config(String configPath) throws IOException {
        reader = new BufferedReader(new FileReader(configPath));

        // Parse first line: p vs ds
        String header = reader.readLine();
        if (header == null) {
            throw new IllegalArgumentException("Config file is empty. Expected: p vs ds on the first line.");
        }
        String[] tokens = header.trim().split("\\s+");
        if (tokens.length != 3) {
            throw new IllegalArgumentException("Invalid config file. First line must have 3 integers: p vs ds");
        }

        p = Integer.parseInt(tokens[0]);
        vs = Integer.parseInt(tokens[1]);
        ds = Integer.parseInt(tokens[2]);
    }

    /**
     * Returns the next proposal from the file as a Set<Integer>.
     * Throws if we've already read all p lines.
     */
    public Set<Integer> readNextProposal() throws IOException {
        if (proposalsReadCount >= p) {
            // We already read all proposals
            throw new IllegalStateException("No more proposals left in config file.");
        }

        String line = reader.readLine();
        if (line == null) {
            // File ended unexpectedly
            throw new IllegalArgumentException(
                String.format("Expected %d total proposals, but config ended early.", p)
            );
        }

        proposalsReadCount++;

        // Parse the line into a set of integers
        String[] setTokens = line.trim().split("\\s+");
        if (setTokens.length > vs) {
            throw new IllegalArgumentException(
                String.format("Line %d has more than %d elements, violating vs constraint.", proposalsReadCount, vs)
            );
        }
        Set<Integer> proposalSet = new HashSet<>(setTokens.length);
        for (String st : setTokens) {
            if (!st.isEmpty()) {
                proposalSet.add(Integer.valueOf(st));
            }
        }

        return proposalSet;
    }

    public int getP() {
        return p;
    }

    public int getVs() {
        return vs;
    }

    public int getDs() {
        return ds;
    }

    /**
     * Closes the reader if you want to stop reading early.
     */
    public void close() throws IOException {
        reader.close();
    }
}
