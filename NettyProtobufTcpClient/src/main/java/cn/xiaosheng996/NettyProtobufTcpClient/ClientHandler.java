package cn.xiaosheng996.NettyProtobufTcpClient;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.lang.reflect.Method;

import proto.AProto.GetCFromBReq_1001;

public class ClientHandler extends ChannelInboundHandlerAdapter {

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		Packet packet = (Packet)msg;

		Class<?> clz = ProtoManager.getRespMap().get(packet.getCmd());
		try {
			Method method = clz.getMethod("parseFrom", byte[].class);
			Object object = method.invoke(clz, packet.getBytes());
			
			System.out.println("【A服】 A->B客户端channel:"+ctx.channel().id().asLongText()+"收到B服数据：");
			ProtoPrinter.print(object);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		cause.printStackTrace();
		ctx.channel().close();
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		if (NettyTcpClient.instance().getChannel() != null) {
			NettyTcpClient.instance().getChannel().disconnect();
		}
		NettyTcpClient.instance().setChannel(ctx.channel());
		
		System.out.println("【A服】 A->B客户端channel:"+ctx.channel().id().asLongText()+"激活！");
		
		login(ctx.channel());
	}
	
    //请求登录
    private static void login(Channel channel){
    	System.out.println("【A服】 A->B客户端channel:"+channel.id().asLongText()+"向B服请求1001协议");
    	GetCFromBReq_1001.Builder builder = GetCFromBReq_1001.newBuilder();
    	builder.setA(1001);
    	NettyTcpClient.instance().send(builder.build());
    }

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		System.out.println("【A服】 A->B客户端channel:"+ctx.channel().id().asLongText()+"断开连接");
	}

}
