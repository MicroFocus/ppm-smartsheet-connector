
/*
 * © Copyright 2019 - 2020 Micro Focus or one of its affiliates.
 */

package com.ppm.integration.agilesdk.connector.smartsheet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.ppm.integration.agilesdk.FunctionIntegration;
import com.ppm.integration.agilesdk.IntegrationConnector;
import com.ppm.integration.agilesdk.ValueSet;
import com.ppm.integration.agilesdk.connector.smartsheet.model.HomeResponse;
import com.ppm.integration.agilesdk.connector.smartsheet.model.SmartsheetSheet;
import com.ppm.integration.agilesdk.connector.smartsheet.service.SmartsheetService;
import com.ppm.integration.agilesdk.connector.smartsheet.service.SmartsheetServiceProvider;
import com.ppm.integration.agilesdk.model.AgileProject;
import com.ppm.integration.agilesdk.ui.*;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * Main Connector class file for Jira Cloud connector.
 * Note that the Jira Cloud version is purely informative - there is no version for Jira Cloud.
 */
public class SmartsheetIntegrationConnector extends IntegrationConnector {

    private final Logger logger = Logger.getLogger(SmartsheetIntegrationConnector.class);

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
        return "0.2";
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
                new CheckBox(SmartsheetConstants.KEY_FORCE_ACCESS_TOKEN_USE, "LABEL_FORCE_ACCESS_TOKEN_USE", false),
                new LineBreaker(),
                new LabelText("", "LABEL_SHEET_RESTRICTION", "Sheet restriction", false),
                new DynamicDropdown(SmartsheetConstants.KEY_WP_SHEET_RESTRICTION, "WP_SHEET", false) {
                    @Override
                    public List<String> getDependencies() {
                        return Arrays.asList(new String[]{SmartsheetConstants.KEY_ACCESS_TOKEN});
                    }

                    @Override
                    public List<Option> getDynamicalOptions(ValueSet values) {

                        List<Option> options = new ArrayList<>();

                        if (!StringUtils.isBlank(values.get(SmartsheetConstants.KEY_ACCESS_TOKEN))) {

                            try {

                                SmartsheetService service = SmartsheetServiceProvider.get(values);

                                List<HomeResponse.Folder> folders = service.getAllFolders();
                                List<HomeResponse.Workspace> workspaces = service.getAllWorkspaces();


                                options.add(new DynamicDropdown.Option("", "N/A"));
                                workspaces.stream().forEach(workspace -> options.add(new DynamicDropdown.Option(SmartsheetConstants.WORKSPACE_RESTRICTION_PREFIX + workspace.id, "[" + workspace.name + "]")));
                                folders.stream().forEach(folder -> options.add(new DynamicDropdown.Option(SmartsheetConstants.FOLDER_RESTRICTION_PREFIX + folder.id, folder.getFullName())));

                            } catch (Exception e) {
                                logger.error(e);
                            }

                        }
                        return options;
                    }
                }
        });
    }

    @Override
    public List<AgileProject> getAgileProjects(ValueSet instanceConfigurationParameters) {
        List<SmartsheetSheet> sheets = SmartsheetServiceProvider.get(instanceConfigurationParameters).getAllSheets(null);

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
