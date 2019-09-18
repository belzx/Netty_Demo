package com.lizhi.netty3;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChatServer {
    private static Logger log = LoggerFactory.getLogger(ChatServer.class);
    private static ChannelGroup channelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    //服务器端
    public static void main(String[] args) {
        NioEventLoopGroup bossGroup = new NioEventLoopGroup();
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();

        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new SimpleChannelInitializer());
        try {
            ChannelFuture sync = serverBootstrap.bind(8123).sync();
            sync.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    static class SimpleChannelInitializer extends ChannelInitializer<SocketChannel> {
        @Override
        protected void initChannel(SocketChannel channel) throws Exception {
            ChannelPipeline pipeline = channel.pipeline();

           //addlast  实际上创建一个 Context 对象包装了 pipeline 和 handler
            //然后通过同步或者异步的方式，间接执行 handler 的 自定义方法-------initChannel 方法。而这个 context 也加入到了 pipeline 的链表节点中
            pipeline.addLast(new HttpServerCodec()); //支持http协议
            pipeline.addLast(new HttpObjectAggregator(65535));

            pipeline.addLast(new ChunkedWriteHandler());//大文件支持
            pipeline.addLast(new WebSocketServerProtocolHandler("/echoserver"));  // *) 对websocket协议的处理--握手处理, ping/pong心跳, 关闭 //设置地址
            pipeline.addLast(new SimpleClientChannelHandler());
            pipeline.addLast(new SimpleClientChannelHandler1());
        }
    }

    static class SimpleClientChannelHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            log.info("Handler[0] channelRead()");
            ctx.fireChannelRead(msg);
        }

        @Override
        public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
            log.info("Handler[0] handlerRemoved()");
            ctx.disconnect(ctx.newPromise());
            ctx.close();
        }

        @Override
        public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
            log.info("Handler[0] channelRegistered()");
            channelGroup.add(ctx.channel());
        }

        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
            log.info("Handler[0] channelReadComplete()");
            ctx.flush();
        }
    }

    static class SimpleClientChannelHandler1 extends ChannelInboundHandlerAdapter {

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            log.info("Handler[1] channelRead()");
            log.info("receive message:"+ ((TextWebSocketFrame)msg).text());
            //writeAndFlush 会立马刷新到channl中
            channelGroup.forEach(d -> d.writeAndFlush(new TextWebSocketFrame("Channl："+ctx.channel().toString()+"：说："+((TextWebSocketFrame)msg).text())));
            new Thread(() -> {
                for (int i = 0 ; i < 2 ; i ++){
                    channelGroup.forEach(d -> d.writeAndFlush(new TextWebSocketFrame("this is a test")));
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
            ctx.writeAndFlush("welcome");
        }

        @Override
        public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
            log.info("Handler[1] handlerRemoved()");
            ctx.disconnect(ctx.newPromise());
            ctx.close();
        }

        @Override
        public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
            log.info("Handler[1] channelRegistered()");
        }
    }

}

