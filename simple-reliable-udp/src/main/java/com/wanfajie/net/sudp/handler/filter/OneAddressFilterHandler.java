package com.wanfajie.net.sudp.handler.filter;

import java.net.InetSocketAddress;

public class OneAddressFilterHandler extends UdpAddressFilterHandler {

    private volatile InetSocketAddress address;

    public OneAddressFilterHandler(InetSocketAddress _address) {
        address = _address;
    }

    @Override
    public boolean filter(InetSocketAddress sender) {
        return address == null || address.equals(sender);
    }

    public void setAddress(InetSocketAddress address) {
        this.address = address;
    }

    public InetSocketAddress getAddress() {
        return address;
    }
}
