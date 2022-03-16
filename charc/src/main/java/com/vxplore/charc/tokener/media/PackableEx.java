package com.vxplore.charc.tokener.media;

public interface PackableEx extends Packable {
    void unmarshal(ByteBuf in);
}
