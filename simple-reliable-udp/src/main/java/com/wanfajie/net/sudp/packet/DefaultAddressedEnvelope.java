package com.wanfajie.net.sudp.packet;

import io.netty.channel.AddressedEnvelope;

import java.net.InetSocketAddress;

public interface DefaultAddressedEnvelope<M> extends AddressedEnvelope<M, InetSocketAddress> {

    @Override
    default M content() {
        return null;
    }

    @Override
    default DefaultAddressedEnvelope<M> retain() {
        return null;
    }

    @Override
    default DefaultAddressedEnvelope<M> retain(int var1) {
        return this;
    }

    @Override
    default DefaultAddressedEnvelope<M> touch() {
        return this;
    }

    @Override
    default DefaultAddressedEnvelope<M> touch(Object var1) {
        return this;
    }

    @Override
    default int refCnt() {
        return 0;
    }

    @Override
    default boolean release() {
        return release(1);
    }

    @Override
    default boolean release(int decrement) {
        return false;
    }
}
