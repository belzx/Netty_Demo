package com.lizhi.http_down.util;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

public class FileUtil {

    public static void createSparseFile(String filePath, long size) throws IOException {
        File file = new File(filePath);
        Files.deleteIfExists(file.toPath());
        try (SeekableByteChannel channel = Files.newByteChannel(file.toPath(), StandardOpenOption.SPARSE, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
            channel.position(size-1);
            channel.write(ByteBuffer.wrap(new byte[]{0}));
        }
    }
}
