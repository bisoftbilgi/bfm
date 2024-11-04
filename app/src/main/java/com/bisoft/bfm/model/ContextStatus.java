package com.bisoft.bfm.model;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ContextStatus {

    private String clusterStatus;
    private String checkPaused;
    private String mailNotifyEnabled;
    private String watchStrategy;
    private List<ContextServer> clusterServers;

    public ContextStatus(String clusterStatus, String checkPaused, String mailNotifyEnabled, String watchStrategy, List<ContextServer> clusterServers) {
        this.clusterStatus = clusterStatus;        
        this.checkPaused = checkPaused;
        this.mailNotifyEnabled = mailNotifyEnabled;
        this.watchStrategy = watchStrategy;
        this.clusterServers = clusterServers;
    }   
    
}
