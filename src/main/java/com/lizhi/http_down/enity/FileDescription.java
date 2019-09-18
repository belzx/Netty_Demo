package com.lizhi.http_down.enity;

import lombok.Data;

@Data
public class FileDescription {
    private String fileName;
    private boolean supportRange;
    private long totalSize;
}
