
/*
 * © Copyright 2019 - 2020 Micro Focus or one of its affiliates.
 */

package com.ppm.integration.agilesdk.connector.smartsheet;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.ppm.integration.agilesdk.FunctionIntegration;
import com.ppm.integration.agilesdk.IntegrationConnector;
import com.ppm.integration.agilesdk.ValueSet;
import com.ppm.integration.agilesdk.connector.smartsheet.model.SmartsheetSheet;
import com.ppm.integration.agilesdk.connector.smartsheet.service.SmartsheetServiceProvider;
import com.ppm.integration.agilesdk.model.AgileProject;
import com.ppm.integration.agilesdk.ui.*;

/**
 * Main Connector class file for Jira Cloud connector.
 * Note that the Jira Cloud version is purely informative - there is no version for Jira Cloud.
 */
public class SmartsheetIntegrationConnector extends IntegrationConnector {

    @Override
    public String getExternalApplicationName() {
        return "Smartsheet";
    }

    @Override
    public String getExternalApplicationVersionIndication() {
        return "2021+";
    }

    @Override
    public String getConnectorVersion() {
        return "0.1";
    }

    @Override
    public String getTargetApplicationIcon() {
        return "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA4AAAAQCAMAAAARSr4IAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAADMUExURf////n5+t/j593g5fT19tjc4UJXbjFIYWR2ieTn6tLX3CI7Vg4pRzlPaLrBysrQ1i5GYE5ieKCqth02Uq22wJijsBMuSxEsSmd3i3CBkg8qSHyLm9nd4iY+WRgyT5mksfX2+JKfrC1FX9vf5HOClBErSZqmsr/Gzh84VDRKZObp7Pv8/F1vgxItSsfN1CE6VT9UbGZ3iiA5VD1TayA5VXGBkuDk59zg5Sc/Wqu0vxkzT3SEldfb4NDV2+rt79TZ3lttgo+cqcPK0ejq7UigOXcAAAAJcEhZcwAADsMAAA7DAcdvqGQAAACASURBVBhXY0AHjEzMMMAC5LKysUMBByeQy8XNAwG8fPxIXAEuQSEEV1hEVAzCFZeQFJaSlpHlgXDl5BUUlZRVeKBcHlU1BnV1DTiXR1NLWweoH8bVVdTTR+LyGBjyGBkjuDwmpmbmFpYQrpWxtQ0jA4OtHZBr7+Do5AykMQEDAwDmkw0dfglNuAAAAABJRU5ErkJggg==";
    }

    @Override
    public List<Field> getDriverConfigurationFields() {
        return Arrays.asList(new Field[]{
                new PlainText(SmartsheetConstants.KEY_PROXY_HOST, "PROXY_HOST", "", false),
                new PlainText(SmartsheetConstants.KEY_PROXY_PORT, "PROXY_PORT", "", false),
                new LineBreaker(),
                new LabelText("", "AUTHENTICATION_SETTINGS_SECTION", "block", false),
                new PasswordText(SmartsheetConstants.KEY_ACCESS_TOKEN, "ACCESS_TOKEN", "", true),
                new CheckBox(SmartsheetConstants.KEY_FORCE_ACCESS_TOKEN_USE, "LABEL_FORCE_ACCESS_TOKEN_USE", false)
        });
    }

    @Override
    public List<AgileProject> getAgileProjects(ValueSet instanceConfigurationParameters) {
        List<SmartsheetSheet> sheets = SmartsheetServiceProvider.get(instanceConfigurationParameters).getAllAvailableSheets();

        return sheets.stream().map(sheet -> {
            AgileProject proj = new AgileProject();
            proj.setValue(sheet.id);
            proj.setDisplayName(sheet.name);
            return proj;
        }).collect(Collectors.toList());

    }

    @Override
    public List<FunctionIntegration> getIntegrations() {
        return Arrays.asList(new FunctionIntegration[]{new SmartsheetWorkPlanIntegration()});
    }

    @Override
    public List<String> getIntegrationClasses() {
        return Arrays.asList(new String[]{"com.ppm.integration.agilesdk.connector.smartsheet.SmartsheetWorkPlanIntegration"});
    }

}
