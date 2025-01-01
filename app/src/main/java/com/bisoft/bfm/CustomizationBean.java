package com.bisoft.bfm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.server.Ssl;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.stereotype.Component;
import com.bisoft.bfm.helper.SymmetricEncryptionUtil;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Data
@Slf4j
@RequiredArgsConstructor
public class CustomizationBean implements
        WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> {

    private final SymmetricEncryptionUtil symmetricEncryptionUtil;
    
    @Autowired
    private ConfigurationManager configurationManager;

    @Override
    public void customize(ConfigurableServletWebServerFactory container) {
        this.configurationManager.loadConf();
        container.setPort(this.configurationManager.getConfiguration().getClusterPort());

        if (this.configurationManager.getConfiguration().getUseSsl()) {
            Ssl ssl = new Ssl();
            ssl.setEnabled(true);
            if (this.configurationManager.getConfiguration().getIsEncrypted()) {
                //log.info(symmetricEncryptionUtil.decrypt(tlsSecret).replace("=", ""));
                ssl.setKeyStorePassword(symmetricEncryptionUtil.decrypt(this.configurationManager.getConfiguration().getTlsSecret()).replace("=", ""));
            } else {
                ssl.setKeyStorePassword(this.configurationManager.getConfiguration().getTlsSecret());
            }
            ssl.setKeyAlias(this.configurationManager.getConfiguration().getTlsKeyAlias());
            ssl.setKeyStoreType(this.configurationManager.getConfiguration().getTlsKeyStoreType());
            ssl.setKeyStore(this.configurationManager.getConfiguration().getTlsKeyStore());
            ssl.setEnabledProtocols(new String[]{"TLSv1.2"});

            //container.set
            container.setSsl(ssl);
        }
    }
}
