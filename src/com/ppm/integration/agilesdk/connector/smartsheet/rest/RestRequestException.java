
/*
 * © Copyright 2019 - 2020 Micro Focus or one of its affiliates.
 */

package com.ppm.integration.agilesdk.connector.smartsheet.rest;

public class RestRequestException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int statusCode;

    public RestRequestException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
   }

    public int getStatusCode() {
        return statusCode;
    }
}
