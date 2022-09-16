package com.bisoft.bfm.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CheckPointDTO {
    private String port;
    private String user;
    private String password;
}
