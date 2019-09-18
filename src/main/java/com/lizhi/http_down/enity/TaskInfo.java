package com.lizhi.http_down.enity;

import lombok.Data;

import java.util.List;

@Data
public class TaskInfo {

    private long downSize;
    private long speed;

    private List<ConnectionInfo> connectionInfos;
}
