package com.bisoft.bfm.helper;

import java.io.IOException;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.TrustSelfSignedStrategy;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.bisoft.bfm.dto.PromoteDTO;
import com.bisoft.bfm.dto.ReBaseUpDTO;
import com.bisoft.bfm.dto.RewindDTO;
import com.bisoft.bfm.model.PostgresqlServer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@Data
@RequiredArgsConstructor
public class MinipgAccessUtil {

    private final SymmetricEncryptionUtil symmetricEncryptionUtil;

    @Value("${minipg.username:postgres}")
    private String username;

    @Value("${minipg.password:bfm}")
    private String password;

    @Value("${minipg.port:9995}")
    private int port;

    @Value("${minipg.timeout:5}")
    private int minipgTimeout;

    @Value("${minipg.use-tls:false}")
    private boolean useTls;

    private String serverUrl;

    @Value("${bfm.user-crypted:false}")
    public boolean isEncrypted;

    private SSLContext sslContext;


    @PostConstruct
    public void init() throws Exception {
        final String scheme = useTls == false?"http":"https";
        serverUrl = scheme+"://{HOST}:"+String.valueOf(port);
        if(isEncrypted) {
            //  log.info(symmetricEncryptionUtil.decrypt(tlsSecret).replace("=",""));
            password = (symmetricEncryptionUtil.decrypt(password).replace("=", ""));
        }
        SSLContext sslContext = SSLContext.getInstance("TLS");
        var trustManager = new X509TrustManager() {
            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[]{};
            }

            @Override
            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
        };
        sslContext.init(null, new TrustManager[]{trustManager}, new SecureRandom());
        this.sslContext = sslContext;
    }

    private static final String getBasicAuthenticationHeader(String username, String password) {
        String valueToEncode = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(valueToEncode.getBytes());
    }

    public String status(PostgresqlServer postgresqlServer) throws Exception{

        try{
            final String serverAddress = postgresqlServer.getServerAddress().split(":")[0];
            String minipgUrl = serverUrl.replace("{HOST}",serverAddress);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(minipgUrl+"/minipg/pgstatus"))
                .timeout(Duration.ofSeconds(this.minipgTimeout))
                .header("Authorization", getBasicAuthenticationHeader(username, password))
                .GET()
                .build();

            HttpResponse<String> response = HttpClient
                .newBuilder()
                .sslContext(this.sslContext)
                .proxy(ProxySelector.getDefault())
                .build()
                .send(request, BodyHandlers.ofString());
            return response.body();
            
        }catch(Exception e){
            log.error("Unable get status of server "+postgresqlServer.getServerAddress());
            log.error("Minipg could be down at  "+postgresqlServer.getServerAddress());
            log.error("Check Minipg status at  "+postgresqlServer.getServerAddress());
        }
        return "OK";

    }

    public String vipUp(PostgresqlServer postgresqlServer) throws Exception{
        //log.info("username : "+username+", password : "+password);
        log.info("vip up sent to "+postgresqlServer.getServerAddress());
        final String serverAddress = postgresqlServer.getServerAddress().split(":")[0];
        String minipgUrl = serverUrl.replace("{HOST}",serverAddress);
        final BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
                new AuthScope(serverAddress, port),
                new UsernamePasswordCredentials(username, password.toCharArray()));
        SSLConnectionSocketFactory scsf = new SSLConnectionSocketFactory(
                SSLContexts.custom().loadTrustMaterial(null, new TrustSelfSignedStrategy()).build(),
                NoopHostnameVerifier.INSTANCE);
        final HttpClientConnectionManager cm = PoolingHttpClientConnectionManagerBuilder.create()
                .setSSLSocketFactory(scsf)
                .build();

        try (CloseableHttpClient httpclient = HttpClients.custom()
                .setConnectionManager(cm)
                .setDefaultCredentialsProvider(credsProvider)
                .build()) {

            HttpGet httpGet = new HttpGet(minipgUrl+"/minipg/vip-up");

            try (CloseableHttpResponse response1 = httpclient.execute(httpGet)) {
                // log.info(response1.getCode() + " " + response1.getReasonPhrase());
                HttpEntity entity1 = (HttpEntity) response1.getEntity();
                // do something useful with the response body
                // and ensure it is fully consumed
                String result = (EntityUtils.toString(response1.getEntity()));
                return result;
            }catch (Exception e){
                log.error("Unable set vip to server "+postgresqlServer.getServerAddress());
            }


        } catch (IOException e) {
            log.error("Unable set vip to server "+postgresqlServer.getServerAddress());
        }

        return "OK";

    }

    public String vipDown(PostgresqlServer postgresqlServer) throws Exception{
        //log.info("username : "+username+", password : "+password);
        log.info("vip down sent to "+postgresqlServer.getServerAddress());
        final String serverAddress = postgresqlServer.getServerAddress().split(":")[0];
        String minipgUrl = serverUrl.replace("{HOST}",serverAddress);
        final BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
                new AuthScope(serverAddress, port),
                new UsernamePasswordCredentials(username, password.toCharArray()));

        SSLConnectionSocketFactory scsf = new SSLConnectionSocketFactory(
                SSLContexts.custom().loadTrustMaterial(null, new TrustSelfSignedStrategy()).build(),
                NoopHostnameVerifier.INSTANCE);

        final HttpClientConnectionManager cm = PoolingHttpClientConnectionManagerBuilder.create()
                .setSSLSocketFactory(scsf)
                .build();

        try (CloseableHttpClient httpclient = HttpClients.custom()
                .setConnectionManager(cm)
                .setDefaultCredentialsProvider(credsProvider)
                .build()) {

            HttpGet httpGet = new HttpGet(minipgUrl+"/minipg/vip-down");

            //timeout if server is shutdown
            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectTimeout(Timeout.of(5, TimeUnit.SECONDS))
                    .setConnectionRequestTimeout(Timeout.of(5, TimeUnit.SECONDS))
                    .build();

            httpGet.setConfig(requestConfig);

            try (CloseableHttpResponse response1 = httpclient.execute(httpGet)) {
                // log.info(response1.getCode() + " " + response1.getReasonPhrase());
                HttpEntity entity1 = (HttpEntity) response1.getEntity();
                // do something useful with the response body
                // and ensure it is fully consumed
                String result = (EntityUtils.toString(response1.getEntity()));
                return result;
            }catch (Exception e){
                log.error("Unable get vip from server "+postgresqlServer.getServerAddress());
            }


        } catch (IOException e) {
            log.error("Unable get vip from server "+postgresqlServer.getServerAddress());
        }

        return "OK";

    }

    public String checkpoint(PostgresqlServer postgresqlServer) throws JsonProcessingException {
        //log.info("username : "+username+", password : "+password);
        final String serverAddress = postgresqlServer.getServerAddress().split(":")[0];
        final String serverPort = postgresqlServer.getServerAddress().split(":")[1];
        String minipgUrl = serverUrl.replace("{HOST}",serverAddress);
        final BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();

        //CheckPointDTO cpdto = CheckPointDTO.builder().user(serverAddress).port(serverPort).password().build();

        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        //String json = ow.writeValueAsString(cpdto);

        credsProvider.setCredentials(
                new AuthScope(serverAddress, port),
                new UsernamePasswordCredentials(username, password.toCharArray()));

        try (CloseableHttpClient httpclient = HttpClients.custom()
                .setDefaultCredentialsProvider(credsProvider)
                .build()) {

            HttpGet httpGet = new HttpGet(minipgUrl+"/minipg/checkpoint");

            try (CloseableHttpResponse response1 = httpclient.execute(httpGet)) {
                // log.info(response1.getCode() + " " + response1.getReasonPhrase());
                HttpEntity entity1 = (HttpEntity) response1.getEntity();
                // do something useful with the response body
                // and ensure it is fully consumed
                String result = (EntityUtils.toString(response1.getEntity()));
                return result;
            }catch (Exception e){
                log.error("Unable perform checkpoint in server "+postgresqlServer.getServerAddress());
            }


        } catch (IOException e) {
            log.error("Unable perform checkpoint in server "+postgresqlServer.getServerAddress());
        }

        return "OK";

    }

    public String promote(PostgresqlServer postgresqlServer) throws Exception {
        //log.info("username : "+username+", password : "+password);
        log.info("promote sent to "+postgresqlServer.getServerAddress());
        final String serverAddress = postgresqlServer.getServerAddress().split(":")[0];
        final String serverPort = postgresqlServer.getServerAddress().split(":")[1];
        String minipgUrl = serverUrl.replace("{HOST}",serverAddress);
        final BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();

        SSLConnectionSocketFactory scsf = new SSLConnectionSocketFactory(
                SSLContexts.custom().loadTrustMaterial(null, new TrustSelfSignedStrategy()).build(),
                NoopHostnameVerifier.INSTANCE);
        final HttpClientConnectionManager cm = PoolingHttpClientConnectionManagerBuilder.create()
                .setSSLSocketFactory(scsf)
                .build();

        PromoteDTO promoteDTO = PromoteDTO.builder().masterIp(postgresqlServer.getServerAddress().split(":")[0]).port(postgresqlServer.getServerAddress().split(":")[1]).user(postgresqlServer.getUsername()).password(postgresqlServer.getPassword()).build();

        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        String json = ow.writeValueAsString(promoteDTO);

        credsProvider.setCredentials(
                new AuthScope(serverAddress, port),
                new UsernamePasswordCredentials(username, password.toCharArray()));

        try (CloseableHttpClient httpclient = HttpClients.custom()
                .setConnectionManager(cm)
                .setDefaultCredentialsProvider(credsProvider)
                .build()) {

            HttpPost request = new HttpPost(minipgUrl+"/minipg/promote");
            request.setHeader("Accept", "application/json");
            request.setHeader("Content-type", "application/json");

            final StringEntity entity = new StringEntity(json);
            request.setEntity(entity);

            try (CloseableHttpResponse response1 = httpclient.execute(request)) {
                // log.info(response1.getCode() + " " + response1.getReasonPhrase());
                HttpEntity entity1 = (HttpEntity) response1.getEntity();
                // do something useful with the response body
                // and ensure it is fully consumed
                String result = (EntityUtils.toString(response1.getEntity()));
                return result;
            }catch (Exception e){
                log.error("Promote failed for "+postgresqlServer.getServerAddress());
            }


        } catch (IOException e) {
            log.error("Promote failed "+postgresqlServer.getServerAddress()+" is unreacable");
        }

        return "OK";

    }

    public String rewind(PostgresqlServer postgresqlServer,PostgresqlServer newMaster) throws Exception {
        //log.info("username : "+username+", password : "+password);
        log.info("rewind sent to "+postgresqlServer.getServerAddress()+" for master "+newMaster.getServerAddress());
        // String targetMiniPGstatus = minipgStatus(postgresqlServer);
        // log.info(postgresqlServer.getServerAddress() + " minipg status :"+ targetMiniPGstatus);

        // String masterMiniPGstatus = minipgStatus(newMaster);
        // log.info(newMaster.getServerAddress() + " minipg status :"+ masterMiniPGstatus);

        final String serverAddress = postgresqlServer.getServerAddress().split(":")[0];
        final String serverPort = postgresqlServer.getServerAddress().split(":")[1];
        String minipgUrl = serverUrl.replace("{HOST}",serverAddress);
        final BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();

        SSLConnectionSocketFactory scsf = new SSLConnectionSocketFactory(
                SSLContexts.custom().loadTrustMaterial(null, new TrustSelfSignedStrategy()).build(),
                NoopHostnameVerifier.INSTANCE);
        final HttpClientConnectionManager cm = PoolingHttpClientConnectionManagerBuilder.create()
                .setSSLSocketFactory(scsf)
                .build();

        RewindDTO rewindDTO = RewindDTO.builder().masterIp(newMaster.getServerAddress().split(":")[0]).port(newMaster.getServerAddress().split(":")[1]).user(postgresqlServer.getUsername()).password(postgresqlServer.getPassword()).build();

        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        String json = ow.writeValueAsString(rewindDTO);

        credsProvider.setCredentials(
                new AuthScope(serverAddress, port),
                new UsernamePasswordCredentials(username, password.toCharArray()));

        try (CloseableHttpClient httpclient = HttpClients.custom()
                .setConnectionManager(cm)
                .setDefaultCredentialsProvider(credsProvider)
                .build()) {

            HttpPost request = new HttpPost(minipgUrl+"/minipg/rewind");
            request.setHeader("Accept", "application/json");
            request.setHeader("Content-type", "application/json");

            final StringEntity entity = new StringEntity(json);
            request.setEntity(entity);

            try (CloseableHttpResponse response1 = httpclient.execute(request)) {
                // log.info(response1.getCode() + " " + response1.getReasonPhrase());
                HttpEntity entity1 = (HttpEntity) response1.getEntity();
                // do something useful with the response body
                // and ensure it is fully consumed
                String result = (EntityUtils.toString(response1.getEntity()));
                return result;
            }catch (Exception e){
                log.error("Rewind failed for "+postgresqlServer.getServerAddress());
                log.info("minipg call error :"+e.getMessage());
            }


        } catch (IOException e) {
            log.error("Rewind failed "+postgresqlServer.getServerAddress()+" is unreacable");
        }

        return "OK";

    }

    public String rebaseUp(PostgresqlServer slaveCandidateServer,PostgresqlServer masterServer) throws Exception {
        // log.info("Trying to Rejoin Slave server "+slaveCandidateServer.getServerAddress()+" to cluster with master "+masterServer.getServerAddress()+" with pg_basebackup.");
        final String serverAddress = slaveCandidateServer.getServerAddress().split(":")[0];
        final String serverPort = slaveCandidateServer.getServerAddress().split(":")[1];
        String minipgUrl = serverUrl.replace("{HOST}",serverAddress);
        final BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();

        SSLConnectionSocketFactory scsf = new SSLConnectionSocketFactory(
                SSLContexts.custom().loadTrustMaterial(null, new TrustSelfSignedStrategy()).build(),
                NoopHostnameVerifier.INSTANCE);
        final HttpClientConnectionManager cm = PoolingHttpClientConnectionManagerBuilder.create()
                .setSSLSocketFactory(scsf)
                .build();

        ReBaseUpDTO rebaseDTO = ReBaseUpDTO.builder().masterIp(masterServer.getServerAddress().split(":")[0]).masterPort(masterServer.getServerAddress().split(":")[1]).repUser(masterServer.getUsername()).repPassword(masterServer.getPassword()).build();
        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        String json = ow.writeValueAsString(rebaseDTO);

        credsProvider.setCredentials(
                new AuthScope(serverAddress, port),
                new UsernamePasswordCredentials(username, password.toCharArray()));

        try (CloseableHttpClient httpclient = HttpClients.custom()
                .setConnectionManager(cm)
                .setDefaultCredentialsProvider(credsProvider)
                .build()) {

            HttpPost request = new HttpPost(minipgUrl+"/minipg/rebaseUp");
            request.setHeader("Accept", "application/json");
            request.setHeader("Content-type", "application/json");

            final StringEntity entity = new StringEntity(json);
            request.setEntity(entity);

            try (CloseableHttpResponse response1 = httpclient.execute(request)) {
                // log.info(response1.getCode() + " " + response1.getReasonPhrase());
                HttpEntity entity1 = (HttpEntity) response1.getEntity();
                // do something useful with the response body
                // and ensure it is fully consumed
                String result = (EntityUtils.toString(response1.getEntity()));
                return result;
            }catch (Exception e){
                log.error("Rejoin with pg_basebackup failed for "+ slaveCandidateServer.getServerAddress());
            }


        } catch (IOException e) {
            log.error("Rejoin with pg_basebackup failed for "+slaveCandidateServer.getServerAddress()+" is unreacable");
        }
        
        return "OK";
    }

    public String stopPg(PostgresqlServer pgServer) throws Exception {
        final String serverAddress = pgServer.getServerAddress().split(":")[0];
        final String serverPort = pgServer.getServerAddress().split(":")[1];
        String minipgUrl = serverUrl.replace("{HOST}",serverAddress);
        final BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();

        SSLConnectionSocketFactory scsf = new SSLConnectionSocketFactory(
                SSLContexts.custom().loadTrustMaterial(null, new TrustSelfSignedStrategy()).build(),
                NoopHostnameVerifier.INSTANCE);
        final HttpClientConnectionManager cm = PoolingHttpClientConnectionManagerBuilder.create()
                .setSSLSocketFactory(scsf)
                .build();

        credsProvider.setCredentials(
                new AuthScope(serverAddress, port),
                new UsernamePasswordCredentials(username, password.toCharArray()));

        try (CloseableHttpClient httpclient = HttpClients.custom()
                .setConnectionManager(cm)
                .setDefaultCredentialsProvider(credsProvider)
                .build()) {

            HttpGet request = new HttpGet(minipgUrl+"/minipg/stop");
            // request.setHeader("Accept", "application/json");
            // request.setHeader("Content-type", "application/json");

            try (CloseableHttpResponse response1 = httpclient.execute(request)) {
                log.info(response1.getCode() + " " + response1.getReasonPhrase());
                HttpEntity entity1 = (HttpEntity) response1.getEntity();
                // do something useful with the response body
                // and ensure it is fully consumed
                String result = (EntityUtils.toString(response1.getEntity()));
                return result;
            }catch (Exception e){
                log.error("Error on Stop DB:"+pgServer.getServerAddress());
                // log.error(e.getMessage);
            }


        } catch (IOException e) {
            log.error("Error on stop db. Server: "+pgServer.getServerAddress()+" is unreacable");
        }
        
        return "OK";
    }

    public String startPg(PostgresqlServer pgServer) throws Exception {
        String result = "";
        final String serverAddress = pgServer.getServerAddress().split(":")[0];
        final String serverPort = pgServer.getServerAddress().split(":")[1];
        String minipgUrl = serverUrl.replace("{HOST}",serverAddress);
        final BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();

        SSLConnectionSocketFactory scsf = new SSLConnectionSocketFactory(
                SSLContexts.custom().loadTrustMaterial(null, new TrustSelfSignedStrategy()).build(),
                NoopHostnameVerifier.INSTANCE);
        final HttpClientConnectionManager cm = PoolingHttpClientConnectionManagerBuilder.create()
                .setSSLSocketFactory(scsf)
                .build();

        credsProvider.setCredentials(
                new AuthScope(serverAddress, port),
                new UsernamePasswordCredentials(username, password.toCharArray()));

        try (CloseableHttpClient httpclient = HttpClients.custom()
                .setConnectionManager(cm)
                .setDefaultCredentialsProvider(credsProvider)
                .build()) {

            HttpGet request = new HttpGet(minipgUrl+"/minipg/start");
            // request.setHeader("Accept", "application/json");
            // request.setHeader("Content-type", "application/json");

            try (CloseableHttpResponse response1 = httpclient.execute(request)) {
                // log.info(response1.getCode() + " " + response1.getReasonPhrase());
                HttpEntity entity1 = (HttpEntity) response1.getEntity();
                // do something useful with the response body
                // and ensure it is fully consumed
                result = (EntityUtils.toString(response1.getEntity()));
            }catch (Exception e){
                log.error("Error on Start DB:"+pgServer.getServerAddress());
                // log.error(e.getMessage);
            }


        } catch (IOException e) {
            log.error("Error on start db. Server: "+pgServer.getServerAddress()+" is unreacable");
        }
        
        if (result.contains("done") && result.contains("server started")){
            return "OK";
        } else {
            return result;
        }        
    }

    public String postSwitchOver(PostgresqlServer old_master, PostgresqlServer new_master)throws Exception {
        //log.info("username : "+username+", password : "+password);
        log.info("Post switchOver starting for server :"+old_master.getServerAddress());
        final String serverAddress = old_master.getServerAddress().split(":")[0];
        final String serverPort = old_master.getServerAddress().split(":")[1];
        String minipgUrl = serverUrl.replace("{HOST}",serverAddress);
        final BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();

        SSLConnectionSocketFactory scsf = new SSLConnectionSocketFactory(
                SSLContexts.custom().loadTrustMaterial(null, new TrustSelfSignedStrategy()).build(),
                NoopHostnameVerifier.INSTANCE);
        final HttpClientConnectionManager cm = PoolingHttpClientConnectionManagerBuilder.create()
                .setSSLSocketFactory(scsf)
                .build();

        PromoteDTO promoteDTO = PromoteDTO.builder()
                                .masterIp(new_master.getServerAddress().split(":")[0])
                                .port(new_master.getServerAddress().split(":")[1])
                                .user(new_master.getUsername())
                                .password(new_master.getPassword()).build();

        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        String json = ow.writeValueAsString(promoteDTO);

        credsProvider.setCredentials(
                new AuthScope(serverAddress, port),
                new UsernamePasswordCredentials(username, password.toCharArray()));

        try (CloseableHttpClient httpclient = HttpClients.custom()
                .setConnectionManager(cm)
                .setDefaultCredentialsProvider(credsProvider)
                .build()) {

            HttpPost request = new HttpPost(minipgUrl+"/minipg/post-so");
            request.setHeader("Accept", "application/json");
            request.setHeader("Content-type", "application/json");

            final StringEntity entity = new StringEntity(json);
            request.setEntity(entity);

            try (CloseableHttpResponse response1 = httpclient.execute(request)) {
                log.info(response1.getCode() + " " + response1.getReasonPhrase());
                HttpEntity entity1 = (HttpEntity) response1.getEntity();
                // do something useful with the response body
                // and ensure it is fully consumed
                String result = (EntityUtils.toString(response1.getEntity()));
                return result;
            }catch (Exception e){
                log.error("Unable get post switch over from server "+old_master.getServerAddress());
            }
        } catch (IOException e) {
            log.error("Unable get post switch over from server "+old_master.getServerAddress());
        }
        return "OK";
    }

    public String prepareForSwitchOver(PostgresqlServer old_master) throws Exception{
        final String serverAddress = old_master.getServerAddress().split(":")[0];
        String minipgUrl = serverUrl.replace("{HOST}",serverAddress);
        final BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
                new AuthScope(serverAddress, port),
                new UsernamePasswordCredentials(username, password.toCharArray()));

        SSLConnectionSocketFactory scsf = new SSLConnectionSocketFactory(
                SSLContexts.custom().loadTrustMaterial(null, new TrustSelfSignedStrategy()).build(),
                NoopHostnameVerifier.INSTANCE);

        final HttpClientConnectionManager cm = PoolingHttpClientConnectionManagerBuilder.create()
                .setSSLSocketFactory(scsf)
                .build();

        try (CloseableHttpClient httpclient = HttpClients.custom()
                .setConnectionManager(cm)
                .setDefaultCredentialsProvider(credsProvider)
                .build()) {

            HttpGet httpGet = new HttpGet(minipgUrl+"/minipg/pre-so");

            //timeout if server is shutdown
            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectTimeout(Timeout.of(5, TimeUnit.SECONDS))
                    .setConnectionRequestTimeout(Timeout.of(5, TimeUnit.SECONDS))
                    .build();

            httpGet.setConfig(requestConfig);

            try (CloseableHttpResponse response1 = httpclient.execute(httpGet)) {
                log.info(response1.getCode() + " " + response1.getReasonPhrase());
                HttpEntity entity1 = (HttpEntity) response1.getEntity();
                // do something useful with the response body
                // and ensure it is fully consumed
                String result = (EntityUtils.toString(response1.getEntity()));
                return result;
            }catch (Exception e){
                log.error("Prepare for SwitchOver failed for "+old_master.getServerAddress());
            }
        } catch (IOException e) {
            log.error("Promote failed "+old_master.getServerAddress()+" is unreacable");
        }

        return "OK";

    }


    public String minipgStatus(PostgresqlServer postgresqlServer) throws Exception{
        //log.info("username : "+username+", password : "+password);
        log.info("MiniPg status request sent to "+postgresqlServer.getServerAddress());
        final String serverAddress = postgresqlServer.getServerAddress().split(":")[0];
        String minipgUrl = serverUrl.replace("{HOST}",serverAddress);
        final BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
                new AuthScope(serverAddress, port),
                new UsernamePasswordCredentials(username, password.toCharArray()));

        SSLConnectionSocketFactory scsf = new SSLConnectionSocketFactory(
                SSLContexts.custom().loadTrustMaterial(null, new TrustSelfSignedStrategy()).build(),
                NoopHostnameVerifier.INSTANCE);

        final HttpClientConnectionManager cm = PoolingHttpClientConnectionManagerBuilder.create()
                .setSSLSocketFactory(scsf)
                .build();

        try (CloseableHttpClient httpclient = HttpClients.custom()
                .setConnectionManager(cm)
                .setDefaultCredentialsProvider(credsProvider)
                .build()) {

            HttpGet httpGet = new HttpGet(minipgUrl+"/minipg/status");

            //timeout if server is shutdown
            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectTimeout(Timeout.of(5, TimeUnit.SECONDS))
                    .setConnectionRequestTimeout(Timeout.of(5, TimeUnit.SECONDS))
                    .build();

            httpGet.setConfig(requestConfig);

            try (CloseableHttpResponse response1 = httpclient.execute(httpGet)) {
                // log.info(response1.getCode() + " " + response1.getReasonPhrase());
                HttpEntity entity1 = (HttpEntity) response1.getEntity();
                // do something useful with the response body
                // and ensure it is fully consumed
                String result = (EntityUtils.toString(response1.getEntity()));
                return result;
            }catch (Exception e){
                log.error("Unable get vip from server "+postgresqlServer.getServerAddress());
            }


        } catch (IOException e) {
            log.error("Unable get vip from server "+postgresqlServer.getServerAddress());
        }

        return null;

    }

}
