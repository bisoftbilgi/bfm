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

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Controller;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.bisoft.bfm.dto.DatabaseStatus;
import com.bisoft.bfm.model.BfmContext;
import com.bisoft.bfm.model.ContextServer;
import com.bisoft.bfm.model.ContextStatus;
import com.bisoft.bfm.model.PostgresqlServer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/bfm")
@RequiredArgsConstructor
public class BfmController {

    private final BfmContext bfmContext;

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

    @RequestMapping(path = "/cluster-status",method = RequestMethod.GET)
    public @ResponseBody String clusterStatus(){
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String retval = "";
        ArrayList<String> serverIPAddress = new ArrayList<String>();

        // try {
        //     Enumeration<NetworkInterface> b = NetworkInterface.getNetworkInterfaces();
        //     while( b.hasMoreElements()){
        //         for ( InterfaceAddress f : b.nextElement().getInterfaceAddresses())
        //             if ( f.getAddress().toString().contains(".") && f.getAddress().toString() !="127.0.0.1")
        //             serverIPAddress.add(f.getAddress().toString().replace("/",""));
        //     }
        // } catch (SocketException e) {
        //     e.printStackTrace();
        // }

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
                                String.format("%-20s", "Last Check Time :");
            retval = retval + "\n"+ 
                                "_".repeat(25) + 
                                "\t" + 
                                "_".repeat(10) + 
                                "\t" + 
                                "_".repeat(20) +
                                "\t" + 
                                "_".repeat(20);
            for(PostgresqlServer pg : this.bfmContext.getPgList()){
                try {
                    pg.getWalPosition();    
                } catch (Exception e) {
                    e.printStackTrace();
                }
                String formattedDate = pg.getLastCheckDateTime().format(dateFormatter);     
                
                retval = retval +"\n" + 
                                String.format("%-25s", pg.getServerAddress())  +
                                "\t" + 
                                String.format("%-10s", pg.getDatabaseStatus()) +
                                "\t" + 
                                String.format("%20s", pg.getWalLogPosition()) +
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
                                    String.format("%-20s", "Last Check Time :");
                retval = retval + "\n"+ 
                                    "_".repeat(25) + 
                                    "\t" + 
                                    "_".repeat(10) + 
                                    "\t" + 
                                    "_".repeat(20) +
                                    "\t" + 
                                    "_".repeat(20);

                for (ContextServer pg : cs.getClusterServers()){
                    retval = retval +"\n" + 
                    String.format("%-25s", pg.getAddress()) + 
                    "\t" + 
                    String.format("%-10s", pg.getDatabaseStatus()) +
                    "\t" + 
                    String.format("%20s", pg.getLastWalPos()) +
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

    @RequestMapping(path = "/switchover/{targetPG}",method = RequestMethod.POST)
    public @ResponseBody String switchOver(@PathVariable(value = "targetPG") String targetPG){
        String retval = "";       
        if (this.bfmContext.isMasterBfm() == Boolean.TRUE){
            try{
                String targetPG_IP = targetPG.split(":")[0];
                String targetPG_Port = targetPG.split(":")[1];
                if (targetPG_IP.length() < 7 || targetPG_Port.length()<1){
                    retval = retval + "Please speicfy target server and port (e.g. 192.168.1.7:5432)\n";
                } else {

                    // set watch_startegy to manual
                    // set mail notification to false
                    // on CheckCluster if bfmContext.switchOverTo is not null run switchOver routine
                        // check replay lag is 0
                        // stop master
                        // promote slave
                        // turn ex-master to slave with pg_rewind, if fail use pg_basebackup
                    // set watch_startegy to available
                    // set mail notification to old value

                }
            } catch (Exception e) {
                retval = retval + "Please speicfy target server and port (e.g. 192.168.1.7:5432)\n";
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
        if (this.bfmContext.isMasterBfm() == Boolean.TRUE){
            if (this.bfmContext.getClusterStatus() == null){
                retval = retval.replace("{{ CLUSTER_STATUS }}", "Cluster Starting...");
                retval = retval.replace("{{ SERVER_ROWS }}", "");
                retval = retval.replace("{{ CLASS_CARD_BODY }}", "bg-primary");
                retval = retval.replace("{{ CLASS_SERVER_ROWS }}", "text-white");
                return retval;        
            } else {
                String server_rows = "";
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
                    String formattedDate = pg.getLastCheckDateTime().format(dateFormatter);
                    server_rows = server_rows +  "<td>"+formattedDate+"</td>";
                    server_rows = server_rows +  "<td>"+(pg.getDatabaseStatus() == DatabaseStatus.MASTER ? " " : pg.getReplayLag())+"</td>";
                    server_rows = server_rows + "</tr>";
                }
        
                retval = retval.replace("{{ CLUSTER_STATUS }}", this.bfmContext.getClusterStatus().toString());
                retval = retval.replace("{{ SERVER_ROWS }}", server_rows);
                retval = retval.replace("{{ CLASS_CARD_BODY }}", "bg-primary");
                retval = retval.replace("{{ CLASS_SERVER_ROWS }}", "text-white");
                return retval;    
            }
        } else {
            
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String server_rows = "";
            try {
                JsonReader reader = new JsonReader(new FileReader("./bfm_status.json"));
                ContextStatus cs = gson.fromJson(reader, ContextStatus.class);
                
                for (ContextServer pg : cs.getClusterServers()){
                    server_rows = server_rows + "<tr>"
                                    +  "<td>"+pg.getAddress()+"</td>"
                                    +  "<td>"+pg.getDatabaseStatus()+"</td>"
                                    +  "<td>"+pg.getLastWalPos()+"</td>"
                                    +  "<td>"+pg.getLastCheck()+"</td>"
                                    +  "<td>"+(pg.getReplayLag() == null ? " " : pg.getReplayLag())+"</td>"
                                    + "</tr>";
                }
                                
                retval = retval.replace("{{ CLUSTER_STATUS }}", cs.getClusterStatus());
                retval = retval.replace("{{ SERVER_ROWS }}", server_rows);
                retval = retval.replace("{{ CLASS_CARD_BODY }}", "bg-warning");
                retval = retval.replace("{{ CLASS_SERVER_ROWS }}", "text-black");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            return retval; 
        }
    }
}
