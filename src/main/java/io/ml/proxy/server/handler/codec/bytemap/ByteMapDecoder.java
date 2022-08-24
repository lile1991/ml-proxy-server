package io.ml.proxy.server.handler.codec.bytemap;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

public class ByteMapDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        ByteBuf newIn = ctx.alloc().buffer(in.readableBytes(), in.capacity());
        for(int i = in.readerIndex(); i < in.writerIndex(); i ++) {
            byte b = in.readByte();
            newIn.writeByte(ByteMapMapper.decode(b));
        }
        out.add(newIn);
    }
}
