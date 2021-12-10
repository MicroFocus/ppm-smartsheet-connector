
/*
 * © Copyright 2019 - 2020 Micro Focus or one of its affiliates.
 */

package com.ppm.integration.agilesdk.connector.smartsheet.rest;


import com.kintana.core.logging.LogLevel;
import com.kintana.core.logging.LogManager;
import com.kintana.core.logging.Logger;
import org.apache.wink.client.ClientConfig;

public class SmartsheetRestConfig {

    private final static Logger logger = LogManager.getLogger(SmartsheetRestConfig.class);

    private ClientConfig clientConfig;

    private String authToken;

    public ClientConfig getClientConfig() {
        return clientConfig;
    }

    public SmartsheetRestConfig() {
        clientConfig = new ClientConfig();
    }


    public ClientConfig setProxy(String proxyHost, String proxyPort) {

        if (SmartsheetRestClient.ENABLE_REST_CALLS_STATUS_LOG) {
            logger.log(LogLevel.STATUS, "Setting proxy: " + proxyHost +":"+proxyPort );
        }

        if (proxyHost != null && !proxyHost.isEmpty() && proxyPort != null && !proxyPort.isEmpty()) {
            clientConfig.proxyHost(proxyHost);
            clientConfig.proxyPort(Integer.parseInt(proxyPort));
        }
        return clientConfig;
    }


    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

}
