package com.bisoft.bfm.helper;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PostConstruct;

import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.classic.methods.HttpGet;
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
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.bisoft.bfm.ConfigurationManager;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@Data
@RequiredArgsConstructor
public class BfmAccessUtil {

    @Autowired
    private ConfigurationManager configurationManager;

    private final SymmetricEncryptionUtil symmetricEncryptionUtil;

    private String port;
    private String serverUrl;

    @PostConstruct
    public void init(){
        if(!this.configurationManager.getConfiguration().getBfmPair().equals("no-pair")) {
            port = this.configurationManager.getConfiguration().getBfmPair().split(":")[1];
        }
        String scheme = this.configurationManager.getConfiguration().getBfmUseTls() == false?"http":"https";
        serverUrl = scheme+"://"+this.configurationManager.getConfiguration().getBfmPair();
        if(this.configurationManager.getConfiguration().getIsEncrypted()) {
            //  log.info(symmetricEncryptionUtil.decrypt(tlsSecret).replace("=",""));
            this.configurationManager.getConfiguration().setPgPassword(symmetricEncryptionUtil.decrypt(this.configurationManager.getConfiguration().getPgPassword()).replace("=", ""));
        }
    }

    public String isPairAlive() throws Exception{

        if(this.configurationManager.getConfiguration().getBfmPair().equals("no-pair")){
            return this.configurationManager.getConfiguration().getBfmPair();
        }

        SSLConnectionSocketFactory scsf = new SSLConnectionSocketFactory(
                SSLContexts.custom().loadTrustMaterial(null, new TrustSelfSignedStrategy()).build(),
                NoopHostnameVerifier.INSTANCE);

        final HttpClientConnectionManager cm = PoolingHttpClientConnectionManagerBuilder.create()
                .setSSLSocketFactory(scsf)
                .build();

        final String serverAddress = this.configurationManager.getConfiguration().getBfmPair().split(":")[0];
        String bfmUrl = serverUrl;
        final BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
                new AuthScope(serverAddress, Integer.valueOf(port)),
                new UsernamePasswordCredentials(this.configurationManager.getConfiguration().getPgUsername(), this.configurationManager.getConfiguration().getPgPassword().toCharArray()));

        try (CloseableHttpClient httpclient = HttpClients.custom()
                .setConnectionManager(cm)
                .setDefaultCredentialsProvider(credsProvider)
                .build()) {

            HttpGet httpGet = new HttpGet(bfmUrl+"/bfm/is-alive");
           // httpGet.setScheme("https");

            //timeout if server is shutdown
            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectTimeout(Timeout.of(10, TimeUnit.SECONDS))
                    .setConnectionRequestTimeout(Timeout.of(10, TimeUnit.SECONDS))
                    .build();

            SocketConfig socketConfig = SocketConfig.custom()
                    .setSoKeepAlive(false)
                    .setSoReuseAddress(true)
                    .setSoTimeout(Timeout.of(10,TimeUnit.SECONDS))
                    .setTcpNoDelay(true).build();

            httpGet.setConfig(requestConfig);

            try (CloseableHttpResponse response1 = httpclient.execute(httpGet)) {
                HttpEntity entity1 = (HttpEntity) response1.getEntity();
                if(response1.getCode() != 200){
                    throw new Exception("Not Healthy");
                }

                String result = (EntityUtils.toString(response1.getEntity()));
                return result;
            }catch (Exception e){
                // log.warn("pair is active error"+e.getMessage());
                return "Unreachable";
            }


        } catch (IOException e) {
            // log.warn("pair is active error"+e.getMessage());
            return "Unreachable";
        }


    }

    public String getLastSavedStatus() throws Exception{

        if(this.configurationManager.getConfiguration().getBfmPair().equals("no-pair")){
            return this.configurationManager.getConfiguration().getBfmPair();
        }

        SSLConnectionSocketFactory scsf = new SSLConnectionSocketFactory(
                SSLContexts.custom().loadTrustMaterial(null, new TrustSelfSignedStrategy()).build(),
                NoopHostnameVerifier.INSTANCE);

        final HttpClientConnectionManager cm = PoolingHttpClientConnectionManagerBuilder.create()
                .setSSLSocketFactory(scsf)
                .build();

        final String serverAddress = this.configurationManager.getConfiguration().getBfmPair().split(":")[0];
        String bfmUrl = serverUrl;
        final BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
                new AuthScope(serverAddress, Integer.valueOf(port)),
                new UsernamePasswordCredentials(this.configurationManager.getConfiguration().getPgUsername(), this.configurationManager.getConfiguration().getPgPassword().toCharArray()));

        try (CloseableHttpClient httpclient = HttpClients.custom()
                .setConnectionManager(cm)
                .setDefaultCredentialsProvider(credsProvider)
                .build()) {

            HttpGet httpGet = new HttpGet(bfmUrl+"/bfm/last-saved-stat");
           // httpGet.setScheme("https");

            //timeout if server is shutdown
            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectTimeout(Timeout.of(10, TimeUnit.SECONDS))
                    .setConnectionRequestTimeout(Timeout.of(10, TimeUnit.SECONDS))
                    .build();

            SocketConfig socketConfig = SocketConfig.custom()
                    .setSoKeepAlive(false)
                    .setSoReuseAddress(true)
                    .setSoTimeout(Timeout.of(10,TimeUnit.SECONDS))
                    .setTcpNoDelay(true).build();

            httpGet.setConfig(requestConfig);

            try (CloseableHttpResponse response1 = httpclient.execute(httpGet)) {
                HttpEntity entity1 = (HttpEntity) response1.getEntity();
                if(response1.getCode() != 200){
                    throw new Exception("Not Healthy");
                }

                String result = (EntityUtils.toString(response1.getEntity()));
                return result;
            }catch (Exception e){
                return "Unreachable";
            }


        } catch (IOException e) {
            return "Unreachable";
        }


    }

}
