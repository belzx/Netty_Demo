package com.lizhi.nettyFile;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.stream.ChunkedWriteHandler;

public class HttpStaticFileServerInitializer extends ChannelInitializer<SocketChannel> {

    private final SslContext sslCtx;

    public HttpStaticFileServerInitializer(SslContext sslCtx) {
        this.sslCtx = sslCtx;
    }

    @Override
    public void initChannel(SocketChannel ch) {
        ChannelPipeline pipeline = ch.pipeline();
        if (sslCtx != null) {
            pipeline.addLast(sslCtx.newHandler(ch.alloc()));
        }
        pipeline.addLast(new HttpServerCodec());
        pipeline.addLast(new HttpObjectAggregator(65536));
        /*
        write方法将数据包装成PendingWrite塞入队列
        flush方法：从队列里面取出数据对于如果是ChunkedInput类型的数据会自动切割
        支持文件，stream，input，httppostrequest和websocketinput几种切割。
        在这切割期间设置了endofInput 每次发送数据完了之后看看是否到了endofInput
        如果没有 在检测channel是否可写 如果可写 则在添加监听者监听发送流程
        不可写则在上面任务写完之后重新判断是否可写 如果可写就结束，但是
        其也重写了channelWritabilityChanged方法，如果可写还会继续发送，
        所以这个ChunkedWriteHandler的大致逻辑就是，首先Write方法都会把数据写入队列
        等到flush的时候才是把数据变成上述几种类型，然后发送到后续的handler 进行发送
        这个过程中如果阻塞则通过注册监听以及channelWritabilityChanged在通道可写的情况下继续去write

        Netty提供了ChunkedWriteHandler来解决大文件或者码流传输过程中可能发生的内存溢出问题。
         */
        pipeline.addLast(new ChunkedWriteHandler());
        pipeline.addLast(new HttpStaticFileServerHandler());
    }
}