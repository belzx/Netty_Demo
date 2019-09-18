package com.lizhi;


import com.lizhi.http_down.HttpDownBootstrap;
import com.lizhi.http_down.enity.DownConfig;
import io.netty.channel.nio.NioEventLoopGroup;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.ExecutionException;

public class HttpDownTest {


    public static String url="https://www.baidu.com/img/bd_logo1.png?where=super";

    @Test
    public void downTest() throws IOException, ExecutionException, InterruptedException {
        NioEventLoopGroup loopGroup = new NioEventLoopGroup(1);
        HttpDownBootstrap.builder()
                .url(new URL(url))
                .downConfig(new DownConfig("E:\\新建文件夹\\b.png",2))
                .loopGroup(loopGroup)
                .build()
                .start().get();

    }
}
