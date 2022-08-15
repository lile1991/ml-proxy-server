package io.ml.relay.handler.codec.lee;

import io.netty.channel.CombinedChannelDuplexHandler;

public class LeeClientCodec extends CombinedChannelDuplexHandler<LeeDecoder, LeeEncoder> {
    public LeeClientCodec(LeeDecoder inboundHandler, LeeEncoder outboundHandler) {
        super(inboundHandler, outboundHandler);
    }

    public LeeClientCodec() {
        this(new LeeDecoder(), new LeeEncoder());
    }
}
