package com.bisoft.bfm;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.stereotype.Component;

import com.bisoft.bfm.model.Configuration;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Data
@Slf4j
@RequiredArgsConstructor
public class ConfigurationManager {

    private Configuration configuration;   

    @PostConstruct
    public Configuration loadConf() {
        Path confFilePath = Paths.get("./configuration.json");
        if (Files.exists(confFilePath)) {
            try {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                JsonReader reader;
                reader = new JsonReader(new FileReader("./configuration.json"));
                this.configuration = gson.fromJson(reader, Configuration.class);
            } catch (FileNotFoundException e) {
                // e.printStackTrace();
                return null;
            }                
        } else {
            this.initConf();
        }
        return this.configuration;

    }

    public String initConf() {

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        this.configuration = new Configuration("BFM-4 Cluster",9994, "no-pair", "bfm", "bfm", false, 3,
                "127.0.0.1:5432", "manual", false,"bfm", false, "b1s0ft14","PKCS12","bfm.p12",false,false,"minipg","minipg",
                7779,10,"select 1","120K","1H","rejoin", true, "user1@example.com,user2@example.com","smtp.gmail.com",587,"smtpuser@gmail.com",
                "smtpPassword",true,true,false);

        String json_str = gson.toJson(this.configuration);
        PrintWriter out;
        try {
            out = new PrintWriter("./configuration.json");
            out.println(json_str);
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            log.warn("Configuration init error...");
        }

        return "OK";
    }

    public String saveConfig() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json_str = gson.toJson(this.configuration);
        PrintWriter out;
        try {
            out = new PrintWriter("./configuration.json");
            out.println(json_str);
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            log.warn("Configuration init error...");
        }
        return "OK";
    }
}
