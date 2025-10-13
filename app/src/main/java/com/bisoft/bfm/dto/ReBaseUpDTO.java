package com.bisoft.bfm.dto;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReBaseUpDTO {
    private String masterIp;
    private String masterPort;
    private String repUser;
    private String repPassword;
    private List<String> tablespaceList;
}
