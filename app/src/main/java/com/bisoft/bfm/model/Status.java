package com.bisoft.bfm.model;

public class Status {
    private String serverAddress;
    private String serverLastWalPos;
    private String serverRole;

    public Status(String serverAddress, String serverLastWalPos, String serverRole) {
        this.serverAddress = serverAddress;
        this.serverLastWalPos = serverLastWalPos;
        this.serverRole = serverRole;
    }
}
