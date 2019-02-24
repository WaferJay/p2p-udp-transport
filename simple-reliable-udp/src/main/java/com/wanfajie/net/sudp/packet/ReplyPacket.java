package com.wanfajie.net.sudp.packet;

import java.net.InetSocketAddress;

public class ReplyPacket extends BasePacket {

    public enum ReplyType {
        RECEIVED(0x00), REJECT(0xff), REPLAY(0x01);

        private byte code;
        ReplyType(int c) {
            code = (byte) c;
        }

        public byte getCode() {
            return code;
        }

        public static ReplyType getType(byte b) {
            for (ReplyType type : values()) {
                if (type.getCode() == b) {
                    return type;
                }
            }

            throw new IllegalArgumentException(String.valueOf(b));
        }
    }

    private ReplyType type;

    public ReplyPacket(InetSocketAddress sender, InetSocketAddress recipient, int seq, byte crc, ReplyType _type) {
        super(sender, recipient, seq, crc);
        type = _type;
    }

    public ReplyPacket(InetSocketAddress recipient, int seq, ReplyType _type) {
        super(recipient, seq);
        type = _type;
    }

    public ReplyType getType() {
        return type;
    }
}
