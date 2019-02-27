package com.wanfajie.net.sudp.packet;

import java.net.InetSocketAddress;

public abstract class BasePacket<M> implements SUdpPacket<M> {

    public static final short PROTOCOL_DATA = (short) 0xfeee;
    public static final short PROTOCOL_REPLY = (short) 0xfeed;

    private int sequence;
    private byte crc;
    private InetSocketAddress sender;
    private InetSocketAddress recipient;

    public BasePacket(InetSocketAddress _sender, InetSocketAddress _recipient, int _seq, byte _crc) {
        sequence = _seq;
        crc = _crc;
        sender = _sender;
        recipient = _recipient;
    }

    public BasePacket(InetSocketAddress _recipient, int _seq) {
        sequence = _seq;
        recipient = _recipient;
    }

    @Override
    public int sequence() {
        return sequence;
    }

    public byte crc8() {
        return crc;
    }

    @Override
    public InetSocketAddress sender() {
        return sender;
    }

    @Override
    public InetSocketAddress recipient() {
        return recipient;
    }
}
