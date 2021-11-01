
/*
 * © Copyright 2019 - 2020 Micro Focus or one of its affiliates.
 */

package com.ppm.integration.agilesdk.connector.smartsheet;

import com.ppm.integration.agilesdk.ValueSet;
import com.ppm.integration.agilesdk.connector.smartsheet.model.SmartsheetSheet;
import com.ppm.integration.agilesdk.connector.smartsheet.model.RowExternalTask;
import com.ppm.integration.agilesdk.connector.smartsheet.service.SmartsheetService;
import com.ppm.integration.agilesdk.connector.smartsheet.service.SmartsheetServiceProvider;
import com.ppm.integration.agilesdk.pm.*;
import com.ppm.integration.agilesdk.provider.LocalizationProvider;
import com.ppm.integration.agilesdk.provider.Providers;
import com.ppm.integration.agilesdk.provider.UserProvider;
import com.ppm.integration.agilesdk.ui.*;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

public class SmartsheetWorkPlanIntegration extends WorkPlanIntegration {


    private final Logger logger = Logger.getLogger(SmartsheetWorkPlanIntegration.class);

    public SmartsheetWorkPlanIntegration() {
    }

    private SmartsheetService service;

    private synchronized SmartsheetService getService(ValueSet config) {
        if (service == null) {
            service = SmartsheetServiceProvider.get(config);
        } else {
            service.refreshRestConfigIfNeeded(config);
        }
        return service;
    }

    @Override
    public List<Field> getMappingConfigurationFields(WorkPlanIntegrationContext context, ValueSet values) {

        final LocalizationProvider lp = Providers.getLocalizationProvider(SmartsheetIntegrationConnector.class);

        List<Field> fields = new ArrayList<>();

        boolean isIntegrationTokenEmpty = StringUtils.isBlank(values.get(SmartsheetConstants.KEY_ACCESS_TOKEN));

        if (isIntegrationTokenEmpty || !"true".equals(values.get(SmartsheetConstants.KEY_FORCE_ACCESS_TOKEN_USE))) {
            // Users can use their own integration token
            fields.addAll(getUserIntegrationTokenFields(isIntegrationTokenEmpty));
        }

        // We only retrieve all databases and include the DB select field if the value of selected database is not already provided
        if (StringUtils.isBlank(values.get(SmartsheetConstants.KEY_WP_SHEET))) {

            DynamicDropdown databasesList = new DynamicDropdown(SmartsheetConstants.KEY_WP_SHEET, "WP_SHEET", true) {
                @Override
                public List<String> getDependencies() {
                    return Arrays.asList(new String[]{SmartsheetConstants.KEY_ACCESS_TOKEN, SmartsheetConstants.KEY_USER_ACCESS_TOKEN});
                }

                @Override
                public List<Option> getDynamicalOptions(ValueSet values) {
                    final List<SmartsheetSheet> sheets = getService(values).getAllAvailableSheets();
                    Collections.sort(sheets, (o1, o2) -> o1.name.compareToIgnoreCase(o2.name));
                    List<Option> options = new ArrayList<>();
                    sheets.stream().forEach(sheet -> options.add(new DynamicDropdown.Option(sheet.id, sheet.name)));
                    return options;
                }
            };

            fields.add(new LabelText("LABEL_SHEET_TO_SYNC", "LABEL_SHEET_TO_SYNC",
                    "Select what sheet to import:", true));

            fields.add(databasesList);

            fields.add(new LineBreaker());
        }

        // Fields mapping section
        fields.addAll(getTaskFieldMappingSectionFields());

        return fields;
    }

    private List<Field> getUserIntegrationTokenFields(boolean isIntegrationTokenEmpty) {
        List<Field> userTokenFields = new ArrayList<>(3);

        userTokenFields.add(new LabelText("LABEL_USER_ACCESS_TOKEN", "LABEL_USER_ACCESS_TOKEN",
                "Integration Token", false));

        if (isIntegrationTokenEmpty) {
            // Mandatory!
            userTokenFields.add(new PasswordText(SmartsheetConstants.KEY_USER_ACCESS_TOKEN, "LABEL_USER_ACCESS_MANDATORY",
                    "", true));
        } else {
            // Optional
            userTokenFields.add(new PasswordText(SmartsheetConstants.KEY_USER_ACCESS_TOKEN, "LABEL_USER_ACCESS_OPTIONAL",
                    "", false));
        }

        userTokenFields.add(new LineBreaker());

        return userTokenFields;
    }

    /**
     * @return the UI fields where users can map a property of the Smartsheet database to each PPM Task field.
     * <p>
     * We will only propose the fields that have a compatible type for matching.
     */
    private List<Field> getTaskFieldMappingSectionFields() {

        List<Field> taskFields = new ArrayList<>(10);

        taskFields.add(new LabelText("LABEL_FIELDS_MAPPING", "LABEL_FIELDS_MAPPING",
                "Pick Properties to use for Task fields", false));

        taskFields.add(createTaskField(SmartsheetConstants.KEY_TMF_TASK_NAME, "LABEL_TMF_TASK_NAME", true, "TEXT_NUMBER"));
        taskFields.add(createTaskField(SmartsheetConstants.KEY_TMF_TASK_START_DATE, "LABEL_TMF_TASK_START_DATE", false, "DATE"));
        taskFields.add(createTaskField(SmartsheetConstants.KEY_TMF_TASK_FINISH_DATE, "LABEL_TMF_TASK_FINISH_DATE", false, "DATE"));
        taskFields.add(createTaskField(SmartsheetConstants.KEY_TMF_TASK_RESOURCES, "LABEL_TMF_TASK_RESOURCES", false, "CONTACT_LIST"));
        taskFields.add(createTaskField(SmartsheetConstants.KEY_TMF_TASK_PERCENT_COMPLETE, "LABEL_TMF_TASK_PERCENT_COMPLETE", false, "TEXT_NUMBER"));
        taskFields.add(createTaskField(SmartsheetConstants.KEY_TMF_TASK_ACTUAL_EFFORT, "LABEL_TMF_TASK_ACTUAL_EFFORT", false, "TEXT_NUMBER"));

        taskFields.add(new LineBreaker());

        return taskFields;

    }

    private Field createTaskField(String fieldKey, String labelKey, boolean isRequired, String... supportedSmartsheetFieldTypes) {
        Field f = new DynamicDropdown(fieldKey, labelKey, isRequired) {

            @Override
            public List<String> getDependencies() {
                return Arrays.asList(new String[]{SmartsheetConstants.KEY_WP_SHEET});
            }

            @Override
            public List<Option> getDynamicalOptions(ValueSet values) {
                String selectedSheetId = values.get(SmartsheetConstants.KEY_WP_SHEET);
                SmartsheetSheet sheet = getService(values).getSmartsheetSheetColumns(selectedSheetId);

                if (sheet == null || sheet.columns == null) {
                    return new ArrayList<>();
                }

                List<Option> options = new ArrayList<>();

                Set<String> supportedTypes = new HashSet<>(Arrays.asList(supportedSmartsheetFieldTypes));

                Arrays.stream(sheet.columns).forEach(col -> {
                    if (supportedTypes.contains("*") || supportedTypes.contains(col.type)) {
                        options.add(new Option(col.id, col.title));
                    }
                });

                return options;
            }
        };

        return f;

    }


    @Override
    /**
     * This method is in Charge of retrieving all Smartsheet DB rows and turning them into a workplan structure to be imported in PPM.
     */
    public ExternalWorkPlan getExternalWorkPlan(WorkPlanIntegrationContext context, final ValueSet values) {

        final String dbId = values.get(SmartsheetConstants.KEY_WP_SHEET);

        final List<SmartsheetSheet.SmartsheetRow> rows = Arrays.asList(getService(values).getSmartsheetSheet(dbId).rows);

        final UserProvider userProvider = SmartsheetServiceProvider.getUserProvider();

        return new ExternalWorkPlan() {

            @Override
            public List<ExternalTask> getRootTasks() {

                final Map<String, RowExternalTask> tasksById = new HashMap<>();

                // This gives a flat list of tasks
                List<RowExternalTask> allTasks = rows.stream().filter(row -> !row.isBlank()).map(row -> new RowExternalTask(row, values, userProvider)).collect(Collectors.toList());

                allTasks.stream().forEach(task -> {
                    tasksById.put(task.getId(), task);
                });

                List<ExternalTask> rootTasks = new ArrayList<>();

                // Let's build the hierarchy.
                allTasks.stream().forEach(task -> {
                    String parentId = task.getParentId();
                    if (StringUtils.isBlank(parentId) || !tasksById.containsKey(parentId)) {
                        rootTasks.add(task);
                    } else {
                        tasksById.get(parentId).addChild(task);
                    }
                });

                return rootTasks;
            }
        };

    }

    /**
     * This will allow to have the information in PPM DB table PPMIC_WORKPLAN_MAPPINGS of what entity in JIRA is effectively linked to the PPM work plan task.
     * It is very useful for reporting purpose.
     *
     * @since 9.42
     */
    public LinkedTaskAgileEntityInfo getAgileEntityInfoFromMappingConfiguration(ValueSet values) {
        LinkedTaskAgileEntityInfo info = new LinkedTaskAgileEntityInfo();

        String dbId = values.get(SmartsheetConstants.KEY_WP_SHEET);

        info.setProjectId(dbId);

        // THere's no specific info in Smartsheet - no Epic or feature or such.

        return info;
    }


    @Override
    public boolean supportTimesheetingAgainstExternalWorkPlan() {
        return true;
    }
}
