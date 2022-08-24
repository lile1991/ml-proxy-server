package io.ml.proxy.server.handler.codec.bytemap;

import io.netty.channel.CombinedChannelDuplexHandler;

public class ByteMapServerCodec extends CombinedChannelDuplexHandler<ByteMapDecoder, ByteMapEncoder> {
    public ByteMapServerCodec(ByteMapDecoder inboundHandler, ByteMapEncoder outboundHandler) {
        super(inboundHandler, outboundHandler);
    }

    public ByteMapServerCodec() {
        this(new ByteMapDecoder(), new ByteMapEncoder());
    }
}
