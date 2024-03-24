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
    public void checkUnavilable(){
        if(this.bfmContext.isMasterBfm()) {
            try {

                long numberOfAliveServers = this.bfmContext.getPgList().stream()
                        .filter(server -> server.getStatus() != DatabaseStatus.INACCESSIBLE)
                        .count();

                PostgresqlServer master ;
                //If the number of alive servers is 1
                // this server should have the status MASTER_WITH_NO_SLAVE
                if(numberOfAliveServers==1){
                     master = this.bfmContext.getPgList().stream()
                            .filter(server -> server.getStatus() == DatabaseStatus.MASTER_WITH_NO_SLAVE).findFirst().get();
                }else{
                    // Else there should be a master server
                    master = this.bfmContext.getPgList().stream()
                            .filter(server -> server.getStatus() == DatabaseStatus.MASTER).findFirst().get();
                }

                this.bfmContext.getPgList().stream()
                        .filter(server -> server.getStatus() == DatabaseStatus.INACCESSIBLE)
                        .forEach(server ->
                                {
                                    try {
                                        minipgAccessUtil.rewind(server, master);
                                    } catch (Exception e) {
                                        log.error(String.format("Unable to rewind %s", server.getServerAddress()));
                                    }
                                }
                        );
            } catch (Exception ex) {
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

        isClusterHealthy();


        log.info(String.format("-----Cluster Status is %s -----",this.bfmContext.getClusterStatus()));

        log.info("-----Cluster Healthcheck Finished-----");
    }

    public void checkServer(PostgresqlServer postgresqlServer) throws Exception {
        DatabaseStatus status = postgresqlServer.getDatabaseStatus();
        log.info(String.format("Status of %s is %s",postgresqlServer.getServerAddress(),status));
        log.info(minipgAccessUtil.status(postgresqlServer));
    }

    public void isClusterHealthy(){
        if(this.bfmContext.getClusterStatus() == ClusterStatus.FAILOVER){
            return;
        }

        long clusterCount = this.bfmContext.getPgList().size();

        long masterCount = this.bfmContext.getPgList().stream().filter(server -> server.getStatus().equals(DatabaseStatus.MASTER)).count();

        long masterWithNoslaveCount = this.bfmContext.getPgList().stream().filter(server -> server.getStatus().equals(DatabaseStatus.MASTER_WITH_NO_SLAVE)).count();

        if(masterCount ==  1L){
            log.info("Cluster has a master node");
            this.bfmContext.setClusterStatus(ClusterStatus.HEALTHY);
            healthy();
        }else if(clusterCount == 2 && masterCount == 0 && masterWithNoslaveCount==1){
            log.error("Cluster has a master with no slave (cluster size is 2), not healthy but ingoring failover");
            healthy();
        }
        else{
            log.error("Cluster has no master");
            this.bfmContext.setClusterStatus(ClusterStatus.NOT_HEALTHY);
            this.nothealthy();
        }
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
                this.bfmContext.getPgList().stream().filter(server -> server.getPriority()!=0).filter(server -> server.getStatus() == DatabaseStatus.SLAVE).count() == 1 &&
                this.bfmContext.getPgList().stream().filter(server -> server.getPriority()!=0).filter(server -> server.getStatus() == DatabaseStatus.MASTER_WITH_NO_SLAVE).count() == 1 ){
            return this.bfmContext.getPgList().stream().filter(server -> server.getPriority()!=0)
                    .filter(server -> server.getStatus() == DatabaseStatus.MASTER_WITH_NO_SLAVE)
                    .sorted(Comparator.comparingInt(PostgresqlServer::getPriority).reversed())
                    .findFirst().get();
        }

        return this.bfmContext.getPgList().stream().filter(server -> server.getPriority()!=0)
                .filter(server -> server.getStatus() == DatabaseStatus.SLAVE)
                .sorted(Comparator.comparingInt(PostgresqlServer::getPriority).reversed())
                .findFirst().get();
    }
}
