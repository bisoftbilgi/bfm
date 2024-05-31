package com.bisoft.bfm;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Controller;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.bisoft.bfm.model.BfmContext;
import com.bisoft.bfm.model.PostgresqlServer;

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

    @RequestMapping(path = "/cluster-status",method = RequestMethod.GET)
    public @ResponseBody String clusterStatus(){
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String retval = "";
        retval = retval + "\nCluster Status : "+this.bfmContext.getClusterStatus();
        for(PostgresqlServer pg : this.bfmContext.getPgList()){
            try {
                pg.getWalPosition();    
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            retval = retval +"\n" + "Server Address : "+pg.getServerAddress();
            retval = retval+ "\nStatus : "+pg.getDatabaseStatus(); 
            retval = retval+ "\nLast Wal Position : "+pg.getWalLogPosition();
            String formattedDate = pg.getLastCheckDateTime().format(dateFormatter);     
            retval = retval+ "\nLast Server Check : "+formattedDate;
            retval = retval + "\n";
        }
        retval = retval+ "\nLast Check Log: \n"+ this.bfmContext.getLastCheckLog() +"\n";

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
            server_rows = server_rows + "</tr>";
        }

        retval = retval.replace("{{ CLUSTER_STATUS }}", this.bfmContext.getClusterStatus().toString());
        retval = retval.replace("{{ SERVER_ROWS }}", server_rows);
        return retval;
    }
}
