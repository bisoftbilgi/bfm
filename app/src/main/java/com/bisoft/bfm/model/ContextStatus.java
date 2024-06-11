package com.bisoft.bfm.model;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ContextStatus {

    private String clusterStatus;
    private List<ContextServer> clusterServers;

    public ContextStatus(String clusterStatus, List<ContextServer> clusterServers) {
        this.clusterStatus = clusterStatus;
        this.clusterServers = clusterServers;
    }   
    
}