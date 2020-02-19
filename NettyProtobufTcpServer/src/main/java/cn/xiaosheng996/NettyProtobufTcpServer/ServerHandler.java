package cn.xiaosheng996.NettyProtobufTcpServer;


import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@ChannelHandler.Sharable
public class ServerHandler extends ChannelInboundHandlerAdapter{

    private static final Logger log = LoggerFactory.getLogger(ServerHandler.class);


    private final ConcurrentMap<Channel, Channel> ref = new ConcurrentHashMap<>();

    protected ServerHandler() {
    	
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        
        System.out.println("【C服】 B->C客户端channel:" + ctx.channel().id().asLongText() + "激活！");
        ref.put(ctx.channel(), ctx.channel());
    }
    
	@Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Packet packet = (Packet)msg;
        Channel channel = ref.get(ctx.channel());
        
        System.out.println("【C服】 B->C客户端channel:"+channel.id().asLongText()+"收到B服协议:"+packet.getCmd());
        ProtoManager.handleProto(packet, channel);
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        
        Channel channel = ref.remove(ctx.channel());
        if (channel != null)
        	channel.close();
        
        System.out.println("【C服】 B->C客户端channel:" + ctx.channel().id().asLongText() + "失去连接！");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("【C服】 B->C客户端channel:"+ctx.channel().id().asLongText()+"exceptionCaught！", cause);
        Channel channel = ref.get(ctx.channel());
        if (channel != null) {
        	channel.close();
        } else {
        	ctx.channel().close();
        }
    }
}
