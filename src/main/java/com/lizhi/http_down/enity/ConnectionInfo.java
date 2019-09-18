package com.lizhi.http_down.enity;

import lombok.Data;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;

@Data
public class ConnectionInfo {
    private long startPosition;
    private long endPosition;
    private long downedSize;
    private SeekableByteChannel fileChannel;

    public long getTotalSize() {
        return endPosition - startPosition + 1;
    }

    public ConnectionInfo(long startPosition, long endPosition, SeekableByteChannel fileChannel) throws IOException {
        this.startPosition = startPosition;
        this.endPosition = endPosition;
        this.fileChannel = fileChannel;
        fileChannel.position(startPosition);
    }
}
