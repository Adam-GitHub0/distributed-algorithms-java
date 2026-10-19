package cs451;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class Message {
    private final int sourceId;
    private final byte[] contentBytes; 

    public Message(int sourceId, byte[] contentBytes) {
        this.sourceId = sourceId;
        this.contentBytes = contentBytes;
    }

    public int getSourceId() {
        return sourceId;
    }

    public byte[] getContentBytes() {
        return contentBytes;
    }

    public static Message fromBytes(ByteBuffer buffer) {
        int sId = buffer.getInt();
        int length = buffer.getInt();
        byte[] cBytes = new byte[length];
        buffer.get(cBytes);
        return new Message(sId, cBytes);
    }

    @Override
    public String toString() {
        return sourceId + ":" + Arrays.toString(contentBytes);
    }
}
