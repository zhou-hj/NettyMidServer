package cn.xiaosheng996.NettyProtobufMidTcpServer;


import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@ChannelHandler.Sharable
public class ServerHandler extends ChannelInboundHandlerAdapter{

    private static final Logger log = LoggerFactory.getLogger(ServerHandler.class);


    public static final ConcurrentMap<Channel, Channel> aTobClientsMap = new ConcurrentHashMap<>();

    protected ServerHandler() {
    	
    }

    @Override
	public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
		super.channelRegistered(ctx);
	}

	@Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        
        System.out.println("【B服】 A->B客户端channel:"+ctx.channel().id().asLongText()+"激活！");
        aTobClientsMap.put(ctx.channel(), ctx.channel());
        
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(ctx.channel().eventLoop())
        	.channel(NioSocketChannel.class)
        	.handler(new ChannelInitializer<Channel>() {
        		@Override
        		protected void initChannel(Channel ch) throws Exception {
        			ChannelPipeline pipeline = ch.pipeline();
        			pipeline.addLast("decoder", new ProtoDecoder(5120));
        			pipeline.addLast("encoder", new ProtoEncoder(2048));
        			pipeline.addLast("serverHandler", new MidClientHandler());
        		}
		});
        
        bootstrap.connect(new InetSocketAddress("192.168.1.2", 38997))
        	.addListener(new ChannelFutureListener(){
				@Override
				public void operationComplete(ChannelFuture future)
						throws Exception {
					aTobClientsMap.put(ctx.channel(), future.channel());
					System.out.println("【B服】 A->B客户端channel:" + ctx.channel().id().asLongText() 
		            		+ ",【B服】 B->C客户端channel:"+future.channel().id().asLongText());
				}
        		
        	});
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Packet packet = (Packet)msg;
        
        System.out.println("【B服】 A->B客户端channel:"+ctx.channel().id().asLongText()+"收到A服协议id:"+packet.getCmd());
        Channel channelC = aTobClientsMap.get(ctx.channel());
        ProtoManager.handleProto(packet, channelC);
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        
        Channel channel = aTobClientsMap.remove(ctx.channel());
        if (channel != null){
        	ctx.channel().close();
        	channel.close();
        }
        
        System.out.println("【B服】 A->B客户端channel:" + ctx.channel().id().asLongText() + "失去连接！");
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        super.channelWritabilityChanged(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("【B服】  A->B客户端channel:"+ctx.channel().id().asLongText()+"exceptionCaught！", cause);
        Channel channel = aTobClientsMap.get(ctx.channel());
        if (channel != null) {
        	ctx.channel().close();
        	channel.close();
        }
    }
}
