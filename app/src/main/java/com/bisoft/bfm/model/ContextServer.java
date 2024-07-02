package com.bisoft.bfm.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ContextServer {
    private String address;
    private String databaseStatus;
    private String lastCheck;
    private String lastWalPos;
    private String replayLag;
    private int timeline;

    public ContextServer(String address, String databaseStatus, String lastCheck, String lastWalPos, String replayLag, int timeline) {
        this.address = address;
        this.databaseStatus = databaseStatus;
        this.lastCheck = lastCheck;
        this.lastWalPos = lastWalPos;
        this.replayLag = replayLag;
        this.timeline = timeline;
    }  
    
    
}
