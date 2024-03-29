package com.lizhi.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public class NettyTelnetServer {

    // 指定端口号
    private static final int PORT = 8888;
    private ServerBootstrap serverBootstrap;

    private EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    private EventLoopGroup workerGroup = new NioEventLoopGroup();

    public void open() throws InterruptedException {
        serverBootstrap = new ServerBootstrap();

        // 指定socket的一些属性
        serverBootstrap.option(ChannelOption.SO_BACKLOG, 1024);

        serverBootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)                  // 指定是一个NIO连接通道
                .handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(new NettyTelnetInitializer());            //制定处理器

        // 绑定对应的端口号,并启动开始监听端口上的连接
        //sync()会同步等待连接操作结果，用户线程将在此wait()，直到连接操作完成之后，线程被notify(),用户代码继续执行
        Channel ch = serverBootstrap.bind(PORT).sync().channel();

        // 等待关闭,同步端口
        ch.closeFuture().sync();
    }

    public void close(){
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }
}