package com.ppm.integration.agilesdk.connector.smartsheet.model;

import com.ppm.integration.agilesdk.pm.ExternalTaskActuals;

import java.util.Date;

/**
 * Fills all the gaps of actuals by automatically computing everything.
 */
public class SmartsheetExternalTaskActuals extends ExternalTaskActuals {

    private double scheduledEffort;
    private double estimatedRemainingEffort;
    private double actualEffort;
    private double percentComplete;
    private long resourceId;
    private Date scheduledStart;
    private Date scheduledFinish;

    public SmartsheetExternalTaskActuals(double scheduledEffort, double estimatedRemainingEffort, double actualEffort, double percentComplete, Date scheduledStart, Date scheduledFinish, Long resourceId) {
        this.scheduledEffort = scheduledEffort;
        this.estimatedRemainingEffort = estimatedRemainingEffort;
        this.actualEffort = actualEffort;
        this.percentComplete = percentComplete;
        this.resourceId = resourceId == null ? -1 : resourceId.longValue();
        this.scheduledFinish = scheduledFinish;
        this.scheduledStart = scheduledStart;
    }

    @Override
    public double getScheduledEffort() {
        return scheduledEffort;
    }

    @Override
    public Date getActualStart() {
        if (percentComplete > 0d) {
            return scheduledStart;
        } else {
            return null;
        }
    }

    @Override
    public Date getActualFinish() {
        if (percentComplete >= 100d) {
            return scheduledFinish;
        } else {
            return null;
        }
    }

    @Override
    public double getActualEffort() {
        return actualEffort;
    }

    @Override
    public double getPercentComplete() {
        return percentComplete;
    }

    @Override
    public long getResourceId() {
        return resourceId;
    }

    @Override
    public Double getEstimatedRemainingEffort() {
        return estimatedRemainingEffort;
    }
}
