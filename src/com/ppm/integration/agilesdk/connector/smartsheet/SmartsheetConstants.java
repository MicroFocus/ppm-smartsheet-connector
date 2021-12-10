
/*
 * © Copyright 2019 - 2020 Micro Focus or one of its affiliates.
 */

package com.ppm.integration.agilesdk.connector.smartsheet;

public class SmartsheetConstants {

    public static final String KEY_PROXY_HOST = "proxyHost";

    public static final String KEY_PROXY_PORT = "proxyPort";

    public static final String KEY_ACCESS_TOKEN = "accessToken";
    public static final String API_ROOT_URL = "https://api.smartsheet.com/";

    public static final String API_V2 = API_ROOT_URL + "2.0/";

    public static final String API_SHEETS = API_V2 + "sheets";

    public static final String API_HOME = API_V2 + "home";

    public static final String API_GET_SINGLE_SHEET = API_V2 + "sheets/";

    public static final String KEY_WP_SHEET = "wpSheet";
    public static final String KEY_WP_SHEET_RESTRICTION = "wpSheetRestriction";
    public static final String KEY_TMF_TASK_NAME = "tmfTaskName";
    public static final String KEY_TMF_TASK_START_DATE = "tmfTaskStartDate";
    public static final String KEY_TMF_TASK_FINISH_DATE = "tmfTaskFinishDate";
    public static final String KEY_TMF_TASK_RESOURCES = "tmfTaskResources";
    public static final String KEY_TMF_TASK_PERCENT_COMPLETE = "tmfTaskPercentComplete";
    public static final String KEY_TMF_TASK_ACTUAL_EFFORT = "tmfTaskActualEffort";
    public static final String KEY_TMF_TASK_SCHEDULED_EFFORT = "tmfTaskScheduledEffort";
    public static final String KEY_TMF_TASK_ERE = "tmfTaskEstimatedRemainingEffort";
    public static final String KEY_FORCE_ACCESS_TOKEN_USE = "forceAccessTokenUse";
    public static final String KEY_USER_ACCESS_TOKEN = "userAccessToken";
    public static final String WORKSPACE_RESTRICTION_PREFIX = "w_";
    public static final String FOLDER_RESTRICTION_PREFIX = "f_";
    public static final String HOME_PATH = "[Home]/";
}
