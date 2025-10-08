package com.bisoft.bfm.dto;

import java.util.List;

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
    private List<String> tablespaceList;
}
