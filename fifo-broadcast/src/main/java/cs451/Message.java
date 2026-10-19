package cs451;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class Message {
    private final int sourceId;
    private final int seqNr;
    private final String content;
    private final byte[] contentBytes; 

    public Message(int sourceId, int seqNr, String content) {
        this.sourceId = sourceId;
        this.seqNr = seqNr;
        this.content = content;
        this.contentBytes = content.getBytes(StandardCharsets.UTF_8);
    }

    public int getSourceId() {
        return sourceId;
    }

    public int getSeqNr() {
        return seqNr;
    }

    public String getContent() {
        return content;
    }

    public byte[] getContentBytes() {
        return contentBytes;
    }

    public static Message fromBytes(ByteBuffer buffer) {
        int sId = buffer.getInt();
        int sNr = buffer.getInt();
        int length = buffer.getInt();
        byte[] cBytes = new byte[length];
        buffer.get(cBytes);
        return new Message(sId, sNr, new String(cBytes, StandardCharsets.UTF_8));
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Message)) return false;
        Message message = (Message) obj;
        return sourceId == message.sourceId && seqNr == message.seqNr;
    }

    @Override
    public int hashCode() {
        // Consider only sourceId and seqNr
        int result = Integer.hashCode(sourceId);
        result = 31 * result + Integer.hashCode(seqNr);
        return result;
    }

    @Override
    public String toString() {
        return sourceId + ":" + seqNr + ":" + content;

    }
}
