package com.lizhi.http_down.util;

import com.lizhi.http_down.enity.FileDescription;
import com.lizhi.http_down.enity.HttpRequestInfo;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.util.internal.StringUtil;

import javax.net.ssl.SSLException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HttpUtil {

    private static volatile SslContext sslContextInstance;

    public static SslContext getSslContext() throws SSLException {
        if (sslContextInstance == null) {
            synchronized (HttpUtil.class) {
                if (sslContextInstance == null) {
                    sslContextInstance = SslContextBuilder
                            .forClient()
                            .trustManager(InsecureTrustManagerFactory.INSTANCE)
                            .build();
                }
            }
        }
        return sslContextInstance;
    }


    public static FileDescription getFileDescription(HttpRequestInfo requestInfo, NioEventLoopGroup eventLoopGroup) {
        HttpResponse response = null;
        while (true){
            response = headRangeRequest(requestInfo, eventLoopGroup);
            System.out.println(response.toString());
            int status = response.status().code();
            if (response.status().code() / 100 == 2) {
                return getFileDescription(requestInfo.getHttpRequest(), response);
            } else if (status / 100 == 3) {
                URL location = null;
                try {
                    location = new URL(response.headers().get(HttpHeaderNames.LOCATION));
                } catch (MalformedURLException e) {
                    throw new RuntimeException("location url format error");
                }
                requestInfo.parseUrl(location);
            } else {
                break;
            }
        }
        throw new RuntimeException("请求失败");
    }

    public static FileDescription getFileDescription(HttpRequest request, HttpResponse response) {
        FileDescription fileDescription = new FileDescription();
        if ("bytes".equals(response.headers().get(HttpHeaderNames.ACCEPT_RANGES))
            || response.status().code() == 206) {
            fileDescription.setSupportRange(true);
        }
        if (response.status().code() == 206) {
            String contentRange = response.headers().get(HttpHeaderNames.CONTENT_RANGE);
            String totalSize = contentRange.substring(contentRange.lastIndexOf("/")+1);
            fileDescription.setTotalSize(Long.parseLong(totalSize));
        } else {
            fileDescription.setTotalSize(response.headers().getInt(HttpHeaders.Names.CONTENT_LENGTH));
        }
        fileDescription.setFileName(getFileName(request, response));
        return fileDescription;
    }

    private static String getFileName(HttpRequest request, HttpResponse response) {
        String fileName = null;
        if (response.headers().contains(HttpHeaderNames.CONTENT_DISPOSITION)) {
            String contentDisposition = response.headers().getAsString(HttpHeaderNames.CONTENT_DISPOSITION);
            Pattern pattern = Pattern.compile("^.*filename=(.*)$");
            Matcher matcher = pattern.matcher(contentDisposition);
            if (matcher.find()) {
               fileName = matcher.group(0);
            }
            if (fileName != null) return fileName;
        }

        Pattern pattern = Pattern.compile("^.*/([^/?]+\\.[^/?]*)(\\?[^?]*)?$");
        Matcher matcher = pattern.matcher(request.getUri());
        if (matcher.find()) {
            fileName = matcher.group(1);
        }
        if (!StringUtil.isNullOrEmpty(fileName)) return fileName;
        else return "unKnown";
    }

    public static HttpResponse headRangeRequest(HttpRequestInfo requestInfo, NioEventLoopGroup eventLoop)  {
        //新建一个新的head请求对象
        HttpRequest request = requestInfo.getHttpRequest();
        DefaultHttpHeaders httpHeaders = new DefaultHttpHeaders();
        httpHeaders.add(request.headers());
        httpHeaders.add(HttpHeaderNames.RANGE, "bytes=0-0");//测试是否支持range
        DefaultHttpRequest headRequest = new DefaultHttpRequest(request.protocolVersion(),
                HttpMethod.HEAD, request.uri(), httpHeaders);

        HttpResponse[] httpResponses = new HttpResponse[1];
        CountDownLatch countDownLatch = new CountDownLatch(1);
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(eventLoop)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer() {
                    @Override
                    protected void initChannel(Channel channel) throws Exception {
                        if (requestInfo.isSsl()) {
                            channel.pipeline().addLast("ssl", getSslContext().newHandler(channel.alloc(), requestInfo.getHost(), requestInfo.getPort()));
                        }
                        channel.pipeline()
                                .addLast("httpcodec", new HttpClientCodec())
                                .addLast(new ChannelInboundHandlerAdapter() {

                                    @Override
                                    public void channelRead(ChannelHandlerContext ctx, Object msg) {
                                        if (msg instanceof HttpResponse) {
                                            httpResponses[0] = (HttpResponse) msg;
                                            ctx.channel().close();
                                            countDownLatch.countDown();
                                        }
                                    }
                                });
                    }
                });
        ChannelFuture cf = null;
        try {
            cf = bootstrap.connect(requestInfo.getHost(), requestInfo.getPort()).sync();
            ChannelFuture finalCf = cf;
            cf.addListener(future -> {
                if (future.isSuccess()) {
                    finalCf.channel().writeAndFlush(headRequest);
                } else {
                    countDownLatch.countDown();
                }
            });

            countDownLatch.await(30, TimeUnit.SECONDS); //等待回复， 最多30秒

            cf.channel().close();
            if (httpResponses[0] == null) {
                throw new RuntimeException("连接超时");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("建立连接失败");
        }
        return httpResponses[0];
    }

    public static HttpRequest clone(HttpRequest request) {
        return new DefaultHttpRequest(request.protocolVersion(),
                request.method(), request.uri(), request.headers());
    }
}
