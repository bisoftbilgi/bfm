package com.bisoft.bfm.scheduler;

import com.bisoft.bfm.dto.DatabaseStatus;
import com.bisoft.bfm.exceptions.NoSuitableMasterFoundException;
import com.bisoft.bfm.helper.BfmAccessUtil;
import com.bisoft.bfm.helper.MinipgAccessUtil;
import com.bisoft.bfm.model.BfmContext;
import com.bisoft.bfm.dto.ClusterStatus;
import com.bisoft.bfm.model.PostgresqlServer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.ProxySelector;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Comparator;

@Component
@Slf4j
@RequiredArgsConstructor
public class ClusterCheckScheduler {

    private final BfmContext bfmContext;
    private  final MinipgAccessUtil minipgAccessUtil;
    private final BfmAccessUtil bfmAccessUtil;

    private String pairStatus ="Active";

    @Value("${watcher.cluster-pair:no-pair}")
    private String bfmPair;

    @Value("${app.timeout-ignorance-count:1}")
    int timeoutIgnoranceCount;

    @Value("${server.pguser:postgres}")
    String pgUsername;

    @Value("${server.pgpassword:postgres}")
    String pgPassword;

    int remainingFailCount;

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

        if(this.bfmContext.isMasterBfm()) {
            try {

                long numberOfAliveServers = this.bfmContext.getPgList().stream()
                        .filter(server -> server.getStatus() != DatabaseStatus.INACCESSIBLE)
                        .count();

                PostgresqlServer master =null ;
                //If the number of alive servers is 1
                // this server should have the status MASTER_WITH_NO_SLAVE
                if(numberOfAliveServers==1){
                     master = this.bfmContext.getPgList().stream()
                            .filter(server -> server.getStatus() == DatabaseStatus.MASTER_WITH_NO_SLAVE || server.getStatus() == DatabaseStatus.MASTER ).findFirst().get();
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
                        .filter(server -> server.getStatus() == DatabaseStatus.INACCESSIBLE)
                        .forEach(server ->
                                {
                                    try {
                                        if (selectedMaster != null){
                                            minipgAccessUtil.rewind(server, selectedMaster);
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

    @Scheduled(fixedDelay = 5000)
    public void checkCluster(){
        if(pairStatus.equals("no-pair") || pairStatus.equals("Passive") || pairStatus.equals("Unreachable")){
            log.info("this is the active bfm pair");
            this.bfmContext.setMasterBfm(true);
        }else{
            log.info(String.format("Bfm pair is active in %s",bfmPair));
            this.bfmContext.setMasterBfm(false);
            return;
        }
        log.info("-----Cluster Healthcheck Started-----");
        

        bfmContext.getPgList().stream().forEach(server -> {
            try {
                checkServer(server);
            }catch(Exception e){
                log.error(String.format("Unable to connect to server : %s",server.getServerAddress()));
            }
        });

        if (bfmContext.getMasterServer() == null){
            PostgresqlServer master = this.selectNewMaster();
            bfmContext.setMasterServer(master);;
        }

        isClusterHealthy();


        log.info(String.format("-----Cluster Status is %s -----",this.bfmContext.getClusterStatus()));

        log.info("-----Cluster Healthcheck Finished-----");
    }

    public void checkServer(PostgresqlServer postgresqlServer) throws Exception {
        DatabaseStatus status = postgresqlServer.getDatabaseStatus();

        if (status.equals(DatabaseStatus.MASTER)){
            this.bfmContext.setMasterServer(postgresqlServer);
        }
        
        if (status.equals(DatabaseStatus.MASTER_WITH_NO_SLAVE) && this.bfmContext.getMasterServer() != null  && this.bfmContext.getMasterServer().getServerAddress().equals(postgresqlServer.getServerAddress())){
            status = DatabaseStatus.MASTER;
            postgresqlServer.setDatabaseStatus(status);
        }
        log.info(String.format("Status of %s is %s",postgresqlServer.getServerAddress(),status));
        log.info(minipgAccessUtil.status(postgresqlServer));
    }

    public void isClusterHealthy(){
        // If cluster is failing over do not check health 
        if(this.bfmContext.getClusterStatus() == ClusterStatus.FAILOVER){
            return;
        }

        // 1. If a cluster has only one master without any slave connected and all the other nodes are unreachable, cluster is in warning status

        // 1.1 Start the slaves

        // 1.2 Connect slaves to the master node


        // 2. If a cluster has more than one master cluster is unhealthy

        // 2.1 Check lsn positions

        // 2.2 Choose master node with higher lsn position 

        // 2.3 Shutdown other nodes  

        // 2.4 Inform the DBA



        // 3. If a cluster has a only one slave node and other nodes are unreachable, cluster is unhealthy

        // 3.1 Check last known master lsn and the current slave lsn (a)

        // 3.2 Wait 5 seconds

        // 3.3 Check last known master lsn and the current slave lsn (b)

        // 3.4 if a->master_lsn ==  b->master_lsn and a->slave_lsn == b->slave_lsn 
        // and pg_wal_lsn_diff(b->master_lsn,b->slave_lsn) between 1 and 'X' bytes then promote slave

        // 3.5 if a->master_lsn <  b->master_lsn recheck cluster 

        // 3.6 if a->master_lsn ==  b->master_lsn and a->slave_lsn < b->slave_lsn  do recheck, inform dba

        // psql -c "select t.*,pg_current_wal_lsn() from pg_ls_waldir() t order by modification desc limit 1"





        // 4. If a cluster has only slave nodes and there is no master node, clsuter is unhealthy

        // 5. If a cluster has more than one or more master without slaves connected and has other nodes with slave status not connected to master nodes, cluster is unhealthy

        // 6. If a cluster has only one master and other servers are slave to that master, cluster is healthy

        long clusterCount = this.bfmContext.getPgList().size();

        long masterCount = this.bfmContext.getPgList().stream().filter(server -> server.getStatus().equals(DatabaseStatus.MASTER)).count();

        long masterWithNoslaveCount = this.bfmContext.getPgList().stream().filter(server -> server.getStatus().equals(DatabaseStatus.MASTER_WITH_NO_SLAVE)).count();

        if(masterCount ==  1L){
            log.info("Cluster has a master node");
            this.bfmContext.setClusterStatus(ClusterStatus.HEALTHY);
            healthy();
        }else if(clusterCount == 2 && masterCount == 0 && masterWithNoslaveCount==1){
            log.warn("Cluster has a master with no slave (cluster size is 2), not healthy but ingoring failover");
            healthy();
        }else if(clusterCount == 2 && masterCount == 0 && masterWithNoslaveCount>1){
            log.error("Cluster has more than one master with no slave (cluster size is 2), not healthy but ingoring failover");
            healthy();
            PostgresqlServer leaderPg = this.findLeader();
            for(PostgresqlServer pg : this.bfmContext.getPgList()){
                if (pg.getServerAddress().equals(leaderPg.getServerAddress()) && pg.getStatus().equals(DatabaseStatus.MASTER_WITH_NO_SLAVE)){
                    pg.setDatabaseStatus(DatabaseStatus.MASTER);
                    this.nothealthy();
                }
            }
        }
        else{
            log.error("Cluster has no master");
            this.bfmContext.setClusterStatus(ClusterStatus.NOT_HEALTHY);
            this.nothealthy();
        }
    }

    public PostgresqlServer findLeader(){
        this.bfmContext.getPgList().stream().forEach( s -> {
            try{
                s.getWalPosition();
            }
            catch(Exception e){
                s.setWalLogPosition(null);
            }
        } );

        PostgresqlServer leader = this.bfmContext.getPgList().stream()
            .sorted(Comparator.<PostgresqlServer, String>comparing(server-> server.getWalLogPosition(), Comparator.reverseOrder()))
            .findFirst().get();

        log.info("leader is "+ leader.getServerAddress());
        return leader;
    }

    public void healthy(){
        remainingFailCount = timeoutIgnoranceCount;
        bfmContext.setClusterStatus(ClusterStatus.HEALTHY);
    }

    public void nothealthy(){
        remainingFailCount--;
        if(remainingFailCount>0){
            log.warn("master server is not healthy");
            log.warn(String.format("remaining ignorance count is: %s",String.valueOf(remainingFailCount)));
        }else{
            failover();
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
}
