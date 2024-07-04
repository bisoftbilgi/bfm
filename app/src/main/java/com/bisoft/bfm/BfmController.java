package com.bisoft.bfm;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Controller;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.bisoft.bfm.dto.ClusterStatus;
import com.bisoft.bfm.dto.DatabaseStatus;
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

@Slf4j
@Controller
@RequestMapping("/bfm")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class BfmController {

    private final BfmContext bfmContext;
    private  final MinipgAccessUtil minipgAccessUtil;

    @Value("${watcher.cluster-pair:no-pair}")
    private String bfmPair;

    @Value("${server.pguser:postgres}")
    private String username;

    @Value("${server.pgpassword:postgres}")
    private String password;

    @RequestMapping(path = "/status",method = RequestMethod.GET)
    public @ResponseBody
    List<PostgresqlServer> status(){
        return bfmContext.getPgList();
    }

    @RequestMapping(path = "/is-alive",method = RequestMethod.GET)
    public @ResponseBody String isAlive(){
        if(bfmContext.isMasterBfm() == true){
            return "Active";
        }
        return "Passive";
    }

    @RequestMapping(path = "/last-saved-stat",method = RequestMethod.GET)
    public @ResponseBody String lastSavedStat(){
        String retval="";
        if(bfmContext.isMasterBfm() == true){            
            try {
            File myObj = new File("./bfm_status.json");
            Scanner myReader = new Scanner(myObj);
            while (myReader.hasNextLine()) {
                String data = myReader.nextLine();
                retval += data;
            }
            myReader.close();
            } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
            }
            return retval;
        } else {
            return "";
        }
    }

    @RequestMapping(path = "/get-active-bfm",method = RequestMethod.GET)
    public @ResponseBody String getActiveBfm(){
        if(bfmContext.isMasterBfm() == true){
            ArrayList<String> serverIPAddress = new ArrayList<String>();

            try {
                Enumeration<NetworkInterface> b = NetworkInterface.getNetworkInterfaces();
                while( b.hasMoreElements()){
                    for ( InterfaceAddress f : b.nextElement().getInterfaceAddresses())
                        if ( f.getAddress().toString().contains(".") && f.getAddress().toString() !="127.0.0.1")
                        serverIPAddress.add(f.getAddress().toString().replace("/",""));
                }
            } catch (SocketException e) {
                e.printStackTrace();
            }

            String bfmIpStr = this.bfmContext.getPgList().stream().filter(s -> (serverIPAddress.contains(s.getServerAddress().split(":")[0]))).findFirst().get().getServerAddress();
            return (bfmIpStr.replace("5432","9994"));
        }
        return bfmPair;
    }

    @RequestMapping(path = "/check-pause",method = RequestMethod.GET)
    public @ResponseBody String clusterCheckPause(){
        this.bfmContext.setCheckPaused(Boolean.TRUE);
        return "Cluster check Paused.\n";
    }

    @RequestMapping(path = "/check-resume",method = RequestMethod.GET)
    public @ResponseBody String clusterCheckResume(){
        this.bfmContext.setCheckPaused(Boolean.FALSE);
        return "Cluster check started.\n";
    }

    @RequestMapping(path = "/mail-pause",method = RequestMethod.GET)
    public @ResponseBody String clusterMailPause(){
        this.bfmContext.setMail_notification_enabled(Boolean.FALSE);
        return "Mail Notifications Paused.\n";
    }

    @RequestMapping(path = "/mail-resume",method = RequestMethod.GET)
    public @ResponseBody String clusterMailResume(){
        this.bfmContext.setMail_notification_enabled(Boolean.TRUE);
        return "Mail Notifications started.\n";
    }

    @RequestMapping(path = "/cluster-status",method = RequestMethod.GET)
    public @ResponseBody String clusterStatus(){
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String retval = "";

        if (this.bfmContext.isMasterBfm() == Boolean.TRUE){
           
            retval = retval + "\nCluster Status : "+this.bfmContext.getClusterStatus();
            retval = retval + "\nWatch Strategy : "+this.bfmContext.getWatch_strategy();
            retval = retval + "\n\n"+ 
                                String.format("%-25s", "Server Address :") + 
                                "\t" + 
                                String.format("%-10s", "Status :") + 
                                "\t" + 
                                String.format("%-20s", "Last Wal Position :") +
                                "\t" + 
                                String.format("%-12s", "Replay Lag :") +
                                "\t" + 
                                String.format("%-10s", "Timeline :") +
                                "\t" + 
                                String.format("%-20s", "Last Check Time :");
            retval = retval + "\n"+ 
                                "_".repeat(25) + 
                                "\t" + 
                                "_".repeat(10) + 
                                "\t" + 
                                "_".repeat(20) +
                                "\t" + 
                                "_".repeat(12) +
                                "\t" + 
                                "_".repeat(10) +
                                "\t" + 
                                "_".repeat(20);
            for(PostgresqlServer pg : this.bfmContext.getPgList()){
                try {
                    pg.getWalPosition();
                    pg.checkTimeLineId();   
                } catch (Exception e) {
                    e.printStackTrace();
                }
                String formattedDate = pg.getLastCheckDateTime().format(dateFormatter);     
                String timeLineStr = Integer.toString(pg.getTimeLineId());
                retval = retval +"\n" + 
                                String.format("%-25s", pg.getServerAddress())  +
                                "\t" + 
                                String.format("%-10s", pg.getDatabaseStatus()) +
                                "\t" + 
                                String.format("%20s", pg.getWalLogPosition()) +
                                "\t" + 
                                String.format("%12s", (pg.getReplayLag() == null ? "" : pg.getReplayLag())) +
                                "\t" + 
                                String.format("%10s", timeLineStr)+
                                "\t" + 
                                String.format("%-20s", formattedDate);
            }
            retval = retval+ "\n\nLast Check Log: \n"+ this.bfmContext.getLastCheckLog() +"\n";
        } else {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            try {
                JsonReader reader = new JsonReader(new FileReader("./bfm_status.json"));
                ContextStatus cs = gson.fromJson(reader, ContextStatus.class);

                retval = retval + "\nCluster Status : "+cs.getClusterStatus();
                retval = retval + "\n"+ 
                                    String.format("%-25s", "Server Address :") + 
                                    "\t" + 
                                    String.format("%-10s", "Status :") + 
                                    "\t" + 
                                    String.format("%-20s", "Last Wal Position :") +
                                    "\t" + 
                                    String.format("%-12s", "Replay Lag :") +
                                    "\t" + 
                                    String.format("%-10s", "Timeline :") +
                                    "\t" + 
                                    String.format("%-20s", "Last Check Time :");
                retval = retval + "\n"+ 
                                "_".repeat(25) + 
                                "\t" + 
                                "_".repeat(10) + 
                                "\t" + 
                                "_".repeat(20) +
                                "\t" + 
                                "_".repeat(12) +
                                "\t" + 
                                "_".repeat(10) +
                                "\t" + 
                                "_".repeat(20);

                for (ContextServer pg : cs.getClusterServers()){
                    String timeLineStr = Integer.toString(pg.getTimeline());
                    retval = retval +"\n" + 
                                    String.format("%-25s", pg.getAddress()) + 
                                    "\t" + 
                                    String.format("%-10s", pg.getDatabaseStatus()) +
                                    "\t" + 
                                    String.format("%20s", pg.getLastWalPos()) +
                                    "\t" + 
                                    String.format("%12s", (pg.getReplayLag() == null ? "" : pg.getReplayLag())) +
                                    "\t" + 
                                    String.format("%10s", timeLineStr)+
                                    "\t" + 
                                    String.format("%-20s", pg.getLastCheck());
                }
                retval = retval + "\n\n";
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        return retval;
    }

    @RequestMapping(path = "/watch-strategy/{strategy}",method = RequestMethod.POST)
    public @ResponseBody String setWatchStrategy(@PathVariable(value = "strategy") String new_strategy){
        String retval = "";       
        if (this.bfmContext.isMasterBfm() == Boolean.TRUE){
            // availability,manual,performance,protection
            // retval = retval + new_strategy;
            if (new_strategy.equals("M")){
                this.bfmContext.setWatch_strategy("manual");
                retval = retval + "Watch strategy set to -manual-\n";
            } else if (new_strategy.equals("A")){
                this.bfmContext.setWatch_strategy("availability");
                retval = retval + "Watch strategy set to -availability-\n";
            } else {
                retval = retval + "Watch strategy set FAIL. invalid Parameter:"+new_strategy +"\n";
            }
        } else {
            retval = retval + "Please run on Active BFM pair...\n";
        }

        return retval;
    }    

    @RequestMapping(path = "/switchover/{target}",method = RequestMethod.POST)
    public @ResponseBody String switchOver(@PathVariable(value = "target") String targetPG){
        String retval ="";       
        if (this.bfmContext.isMasterBfm() == Boolean.TRUE){
            try{

                if (targetPG.split(":")[0] == null 
                    || targetPG.split(":")[1] == null 
                    || (targetPG.split(":")[0]).length() < 7 
                    || (targetPG.split(":")[1]).length() < 1){
                    retval = retval + "Please specify target server and port (e.g. 192.168.1.7:5432)"+ targetPG + "\n";
                } else {
                    try {
                        PostgresqlServer switchOverToPG = this.bfmContext.getPgList().stream()
                        .filter(s -> (s.getServerAddress().equals(targetPG) 
                                        && s.getDatabaseStatus().equals(DatabaseStatus.SLAVE))).findFirst().get();

                        if (switchOverToPG == null){
                            retval = retval + targetPG+ " Server not found in BFM Cluster or Its not SLAVE.\n";
                        } else {
                            if (switchOverToPG.getReplayLag().equals("0")){
                                this.bfmContext.setCheckPaused(Boolean.TRUE);
                                String ws = this.bfmContext.getWatch_strategy();
                                Boolean mail_notify = this.bfmContext.isMail_notification_enabled();
                                
                                this.bfmContext.setWatch_strategy("manual");
                                this.bfmContext.setMail_notification_enabled(Boolean.FALSE);
    
                                PostgresqlServer old_master = this.bfmContext.getMasterServer();
                                String result ="";
                                
                                result = minipgAccessUtil.prepareForSwitchOver(old_master);
                                log.info("Prepare for SwitchOver result :" + result);

                                minipgAccessUtil.vipDown(old_master);

                                result = minipgAccessUtil.promote(switchOverToPG);
                                log.info("Slave Promote Result :"+ result);

                                minipgAccessUtil.vipUp(switchOverToPG);
                                result = minipgAccessUtil.postSwitchOver(old_master, switchOverToPG);
                                log.info("Ex-Master Rejoin Result :"+ result);

                                this.bfmContext.setCheckPaused(Boolean.FALSE);
                                int checkCount = 3;
                                while (((this.bfmContext.getClusterStatus() != ClusterStatus.HEALTHY) || 
                                                                    (this.bfmContext.getPgList().stream()
                                                                    .filter(s -> (s.getDatabaseStatus().equals(DatabaseStatus.INACCESSIBLE))).count()) > 0) 
                                        && (checkCount > 0)){
                                    TimeUnit.SECONDS.sleep(5);
                                    checkCount--;
                                }                                
                                
                                this.bfmContext.setWatch_strategy(ws);
                                this.bfmContext.setMail_notification_enabled(mail_notify);
                                retval = retval +"Switch Over Completed Succesfully :\n";
                                
    
                            } else {
                                retval = retval + " Replay Lag is Not Zero(0) for selected Slave :"+ targetPG+"\n";
                            }
                        }
                    } catch (Exception e) {
                        retval = retval + targetPG+ " Server not found in BFM Cluster or Its not SLAVE.\n";
                    }
                    
                }
            } catch (Exception e) {
                retval = retval + e.getMessage()+"\n-->"+targetPG+"\n";
            }            
        } else {
            retval = retval + "Please run on Active BFM pair...\n";
        }

        return retval;
    } 
    
    @RequestMapping(path = "/reinit/{target}",method = RequestMethod.POST)
    public @ResponseBody String reInit(@PathVariable(value = "target") String targetPG){
        String retval ="";       
        if (this.bfmContext.isMasterBfm() == Boolean.TRUE){

            if (targetPG.split(":")[0] == null || targetPG.split(":")[1] == null 
                        || (targetPG.split(":")[0]).length() < 7 || (targetPG.split(":")[1]).length() < 1){

                    retval = retval + "Please specify target server and port (e.g. 192.168.1.7:5432)"+ targetPG + "\n";

                } else {
                    try {
                        PostgresqlServer target_server = this.bfmContext.getPgList().stream()
                        .filter(s -> (s.getServerAddress().equals(targetPG) 
                                        && (s.getDatabaseStatus().equals(DatabaseStatus.SLAVE) || s.getDatabaseStatus().equals(DatabaseStatus.INACCESSIBLE)))).findFirst().get();

                        if (target_server == null){
                            retval = retval + targetPG+ " Server not found in BFM Cluster or Its not SLAVE.\n";
                        } else {
                            try {
                                this.bfmContext.setCheckPaused(Boolean.TRUE);
                                String ws = this.bfmContext.getWatch_strategy();
                                Boolean mail_notify = this.bfmContext.isMail_notification_enabled();
                                
                                this.bfmContext.setWatch_strategy("manual");
                                this.bfmContext.setMail_notification_enabled(Boolean.FALSE);

                                PostgresqlServer master_server = this.bfmContext.getPgList().stream()
                                .filter(server -> server.getStatus() == DatabaseStatus.MASTER_WITH_NO_SLAVE || server.getStatus() == DatabaseStatus.MASTER ).findFirst().get();                    

                                target_server.setRewindStarted(Boolean.TRUE);
                                String rewind_result = minipgAccessUtil.rewind(target_server, master_server);
                                if (! rewind_result.equals("OK")){
                                    log.info("pg_rewind was FAILED. starting rejoin with pg_basebackup. Target:",target_server);
                                        String rejoin_result = minipgAccessUtil.rebaseUp(target_server, master_server);
                                        log.info("pg_basebackup join cluster result is:"+rejoin_result);
                                }
                                target_server.setRewindStarted(Boolean.FALSE);

                                this.bfmContext.setCheckPaused(Boolean.FALSE);
                                int checkCount = 3;
                                while ((this.bfmContext.getClusterStatus() != ClusterStatus.HEALTHY) && (checkCount > 0)){
                                    TimeUnit.SECONDS.sleep(5);
                                    checkCount--;
                                }   
                                this.bfmContext.setWatch_strategy(ws);
                                this.bfmContext.setMail_notification_enabled(mail_notify);
                                retval = retval +targetPG+" re-initialize Completed Succesfully :\n";

                            } catch (Exception e) {
                                log.warn("No Master server found in cluster...");
                            }
                        }                        
                    } catch (Exception e) {
                        retval = retval + targetPG+ " Server not found in BFM Cluster or Its not SLAVE.\n";
                    }     
                }
        } else {
            retval = retval + "Please run on Active BFM pair...\n";
        }
        return retval;
    }


    public static String asString(Resource resource) {
        try (Reader reader = new InputStreamReader(resource.getInputStream())) {
            return FileCopyUtils.copyToString(reader);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @RequestMapping(path = "/clusterstatus.html",method = RequestMethod.GET)
    public @ResponseBody String clusterStatusHtml(){
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        ResourceLoader resourceLoader = new DefaultResourceLoader();
        Resource resource = resourceLoader.getResource("classpath:template.html");
        String retval = asString(resource);
        retval = retval.replace("{{ USERNAME }}", username);
        retval = retval.replace("{{ PASSWORD }}", password);
        if (this.bfmContext.isMasterBfm() == Boolean.TRUE){
            if (this.bfmContext.getClusterStatus() == null){
                retval = retval.replace("{{ CLUSTER_STATUS }}", "Cluster Starting...");
                retval = retval.replace("{{ SERVER_ROWS }}", "");
                retval = retval.replace("{{ CLASS_CARD_BODY }}", "bg-primary");
                retval = retval.replace("{{ CLASS_SERVER_ROWS }}", "text-white");
                retval = retval.replace("{{ CHECK_PAUSED }}", "");
                retval = retval.replace("{{ MAIL_ENABLED }}", "");
                retval = retval.replace("{{ ACTIVE_BFM }}", "");
                retval = retval.replace("{{ WATCH_STRATEGY }}", "");
                return retval;        
            } else {
                String server_rows = "";
                String slave_options = "";
                for(PostgresqlServer pg : this.bfmContext.getPgList()){
                    try {
                        pg.getWalPosition();    
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    server_rows = server_rows + "<tr>";
                    server_rows = server_rows +  "<td>"+pg.getServerAddress()+"</td>";
                    server_rows = server_rows +  "<td>"+pg.getDatabaseStatus()+"</td>";
                    server_rows = server_rows +  "<td>"+pg.getWalLogPosition()+"</td>";
                    server_rows = server_rows +  "<td>"+(pg.getReplayLag() == null ? "0" : pg.getReplayLag())+"</td>";
                    server_rows = server_rows +  "<td>"+pg.getTimeLineId()+"</td>";
                    String formattedDate = pg.getLastCheckDateTime().format(dateFormatter);
                    server_rows = server_rows +  "<td>"+formattedDate+"</td>";
                    server_rows = server_rows + "</tr>";

                    if (pg.getDatabaseStatus().equals(DatabaseStatus.SLAVE)){
                        slave_options = slave_options + 
                                        "<option value=\""+pg.getServerAddress()+"\">"+pg.getServerAddress()+"</option>";
                    }
                }
        
                retval = retval.replace("{{ CLUSTER_STATUS }}", this.bfmContext.getClusterStatus().toString());
                retval = retval.replace("{{ SERVER_ROWS }}", server_rows);
                retval = retval.replace("{{ CLASS_CARD_BODY }}", "bg-primary");
                retval = retval.replace("{{ CLASS_SERVER_ROWS }}", "text-white");
                retval = retval.replace("{{ CHECK_PAUSED }}", (this.bfmContext.isCheckPaused() == Boolean.TRUE ? "TRUE" : "FALSE"));
                retval = retval.replace("{{ MAIL_ENABLED }}", (this.bfmContext.isMail_notification_enabled() == Boolean.TRUE ? "TRUE" : "FALSE"));
                retval = retval.replace("{{ ACTIVE_BFM }}", this.getActiveBfm());
                retval = retval.replace("{{ SLAVE_OPTIONS }}", slave_options);
                retval = retval.replace("{{ WATCH_STRATEGY }}", StringUtils.capitalize(this.bfmContext.getWatch_strategy()));
                return retval;    
            }
        } else {
            
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String server_rows = "";
            String slave_options = "";
            try {
                JsonReader reader = new JsonReader(new FileReader("./bfm_status.json"));
                ContextStatus cs = gson.fromJson(reader, ContextStatus.class);
                
                for (ContextServer pg : cs.getClusterServers()){
                    server_rows = server_rows + "<tr>"
                                    +  "<td>"+pg.getAddress()+"</td>"
                                    +  "<td>"+pg.getDatabaseStatus()+"</td>"
                                    +  "<td>"+pg.getLastWalPos()+"</td>"
                                    +  "<td>"+(pg.getReplayLag() == null ? "0" : pg.getReplayLag())+"</td>"
                                    +  "<td>"+pg.getTimeline()+"</td>"
                                    +  "<td>"+pg.getLastCheck()+"</td>"
                                    + "</tr>";
                    if (pg.getDatabaseStatus().equals("SLAVE")){
                        slave_options = slave_options + 
                                        "<option value=\""+pg.getAddress()+"\">"+pg.getAddress()+"</option>";
                    }
                }
                                
                retval = retval.replace("{{ CLUSTER_STATUS }}", cs.getClusterStatus());
                retval = retval.replace("{{ SERVER_ROWS }}", server_rows);
                retval = retval.replace("{{ CLASS_CARD_BODY }}", "bg-warning");
                retval = retval.replace("{{ CLASS_SERVER_ROWS }}", "text-black");
                retval = retval.replace("{{ CHECK_PAUSED }}",cs.getCheckPaused());
                retval = retval.replace("{{ MAIL_ENABLED }}",cs.getMailNotifyEnabled());
                retval = retval.replace("{{ ACTIVE_BFM }}", this.getActiveBfm());
                retval = retval.replace("{{ SLAVE_OPTIONS }}", slave_options);
                retval = retval.replace("{{ WATCH_STRATEGY }}", StringUtils.capitalize(cs.getWatchStrategy()));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            return retval; 
        }
    }
}
