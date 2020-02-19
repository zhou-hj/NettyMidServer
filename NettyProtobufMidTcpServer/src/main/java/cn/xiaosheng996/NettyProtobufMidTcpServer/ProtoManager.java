package cn.xiaosheng996.NettyProtobufMidTcpServer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import proto.AProto.GetCFromBReq_1001;
import proto.AProto.GetCFromBResp_1001;
import proto.BProto.GetCFromCReq_2001;
import proto.BProto.GetCFromCResp_2001;

import com.google.protobuf.Message;

@SuppressWarnings("rawtypes")
public class ProtoManager {
	
	private static Map<Integer, Class<?>> reqMap = null;
	private static Map<Integer, Class<?>> respMap = null;

    static {
        String packageName = "proto";
        Class clazz = Message.class;
        try {
            reqMap = ClassUtils.getClasses(packageName, clazz, "Req_");
            respMap = ClassUtils.getClasses(packageName, clazz, "Resp_");
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static ByteBuf wrapBuffer(Message msg, boolean isReq) {
        ByteBufAllocator alloc = ByteBufAllocator.DEFAULT;
        int protocol = 0;
        Set<Entry<Integer, Class<?>>> set = null;
        if(isReq){
        	set = reqMap.entrySet();
        }else{
        	set = respMap.entrySet();
        }
        
        for (Entry<Integer, Class<?>> entry : set) {
            if (entry.getValue().isInstance(msg)) {
                protocol = entry.getKey();
                break;
            }
        }
        byte[] data = msg.toByteArray();
        // 消息长度=协议号4位+数据体长度
        int length = data.length + 4;
        // 数据包=消息长度+协议号+数据体
        // 数据包长度=4+消息长度
        // ByteBuf buffer = Unpooled.buffer(length + 4);
        ByteBuf buffer = alloc.buffer(length + 4);
       /* buffer.writeInt(length);
        buffer.writeInt(protocol);
        buffer.writeBytes(data);*/
        //HEAD_TCP = (byte)0x80
        buffer.writeByte((byte)0x80);
        buffer.writeShort(length);
        buffer.writeInt(protocol);
        buffer.writeBytes(data);

        if (buffer.readableBytes() > 4096) {
//            LogUtil.warn(ProtobufCenter.toString(protocol) + " " + buffer.readableBytes() + " too big");
        }
        return buffer;
    }
    
    public static Map<Integer, Class<?>> getReqMap() {
    	return reqMap;
    }
    
    public static Map<Integer, Class<?>> getRespMap() {
    	return respMap;
    }
    
    public static void handleProto(Packet packet, Channel channel){
    	//游戏业务线程池处理游戏逻辑
    	//......
    	//......
    	
    	int cmd = packet.getCmd();
    	if(cmd == 1001){
    		
    		Class<?> clz = reqMap.get(cmd);
            try {
            	Method method = clz.getMethod("parseFrom", byte[].class);
            	Object object = method.invoke(clz, packet.getBytes());
            	GetCFromBReq_1001 resp = (GetCFromBReq_1001)object;
            	System.out.println("【B服】 B->C客户端channel:"+channel.id().asLongText()+"请求C服数据,A:a:"+resp.getA());
	    		GetCFromB(channel, resp.getA());//把客户端A的数据转发给C
			} catch (Exception e) {
				e.printStackTrace();
			}
    	}else if(cmd == 2001){
    		
    		Class<?> clz = respMap.get(cmd);
            try {
            	Method method = clz.getMethod("parseFrom", byte[].class);
            	Object object = method.invoke(clz, packet.getBytes());
				GetCFromCResp_2001 resp = (GetCFromCResp_2001)object;
				System.out.println("【B服】 A->B客户端channel:"+channel.id().asLongText()+"返回C服数据:" + resp.getC());
	    		ReturnCFromB(channel, resp.getC());
			} catch (Exception e) {
				e.printStackTrace();
			}
    	}
    }
	
	private static void GetCFromB(Channel channel, int a) {
		GetCFromCReq_2001.Builder builder = GetCFromCReq_2001.newBuilder();
		builder.setA(a);
		
		channel.writeAndFlush(wrapBuffer(builder.build(), true));
	}
	
	private static void ReturnCFromB(Channel channel, int c){
		GetCFromBResp_1001.Builder builder = GetCFromBResp_1001.newBuilder();
		builder.setC(c);
		
		channel.writeAndFlush(wrapBuffer(builder.build(), false));
	}
}
