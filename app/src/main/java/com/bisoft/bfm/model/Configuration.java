package com.bisoft.bfm.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Configuration {
    private String clusterName;
    private Integer clusterPort;
    private String bfmPair;
    private String pgUsername;
    private String pgPassword;
    private Boolean isEncrypted;
    private Integer ignoranceCount;
    private String pgServerList;
    private String watchStrategy;
    private Boolean mailEnabled;
    private String tlsKeyAlias;
    private Boolean useSsl;
    private String tlsSecret;
    private String tlsKeyStoreType;
    private String tlsKeyStore;
    private Boolean bfmUseTls;
    private Boolean minipgUseTls;
    private String minipgUsername;
    private String minipgPassword;
    private Integer minipgPort;
    private Integer heartbeatInterval;
    private String heartbeatQuery;
    private String dataLossTolerance; //1G, 24M, 120K
    private String statusFileExpire; //3H=3 Hour or 1D=1 Day
    private String exMasterBehavior; //rejoin or stop
    private Boolean basebackupSlaveJoin;
    private String mailReceivers; //#multiple receiver must seperate with comma user1.email@gmail.com,user2.email@gmail.com
    private String smtpServerAddress;
    private Integer smtpServerPort;
    private String smtpUsername;
    private String smtpPassword;
    private Boolean smtpAuth;
    private Boolean smtpTLSenabled;
    private Boolean isConfigured;

    public Configuration(String clusterName, Integer clusterPort, String bfmPair, String pgUsername, String pgPassword, Boolean isEncrypted, Integer ignoranceCount,
            String pgServerList, String watchStrategy, Boolean mailEnabled, String tlsKeyAlias, Boolean useSsl, String tlsSecret, String tlsKeyStoreType,
            String tlsKeyStore, Boolean bfmUseTls, Boolean minipgUseTls, String minipgUsername, String minipgPassword, Integer minipgPort, Integer heartbeatInterval,
            String heartbeatQuery, String dataLossTolerance, String statusFileExpire, String exMasterBehavior, Boolean basebackupSlaveJoin, 
            String mailReceivers,String smtpServerAddress, Integer smtpServerPort, String smtpUsername, String smtpPassword, Boolean smtpAuth, Boolean smtpTLSenabled,
            Boolean isConfigured) {

        this.clusterName = clusterName;
        this.clusterPort = clusterPort;
        this.bfmPair = bfmPair;
        this.pgUsername = pgUsername;
        this.pgPassword = pgPassword;
        this.isEncrypted = isEncrypted;
        this.ignoranceCount = ignoranceCount;
        this.pgServerList = pgServerList;
        this.watchStrategy = watchStrategy;
        this.mailEnabled = mailEnabled;
        this.tlsKeyAlias = tlsKeyAlias;
        this.useSsl = useSsl;
        this.tlsSecret = tlsSecret;
        this.tlsKeyStoreType = tlsKeyStoreType;
        this.tlsKeyStore = tlsKeyStore;
        this.bfmUseTls = bfmUseTls;
        this.minipgUseTls = minipgUseTls;
        this.minipgUsername = minipgUsername;
        this.minipgPassword = minipgPassword;
        this.minipgPort = minipgPort;
        this.heartbeatInterval = heartbeatInterval;
        this.heartbeatQuery = heartbeatQuery;
        this.dataLossTolerance = dataLossTolerance;
        this.statusFileExpire = statusFileExpire;
        this.exMasterBehavior = exMasterBehavior;
        this.basebackupSlaveJoin = basebackupSlaveJoin;
        this.mailReceivers = mailReceivers;
        this.smtpServerAddress = smtpServerAddress;
        this.smtpServerPort = smtpServerPort;
        this.smtpUsername = smtpUsername;
        this.smtpPassword = smtpPassword;
        this.smtpAuth = smtpAuth;
        this.smtpTLSenabled = smtpTLSenabled;
        this.isConfigured = isConfigured;
    }

}
