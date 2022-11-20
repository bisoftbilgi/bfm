package com.bisoft.bfm.helper;

import com.bisoft.bfm.model.PostgresqlServer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
@Data
@RequiredArgsConstructor
public class BfmAccessUtil {

    private final SymmetricEncryptionUtil symmetricEncryptionUtil;

    @Value("${server.pguser:postgres}")
    private String username;

    @Value("${server.pgpassword:postgres}")
    private String password;


    @Value("${bfm.use-tls:false}")
    private boolean useTls;

    private String port;

    @Value("${watcher.cluster-pair:no-pair}")
    private String bfmPair;

    private String serverUrl;

    @Value("${bfm.user-crypted:false}")
    public boolean isEncrypted;



    @PostConstruct
    public void init(){
        if(!bfmPair.equals("no-pair")) {
            port = bfmPair.split(":")[1];
        }
        String scheme = useTls == false?"http":"https";
        serverUrl = scheme+"://"+bfmPair;
        if(isEncrypted) {
            //  log.info(symmetricEncryptionUtil.decrypt(tlsSecret).replace("=",""));
            password = (symmetricEncryptionUtil.decrypt(password).replace("=", ""));
        }
    }

    public String isPairAlive() throws Exception{

        if(bfmPair.equals("no-pair")){
            return bfmPair;
        }

        SSLConnectionSocketFactory scsf = new SSLConnectionSocketFactory(
                SSLContexts.custom().loadTrustMaterial(null, new TrustSelfSignedStrategy()).build(),
                NoopHostnameVerifier.INSTANCE);

        final HttpClientConnectionManager cm = PoolingHttpClientConnectionManagerBuilder.create()
                .setSSLSocketFactory(scsf)
                .build();

        final String serverAddress = bfmPair.split(":")[0];
        String bfmUrl = serverUrl;
        final BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
                new AuthScope(serverAddress, Integer.valueOf(port)),
                new UsernamePasswordCredentials(username, password.toCharArray()));

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
