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

    public ContextServer(String address, String databaseStatus, String lastCheck, String lastWalPos, String replayLag) {
        this.address = address;
        this.databaseStatus = databaseStatus;
        this.lastCheck = lastCheck;
        this.lastWalPos = lastWalPos;
        this.replayLag = replayLag;
    }  
    
    
}
