package com.bisoft.bfm.model;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bisoft.bfm.dto.DatabaseStatus;
import com.bisoft.bfm.dto.PgVersion;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;


@Data
@Builder
@Slf4j
@ToString(doNotUseGetters = true)
public class PostgresqlServer {
    private Boolean isMaster;
    private String serverAddress;
    private PgVersion pgVersion;
    private String username;
    private String password;
    private Connection connection;
    private Boolean hasSlave;
    private DatabaseStatus databaseStatus;
    private int priority;
    private String walLogPosition;
    private int timeLineId;
    private int sizeBehindMaster;
    private LocalDateTime lastCheckDateTime;
    private String replayLag;
    @Builder.Default
    private Boolean rejoinStarted = Boolean.FALSE;
    @Builder.Default
    private Boolean rewindStarted = Boolean.FALSE;

    public DatabaseStatus getStatus(){
        return databaseStatus;
    }

    public Connection getServerConnection(){ 
        try {
            Class.forName("org.postgresql.Driver");
            if (connection == null || this.databaseStatus == DatabaseStatus.INACCESSIBLE) {
                connection = DriverManager.getConnection("jdbc:postgresql://" + serverAddress + "/postgres",username,password);
            } else {
                connection.close();
                connection = DriverManager.getConnection("jdbc:postgresql://" + serverAddress + "/postgres",username,password);
            }               
        } catch (Exception e) {
            log.warn("Can't connect to server "+this.getServerAddress());
        }
        return connection;
    }

    public Boolean isServerMaster(){
        Boolean is_in_recovery = null;
        try {
            Statement statement = this.getServerConnection().createStatement();
            ResultSet rs = statement.executeQuery("select pg_is_in_recovery()");
            while(rs.next()){
                is_in_recovery = ! rs.getBoolean(1);
            }
        }catch (Exception e){
            // log.error(e.getMessage());
            log.error("Unable get master status of "+this.getServerAddress());
        }
        this.isMaster = is_in_recovery;
        return is_in_recovery;
    }

    public Boolean hasSlaveServer(){
        Boolean hasSlave = null;
        try {
            Statement statement = this.getServerConnection().createStatement();
            ResultSet rs = statement.executeQuery("select * from pg_stat_replication");
            hasSlave = false;
            while(rs.next()){
                hasSlave = true;
            }
            this.hasSlave = hasSlave;
            return hasSlave;
        }catch (Exception e){
            // log.error(e.getMessage());
            log.error("Unable to get replication status of "+this.getServerAddress());
        }
        this.hasSlave = hasSlave;
        return hasSlave;
    }

    public void getWalPosition() throws ClassNotFoundException, SQLException{
        if (this.databaseStatus.equals(DatabaseStatus.MASTER) || this.databaseStatus.equals(DatabaseStatus.MASTER_WITH_NO_SLAVE)){
            try {
                Connection con  = this.getServerConnection();
                PreparedStatement ps = con.prepareStatement("select pg_current_wal_lsn() as wal_pos");
                ps.executeQuery();
                ResultSet rs = ps.getResultSet();

                rs.next();

                String wal_pos = rs.getString("wal_pos");

                this.setWalLogPosition(wal_pos);
            } catch (Exception e) {
                log.warn("Connection Failed to server:"+this.getServerAddress());
                this.databaseStatus = DatabaseStatus.INACCESSIBLE;
            }
        } else {
            try {
                Connection con  = this.getServerConnection();
                PreparedStatement ps = con.prepareStatement("select pg_last_wal_replay_lsn() as wal_pos");
                ps.executeQuery();
                ResultSet rs = ps.getResultSet();
                rs.next();
                String wal_pos = rs.getString("wal_pos");
                this.setWalLogPosition(wal_pos);
            } catch (Exception e) {
                log.warn("Connection Failed to server:"+this.getServerAddress());
                this.databaseStatus = DatabaseStatus.INACCESSIBLE;
            }
        }
    }

    public void checkTimeLineId() throws ClassNotFoundException, SQLException{
        try {
            Connection con  = this.getServerConnection();
            PreparedStatement ps = con.prepareStatement("SELECT timeline_id FROM pg_control_checkpoint();");
            ps.executeQuery();
            ResultSet rs = ps.getResultSet();

            rs.next();

            String timeline_id = rs.getString("timeline_id");

            this.setTimeLineId(Integer.parseInt(timeline_id));
            // log.info(this.getServerAddress() + " timeline_id: "+ this.getTimeLineId());
        } catch (Exception e) {
            log.warn("Connection Failed to server:"+this.getServerAddress());
            this.databaseStatus = DatabaseStatus.INACCESSIBLE;
        }
    }

    public Map<String,String> getReplayLagMap(){
        Map<String,String> replayLagMap = new HashMap<>();
        if (this.databaseStatus.equals(DatabaseStatus.MASTER)){
            try {
                Connection con  = this.getServerConnection();
                PreparedStatement ps = con.prepareStatement("select client_addr,replay_lag from pg_stat_replication;");
                ps.executeQuery();
                ResultSet rs = ps.getResultSet();
                while(rs.next()){
                    String slave_addr = rs.getString("client_addr");
                    String replay_lag = rs.getString("replay_lag");
                    if (replay_lag == null) replay_lag = "0";
                    replayLagMap.put(slave_addr, replay_lag);
                }

            } catch (Exception e) {
                log.warn("Connection Failed to server:"+this.getServerAddress());
            }
        }

        return replayLagMap;
    }

    public double getDataLossSize(String masterLastWalPos){
        double retval = 0.0;
        try {
            Connection con  = this.getServerConnection();
            PreparedStatement ps = con.prepareStatement("select pg_wal_lsn_diff('"+ masterLastWalPos +"',pg_last_wal_replay_lsn()) ;");
            ps.executeQuery();
            ResultSet rs = ps.getResultSet();
            while(rs.next()){
                String diff_size = rs.getString("pg_wal_lsn_diff");
                retval = Double.parseDouble(diff_size);
            }
        } catch (Exception e) {
            log.warn("Connection Failed to server:"+this.getServerAddress());
            this.databaseStatus = DatabaseStatus.INACCESSIBLE;
        }

        return retval;
    }

    public DatabaseStatus getDatabaseStatus(){
        this.setLastCheckDateTime(LocalDateTime.now());
        if (this.getServerConnection() != null){
            this.hasSlaveServer();
            this.isServerMaster();
            if (this.isMaster == null && this.hasSlave == null) {
                this.databaseStatus = DatabaseStatus.INACCESSIBLE;
            }else if (this.isMaster == true && this.hasSlave == true) {
                this.databaseStatus = DatabaseStatus.MASTER;
            }else if (this.isMaster == true && this.hasSlave == false) {
                this.databaseStatus = DatabaseStatus.MASTER_WITH_NO_SLAVE;
            }else if (this.isMaster == false && this.hasSlave == false) {
                this.databaseStatus = DatabaseStatus.SLAVE;
            }else if(this.isMaster == false && this.hasSlave == true) {
                this.databaseStatus = DatabaseStatus.SLAVE_WITH_SLAVE;
            }
        } else {
            this.databaseStatus = DatabaseStatus.INACCESSIBLE;
        }
        
        return this.databaseStatus;
    }

    public List<String> executeStatement(String sql)  {
        try {
            List<String> result = new ArrayList<>();
            Statement statement = this.getServerConnection().createStatement();
            ResultSet rs = statement.executeQuery(sql);
            while(rs.next()){
                result.add(rs.getString(1));
            }
            return result;
        }catch (Exception e){
            log.error(e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    public Boolean getHasMasterServer(){
        Boolean hasMaster = null;
        try {
            Statement statement = this.getServerConnection().createStatement();
            ResultSet rs = statement.executeQuery("select * from pg_stat_wal_receiver");
            hasMaster = false;
            while(rs.next()){
                hasMaster = true;
            }
            return hasMaster;
        }catch (Exception e){
            log.error(e.getMessage());
            log.error("Unable to get replication status of "+this.getServerAddress());
        }
        return hasMaster;
    }

}
