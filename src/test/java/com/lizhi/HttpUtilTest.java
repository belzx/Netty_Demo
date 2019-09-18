package com.lizhi;

import com.lizhi.http_down.enity.FileDescription;
import com.lizhi.http_down.enity.HttpRequestInfo;
import com.lizhi.http_down.util.HttpUtil;
import io.netty.channel.nio.NioEventLoopGroup;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;

public class HttpUtilTest {

    public static String url="https://www.baidu.com/img/bd_logo1.png?where=super";

    @Test
    public void fileDescriptionTest() throws IOException {
        HttpRequestInfo requestInfo = new HttpRequestInfo(new URL(url));
        System.out.println(requestInfo.toString());
        NioEventLoopGroup loopGroup = new NioEventLoopGroup(1);
        FileDescription fileDescription = HttpUtil.getFileDescription(requestInfo, loopGroup);
        System.out.println(fileDescription.toString());
    }
}
