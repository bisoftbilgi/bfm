package com.bisoft.bfm;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Controller;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.bisoft.bfm.dto.ClusterStatus;
import com.bisoft.bfm.dto.DatabaseStatus;
import com.bisoft.bfm.helper.MinipgAccessUtil;
import com.bisoft.bfm.helper.SymmetricEncryptionUtil;
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
    private final SymmetricEncryptionUtil symmetricEncryptionUtil;

    @Value("${watcher.cluster-pair:no-pair}")
    private String bfmPair;

    @Value("${server.pguser:postgres}")
    private String username;

    @Value("${server.pgpassword:postgres}")
    private String password;

    @Value("${watcher.cluster-port:9994}")
    private String cluster_port;

    @Value("${bfm.basebackup-slave-join:false}")
    public boolean basebackup_slave_join;

    @Value("${app.custom-logo-path:no-file}")
    public String custom_logo_path;
    

    // @RequestMapping(path = "/login",method = RequestMethod.GET)
    // public @ResponseBody
    // String login(){
    //     ResourceLoader resourceLoader = new DefaultResourceLoader();
    //     Resource resource = resourceLoader.getResource("classpath:login.html");
    //     return asString(resource);
    // }

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

    @RequestMapping(path = "/encrypt/{clearStr}",method = RequestMethod.POST)
    public @ResponseBody String encryptString(@PathVariable(value = "clearStr") String clearStr){

        return symmetricEncryptionUtil.encrypt(clearStr) +"\n";
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
            return "T";
        } else {
            return this.bfmPair;
        }
    }

    @RequestMapping(path = "/check-pause",method = RequestMethod.GET)
    public @ResponseBody String clusterCheckPause(){
        this.bfmContext.setCheckPaused(Boolean.TRUE);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
            try {
                JsonReader reader = new JsonReader(new FileReader("./bfm_status.json"));
                ContextStatus cs = gson.fromJson(reader, ContextStatus.class);
                cs.setCheckPaused("TRUE");
                String json_str = gson.toJson(cs);
                PrintWriter out;
                try {
                    out = new PrintWriter("./bfm_status.json");
                    out.println(json_str);
                    out.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
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
            retval = retval + "\nCluster Check : "+(this.bfmContext.isCheckPaused() == Boolean.TRUE ? "Paused" : "Processing");
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
                retval = retval + "\nCluster Check : "+(cs.getCheckPaused() == "TRUE" ? "Paused" : "Processing");
                retval = retval + "\nWatch Strategy : "+cs.getWatchStrategy();
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
                                        && (!s.getStatus().equals(DatabaseStatus.MASTER))
                                        && (!s.getStatus().equals(DatabaseStatus.INACCESSIBLE)))).findFirst().get();

                        if (switchOverToPG == null){
                            retval = retval + targetPG+ " Server not found in BFM Cluster or It's MASTER or INACCESSIBLE.\n";
                        } else {
                            if (switchOverToPG.getReplayLag().equals("0")){
                                this.bfmContext.setCheckPaused(Boolean.TRUE);
                                String ws = this.bfmContext.getWatch_strategy();
                                Boolean mail_notify = this.bfmContext.isMail_notification_enabled();
                                
                                this.bfmContext.setWatch_strategy("manual");
                                this.bfmContext.setMail_notification_enabled(Boolean.FALSE);
    
                                PostgresqlServer old_master = this.bfmContext.getMasterServer();
                                String result ="";
                                
                                // result = minipgAccessUtil.prepareForSwitchOver(old_master);
                                // log.info("Prepare for SwitchOver result :" + result);

                                result = minipgAccessUtil.stopPg(old_master);
                                log.info("Stop old master result :" + result);

                                minipgAccessUtil.vipDown(old_master);

                                result = minipgAccessUtil.promote(switchOverToPG);
                                log.info("Slave Promote Result :"+ result);

                                minipgAccessUtil.vipUp(switchOverToPG);
                                switchOverToPG.setApplication_name("");
                                switchOverToPG.setSyncState("");                    
                                
                                this.bfmContext.getPgList().stream().filter(s -> (!s.getServerAddress().equals(switchOverToPG.getServerAddress())))
                                                                    .forEach(pg -> {
                                                                        try {
                                                                            pg.setRewindStarted(Boolean.TRUE);
                                                                            String rewind_result = minipgAccessUtil.rewind(pg, switchOverToPG);
                                                                            if (! rewind_result.equals("OK")){
                                                                                log.info("on SwitchOver pg_rewind was FAILED. Response is : "+rewind_result+" Slave Target:" + pg.getServerAddress());
                                                                                if (basebackup_slave_join == Boolean.TRUE){
                                                                                    String rejoin_result = minipgAccessUtil.rebaseUp(pg, switchOverToPG);
                                                                                    log.info("pg_basebackup join cluster result is:"+rejoin_result);
                                                                                } else {
                                                                                    log.warn("basebackup rejoin disabled. Please check server manually:"+ pg.getServerAddress());
                                                                                }
                                                                            }
                                                                            pg.setRewindStarted(Boolean.FALSE);                                                                            
                                                                        } catch (Exception e) {
                                                                            log.warn(pg.getServerAddress() + " Rejoin FAILED. error :"+e);
                                                                        }
                                                                    });
                                                                    
                                // result = minipgAccessUtil.postSwitchOver(old_master, switchOverToPG);
                                // log.info("Ex-Master R/W Set Result :"+ result);
                                    
                                this.bfmContext.setCheckPaused(Boolean.FALSE);
                                int checkCount = 3;
                                while (((this.bfmContext.getClusterStatus() != ClusterStatus.HEALTHY) || 
                                                                    (this.bfmContext.getPgList().stream()
                                                                    .filter(s -> (s.getStatus().equals(DatabaseStatus.INACCESSIBLE))).count()) > 0) 
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
                                        && (!s.getDatabaseStatus().equals(DatabaseStatus.MASTER)))).findFirst().get();

                        if (target_server == null){
                            retval = retval + targetPG+ " Server not found in BFM Cluster or Its MASTER.\n";
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

        Resource growl_css = resourceLoader.getResource("classpath:jquery.growl.css");
        String growl_css_str = asString(growl_css);
        retval = retval.replace("{{ GROWL_STYLE }}", growl_css_str);

        Resource growl_js = resourceLoader.getResource("classpath:jquery.growl.js");
        String growl_js_str = asString(growl_js);
        retval = retval.replace("{{ GROWL_SCRIPT }}", growl_js_str);

        retval = retval.replace("{{ USERNAME }}", username);
        retval = retval.replace("{{ PASSWORD }}", password);
        if (this.bfmContext.isMasterBfm() == Boolean.TRUE){
            if (this.bfmContext.getClusterStatus() == null){
                retval = retval.replace("{{ CLUSTER_STATUS }}", "Cluster Starting...");
                retval = retval.replace("{{ SERVER_ROWS }}", "");
                retval = retval.replace("{{ CLASS_CARD_BODY }}", "bg-primary");
                retval = retval.replace("{{ CLASS_SERVER_ROWS }}", "text-white");
                retval = retval.replace("{{ CHECK_STATUS }}", "");
                retval = retval.replace("{{ MAIL_ENABLED }}", "");
                retval = retval.replace("{{ ACTIVE_BFM }}", "");
                retval = retval.replace("{{ WATCH_STRATEGY }}", "");
                return retval;        
            } else {
                String server_rows = "";
                String slave_options = "";
                for(PostgresqlServer pg : this.bfmContext.getPgList()){
                    if (pg.getWalLogPosition() == null){
                        try {
                            pg.getWalPosition();    
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    
                    server_rows = server_rows + "<tr>";
                    server_rows = server_rows +  "<td>"+pg.getServerAddress()+"</td>";
                    server_rows = server_rows +  "<td>"+pg.getStatus()+"</td>";
                    server_rows = server_rows +  "<td>"+pg.getWalLogPosition()+"</td>";
                    server_rows = server_rows +  "<td>"+(pg.getReplayLag() == null ? "0" : pg.getReplayLag())+"</td>";
                    server_rows = server_rows +  "<td>"+pg.getTimeLineId()+"</td>";
                    String formattedDate = pg.getLastCheckDateTime().format(dateFormatter);
                    server_rows = server_rows +  "<td>"+formattedDate+"</td>";
                    server_rows = server_rows + "</tr>";

                    if (!pg.getDatabaseStatus().equals(DatabaseStatus.MASTER)) {
                        slave_options = slave_options + 
                                        "<option value=\""+pg.getServerAddress()+"\">"+pg.getServerAddress()+"</option>";
                    }
                }
        
                retval = retval.replace("{{ CLUSTER_STATUS }}", this.bfmContext.getClusterStatus().toString());
                retval = retval.replace("{{ SERVER_ROWS }}", server_rows);
                retval = retval.replace("{{ CLASS_CARD_BODY }}", "bg-primary");
                retval = retval.replace("{{ CLASS_SERVER_ROWS }}", "text-white");
                retval = retval.replace("{{ CHECK_STATUS }}", (this.bfmContext.isCheckPaused() == Boolean.TRUE ? " " : "checked"));
                retval = retval.replace("{{ MAIL_ENABLED }}", (this.bfmContext.isMail_notification_enabled() == Boolean.TRUE ? "checked" : " "));
                retval = retval.replace("{{ ACTIVE_BFM }}", this.getActiveBfm());
                retval = retval.replace("{{ SLAVE_OPTIONS }}", slave_options);
                retval = retval.replace("{{ WATCH_STRATEGY }}", (this.bfmContext.getWatch_strategy().equals("availability") ? "checked" : " "));
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
                    if (!pg.getDatabaseStatus().equals("MASTER")){
                        slave_options = slave_options + 
                                        "<option value=\""+pg.getAddress()+"\">"+pg.getAddress()+"</option>";
                    }
                }
                                
                retval = retval.replace("{{ CLUSTER_STATUS }}", cs.getClusterStatus());
                retval = retval.replace("{{ SERVER_ROWS }}", server_rows);
                retval = retval.replace("{{ CLASS_CARD_BODY }}", "bg-warning");
                retval = retval.replace("{{ CLASS_SERVER_ROWS }}", "text-black");
                retval = retval.replace("{{ CHECK_STATUS }}",(cs.getCheckPaused().equals("TRUE") ? " " : "checked"));
                retval = retval.replace("{{ MAIL_ENABLED }}",(cs.getMailNotifyEnabled().equals("TRUE") ? "checked" : " "));
                retval = retval.replace("{{ ACTIVE_BFM }}", this.getActiveBfm());
                retval = retval.replace("{{ SLAVE_OPTIONS }}", slave_options);
                retval = retval.replace("{{ WATCH_STRATEGY }}", (cs.getWatchStrategy().equals("availability") ? "checked" : " "));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            return retval; 
        }
    }

    @RequestMapping(path = "/index.html",method = RequestMethod.GET)
    public @ResponseBody String webRoot(){
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        ResourceLoader resourceLoader = new DefaultResourceLoader();
        String retval = " ";
        if (this.bfmContext.isMasterBfm() == Boolean.TRUE){
            Resource resource = resourceLoader.getResource("classpath:index.html");
            retval = asString(resource);
    
            retval = retval.replace("{{ USERNAME }}", username);
            retval = retval.replace("{{ PASSWORD }}", password);
            if (custom_logo_path.equals("no-file") || custom_logo_path.trim().equals("")){
                retval = retval.replace("{{ CUSTOM_LOGO }}", " ");
            } else {
                retval = retval.replace("{{ CUSTOM_LOGO }}", "<div class=\"d-flex justify-content-end\"><img class=\"custom-logo\" src=\""+custom_logo_path+"\" alt=\"custom-logo\"></div>");
            }
            
            if (this.bfmContext.getClusterStatus() == null){
                retval = retval.replace("{{ CHECK_STATUS }}", "");
                retval = retval.replace("{{ MAIL_ENABLED }}", "");
                retval = retval.replace("{{ WATCH_STRATEGY }}", "");
                retval = retval.replace("{{ ACTIVE_BFM }}", "");                
                return retval;        
            } else {
                String server_rows = "";
                String slave_options = "";
                for(PostgresqlServer pg : this.bfmContext.getPgList()){
                    if (pg.getWalLogPosition() == null){
                        try {
                            pg.getWalPosition();    
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    
                    server_rows = server_rows + "<tr>";
                    server_rows = server_rows +  "<td>"+pg.getServerAddress()+"</td>";
                    server_rows = server_rows +  "<td>"+pg.getStatus()+"</td>";
                    server_rows = server_rows +  "<td>"+pg.getWalLogPosition()+"</td>";
                    server_rows = server_rows +  "<td>"+(pg.getReplayLag() == null ? "0" : pg.getReplayLag())+"</td>";
                    server_rows = server_rows +  "<td>"+pg.getTimeLineId()+"</td>";
                    String formattedDate = pg.getLastCheckDateTime().format(dateFormatter);
                    server_rows = server_rows +  "<td>"+formattedDate+"</td>";
                    server_rows = server_rows + "</tr>";

                    if (!pg.getDatabaseStatus().equals(DatabaseStatus.MASTER)) {
                        slave_options = slave_options + 
                                        "<option value=\""+pg.getServerAddress()+"\">"+pg.getServerAddress()+"</option>";
                    }
                }
                retval = retval.replace("{{ ACTIVE_BFM }}", this.getActiveBfm());
                retval = retval.replace("{{ SLAVE_OPTIONS }}", slave_options);
                retval = retval.replace("{{ CHECK_STATUS }}", (this.bfmContext.isCheckPaused() == Boolean.TRUE ? " " : "checked"));
                retval = retval.replace("{{ MAIL_ENABLED }}", (this.bfmContext.isMail_notification_enabled() == Boolean.TRUE ? "checked" : " "));
                retval = retval.replace("{{ WATCH_STRATEGY }}", (this.bfmContext.getWatch_strategy().equals("availability") ? "checked" : " "));
                return retval;    
            }
        } else {
            Resource resource = resourceLoader.getResource("classpath:index-passive-forward.html");
            retval = asString(resource);
            retval = retval.replace("{{ ACTIVE_BFM }}", this.getActiveBfm());
        }
        return retval;
    }
    
    public static String[] getLastNLinesFromFile(String filePath, int numLines) throws IOException {
        try (Stream<String> stream = Files.lines(Paths.get(filePath))) {
            AtomicInteger offset = new AtomicInteger();
            String[] lines = new String[numLines];
            stream.forEach(line -> {
                lines[offset.getAndIncrement() % numLines] = line;
            });
            List<String> list = IntStream.range(offset.get() < numLines ? 0 : offset.get() - numLines, offset.get())
                    .mapToObj(idx -> lines[idx % numLines]).collect(Collectors.toList());
            return list.toArray(new String[0]);
        }
    }

    @RequestMapping(path = "/getLogs",method = RequestMethod.GET)
    public @ResponseBody String getLogs(){
        if (this.bfmContext.isMasterBfm() == Boolean.TRUE){
            
            try {
                return String.join("\n", getLastNLinesFromFile("log/app.log",10));
            } catch (IOException e) {
                e.printStackTrace();
                return "BFM Cluster Manager.";
            }
        } else {
            return "BFM Cluster Manager.";
        }
    }

    @RequestMapping(path = "/getClsStatus",method = RequestMethod.GET)
    public @ResponseBody String getClsStatus(){
        if (this.bfmContext.isMasterBfm() == Boolean.TRUE){
            String clsState = "";
            if (this.bfmContext.getClusterStatus() != null){
                clsState = (this.bfmContext.getClusterStatus().toString()).substring(0,1).toUpperCase() + (this.bfmContext.getClusterStatus().toString()).substring(1).toLowerCase();
            }
            
            return clsState;
            
        } else {
            return "Requesting";
        }
    }

    @RequestMapping(path = "/getClsRows",method = RequestMethod.GET)
    public @ResponseBody String getClsRows(){
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        if (this.bfmContext.isMasterBfm() == Boolean.TRUE){            
            String server_rows = "";
            for(PostgresqlServer pg : this.bfmContext.getPgList()){
                if (pg.getWalLogPosition() == null){
                    try {
                        pg.getWalPosition();    
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                
                server_rows = server_rows + "<tr>";
                server_rows = server_rows +  "<td>"+pg.getServerAddress()+"</td>";
                server_rows = server_rows +  "<td>"+pg.getStatus()+"</td>";
                if (pg.getStatus() == DatabaseStatus.SLAVE){
                    server_rows = server_rows +  "<td>";
                    server_rows = server_rows +  "<a href=\"#\" id=\"ah_"+pg.getServerAddress()+"\" class=\"tooltip-test\" title=\"Priority:"+pg.getPriority()+"\">";                    
                    server_rows = server_rows +  "<label class=\"switch-sm\">";
                    server_rows = server_rows +  "<input id=\"cb_priority_"+ pg.getServerAddress() +"\" type=\"checkbox\" "+(pg.getPriority() > 0 ? "checked" : "" ) + " onchange=\"setPriority('"+ pg.getServerAddress() +"', '"+pg.getPriority()+"',this);\">";
                    server_rows = server_rows +  "<span class=\"slider-sm round\"></span>";
                    server_rows = server_rows +  "</label>";
                    server_rows = server_rows +  "</a>";    
                    server_rows = server_rows +  "</td>";    
                } else {
                    server_rows = server_rows +  "<td></td>";
                }
                server_rows = server_rows +  "<td>"+pg.getWalLogPosition()+"</td>";
                server_rows = server_rows +  "<td>"+(pg.getReplayLag() == null ? "0" : pg.getReplayLag())+"</td>";
                server_rows = server_rows +  "<td>"+pg.getTimeLineId()+"</td>";
                String formattedDate = pg.getLastCheckDateTime().format(dateFormatter);
                server_rows = server_rows +  "<td>"+formattedDate+"</td>";
                server_rows = server_rows +  "<td>"+(pg.getApplication_name() == null ? "" : pg.getApplication_name()) +"</td>";
                server_rows = server_rows +  "<td>"+(pg.getSyncState() == null ? "" : pg.getSyncState())+"</td>";
                if (pg.getStatus() == DatabaseStatus.SLAVE){
                    server_rows = server_rows +  "<td>";
                    server_rows = server_rows +  "<label class=\"switch-sm\">";
                    server_rows = server_rows +  "<input id=\"cb_sync_"+(pg.getApplication_name() == null ? "" : pg.getApplication_name()) +"\" type=\"checkbox\" "+ (pg.getSyncState() == null ? "async" : (pg.getSyncState()).equals("sync") ? "checked" : ((pg.getSyncState()).equals("potential") ? "checked" : "")) + " onchange=\"setSyncAsync('"+ (pg.getApplication_name() == null ? "" : pg.getApplication_name()) +"');\">";
                    server_rows = server_rows +  "<span class=\"slider-sm round\"></span>";
                    server_rows = server_rows +  "</label>";                                           
                    server_rows = server_rows +  "</td>";
                }
                
                server_rows = server_rows + "</tr>";
            }
            return server_rows;
            
        } else {
            return "Requesting";
        }
    }

    @RequestMapping(path = "/getSlvRows",method = RequestMethod.GET)
    public @ResponseBody String getSlaveRows(){
        if (this.bfmContext.isMasterBfm() == Boolean.TRUE){            
            String slave_rows = "<option value=\"\" selected>Select Slave</option>";
            for(PostgresqlServer pg : this.bfmContext.getPgList()){
                if (pg.getStatus() == DatabaseStatus.SLAVE){                    
                    slave_rows += "<option value=\""+pg.getServerAddress()+"\">"+pg.getServerAddress()+"</option>";
                } 
            }
            return slave_rows;
            
        } else {
            return "Requesting";
        }
    }

    @RequestMapping(path = "/setsync/{target}",method = RequestMethod.POST)
    public @ResponseBody String setSync(@PathVariable(value = "target") String targetAppName){
        String retval ="";       
        if (this.bfmContext.isMasterBfm() == Boolean.TRUE){
            PostgresqlServer master_server = this.bfmContext.getPgList().stream()
            .filter(server -> server.getStatus() == DatabaseStatus.MASTER ).findFirst().get();                    
            try {
                String sync_result = minipgAccessUtil.setReplicationToSync(master_server, targetAppName);
                PostgresqlServer syncReplica = this.bfmContext.getPgList().stream().filter(r -> (r.getApplication_name() != null ? r.getApplication_name() :"").equals(targetAppName)).findFirst().get();
                syncReplica.setSyncState("sync");
                this.bfmContext.getSyncReplicas().add(syncReplica);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {

        }
        return retval;
    }    

    @RequestMapping(path = "/setasync/{target}",method = RequestMethod.POST)
    public @ResponseBody String setAsync(@PathVariable(value = "target") String targetAppName){
        String retval ="";       
        if (this.bfmContext.isMasterBfm() == Boolean.TRUE){
            PostgresqlServer master_server = this.bfmContext.getPgList().stream()
            .filter(server -> server.getStatus() == DatabaseStatus.MASTER ).findFirst().get();                    
            try {
                String async_result = minipgAccessUtil.setReplicationToAsync(master_server, targetAppName);
                PostgresqlServer oldSyncReplica = this.bfmContext.getPgList().stream().filter(r -> (r.getApplication_name() != null ? r.getApplication_name() :"").equals(targetAppName)).findFirst().get();
                this.bfmContext.getSyncReplicas().remove(oldSyncReplica);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {

        }
        return retval;
    }
    
    @RequestMapping(path = "/setpriority/{target}/{priority}",method = RequestMethod.POST)
    public @ResponseBody String setPriority(@PathVariable(value = "target") String targetPg, @PathVariable(value = "priority") Integer priority){
        String retval ="";       
        if (this.bfmContext.isMasterBfm() == Boolean.TRUE){
            this.bfmContext.getPgList().stream()
            .filter(s -> s.getServerAddress().equals(targetPg))
            .forEach(pg -> {
                pg.setPriority(priority);
            });
            retval = "OK";
        } else {
            retval = "PAS";
        }
        return retval;
    }
}
