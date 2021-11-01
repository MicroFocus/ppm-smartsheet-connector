package com.ppm.integration.agilesdk.connector.smartsheet.model;

import com.ppm.integration.agilesdk.pm.ExternalTaskActuals;

import java.util.Date;

/**
 * Fills all the gaps of actuals by automatically computing everything.
 */
public class SmartsheetExternalTaskActuals extends ExternalTaskActuals {

    private double actualEffort;
    private double percentComplete;
    private long resourceId;
    private Date scheduledStart;
    private Date scheduledFinish;

    public SmartsheetExternalTaskActuals(double actualEffort, double percentComplete, Date scheduledStart, Date scheduledFinish, Long resourceId) {
        this.actualEffort = actualEffort;
        this.percentComplete = percentComplete;
        this.resourceId = resourceId == null ? -1 : resourceId.longValue();
        this.scheduledFinish = scheduledFinish;
        this.scheduledStart = scheduledStart;
    }

    @Override
    public double getScheduledEffort() {
        return actualEffort + getEstimatedRemainingEffort();
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
        // PPM enforces that PC = AE / (AE + ERE), so we have to compute ERE accordingly otherwise it will modify PC.
        if (percentComplete <= 0) {
            return actualEffort;
        } else {
            return actualEffort * (100 - percentComplete) / percentComplete;
        }
    }
}
