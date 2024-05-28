package com.bisoft.bfm.model;

import com.bisoft.bfm.dto.DatabaseStatus;
import com.bisoft.bfm.dto.PgVersion;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;


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
    private int sizeBehindMaster;

    public DatabaseStatus getStatus(){
        return databaseStatus;
    }

    public Connection getServerConnection() throws ClassNotFoundException, SQLException {
        if(connection == null || this.databaseStatus == DatabaseStatus.INACCESSIBLE) {
            Class.forName("org.postgresql.Driver");
            connection = DriverManager.getConnection("jdbc:postgresql://" + serverAddress + "/postgres",username,password);
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
            log.error(e.getMessage());
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
            log.error(e.getMessage());
            log.error("Unable to get replication status of "+this.getServerAddress());
        }
        this.hasSlave = hasSlave;
        return hasSlave;
    }

    public void getWalPosition() throws ClassNotFoundException, SQLException{
        Connection con  = this.getServerConnection();
        PreparedStatement ps = con.prepareStatement("select pg_current_wal_lsn() as wal_pos");
        ps.executeQuery();
        ResultSet rs = ps.getResultSet();

        rs.next();

        String wal_pos = rs.getString("wal_pos");

        this.setWalLogPosition(wal_pos);
    }

    public DatabaseStatus getDatabaseStatus(){
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
