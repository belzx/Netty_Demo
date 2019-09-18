package com.lizhi.rpcdemo;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

public class InvokerHandler extends ChannelInboundHandlerAdapter {

    private static Logger log = LoggerFactory.getLogger(RPCServer.class);

    public static ConcurrentHashMap<String, Object> classMap = new ConcurrentHashMap<String, Object>();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        //ckassInfo作为包装类
        ClassInfo classInfo = (ClassInfo) msg;
        Object claszz = null;
        //className是否包含
        if (!classMap.containsKey(classInfo.getClassName())) {
            try {
                //获取实例
                claszz = Class.forName(classInfo.getClassName()).newInstance();
                //将实例保存到map中
                classMap.put(classInfo.getClassName(), claszz);
            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        } else {
            claszz = classMap.get(classInfo.getClassName());
        }

        //利用反射调用实例的方法
        Method method = claszz.getClass().getMethod(classInfo.getMethodName(), classInfo.getTypes());
        Object result = method.invoke(claszz, classInfo.getObjects());
        ctx.write(result);
        ctx.flush();
        ctx.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

}  

