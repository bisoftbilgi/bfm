package com.bisoft.bfm.dto;

public enum ClusterStatus {
    HEALTHY,
    IGNORING,
    FAILOVER,
    SWITCHOVER,
    NOT_HEALTHY,
    MASTER_ONLY,
    WARNING
}
