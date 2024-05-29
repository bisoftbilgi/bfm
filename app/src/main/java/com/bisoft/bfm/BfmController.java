package com.bisoft.bfm;

import com.bisoft.bfm.dto.DatabaseStatus;
import com.bisoft.bfm.model.BfmContext;
import com.bisoft.bfm.model.PostgresqlServer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

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
        String slaveServerAddresses = "";

        for(PostgresqlServer pg : this.bfmContext.getPgList()){
            if (pg.getStatus().equals(DatabaseStatus.SLAVE)){
                if (slaveServerAddresses.length() > 3){
                    slaveServerAddresses = slaveServerAddresses + " - ";    
                }

                slaveServerAddresses = slaveServerAddresses + pg.getServerAddress();
            }
        }

        String retval = "";
        retval = retval + "Cluster Status\t:\t\tMaster Server Address\t:\t\tSlave Server Addresses\t:";
        retval = retval + "\n---------------------------------------------------------------------------------------------------------------------------";
        retval = retval + "\n"+this.bfmContext.getClusterStatus()+"\t\t\t"+this.bfmContext.getMasterServer().getServerAddress()+"\t\t\t"+slaveServerAddresses;

        retval = retval + "\n";

        return retval;
    }

}
