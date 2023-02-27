package com.bisoft.bfm.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RewindDTO {
    private String serverIp;
    private String port;
    private String user;
    private String password;
    private String masterIp;
}
