package cn.xiaosheng996.NettyProtobufMidTcpServer;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.Map;

public class MidClientHandler extends ChannelInboundHandlerAdapter {

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		Packet packet = (Packet)msg;
		
		Channel channelA = null;
		for(Map.Entry<Channel, Channel> entry : ServerHandler.aTobClientsMap.entrySet()){
			if(entry.getValue() == ctx.channel()){
				channelA = entry.getKey();
				break;
			}
		}
		if (channelA == null){
			System.out.println("【B服】 B->C客户端channel:"+ctx.channel().id().asLongText()+"找不到A服客户端");
			return;
		}

		System.out.println("【B服】 B->C客户端channel:"+ctx.channel().id().asLongText()+"收到C服协议id:"+packet.getCmd());
        ProtoManager.handleProto(packet, channelA);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		cause.printStackTrace();
		ctx.channel().close();
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		super.channelActive(ctx);
		System.out.println("【B服】 B->C客户端channel:"+ctx.channel().id().asLongText()+"激活！");
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		super.channelActive(ctx);
		System.out.println("【B服】 B->C客户端channel:"+ctx.channel().id().asLongText()+"断开连接！");
	}

}
