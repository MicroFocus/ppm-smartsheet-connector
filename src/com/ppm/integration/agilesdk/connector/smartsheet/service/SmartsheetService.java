package com.ppm.integration.agilesdk.connector.smartsheet.service;

import com.google.gson.*;
import com.ppm.integration.agilesdk.ValueSet;
import com.ppm.integration.agilesdk.connector.smartsheet.SmartsheetConstants;
import com.ppm.integration.agilesdk.connector.smartsheet.model.*;
import com.ppm.integration.agilesdk.connector.smartsheet.rest.SmartsheetRestClient;
import com.ppm.integration.agilesdk.connector.smartsheet.rest.SmartsheetRestConfig;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.wink.client.ClientResponse;

import java.util.*;

/**
 * Class in charge of making calls to Smartsheet REST API when needed. Contains a cache, so the service should not be a static member of a class, as the caches are never invalidated and might contain stale data if used as such.
 *
 * This class not thread safe.
 */
public class SmartsheetService {

    private final static Logger logger = Logger.getLogger(SmartsheetService.class);

    private SmartsheetRestClient restClient;

    // We cache Sheet column info to avoid making the call for every DDL
    Map<String, SmartsheetSheet> basicSheetInfoById = new HashMap<>();

    public SmartsheetService(SmartsheetRestClient restClient) {
        this.restClient = restClient;
    }

    public List<SmartsheetSheet> getAllAvailableSheets() {

        String url = SmartsheetConstants.API_SHEETS + "?includeAll=true";

        ClientResponse response = restClient.sendGet(url);

        SheetSearchResponse responseObject = new Gson().fromJson(response.getEntity(String.class), SheetSearchResponse.class);

        return (Arrays.asList(responseObject.data));
    }

    /**
     * This method has its result cached as it's called many times by each Task DDL on the work plan mapping page.
     * It will NOT include any row information, only the columns.
     */
    public SmartsheetSheet getSmartsheetSheetColumns(String sheetId) {
        if (!basicSheetInfoById.containsKey(sheetId)) {

            // We add rowIds=1 at the end to not retrieve any row info since we only need the column definitions here.
            // And we cannot use the /summary API as it's not available in entry-level Smartsheet subscription.
            String url = SmartsheetConstants.API_GET_SINGLE_SHEET + sheetId + "?rowIds=1";

            ClientResponse response = restClient.sendGet(url);

            SmartsheetSheet sheet = new Gson().fromJson(response.getEntity(String.class), SmartsheetSheet.class);

            basicSheetInfoById.put(sheetId, sheet);
        }

        return basicSheetInfoById.get(sheetId);
    }


    /** Returns all the info of the sheet, including all rows. Not cached, since it's only called upon work plan sync. */
    public SmartsheetSheet getSmartsheetSheet(String sheetId) {

        String url = SmartsheetConstants.API_GET_SINGLE_SHEET + sheetId + "?includeAll=true";

        ClientResponse response = restClient.sendGet(url);

        SmartsheetSheet sheet = new Gson().fromJson(response.getEntity(String.class), SmartsheetSheet.class);

        return sheet;
    }

    public synchronized void refreshRestConfigIfNeeded(ValueSet config) {
        String currentIntegrationToken = restClient.getIntegrationToken();
        String configIntegrationToken = SmartsheetServiceProvider.getIntegrationToken(config);

        if (!currentIntegrationToken.equals(configIntegrationToken)) {
            // We should use a different Integration Token - Let's reset the REST client and clear all caches.
            SmartsheetRestConfig restConfig = this.restClient.getSmartsheetRestConfig();
            restConfig.setAuthToken(configIntegrationToken);
            this.restClient = new SmartsheetRestClient(restConfig);
            this.basicSheetInfoById.clear();
        }
    }
}
