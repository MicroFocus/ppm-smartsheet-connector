/*
 * © Copyright 2019 - 2020 Micro Focus or one of its affiliates.
 */

package com.ppm.integration.agilesdk.connector.smartsheet.service;

import com.ppm.integration.agilesdk.ValueSet;
import com.ppm.integration.agilesdk.connector.smartsheet.SmartsheetConstants;
import com.ppm.integration.agilesdk.connector.smartsheet.SmartsheetIntegrationConnector;
import com.ppm.integration.agilesdk.connector.smartsheet.rest.SmartsheetRestClient;
import com.ppm.integration.agilesdk.connector.smartsheet.rest.SmartsheetRestConfig;
import com.ppm.integration.agilesdk.provider.Providers;
import com.ppm.integration.agilesdk.provider.UserProvider;
import org.apache.commons.lang.StringUtils;

public class SmartsheetServiceProvider {

    public static UserProvider getUserProvider() {
        return Providers.getUserProvider(SmartsheetIntegrationConnector.class);
    }

    public static SmartsheetService get(ValueSet config) {

        String proxyHost = config.get(SmartsheetConstants.KEY_PROXY_HOST);

        SmartsheetRestConfig restConfig = new SmartsheetRestConfig();

        if (!StringUtils.isBlank(proxyHost)) {
            String proxyPort = config.get(SmartsheetConstants.KEY_PROXY_PORT);
            if (StringUtils.isBlank(proxyPort)) {
                proxyPort = "80";
            }

            restConfig.setProxy(proxyHost, proxyPort);
        }
        restConfig.setAuthToken(getIntegrationToken(config));

        return new SmartsheetService(new SmartsheetRestClient(restConfig));
    }

    public static String getIntegrationToken(ValueSet config) {
        String integrationToken = config.get(SmartsheetConstants.KEY_USER_ACCESS_TOKEN);
        if (!StringUtils.isBlank(integrationToken)) {
            return integrationToken;
        } else {
            return config.get(SmartsheetConstants.KEY_ACCESS_TOKEN);
        }
    }

}
