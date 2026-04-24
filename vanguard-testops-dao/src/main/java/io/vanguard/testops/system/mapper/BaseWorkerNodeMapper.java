package io.vanguard.testops.system.mapper;

import io.vanguard.testops.sdk.domain.WorkerNode;

public interface BaseWorkerNodeMapper {
    int insert(WorkerNode record);
}