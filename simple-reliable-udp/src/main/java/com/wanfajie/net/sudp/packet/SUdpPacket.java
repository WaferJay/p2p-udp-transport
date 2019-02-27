package com.wanfajie.net.sudp.packet;

public interface SUdpPacket<M> extends DefaultAddressedEnvelope<M> {
    int sequence();
}
