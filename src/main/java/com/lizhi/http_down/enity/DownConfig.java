package com.lizhi.http_down.enity;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DownConfig {
    //
    private String filePath;
    //线程数
    private int connections;

    public static DownConfig defaultDownConfig() {
        return new DownConfig(null, 2);
    }
}
