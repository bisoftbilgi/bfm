package com.bisoft.bfm.model;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ContextStatus {

    private String clusterStatus;
    private List<ContextServer> clusterServers;
    private String checkPaused;
    private String mailNotifyEnabled;

    public ContextStatus(String clusterStatus, List<ContextServer> clusterServers, String checkPaused, String mailNotifyEnabled) {
        this.clusterStatus = clusterStatus;
        this.clusterServers = clusterServers;
        this.checkPaused = checkPaused;
        this.mailNotifyEnabled = mailNotifyEnabled;
    }   
    
}
