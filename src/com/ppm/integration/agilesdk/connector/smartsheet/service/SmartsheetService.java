package com.ppm.integration.agilesdk.connector.smartsheet.service;

import com.google.gson.*;
import com.ppm.integration.agilesdk.ValueSet;
import com.ppm.integration.agilesdk.connector.smartsheet.SmartsheetConstants;
import com.ppm.integration.agilesdk.connector.smartsheet.model.*;
import com.ppm.integration.agilesdk.connector.smartsheet.rest.SmartsheetRestClient;
import com.ppm.integration.agilesdk.connector.smartsheet.rest.SmartsheetRestConfig;
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

    /**
     *
     * Returns all sheets, possibly only the ones located in a given folder or workspace.
     *
     * We're always retrieving the whole content of /home even when filtered by workspace/folder,
     * as it's the only way to get all the sheets without looking in all the sub-folders one by one.
     *
     * @param sheetRestriction w_workspace_id or f_folder_id or null/empty if you want all sheets.
     * @return
     */
    public List<SmartsheetSheet> getAllSheets(String sheetRestriction) {

        HomeResponse home = getHome();

        List<SmartsheetSheet> sheets = null;

        if (sheetRestriction != null && sheetRestriction.startsWith(SmartsheetConstants.WORKSPACE_RESTRICTION_PREFIX)) {
            sheets = home.getWorkspaceSheets(sheetRestriction.substring(SmartsheetConstants.WORKSPACE_RESTRICTION_PREFIX.length()));
        } else if (sheetRestriction != null && sheetRestriction.startsWith(SmartsheetConstants.FOLDER_RESTRICTION_PREFIX)) {
            sheets = home.getFolderSheets(sheetRestriction.substring(SmartsheetConstants.FOLDER_RESTRICTION_PREFIX.length()));
        } else {
            // Get all sheets
            sheets = home.getAllSheets();
        }

        return sheets;
    }

    public HomeResponse getHome() {

        String url = SmartsheetConstants.API_HOME + "?includeAll=true";

        ClientResponse response = restClient.sendGet(url);

        HomeResponse responseObject = new Gson().fromJson(response.getEntity(String.class), HomeResponse.class);

        return responseObject;
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

    public List<HomeResponse.Folder> getAllFolders() {

        List<HomeResponse.Folder> folders = new ArrayList<>();

        HomeResponse home = getHome();

        return home.getAllFolders();
    }

    public List<HomeResponse.Workspace> getAllWorkspaces() {
        List<HomeResponse.Workspace> workspaces = new ArrayList<>();

        HomeResponse home = getHome();

        if (home != null && home.workspaces != null) {
            workspaces.addAll(Arrays.asList(home.workspaces));
        }

        return workspaces;
    }


}
