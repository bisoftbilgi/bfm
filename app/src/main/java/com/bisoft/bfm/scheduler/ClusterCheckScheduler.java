package com.bisoft.bfm.scheduler;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.bisoft.bfm.dto.ClusterStatus;
import com.bisoft.bfm.dto.DatabaseStatus;
import com.bisoft.bfm.helper.BfmAccessUtil;
import com.bisoft.bfm.helper.EmailService;
import com.bisoft.bfm.helper.MinipgAccessUtil;
import com.bisoft.bfm.model.BfmContext;
import com.bisoft.bfm.model.ContextServer;
import com.bisoft.bfm.model.ContextStatus;
import com.bisoft.bfm.model.PostgresqlServer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class ClusterCheckScheduler {

    private final BfmContext bfmContext;
    private  final MinipgAccessUtil minipgAccessUtil;
    private final BfmAccessUtil bfmAccessUtil;

    @Autowired
    private EmailService mailService;

    private String pairStatus ="Active";

    @Value("${watcher.cluster-pair:no-pair}")
    private String bfmPair;

    @Value("${app.timeout-ignorance-count:3}")
    int timeoutIgnoranceCount;

    @Value("${server.pguser:postgres}")
    String pgUsername;

    @Value("${server.pgpassword:postgres}")
    String pgPassword;

    @Value("${bfm.basebackup-slave-join:false}")
    public boolean basebackup_slave_join;

    @Value("${bfm.data-loss-tolerance:100K}")
    public String data_loss_tolerance;

    @Value("${bfm.status-file-expire:1H}")
    public String status_file_expire;

    @Value("${bfm.ex-master-behavior:rejoin}")
    public String ex_master_behavior;

    int remainingFailCount = (timeoutIgnoranceCount == 0) ? 3 : timeoutIgnoranceCount;

    String leaderSlaveLastWalPos = "";

    Boolean isWarningMailSended = Boolean.FALSE;

    public double getDoubleFromString(String strParam){
        double retval = 0.0;
        if (strParam.endsWith("G")){
            strParam = strParam.replaceAll("[^\\p{IsDigit}]", "");
            retval = Double.parseDouble(strParam);
            retval = retval * 1024 * 1024 * 1024; 
        } else if (strParam.endsWith("M")){
            strParam = strParam.replaceAll("[^\\p{IsDigit}]", "");
            retval = Double.parseDouble(strParam);
            retval = retval * 1024 * 1024; 
        } else if (strParam.endsWith("K")){
            strParam = strParam.replaceAll("[^\\p{IsDigit}]", "");
            retval = Double.parseDouble(strParam);
            retval = retval * 1024; 
        } else {
            strParam = strParam.replaceAll("[^\\p{IsDigit}]", "");
            retval = Double.parseDouble(strParam);
        }

        return retval;
    }

    @Scheduled(fixedDelay = 11000)
    public void amIMasterBfm(){
        try {
            pairStatus = bfmAccessUtil.isPairAlive();
            if(pairStatus.equals("no-pair")){
                log.warn("no bfm cluster pair");
            }else{
                log.info(String.format("Status of bfm pair is : %s", pairStatus));
            }
        }catch(Exception e){
            pairStatus = "Unreachable";
            log.error("Unable to connect to bfm pair");

        }
    }

    @Scheduled(fixedDelay = 7000)
    public void checkUnavilable() {
        if (this.bfmContext.isCheckPaused() == Boolean.TRUE){
            log.info("Cluster Check Paused...");
        } else {
            if(this.bfmContext.isMasterBfm()) {
                try {
    
                    long numberOfAliveServers = this.bfmContext.getPgList().stream()
                            .filter(server -> server.getStatus() != DatabaseStatus.INACCESSIBLE)
                            .count();
    
                    PostgresqlServer master =null ;
                    //If the number of alive servers is 1
                    // this server should have the status MASTER_WITH_NO_SLAVE
                    if(numberOfAliveServers==1){
                        try {
                            master = this.bfmContext.getPgList().stream()
                            .filter(server -> server.getStatus() == DatabaseStatus.MASTER_WITH_NO_SLAVE || server.getStatus() == DatabaseStatus.MASTER ).findFirst().get();                    
                        } catch (Exception e) {
                            log.warn("No Master server found in cluster...");
                        }
                    }else{
                        // Else there should be a master server
                        Long count = this.bfmContext.getPgList().stream()
                        .filter(server -> server.getStatus() == DatabaseStatus.MASTER).count();
                        if (count == 1L ){
                            master = this.bfmContext.getPgList().stream()
                                .filter(server -> server.getStatus() == DatabaseStatus.MASTER).findFirst().get();
                        }
                    }
    
                    final PostgresqlServer selectedMaster = master;
    
                    this.bfmContext.getPgList().stream()
                            .filter(server -> server.getStatus() == DatabaseStatus.INACCESSIBLE && server != this.bfmContext.getSplitBrainMaster())
                            .forEach(server ->
                                    {
                                        try {
                                            if (this.bfmContext.getWatch_strategy().equals("availability")){                            
                                                if (selectedMaster != null){
                                                    if (server.getRewindStarted() == Boolean.FALSE){
                                                        String start_result = minipgAccessUtil.startPg(server);
                                                        if (start_result != "OK"){
                                                            server.setRewindStarted(Boolean.TRUE);
                                                            String rewind_result = minipgAccessUtil.rewind(server, selectedMaster);
                                                            if (! rewind_result.equals("OK")){
                                                                log.info("MiniPG rewind was FAILED. Slave Target:",server);
                                                                if (basebackup_slave_join == true){
                                                                    String rejoin_result = rejoinCluster(selectedMaster, server);
                                                                    log.info("pg_basebackup join cluster result is:"+rejoin_result);
                                                                } else {
                                                                    log.info("pg_basebackup join is set to FALSE. passing for slave server:",server);
                                                                }
                                                            }
                                                            server.setRewindStarted(Boolean.FALSE);        
                                                        }
                                                    } else {
                                                        log.info("Rejoin processing...Passing.");
                                                    }
                                                    
                                                }
                                            } else {
                                                log.info("Rewind or ReBaseUp ignoring..BFM watch strategy is:"+this.bfmContext.getWatch_strategy());
                                                if (this.bfmContext.isMail_notification_enabled() == Boolean.TRUE){
                                                    mailService.sendMail(String.format("BFM Cluster in %s Status",String.valueOf(this.bfmContext.getClusterStatus())), 
                                                    "This is an automatic mail notification."+"\nBFM Cluster Status is:"+this.bfmContext.getClusterStatus() 
                                                    + "\nWatch Strategy is MANUAL. SLAVE JOIN (Rewind or Rebase) ignoring. Please manual respond to failure...Selected Master Server : " + selectedMaster.getServerAddress());
                                                }    
                                            }
                                        } catch (Exception e) {
                                            log.error(String.format("Unable to rewind %s", server.getServerAddress()));
                                        }
                                    }
                            );
                } catch (Exception ex) {
                    ex.printStackTrace();
                    log.error("Unable to find master server for cluster");
                }
            }
        }       
    }

    @Scheduled(fixedDelay = 5000)
    public void checkCluster(){
        if (this.bfmContext.isCheckPaused() == Boolean.TRUE){
            log.info("Cluster Check Paused...");
        } else {
            this.bfmContext.setLastCheckLog("");

            if (this.bfmContext.getSplitBrainMaster() != null){
                log.info("Server:"+this.bfmContext.getSplitBrainMaster().getServerAddress()+ " is stopped for avoid to Split Brain status..");
                this.bfmContext.setLastCheckLog(this.bfmContext.getLastCheckLog() + 
                "Server:"+this.bfmContext.getSplitBrainMaster().getServerAddress()+ " is stopped for avoid to Split Brain status..\n");
            }
            if(pairStatus.equals("no-pair") || pairStatus.equals("Passive") || pairStatus.equals("Unreachable")){
                log.info("this is the active bfm pair");
                this.bfmContext.setMasterBfm(true);
            }else{
                log.info(String.format("Bfm pair is active in %s",bfmPair));
                this.bfmContext.setMasterBfm(false);
                try {
                    Gson gson = new GsonBuilder().setPrettyPrinting().create();
                    String last_saved_status = bfmAccessUtil.getLastSavedStatus();
                    if (last_saved_status != "Unreachable"){
                        try {
                            PrintWriter out = new PrintWriter("./bfm_status.json");
                            out.println(gson.toJson(gson.fromJson(last_saved_status, ContextStatus.class)));
                            out.close();
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }                
                    
                } catch (Exception e) {
                    e.printStackTrace();
                }
                
                return;
            }
            // log.info("-----Cluster Healthcheck Started-----");
            

            bfmContext.getPgList().stream().forEach(server -> {
                try {
                    checkServer(server);
                }catch(Exception e){
                    log.error(String.format("Unable to connect to server : %s",server.getServerAddress()));
                    this.bfmContext.setLastCheckLog(this.bfmContext.getLastCheckLog() + 
                    String.format("Unable to connect to server : %s",server.getServerAddress())+ "\n");
        
                }
            });

            if (bfmContext.getMasterServer() == null){
                PostgresqlServer master = this.selectNewMaster();
                bfmContext.setMasterServer(master);
            }

            isClusterHealthy();


            log.info(String.format("Cluster Status is %s \n\n\n",this.bfmContext.getClusterStatus()));
            this.bfmContext.setLastCheckLog(this.bfmContext.getLastCheckLog() +
                                            String.format("Cluster Status is %s ",this.bfmContext.getClusterStatus())+ "\n");

            // log.info("-----Cluster Healthcheck Finished-----\n\n\n");
        }
    }

    public void checkServer(PostgresqlServer postgresqlServer) throws Exception {
        DatabaseStatus status = postgresqlServer.getDatabaseStatus();

        if (status.equals(DatabaseStatus.MASTER)){
            this.bfmContext.setMasterServer(postgresqlServer);
        }
        
        log.info(String.format("Status of %s is %s",postgresqlServer.getServerAddress(),status));
        this.bfmContext.setLastCheckLog(this.bfmContext.getLastCheckLog() +
                                        String.format("Status of %s is %s",postgresqlServer.getServerAddress(),status)+"\n");
        //log.info(minipgAccessUtil.status(postgresqlServer));
    }

    public void isClusterHealthy(){
        // If cluster is failing over do not check health 
        if(this.bfmContext.getClusterStatus() == ClusterStatus.FAILOVER){
            return;
        }

        long clusterCount = this.bfmContext.getPgList().size();

        long masterCount = this.bfmContext.getPgList().stream().filter(server -> server.getStatus().equals(DatabaseStatus.MASTER)).count();

        long masterWithNoslaveCount = this.bfmContext.getPgList().stream().filter(server -> server.getStatus().equals(DatabaseStatus.MASTER_WITH_NO_SLAVE)).count();

        if(masterCount ==  1L){
            // log.info("Cluster has a master node");
            this.bfmContext.setClusterStatus(ClusterStatus.HEALTHY);
            healthy();
            checkLastWalPositions();
        }else if(clusterCount == 2 && masterCount == 0 && masterWithNoslaveCount==1){
            log.warn("Cluster has a master with no slave (cluster size is 2), not healthy but ingoring failover");
            warning();
            checkSlaves();
            if (this.bfmContext.isMail_notification_enabled() == Boolean.TRUE && this.isWarningMailSended == Boolean.FALSE){
                mailService.sendMail(String.format("BFM Cluster in %s Status",String.valueOf(this.bfmContext.getClusterStatus())), 
                    "This is an automatic mail notification."+"\nBFM Cluster Status is:"+this.bfmContext.getClusterStatus() 
                    + "\nMaster (With No Slave) Server:"+ this.bfmContext.getMasterServer().getServerAddress());
                this.isWarningMailSended = Boolean.TRUE;
            }        
            checkLastWalPositions();
    
        }else if(clusterCount == 2 && masterCount == 0 && masterWithNoslaveCount>1){
            log.error("Cluster has more than one master with no slave (cluster size is 2), not healthy but ingoring failover");
            warning();
            PostgresqlServer leaderPg = this.findLeader();
            for(PostgresqlServer pg : this.bfmContext.getPgList()){
                if (pg.getServerAddress() != leaderPg.getServerAddress() && pg.getStatus().equals(DatabaseStatus.MASTER_WITH_NO_SLAVE)){
                    try {
                        if (ex_master_behavior.equals("stop")){
                            this.bfmContext.setSplitBrainMaster(pg);
                            minipgAccessUtil.stopPg(pg);
                            log.info("Ex Master Stoppped...");
                        } else if (ex_master_behavior.equals("rejoin")){
                            log.info("Ex Master Rejoining to Cluster as Slave..");
                            String rewind_result = "??";
                            try {
                                rewind_result = minipgAccessUtil.rewind(pg, leaderPg);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            if (! rewind_result.equals("OK")){
                                log.info("MiniPG rewind was FAILED. rewind_result:" + rewind_result);
                                if (basebackup_slave_join == true){
                                    log.info("Rejoin to cluster Wtih pg_basebackup started..");            
                                    rejoinCluster(leaderPg, pg);
                                    if (this.bfmContext.isMail_notification_enabled() == Boolean.TRUE){
                                        mailService.sendMail("Slave Server "+pg.getServerAddress()+" Out Of CLuster",
                                        "Slave server :"+ pg.getServerAddress()+" has NO MASTER."
                                        + "\nCluster basebackup slave join is "+ basebackup_slave_join+". Slave server rejoin process completed."
                                        + "\nCluster Master Server is:"+ leaderPg.getServerAddress());
                                    }
                                }
                            } 
                        } else {
                            log.warn("Ex Master Behavior not set properly..");
                        }
                        
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (this.bfmContext.isMail_notification_enabled() == Boolean.TRUE){
                        mailService.sendMail(String.format("BFM Cluster in %s Status",String.valueOf(this.bfmContext.getClusterStatus())), 
                            "This is an automatic mail notification."+"\nBFM Cluster Status is:"+this.bfmContext.getClusterStatus()
                            +"\nCluster has more than one MASTER server. Leader Master is :"+leaderPg.getServerAddress()  
                            + "\nServer:"+ pg.getServerAddress()+ " was STOPPED. Please check cluster.");
                    } 
                }
            }
            this.bfmContext.setMasterServer(leaderPg);
            checkLastWalPositions();
        }
        else{
            log.error("Cluster has no master");
            this.bfmContext.setClusterStatus(ClusterStatus.NOT_HEALTHY);
            this.nothealthy();
            if (this.bfmContext.isMail_notification_enabled() == Boolean.TRUE){

                String slaveServerAddresses = "";

                for(PostgresqlServer pg : this.bfmContext.getPgList()){
                    if (pg.getStatus().equals(DatabaseStatus.SLAVE)){
                        if (slaveServerAddresses.length() > 3){
                            slaveServerAddresses = slaveServerAddresses + " - ";    
                        }

                        slaveServerAddresses = slaveServerAddresses + pg.getServerAddress();
                    }
                }

                mailService.sendMail(String.format("BFM Cluster in %s Status",String.valueOf(this.bfmContext.getClusterStatus())), 
                    "This is an automatic mail notification."+"\nBFM Cluster Status is:"+this.bfmContext.getClusterStatus() 
                    + "\nCluster has NO Master Server. Slave Server Adddresses : "+ slaveServerAddresses);
            }        

        }
    }

    public void checkLastWalPositions(){
        try {
            this.bfmContext.getMasterServer().getWalPosition(); 
        } catch (Exception e) {
            log.info("Gel Wal position error.");
            e.printStackTrace();            
        }
        this.bfmContext.setMasterServerLastWalPos(this.bfmContext.getMasterServer().getWalLogPosition());

        // Gson gson = new Gson();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        List<ContextServer> contextServerList = new ArrayList<ContextServer>();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        this.bfmContext.getPgList().stream()
        .forEach(s -> {
            try {
                s.getWalPosition();    
                s.checkTimeLineId();
            } catch (Exception e) {
                e.printStackTrace();
            }
            String formattedDate = s.getLastCheckDateTime().format(dateFormatter);
            ContextServer server = new ContextServer(s.getServerAddress(), s.getDatabaseStatus().toString(), 
            formattedDate, s.getWalLogPosition() ,(s.getReplayLag() == null ? "0" : s.getReplayLag() ), s.getTimeLineId());
            contextServerList.add(server);
        });
        ContextStatus cs = new ContextStatus(this.bfmContext.getClusterStatus().toString(), contextServerList);
        String json_str = gson.toJson(cs);
        PrintWriter out;
        try {
            out = new PrintWriter("./bfm_status.json");
            out.println(json_str);
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
            
        
        // if (this.bfmContext.getMasterServerLastWalPos() != null){
        //     log.info("Master Last Wal Pos(a) :"+ this.bfmContext.getMasterServerLastWalPos());
        // }
    }

    public PostgresqlServer findLeader(){
        this.bfmContext.getPgList().stream().forEach( s -> {
            try{
                s.checkTimeLineId();
                s.getWalPosition();
            }
            catch(Exception e){
                s.setWalLogPosition(null);
                s.setTimeLineId(-1);
            }
        } );

        // PostgresqlServer leader_old = this.bfmContext.getPgList().stream()
        //     .sorted(Comparator.<PostgresqlServer, String>comparing(server-> server.getWalLogPosition(), Comparator.reverseOrder()))
        //     .findFirst().get();

        // log.info("Old Style Leader :"+ leader_old.getServerAddress());

        PostgresqlServer leader = this.bfmContext.getPgList().stream()
            .sorted(Comparator.<PostgresqlServer, Integer>comparing(server -> server.getTimeLineId() , Comparator.reverseOrder())
            .thenComparing(server -> server.getWalLogPosition(), Comparator.reverseOrder()))
            .findFirst().get();

        log.info("leader is "+ leader.getServerAddress());
        return leader;
    }

    public PostgresqlServer findLeaderSlave(){
        this.bfmContext.getPgList().stream()
        .filter(server -> server.getStatus().equals(DatabaseStatus.SLAVE))
        .forEach( s -> {
            try{
                s.getWalPosition();
            }
            catch(Exception e){
                s.setWalLogPosition(null);
            }
        } );

        PostgresqlServer leaderSlave = this.bfmContext.getPgList().stream()
            .filter(server -> server.getStatus().equals(DatabaseStatus.SLAVE))
            .sorted(Comparator.<PostgresqlServer, String>comparing(server-> server.getWalLogPosition(), Comparator.reverseOrder()))
            .findFirst().get();

        return leaderSlave;
    }

    public void healthy(){
        remainingFailCount = timeoutIgnoranceCount;
        isWarningMailSended = Boolean.FALSE;
        bfmContext.setClusterStatus(ClusterStatus.HEALTHY);
        bfmContext.setSplitBrainMaster(null);
        checkReplayLag();
    }

    public void warning(){
        bfmContext.setClusterStatus(ClusterStatus.WARNING);
        checkReplayLag();
    }

    public void nothealthy(){
        remainingFailCount--;
        log.info("remainingFailCount:"+remainingFailCount);
        if(remainingFailCount>0){
            this.leaderSlaveLastWalPos = findLeaderSlave().getWalLogPosition();            
            log.warn("cluster is not healthy");
            if (this.bfmContext.getWatch_strategy() != "manual"){
                try {
                    String result = minipgAccessUtil.startPg(this.bfmContext.getMasterServer());
                    log.info("Master Server start result is "+result);
                } catch (Exception e) {
                    log.info("Error on Master Server start error:");
                    e.printStackTrace();
                }
            }
            log.warn(String.format("remaining ignorance count is: %s",String.valueOf(remainingFailCount)));
        }else{
            if (this.bfmContext.getWatch_strategy() != "manual"){
                String leaderSlaveCurrentWalPos = findLeaderSlave().getWalLogPosition();

                // str1.compareTo (str2); 
                // If str1 is lexicographically less than str2, a negative number will be returned, 
                // 0 if equal or a positive number if str1 is greater.
                if (leaderSlaveCurrentWalPos.compareTo(this.leaderSlaveLastWalPos) > 0){
                    log.info("Leader Slave Last Wal Pos:"+ leaderSlaveLastWalPos+ " Leader Slave Current Wal Pos:"+leaderSlaveCurrentWalPos);
                    log.info("Slave Wal Pos is move forwarding..Possibly BFM cant reach Master Server. Ignoring Failover..");
                } else {
                    String downMasterLastWalPos = this.bfmContext.getMasterServerLastWalPos();
                    long diff_hours = 0;
                    long diff_days = 0;
                    Boolean doFailover = Boolean.TRUE;
                    if ( downMasterLastWalPos == null){                        
                        Gson gson = new GsonBuilder().setPrettyPrinting().create();
                        try {
                            JsonReader reader = new JsonReader(new FileReader("./bfm_status.json"));
                            ContextStatus cs = gson.fromJson(reader, ContextStatus.class); 
                            if (cs != null){
                                String downMasterLastCheck = cs.getClusterServers().stream().filter(s -> s.getDatabaseStatus().equals("MASTER")).findFirst().get().getLastCheck();
                                downMasterLastWalPos = cs.getClusterServers().stream().filter(s -> s.getDatabaseStatus().equals("MASTER")).findFirst().get().getLastWalPos();
                                DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                                LocalDateTime downMasterLastCheckDT  = LocalDateTime.parse(downMasterLastCheck, dateFormatter);
                                diff_hours = java.time.Duration.between(LocalDateTime.now(), downMasterLastCheckDT).toHours(); 
                                diff_days = java.time.Duration.between(LocalDateTime.now(), downMasterLastCheckDT).toDays();
                            } else {
                                log.warn("bfm_status.json is empty. Ignore Failover routine. Please manual respond.");
                                status_file_expire = "99E";
                            }  
                            
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                            log.warn("Cluster has no Master server, bfm_status.json not found..! Master Server Last Wal Position is null..");
                        }
                    
                        if (status_file_expire.contains("H")){
                            if (diff_hours > Integer.parseInt(status_file_expire.replace("H", ""))){
                                doFailover = Boolean.FALSE;
                                log.warn("Master Server Last Wal Position:"+ downMasterLastWalPos +".Ignoring failover Because this data not updated, "+diff_hours+ " Hours) old..");
                                if (this.bfmContext.isMail_notification_enabled() == Boolean.TRUE){
                                    log.warn("Master Server Last Wal Position:"+ downMasterLastWalPos +".Ignoring failover Because this data not updated, " + diff_hours + " Hours) old..");
                                    mailService.sendMail(String.format("BFM Cluster in %s Status",String.valueOf(this.bfmContext.getClusterStatus())), 
                                    "Master Server Last Wal Position:"+ downMasterLastWalPos +".Ignoring failover Because this data not updated, " + diff_hours + " Hours) old..");  
                                }
                            }
                        } else if (status_file_expire.contains("D")){
                            if (diff_days > Integer.parseInt(status_file_expire.replace("D", ""))){
                                doFailover = Boolean.FALSE;
                                log.warn("Master Server Last Wal Position:"+ downMasterLastWalPos +".Ignoring failover Because this data not updated, "+diff_hours+ " Hours) old..");
                                if (this.bfmContext.isMail_notification_enabled() == Boolean.TRUE){
                                    log.warn("Master Server Last Wal Position:"+ downMasterLastWalPos +".Ignoring failover Because this data not updated, " + diff_hours + " Hours) old..");
                                    mailService.sendMail(String.format("BFM Cluster in %s Status",String.valueOf(this.bfmContext.getClusterStatus())), 
                                    "Master Server Last Wal Position:"+ downMasterLastWalPos +".Ignoring failover Because this data not updated, " + diff_hours + " Hours) old..");  
                                }
                            }
                        } else {
                            doFailover = Boolean.FALSE;
                            if (status_file_expire != "99E"){
                                log.warn("status-file-expire parameter error:"+status_file_expire);
                            }                            
                        }

                    } 

                    //pg_wal_lsn_diff(b->master_lsn,b->slave_lsn) between 1 and 'X' bytes
                    Double data_loss_size = findLeaderSlave().getDataLossSize(downMasterLastWalPos);
                    if ((data_loss_size < getDoubleFromString(data_loss_tolerance)) && doFailover == Boolean.TRUE){
                        failover();
                    } else {
                        log.warn("Data Loss Tolerance is:"+ getDoubleFromString(data_loss_tolerance)+ " doFailover flag is:"+ doFailover 
                        + ".Data loss size calculated as " + Double.toString(data_loss_size) + " between Leader Slave Wal Position and Master Last Wal Position."
                        + "Failover ignored.. Please manual respond to failure.. ");
                        if (this.bfmContext.isMail_notification_enabled() == Boolean.TRUE){
                            mailService.sendMail(String.format("BFM Cluster in %s Status",String.valueOf(this.bfmContext.getClusterStatus())), 
                            "Data Loss Tolerance is:"+ getDoubleFromString(data_loss_tolerance) + " doFailover flag is:"+ doFailover 
                            + ".Data loss size calculated as " + Double.toString(data_loss_size) + " between Leader Slave Wal Position and Master Last Wal Position."
                            + "Failover ignored.. Please manual respond to failure.. ");                                    
                        }
                    }                 
                }    

            } else {
                this.bfmContext.setClusterStatus(ClusterStatus.NOT_HEALTHY);
                if (this.bfmContext.isMail_notification_enabled() == Boolean.TRUE){
                    mailService.sendMail(String.format("BFM Cluster in %s Status",String.valueOf(this.bfmContext.getClusterStatus())), 
                    "This is an automatic mail notification."+"\nBFM Cluster Status is:"+this.bfmContext.getClusterStatus() 
                    + "\nAutomatic Failover is OFF. Please manual respond to failure...Master Server : " + this.bfmContext.getMasterServer().getServerAddress());    
                }
            }
            
        }

    }


    public void failover(){
        log.error("Failover Started");
        this.bfmContext.setClusterStatus(ClusterStatus.FAILOVER);
        try {
            PostgresqlServer newMaster = selectNewMaster();
            log.info(String.format("New master server is %s",newMaster.getServerAddress()));
            bfmContext.getPgList().stream().
                    filter(server -> !server.getServerAddress().equals(newMaster.getServerAddress()))
                    .forEach(
                    server -> {
                        try {
                            minipgAccessUtil.vipDown(server);
                        } catch (Exception e) {
                           log.error(String.format("Unable to down vip in server %s",server.getServerAddress()));
                        }
                    }
            );
            minipgAccessUtil.promote(newMaster);
            minipgAccessUtil.vipUp(newMaster);
            bfmContext.getPgList().stream().
                    filter(server -> !server.getServerAddress().equals(newMaster.getServerAddress()))
                    .forEach(
                            server -> {
                                try {
                                    minipgAccessUtil.rewind(server,newMaster);
                                } catch (Exception e) {
                                    log.error(String.format("Unable to rewind server : %s",server.getServerAddress()));
                                }
                            }
                    );
        }catch (Exception e){
            e.printStackTrace();
            log.error(e.getMessage());
            log.error("************Failover failed***************");
            this.bfmContext.setClusterStatus(ClusterStatus.NOT_HEALTHY);
        }
        log.error("Failover Finished");
        this.bfmContext.setClusterStatus(ClusterStatus.HEALTHY);
        remainingFailCount = timeoutIgnoranceCount;
    }

    public PostgresqlServer selectNewMaster() {
        if(this.bfmContext.getPgList().size() == 2 &&
        this.bfmContext.getPgList().stream().filter(server -> server.getPriority()!=0).filter(server -> server.getStatus() == DatabaseStatus.SLAVE).count() == 0){
            return this.bfmContext.getPgList().stream().filter(server -> server.getPriority()!=0)
                    .filter(server -> server.getStatus() == DatabaseStatus.MASTER_WITH_NO_SLAVE)
                    .sorted(Comparator.comparingInt(PostgresqlServer::getPriority).reversed())
                    .findFirst().get();
        }

        if(this.bfmContext.getPgList().size() == 2 &&
        this.bfmContext.getPgList().stream().filter(server -> server.getPriority()!=0).filter(server -> server.getStatus() == DatabaseStatus.MASTER_WITH_NO_SLAVE).count() > 1){
            
            return this.findLeader();

        }


        return this.bfmContext.getPgList().stream().filter(server -> server.getPriority()!=0)
                .filter(server -> server.getStatus() == DatabaseStatus.SLAVE)
                .sorted(Comparator.comparingInt(PostgresqlServer::getPriority).reversed())
                .findFirst().get();
        
    }

    public String rejoinCluster(PostgresqlServer masterServer, PostgresqlServer targetSlave){
        log.info("Joining to Cluster with pg_basebackup to MASTER:",masterServer);
        //try to rejoin with pg_basebackup method 
        String rebase_result;
        try {
            rebase_result = minipgAccessUtil.rebaseUp(targetSlave, masterServer);
            return rebase_result;    
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void checkSlaves(){
            this.bfmContext.getPgList().stream()
            .filter(server -> server.getStatus() == DatabaseStatus.SLAVE)
            .forEach(server ->
                    {
                        // log.info("server: "+ server.getServerAddress()+" status :"+ server.getDatabaseStatus()+ " hasMaster:"+ server.getHasMasterServer());
                        remainingFailCount--;
                        if (server.getHasMasterServer() == Boolean.FALSE){

                            log.info("Slave Server "+ server.getServerAddress()+" has NO MASTER. Replication could be down...");
                            if (remainingFailCount <= 0){

                                if (this.bfmContext.getWatch_strategy() == "manual"){
                                    log.info("BFM is in"+ this.bfmContext.getWatch_strategy()+". Please manual respond to incident..");
                                    if (this.bfmContext.isMail_notification_enabled() == Boolean.TRUE){
                                        mailService.sendMail("Slave Server "+server.getServerAddress()+" Out Of CLuster",
                                        "Slave server :"+ server.getServerAddress()+" has NO MASTER. Replication could be down..."
                                        + "BFM is in"+ this.bfmContext.getWatch_strategy()+". Please manual respond to incident..");
                                    }
                                } else {
                                    PostgresqlServer master = this.bfmContext.getPgList().stream()
                                    .filter(s -> s.getStatus() == DatabaseStatus.MASTER_WITH_NO_SLAVE || s.getStatus() == DatabaseStatus.MASTER ).findFirst().get();

                                    String rewind_result = "??";
                                    try {
                                        rewind_result = minipgAccessUtil.rewind(server, master);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                    if (! rewind_result.equals("OK")){
                                        log.info("MiniPG rewind was FAILED. rewind_result:"+rewind_result);
                                        if (basebackup_slave_join == true){
                                            log.info("Rejoin to cluster Wtih pg_basebackup started..");            
                                            String rejoin_result = rejoinCluster(master, server);
                                            log.info("Slave server :"+server.getServerAddress()+" reJoin result is:"+rejoin_result);
                                            if (this.bfmContext.isMail_notification_enabled() == Boolean.TRUE){
        
                                                mailService.sendMail("Slave Server "+server.getServerAddress()+" Out Of CLuster",
                                                "Slave server :"+ server.getServerAddress()+" has NO MASTER."
                                                + "\nCluster basebackup slave join is "+ basebackup_slave_join+". Slave server rejoin process completed."
                                                + "\nCluster Master Server is:"+ master.getServerAddress());
                                            }
            
                                        }
                                    }                                   
        
                                }

                            } else {
                                log.info("remainingFailCount:"+remainingFailCount+ ". Ignoring slave operations...");
                            }
                             
                        }
                        
                    });         
    }
    public void checkReplayLag(){
        PostgresqlServer masterServer = this.bfmContext.getMasterServer();
        Map<String,String> replayLagMap = masterServer.getReplayLagMap();
        this.bfmContext.getPgList().stream()
        .filter(s -> s.getDatabaseStatus().equals(DatabaseStatus.SLAVE))
        .forEach(slave -> {
            slave.setReplayLag(replayLagMap.get(slave.getServerAddress().split(":")[0]));
        }); 
    }
}
