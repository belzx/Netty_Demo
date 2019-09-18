package com.lizhi.http_down.enity;

import io.netty.handler.codec.http.*;
import lombok.Data;

import java.net.URL;

@Data
public class HttpRequestInfo {

    private HttpRequest httpRequest;
    private String host;
    private int port;
    private boolean ssl;

    public HttpRequestInfo(URL url) {
        parseUrl(url);
    }

    public void parseUrl(URL url) {
        String proto = url.getProtocol();
        if (proto == null || (!proto.equals("http") && !proto.equals("https"))) {
            throw new RuntimeException("非http 协议");
        }

        DefaultHttpHeaders httpHeaders = new DefaultHttpHeaders();
        httpHeaders.add("Host", url.getHost())
                .add("Connection", "keep-alive")
                .add("Accept", "*/*")
                .add("Referer", url.getProtocol() + "://" + url.getHost());
        httpRequest = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, url.getFile(), httpHeaders);
        host = url.getHost();
        if (proto.equals("https")) {
            ssl = true;
            port = 443;
        } else {
            ssl = false;
            port = 80;
        }
    }
}

