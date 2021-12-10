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
    private double scheduledEffort = 0.0d;
    private double estimatedRemainingEffort = 0.0d;
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
            Double actualEffortValue = getNumberField(actualEffortField);
            if (actualEffortValue != null) {
                actualEffort = actualEffortValue.doubleValue();
                if (actualEffort < 0d) {
                    actualEffort = 0d;
                }
            }
        }

        String scheduledEffortField = config.get(SmartsheetConstants.KEY_TMF_TASK_SCHEDULED_EFFORT);
        if (scheduledEffortField != null) {
            Double scheduledEffortValue = getNumberField(scheduledEffortField);
            if (scheduledEffortValue != null) {
                scheduledEffort = scheduledEffortValue.doubleValue();
                if (scheduledEffort < 0d) {
                    scheduledEffort = 0d;
                }
            }
        }

        String ereField = config.get(SmartsheetConstants.KEY_TMF_TASK_ERE);
        if (ereField != null) {
            Double ereValue = getNumberField(ereField);
            if (ereValue != null) {
                estimatedRemainingEffort = ereValue.doubleValue();
                if (estimatedRemainingEffort < 0d) {
                    estimatedRemainingEffort = 0d;
                }
            }
        }

        String percentCompleteField = config.get(SmartsheetConstants.KEY_TMF_TASK_PERCENT_COMPLETE);
        if (percentCompleteField != null) {
            Double percentValue = getPercentField(percentCompleteField);
            if (percentValue != null) {
                percentComplete = percentValue; // Percent value should be written in 0-100 value in smartsheet.

                if (percentComplete < 0) {
                    percentComplete = 0d;
                }
                if (percentComplete > 100d) {
                    percentComplete = 100d;
                }
            }
        }

        computeMissingEffortFields();

    }


    /**
     * Exactly 2 from the above fields should be mapped/set - and PPM will compute the other fields based on the formulas:
     * SE = AE + ERE
     * PC = AE/SE
     * If more than 2 fields were set, we will arbitrarily adjust values to ensure that above formulas do apply.
     */
    private void computeMissingEffortFields() {

        if (actualEffort == 0d && scheduledEffort == 0d && percentComplete == 0d && estimatedRemainingEffort == 0d) {
            // This is a task with zero actual info - that's a valid set of values!
            return;
        }

        // We compute AE first
        if (actualEffort == 0d) {
            if (scheduledEffort != 0d) {
                if (estimatedRemainingEffort != 0d) {
                    // AE = SE - ERE
                    actualEffort = scheduledEffort - estimatedRemainingEffort;
                }

                if (percentComplete != 0d) {
                    // AE = SE * PC
                    actualEffort = scheduledEffort * (percentComplete / 100d);
                }
            }

            if (estimatedRemainingEffort != 0d && percentComplete != 0d) {
                // AE = ERE * PC / (1-PC)
                actualEffort = estimatedRemainingEffort * (percentComplete / 100d) / (1d - (percentComplete / 100d));
            }
        }

        // We then compute Scheduled Effort
        if (scheduledEffort == 0d) {
            if (estimatedRemainingEffort != 0d) {
                // SE = AE + ERE
                scheduledEffort = actualEffort + estimatedRemainingEffort;

            } else if (percentComplete != 0d) {
                // SE = AE / PC
                scheduledEffort = actualEffort / (percentComplete / 100d);
            }
        }

        // Then Estimated Remaining Effort
        if (estimatedRemainingEffort == 0d) {
            // ERE = SE - AE
            estimatedRemainingEffort = scheduledEffort - actualEffort;
        }

        // We (re)compute percent complete at last to ensure the formula is valid before PPM enforces it.
        if (actualEffort == 0d || scheduledEffort == 0d) {
            percentComplete = 0d;
        } else {
            percentComplete = actualEffort / scheduledEffort * 100d;
        }

        if (actualEffort > 0d && percentComplete <= 0d) {
            // Task with actual effort must be in progress and thus cannot have percent complete = 0%.
            percentComplete = 1d;
        }

        if (actualEffort <= 0d && percentComplete > 0d) {
            // Tasks that have been started must have some actual effort entered.
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
            ExternalTaskActuals unassignedActuals = new SmartsheetExternalTaskActuals(scheduledEffort, estimatedRemainingEffort, actualEffort, percentComplete, getScheduledStart(), getScheduledFinish(), null);
            actuals.add(unassignedActuals);
        } else {
            // One Actual entry per resource.
            for (final Long resourceId : resourcesIds) {
                ExternalTaskActuals resourceActuals = new SmartsheetExternalTaskActuals(scheduledEffort/ numResources, estimatedRemainingEffort/ numResources,actualEffort / numResources, percentComplete, getScheduledStart(), getScheduledFinish(), resourceId);
                actuals.add(resourceActuals);
            }
        }

        return actuals;
    }

    /**
     * Only for summary tasks
     */
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
            logger.error("Error parsing field value to a number:" + prop.value, e);
            return null;
        }
    }

    private Double getPercentField(String fieldId) {
        SmartsheetSheet.SmartsheetRow.SmartsheetCell prop = cellsByColumnId.get(fieldId);

        if (prop == null || prop.value == null || StringUtils.isBlank(prop.value)) {
            return null;
        }

        // We check display value because a display value of 25% has a stored  value of 0.25
        String strValue = prop.displayValue.trim();

        if (strValue.endsWith("%")) {
            strValue = StringUtils.removeEnd(strValue, "%").trim();
        }

        try {
            double value = Double.parseDouble(strValue);
            return value;
        } catch (Exception e) {
            logger.error("Error parsing field value to a Percentage: " + prop.value, e);
            return null;
        }
    }

    public String getParentId() {
        return row.parentId;
    }
}
