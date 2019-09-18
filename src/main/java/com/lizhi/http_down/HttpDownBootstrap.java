package com.lizhi.http_down;


import com.lizhi.http_down.enity.*;
import com.lizhi.http_down.util.FileUtil;
import com.lizhi.http_down.util.HttpUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.util.ReferenceCountUtil;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * 1：获取file大小
 * 2：按照线程大小分批执行下载
 * 3：拼接文件
 */
@Slf4j
public class HttpDownBootstrap {
    private final HttpRequestInfo httpRequestInfo;
    private final FileDescription fileDescription;
    private final DownConfig downConfig;
    private final NioEventLoopGroup loopGroup;
    private final TaskInfo taskInfo;
    private CountDownLatch countDownLatch;

    private HttpDownBootstrap(HttpRequestInfo httpRequestInfo, FileDescription fileDescription, DownConfig downConfig, NioEventLoopGroup loopGroup, TaskInfo taskInfo) {
        this.httpRequestInfo = httpRequestInfo;
        this.fileDescription = fileDescription;
        this.downConfig = downConfig;
        this.loopGroup = loopGroup;
        this.taskInfo = taskInfo;
    }

    public static HttpDownBootstrapBuilder builder() {
        return new HttpDownBootstrapBuilder();
    }

    public Future<Void> start() throws IOException {
        log.info("start down");
        String filePath = downConfig.getFilePath();
        FileUtil.createSparseFile(filePath, fileDescription.getTotalSize());
        buildConnections();
        connect();
        return new Future<Void>() {
            @Override
            public boolean cancel(boolean b) {
                return false;
            }

            @Override
            public boolean isCancelled() {
                return false;
            }

            @Override
            public boolean isDone() {
                return countDownLatch.getCount() == 0;
            }

            @Override
            public Void get() throws InterruptedException {
                countDownLatch.await();
                return null;
            }

            @Override
            public Void get(long l, TimeUnit timeUnit) throws InterruptedException {
                countDownLatch.await(l, timeUnit);
                return null;
            }
        };
    }

    private void buildConnections() throws IOException {
        if (fileDescription.getTotalSize() / downConfig.getConnections() <= 0) {
            downConfig.setConnections((int) fileDescription.getTotalSize());
        }
        if (!fileDescription.isSupportRange()) {
            downConfig.setConnections(1);
        }
        countDownLatch = new CountDownLatch(downConfig.getConnections());
        List<ConnectionInfo> connectionInfoList = new ArrayList<>(downConfig.getConnections());
        long splitSize = fileDescription.getTotalSize() / downConfig.getConnections();
        Path path = Paths.get(downConfig.getFilePath());
        for (int i = 0; i < downConfig.getConnections()-1; i++) {
            ConnectionInfo connectionInfo = new ConnectionInfo(i*splitSize, (i+1)*splitSize-1, Files.newByteChannel(path, StandardOpenOption.WRITE));
            connectionInfoList.add(connectionInfo);
        }
        int endIndex = downConfig.getConnections()-1;
        ConnectionInfo endConnectionInfo = new ConnectionInfo(endIndex * splitSize, fileDescription.getTotalSize() - 1, Files.newByteChannel(path, StandardOpenOption.WRITE));
        connectionInfoList.add(endConnectionInfo);
        taskInfo.setConnectionInfos(connectionInfoList);
    }

    public void connect() {
        for (ConnectionInfo conn : taskInfo.getConnectionInfos()) {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.channel(NioSocketChannel.class)
                    .group(loopGroup)
                    .handler(new ChannelInitializer() {

                        @Override
                        protected void initChannel(Channel channel) throws Exception {
                            ChannelPipeline pipeline = channel.pipeline();
                            if (httpRequestInfo.isSsl()) {
                                pipeline.addLast("ssl", HttpUtil.getSslContext().newHandler(channel.alloc(), httpRequestInfo.getHost(), httpRequestInfo.getPort()));
                            }
                            pipeline.addLast("http", new HttpClientCodec())
                                    .addLast(new ChannelInboundHandlerAdapter() {

                                        @Override
                                        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                            if (msg instanceof HttpContent) {
                                                HttpContent httpContent = (HttpContent) msg;
                                                ByteBuf byteBuffer = httpContent.content();
                                                long size = byteBuffer.readableBytes();
                                                if (size + conn.getDownedSize() > conn.getTotalSize()) {
                                                    size = conn.getTotalSize() - conn.getDownedSize();
                                                }
                                                log.info("read size:" + size);
                                                conn.getFileChannel().write(byteBuffer.nioBuffer());
                                                conn.setDownedSize(conn.getDownedSize() + size);
                                                if (conn.getDownedSize() >= conn.getTotalSize()) {
                                                    log.info(conn.toString()+"下载完成");
                                                    ctx.close();
                                                    conn.getFileChannel().close();
                                                    countDownLatch.countDown();
                                                }
                                            }
                                            ReferenceCountUtil.release(msg);
                                        }
                                    });
                        }
                    });
            ChannelFuture cf = bootstrap.connect(httpRequestInfo.getHost(), httpRequestInfo.getPort());
            cf.addListener(future -> {
                if (future.isSuccess()) {
                    HttpRequest request = HttpUtil.clone(httpRequestInfo.getHttpRequest());
                    request.headers().add(HttpHeaderNames.RANGE, "bytes="+conn.getStartPosition()+"-"+conn.getEndPosition());
                    cf.channel().writeAndFlush(request);
                }
            });
        }
    }

    @NonNull
    public HttpRequestInfo getHttpRequestInfo() {
        return this.httpRequestInfo;
    }

    @NonNull
    public FileDescription getFileDescription() {
        return this.fileDescription;
    }

    @NonNull
    public DownConfig getDownConfig() {
        return this.downConfig;
    }

    @NonNull
    public NioEventLoopGroup getLoopGroup() {
        return this.loopGroup;
    }

    @NonNull
    public TaskInfo getTaskInfo() {
        return this.taskInfo;
    }

    public String toString() {
        return "HttpDownBootstrap(httpRequestInfo=" + this.getHttpRequestInfo() + ", fileDescription=" + this.getFileDescription() + ", downConfig=" + this.getDownConfig() + ", loopGroup=" + this.getLoopGroup() + ", taskInfo=" + this.getTaskInfo() + ")";
    }

    public static class HttpDownBootstrapBuilder {
        private HttpRequestInfo httpRequestInfo;
        private FileDescription fileDescription;
        private DownConfig downConfig;
        private NioEventLoopGroup loopGroup;
        private TaskInfo taskInfo;
        private URL url;

        HttpDownBootstrapBuilder() {
        }

        public HttpDownBootstrap.HttpDownBootstrapBuilder url(URL url) {
            this.url = url;
            return this;
        }

        public HttpDownBootstrap.HttpDownBootstrapBuilder httpRequestInfo(HttpRequestInfo httpRequestInfo) {
            this.httpRequestInfo = httpRequestInfo;
            return this;
        }

        public HttpDownBootstrap.HttpDownBootstrapBuilder fileDescription(FileDescription fileDescription) {
            this.fileDescription = fileDescription;
            return this;
        }

        public HttpDownBootstrap.HttpDownBootstrapBuilder downConfig(DownConfig downConfig) {
            this.downConfig = downConfig;
            return this;
        }

        public HttpDownBootstrap.HttpDownBootstrapBuilder loopGroup(NioEventLoopGroup loopGroup) {
            this.loopGroup = loopGroup;
            return this;
        }

        public HttpDownBootstrap.HttpDownBootstrapBuilder taskInfo(TaskInfo taskInfo) {
            this.taskInfo = taskInfo;
            return this;
        }

        public HttpDownBootstrap build() {
            if (loopGroup == null) {
                throw new RuntimeException("loop group null");
            }
            if (taskInfo == null) {
                taskInfo = new TaskInfo();
            }
            if (url != null) {
                httpRequestInfo = new HttpRequestInfo(url);
                fileDescription = HttpUtil.getFileDescription(httpRequestInfo, loopGroup);
            }
            if (downConfig.getFilePath() == null) {
                downConfig.setFilePath(fileDescription.getFileName());
            }
            return new HttpDownBootstrap(httpRequestInfo, fileDescription, downConfig, loopGroup, taskInfo);
        }

        public String toString() {
            return "HttpDownBootstrap.HttpDownBootstrapBuilder(httpRequestInfo=" + this.httpRequestInfo + ", fileDescription=" + this.fileDescription + ", downConfig=" + this.downConfig + ", loopGroup=" + this.loopGroup + ", taskInfo=" + this.taskInfo + ")";
        }
    }
}
