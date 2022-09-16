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
                log.info("status of bfm pair is :"+pairStatus);
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
                PostgresqlServer master = this.bfmContext.getPgList().stream()
                        .filter(server -> server.getStatus() == DatabaseStatus.MASTER).findFirst().get();

                this.bfmContext.getPgList().stream()
                        .filter(server -> server.getStatus() == DatabaseStatus.INACCESSIBLE)
                        .forEach(server ->
                                {
                                    try {
                                        minipgAccessUtil.rewind(server, master);
                                    } catch (Exception e) {
                                        log.error("Unable to rewind " + server.getServerAddress());
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
            log.info("Bfm pair is active in "+bfmPair);
            this.bfmContext.setMasterBfm(false);
            return;
        }
        log.info("-----Cluster Healthcheck Started-----");

        bfmContext.getPgList().stream().forEach(server -> {
            try {
                checkServer(server);
            }catch(Exception e){
                log.error("Unable to connect to server : "+server.getServerAddress());
            }
        });

        isClusterHealthy();


        log.info("-----Cluster Status is "+this.bfmContext.getClusterStatus()+"-----");

        log.info("-----Cluster Healthcheck Finished-----");
    }

    public void checkServer(PostgresqlServer postgresqlServer) throws Exception {
        DatabaseStatus status = postgresqlServer.getDatabaseStatus();
        log.info("Status of "+postgresqlServer.getServerAddress()+" is "+status);
        log.info(minipgAccessUtil.status(postgresqlServer));
    }

    public void isClusterHealthy(){
        if(this.bfmContext.getClusterStatus() == ClusterStatus.FAILOVER){
            return;
        }
        long masterCount = this.bfmContext.getPgList().stream().filter(server -> server.getStatus().equals(DatabaseStatus.MASTER)).count();
        if(masterCount ==  1L){
            log.info("Cluster has a master node");
            this.bfmContext.setClusterStatus(ClusterStatus.HEALTHY);
            healthy();
        }else{
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
            log.warn("remaining ignorance count is: "+String.valueOf(remainingFailCount));
        }else{
            failover();
        }
    }


    public void failover(){
        log.error("Failover Started");
        this.bfmContext.setClusterStatus(ClusterStatus.FAILOVER);
        try {
            PostgresqlServer newMaster = selectNewMaster();
            log.info("New master server is "+newMaster.getServerAddress());
            bfmContext.getPgList().stream().
                    filter(server -> !server.getServerAddress().equals(newMaster.getServerAddress()))
                    .forEach(
                    server -> {
                        try {
                            minipgAccessUtil.vipDown(server);
                        } catch (Exception e) {
                           log.error("Unable to down vip in server "+server.getServerAddress());
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
                                    log.error("Unable to rewind server : "+server.getServerAddress());
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
        return this.bfmContext.getPgList().stream().filter(server -> server.getPriority()!=0).filter(server -> server.getStatus() == DatabaseStatus.SLAVE).findFirst().get();
    }
}
