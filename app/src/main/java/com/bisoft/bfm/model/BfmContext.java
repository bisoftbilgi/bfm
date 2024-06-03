package com.bisoft.bfm.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.bisoft.bfm.dto.ClusterStatus;
import com.bisoft.bfm.helper.SymmetricEncryptionUtil;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Component
@Data
@RequiredArgsConstructor
public class BfmContext {

    private final SymmetricEncryptionUtil symmetricEncryptionUtil;

    private List<PostgresqlServer> pgList;

    @Value("${server.pglist:127.0.0.1:5432}")
    String pgServerList;

    @Value("${server.pguser:postgres}")
    String pgUser;

    @Value("${server.pgpassword:postgres}")
    String pgPassword;

    @Value("${bfm.user-crypted:false}")
    public boolean isEncrypted;

    boolean isMasterBfm;

    ClusterStatus clusterStatus;

    PostgresqlServer masterServer;

    String masterServerLastWalPos;

    PostgresqlServer splitBrainMaster;

    String lastCheckLog = "";

    Map<String,String> replayLagMap = new HashMap<>();

    @PostConstruct
    public void init(){
        pgList = new ArrayList<>();
        if(isEncrypted) {
            //  log.info(symmetricEncryptionUtil.decrypt(tlsSecret).replace("=",""));
            pgPassword = (symmetricEncryptionUtil.decrypt(pgPassword).replace("=", ""));
        }
        Arrays.stream(pgServerList.split(",")).forEach( server -> {
            String serverAdress =server;
            int priority = 1;
            if(server.contains("|")){
                serverAdress=server.split("\\|")[0];
                priority = Integer.valueOf(server.split("\\|")[1]);
            }
            PostgresqlServer pgserver = PostgresqlServer.builder().priority(priority).serverAddress(serverAdress).username(pgUser).password(pgPassword).build();
            pgList.add(pgserver);

        });
    }

}
