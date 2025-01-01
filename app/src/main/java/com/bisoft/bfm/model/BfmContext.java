package com.bisoft.bfm.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.bisoft.bfm.ConfigurationManager;
import com.bisoft.bfm.dto.ClusterStatus;
import com.bisoft.bfm.helper.SymmetricEncryptionUtil;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Component
@Data
@RequiredArgsConstructor
public class BfmContext {

    @Autowired
    private ConfigurationManager configurationManager;
    private final SymmetricEncryptionUtil symmetricEncryptionUtil;

    private List<PostgresqlServer> pgList;
    
    boolean isMasterBfm;

    ClusterStatus clusterStatus;

    PostgresqlServer masterServer;

    String masterServerLastWalPos;

    PostgresqlServer splitBrainMaster;

    boolean isCheckPaused = Boolean.FALSE;

    String lastCheckLog = "";

    @PostConstruct
    public void init(){
        System.out.println("context init run...");
        pgList = new ArrayList<>();
        if(this.configurationManager.getConfiguration().getIsEncrypted()) {
            //  log.info(symmetricEncryptionUtil.decrypt(tlsSecret).replace("=",""));
            // pgPassword = (symmetricEncryptionUtil.decrypt(pgPassword).replace("=", ""));
            this.configurationManager.getConfiguration().setPgPassword((symmetricEncryptionUtil.decrypt(this.configurationManager.getConfiguration().getPgPassword())));
        }
        Arrays.stream(this.configurationManager.getConfiguration().getPgServerList().split(",")).forEach( server -> {
            String serverAdress =server;
            int priority = 1;
            if(server.contains("|")){
                serverAdress=server.split("\\|")[0];
                priority = Integer.valueOf(server.split("\\|")[1]);
            }
            PostgresqlServer pgserver = PostgresqlServer.builder()
                                                        .priority(priority)
                                                        .serverAddress(serverAdress)
                                                        .username(this.configurationManager.getConfiguration().getPgUsername())
                                                        .password(this.configurationManager.getConfiguration().getPgPassword())
                                                        .build();
            pgList.add(pgserver);

        });
    }

}
