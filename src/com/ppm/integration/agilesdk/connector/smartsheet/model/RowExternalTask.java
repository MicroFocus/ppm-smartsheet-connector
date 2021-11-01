package com.ppm.integration.agilesdk.connector.smartsheet.model;

import com.kintana.core.logging.LogManager;
import com.kintana.core.logging.Logger;
import com.ppm.integration.agilesdk.ValueSet;
import com.ppm.integration.agilesdk.connector.smartsheet.SmartsheetConstants;
import com.ppm.integration.agilesdk.pm.ExternalTask;
import com.ppm.integration.agilesdk.pm.ExternalTaskActuals;
import com.ppm.integration.agilesdk.provider.UserProvider;
import com.sun.jimi.core.util.P;
import org.apache.commons.lang.StringUtils;

import java.util.*;

/**
 * Exposes a Smartsheet Page object as an External Task, based on the passed config.
 */
public class RowExternalTask extends ExternalTask {

    private final static Logger logger = LogManager.getLogger(RowExternalTask.class);

    private SmartsheetSheet.SmartsheetRow row;
    private UserProvider userProvider;
    private ValueSet config;
    private Map<String, SmartsheetSheet.SmartsheetRow.SmartsheetCell> cellsByColumnId = new HashMap<>();
    private double percentComplete = 0.0d;
    private double actualEffort = 0.0d;
    private List<Long> resourcesIds = new ArrayList<>();
    private List<ExternalTask> children = new ArrayList<>();

    @Override
    public List<ExternalTask> getChildren() {
        return children;
    }

    public void addChild(ExternalTask child) {
        children.add(child);
    }

    public RowExternalTask(SmartsheetSheet.SmartsheetRow row, ValueSet config, UserProvider userProvider) {
        this.row = row;
        this.config = config;
        this.userProvider = userProvider;
        Arrays.stream(this.row.cells).forEach(cell -> cellsByColumnId.put(cell.columnId, cell));

        String resourceField = config.get(SmartsheetConstants.KEY_TMF_TASK_RESOURCES);
        if (resourceField != null) {
            resourcesIds = getPeoplesField(resourceField);
        }

        String actualEffortField = config.get(SmartsheetConstants.KEY_TMF_TASK_ACTUAL_EFFORT);
        if (actualEffortField != null) {
            Double effortValue = getNumberField(actualEffortField);
            if (effortValue != null) {
                actualEffort = effortValue.doubleValue();
            }
        }

        String percentCompleteField = config.get(SmartsheetConstants.KEY_TMF_TASK_PERCENT_COMPLETE);
        if (percentCompleteField != null) {
            Double percentValue = getPercentField(percentCompleteField);
            if (percentValue != null) {
                percentComplete = percentValue * 100d; // PPM needs value between 0 and 100, while Smartsheet stores percent in real value.

                if (percentComplete < 0) {
                    percentComplete = 0d;
                }
                if (percentComplete > 100d) {
                    percentComplete = 100d;
                }
            }
        }

        if (actualEffort > 0d && percentComplete <= 0d) {
            percentComplete = 1d;
        }

        if (actualEffort <= 0d && percentComplete > 0d) {
            actualEffort = 1d;
        }
    }

    @Override
    public TaskStatus getStatus() {
        // We compute task Status based on the percent complete value.
        if (percentComplete <= 0d) {
            return TaskStatus.READY;
        } else if (percentComplete < 100d) {
            return TaskStatus.IN_PROGRESS;
        } else {
            return TaskStatus.COMPLETED;
        }
    }

    @Override
    public String getId() {
        return row.id;
    }

    @Override
    public String getName() {
        String fieldId = config.get(SmartsheetConstants.KEY_TMF_TASK_NAME);
        if (fieldId != null) {
            String name = getTextField(fieldId);
            if (name == null) {
                // It's common to have one empty line at the end of table in Smartsheet Database
                name = "?";
            }
            return name;
        } else {
            return super.getName();
        }
    }

    @Override
    public Date getScheduledStart() {
        String fieldId = config.get(SmartsheetConstants.KEY_TMF_TASK_START_DATE);
        if (fieldId != null) {
            Date date = getDateField(fieldId);
            if (date != null) {
                return adjustStartDateTime(date);
            }
        }
        return super.getScheduledStart();

    }

    @Override
    public Date getScheduledFinish() {
        String fieldId = config.get(SmartsheetConstants.KEY_TMF_TASK_FINISH_DATE);
        if (fieldId != null) {
            Date date = getDateField(fieldId);
            if (date != null) {
                return adjustFinishDateTime(date);
            }
        }
        return super.getScheduledFinish();
    }

    @Override
    public List<ExternalTaskActuals> getActuals() {

        List<ExternalTaskActuals> actuals = new ArrayList<ExternalTaskActuals>();


        final double numResources = resourcesIds.size();

        if (resourcesIds.isEmpty()) {
            // All is unassigned effort
            ExternalTaskActuals unassignedActuals = new SmartsheetExternalTaskActuals(actualEffort, percentComplete, getScheduledStart(), getScheduledFinish(), null);
            actuals.add(unassignedActuals);
        } else {
            // One Actual entry per resource.
            for (final Long resourceId : resourcesIds) {
                ExternalTaskActuals resourceActuals = new SmartsheetExternalTaskActuals(actualEffort / numResources, percentComplete, getScheduledStart(), getScheduledFinish(), resourceId);
                actuals.add(resourceActuals);
            }
        }

        return actuals;
    }

    /** Only for summary tasks */
    @Override
    public long getOwnerId() {
        if (resourcesIds == null || resourcesIds.isEmpty()) {
            return super.getOwnerId();
        } else {
            return resourcesIds.get(0);
        }
    }

    private String getTextField(String fieldId) {
        SmartsheetSheet.SmartsheetRow.SmartsheetCell prop = cellsByColumnId.get(fieldId);

        if (prop == null) {
            return null;
        }

        return prop.value;
    }

    private Date getDateField(String fieldId) {

        SmartsheetSheet.SmartsheetRow.SmartsheetCell prop = cellsByColumnId.get(fieldId);

        if (prop == null) {
            return null;
        }

        return prop.getValueAsDate();
    }

    private List<Long> getPeoplesField(String fieldId) {
        SmartsheetSheet.SmartsheetRow.SmartsheetCell prop = cellsByColumnId.get(fieldId);

        if (prop == null) {
            return null;
        }

        return prop.getPeoplesValue(userProvider);
    }

    private Double getNumberField(String fieldId) {
        SmartsheetSheet.SmartsheetRow.SmartsheetCell prop = cellsByColumnId.get(fieldId);

        if (prop == null || prop.value == null || StringUtils.isBlank(prop.value)) {
            return null;
        }

        try {
            return Double.parseDouble(prop.value);
        } catch (Exception e) {
            logger.error("Error parsing field value to a number:"+prop.value, e);
            return null;
        }
    }

    private Double getPercentField(String fieldId) {
        SmartsheetSheet.SmartsheetRow.SmartsheetCell prop = cellsByColumnId.get(fieldId);

        if (prop == null || prop.value == null || StringUtils.isBlank(prop.value)) {
            return null;
        }

        String strValue = prop.value.trim();

         boolean isPercent = false;

         if (strValue.endsWith("%")) {
             isPercent = true;
             strValue = StringUtils.removeEnd(strValue, "%").trim();
         }

        try {
            double value = Double.parseDouble(strValue);
            return (isPercent ? value  / 100d : value);
        } catch (Exception e) {
            logger.error("Error parsing field value to a Percentage: "+prop.value, e);
            return null;
        }
    }

    public String getParentId() {
        return row.parentId;
    }
}
