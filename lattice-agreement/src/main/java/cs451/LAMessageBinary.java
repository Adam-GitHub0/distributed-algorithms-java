package cs451;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;

public class LAMessageBinary {

    public enum Type {
        PROPOSAL((byte)0),
        ACK((byte)1),
        NACK((byte)2);

        private final byte code;
        Type(byte code) { this.code = code; }
        public byte getCode() { return code; }

        public static Type fromByte(byte b) {
            switch (b) {
                case 0: return PROPOSAL;
                case 1: return ACK;
                case 2: return NACK;
                default: throw new IllegalArgumentException("Invalid LA message type code: " + b);
            }
        }
    }

    private final Type type;
    private final int slotId;
    private final int proposalNumber;
    private final Set<Integer> value; // can be null for ACK

    public LAMessageBinary(Type type, int slotId, int proposalNumber, Set<Integer> value) {
        this.type = type;
        this.slotId = slotId;
        this.proposalNumber = proposalNumber;
        this.value = value; 
    }

    public Type getType() { return type; }
    public int getSlotId() { return slotId; }
    public int getProposalNumber() { return proposalNumber; }
    public Set<Integer> getValue() { return value; }

    /**
     * Binary format:
     *   1 byte  => type (0=PROP,1=ACK,2=NACK)
     *   4 bytes => slotId
     *   4 bytes => proposalNumber
     *   2 bytes => setLength
     *   (setLength * 4) => set elements
     */
    public byte[] serialize() {
        // If it's ACK, we do not send the set at all
        short setSize = 0;
        if (type != Type.ACK && value != null) {
            setSize = (short) value.size();
        }

        // 1 + 4 + 4 + 2 + (setSize * 4)
        int totalBytes = 1 + 4 + 4 + 2 + (setSize * 4);
        ByteBuffer buffer = ByteBuffer.allocate(totalBytes);

        // Write header
        buffer.put(type.getCode());
        buffer.putInt(slotId);
        buffer.putInt(proposalNumber);
        buffer.putShort(setSize);

        // Only write set elements if not ACK
        if (type != Type.ACK && value != null) {
            for (int elem : value) {
                buffer.putInt(elem);
            }
        }

        return buffer.array();
    }

    public static LAMessageBinary deserialize(byte[] raw) {
        ByteBuffer buffer = ByteBuffer.wrap(raw);

        byte typeByte = buffer.get();
        Type t = Type.fromByte(typeByte);

        int slotId = buffer.getInt();
        int proposalNumber = buffer.getInt();
        short setLength = buffer.getShort();

        Set<Integer> set = null;
        // If it's ACK or setLength == 0, no set is read
        if (t == Type.ACK || setLength == 0) {
            set = null; 
        } else {
            set = new HashSet<>(setLength);
            for (int i = 0; i < setLength; i++) {
                set.add(buffer.getInt());
            }
        }

        return new LAMessageBinary(t, slotId, proposalNumber, set);
    }
}
