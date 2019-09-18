package com.lizhi.netty2;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class EchoServer {
    private final int port;

    public EchoServer(int port) {
        this.port = port;
    }

    public static void main(String[] args) throws Exception {
//        System.out.println(args);
//        int i = Integer.parseInt(args[0]);
        new EchoServer(8080).start();
    }

    private void start() throws Exception {
        //创建处理io的多线程事件处理器
        NioEventLoopGroup group = new NioEventLoopGroup();
        try{
            ServerBootstrap b = new ServerBootstrap();
            b.group(group)
                    .channel(NioServerSocketChannel.class)//指定 NioServerSocketChannel（5）为信道类型
                    .localAddress(port)//制定端口
                    .childHandler(new ChannelInitializer(){
                        //在这里我们使用一个特殊的类，ChannelInitializer 。
                        // 当一个新的连接被接受，一个新的子 Channel 将被创建， ChannelInitializer 会添加我们EchoServerHandler
                        // 的实例到 Channel 的 ChannelPipeline。正如我们如前所述，如果有入站信息，这个处理器将被通知。
                        @Override
                        protected void initChannel(Channel channel) throws Exception {
                            channel.pipeline().addLast(
                                    new EchoServerHandler());
                        }
                    });

            ChannelFuture sync = b.bind().sync();//们绑定的服务器，等待绑定完成。 （调用 sync() 的原因是当前线程阻塞
            System.out.println(EchoServer.class.getName() + " started and listen on " + sync.channel().localAddress());
            //第9步的应用程序将等待服务器 Channel 关闭（因为我们 在 Channel 的 CloseFuture 上调用 sync()）。
            // 现在，我们可以关闭下 EventLoopGroup 并释放所有资源，包括所有创建的线程（10）。
            sync.channel().closeFuture().sync();            //9
        }finally {
            group.shutdownGracefully().sync();            //10
        }


    }


}
