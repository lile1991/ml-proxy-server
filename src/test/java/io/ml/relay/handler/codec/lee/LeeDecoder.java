package io.ml.relay.handler.codec.lee;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

public class LeeDecoder extends ByteToMessageDecoder {
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        ByteBuf newIn = ctx.alloc().buffer(in.readableBytes(), in.capacity());
        for(int i = in.readerIndex(); i < in.writerIndex(); i ++) {
            byte b = in.readByte();
            if(b == Byte.MAX_VALUE) {
                b = Byte.MIN_VALUE;
            } else {
                b ++;
            }
            newIn.writeByte(b);
        }
        out.add(newIn);
    }
}
