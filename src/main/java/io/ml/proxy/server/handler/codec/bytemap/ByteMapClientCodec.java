package io.ml.proxy.server.handler.codec.bytemap;

import io.netty.channel.CombinedChannelDuplexHandler;

public class ByteMapClientCodec extends CombinedChannelDuplexHandler<ByteMapDecoder, ByteMapEncoder> {
    public ByteMapClientCodec(ByteMapDecoder inboundHandler, ByteMapEncoder outboundHandler) {
        super(inboundHandler, outboundHandler);
    }

    public ByteMapClientCodec() {
        this(new ByteMapDecoder(), new ByteMapEncoder());
    }
}
