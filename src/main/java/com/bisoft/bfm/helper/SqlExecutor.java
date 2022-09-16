package com.bisoft.bfm.helper;

import com.bisoft.bfm.dto.PgVersion;
import com.bisoft.bfm.model.PostgresqlServer;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@AllArgsConstructor
@RequiredArgsConstructor
public class SqlExecutor {
    @Value("${server.pguser:postgres}")
    private String username;

    @Value("${server.pgpassword:postgres}")
    private String password;


    public List<String> retrieveSqlResult(String sqlString, PostgresqlServer postgresqlServer) {

        log.info("sql executing:" + sqlString);

        List<String> cellValues = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection("jdbc:postgresql://"+postgresqlServer.getServerAddress()+ "/postgres",
                username,password)) {
            Statement stmt = conn.createStatement();

            Class.forName("org.postgresql.Driver");

            ResultSet result = stmt.executeQuery(sqlString);
            String    line;
            while (result.next()) {
                line = result.getString(1);
                log.trace(line);
                cellValues.add(line);
            }

            stmt.close();
            conn.close();
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());

        }

        return cellValues;
    }

    public void executeSql(String sqlString,PostgresqlServer postgresqlServer) {


        try (Connection conn = DriverManager.getConnection("jdbc:postgresql://"+postgresqlServer.getServerAddress() +"/postgres",
                username, password)) {
            Statement stmt = conn.createStatement();
            Class.forName("org.postgresql.Driver");
            stmt.executeUpdate(sqlString);

            stmt.close();
            conn.close();

        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());

        }

    }

    public void tryExecuteLocalSql(String sqlString,PostgresqlServer postgresqlServer)
            throws Exception {

        log.info("sql executing:" + sqlString);

        Connection conn = DriverManager.getConnection("jdbc:postgresql://"+postgresqlServer.getServerAddress()+"/postgres",
                username, password);
        Statement stmt = conn.createStatement();

        Class.forName("org.postgresql.Driver");

        stmt.execute(sqlString);

        stmt.close();
        conn.close();


    }

    public PgVersion getPgVersion(PostgresqlServer postgresqlServer) {

        String versionText = "";
        // default
        PgVersion result = PgVersion.V10X;
        try (Connection conn = DriverManager.getConnection("jdbc:postgresql://"+postgresqlServer.getServerAddress() + "/postgres",
                username, password)) {
            Statement stmt = conn.createStatement();

            Class.forName("org.postgresql.Driver");

            ResultSet resultSet = stmt.executeQuery("SELECT version()");

            if (resultSet.next()) {
                versionText = resultSet.getString(1);
            }
            resultSet.close();
            stmt.close();
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getClass().getName() + ": " + e.getMessage());

        }finally {

        }
        if (versionText.startsWith("PostgreSQL 9.")) {
            result = PgVersion.V9X;
        } else if (versionText.startsWith("PostgreSQL 10.")) {
            result = PgVersion.V10X;
        } else if (versionText.startsWith("PostgreSQL 11.")) {
            result = PgVersion.V11X;
        } else if (versionText.startsWith("PostgreSQL 12.")) {
            result = PgVersion.V12X;
        }else if (versionText.startsWith("PostgreSQL 13.")) {
            result = PgVersion.V13X;
        }else if (versionText.startsWith("PostgreSQL 14.")) {
            result = PgVersion.V14X;
        }
        return result;
    }

}
