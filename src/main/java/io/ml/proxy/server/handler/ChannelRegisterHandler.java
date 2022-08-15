package io.ml.proxy.server.handler;

import io.ml.proxy.server.GlobalChannelManage;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;

public class ChannelRegisterHandler extends ChannelHandlerAdapter {

    GlobalChannelManage globalChannelManage;
    public ChannelRegisterHandler(GlobalChannelManage globalChannelManage) {
        this.globalChannelManage = globalChannelManage;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        globalChannelManage.add(ctx.channel());
        super.handlerAdded(ctx);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {

    }

}
