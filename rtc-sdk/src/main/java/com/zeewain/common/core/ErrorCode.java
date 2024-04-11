package com.zeewain.common.core;

public class ErrorCode {

    /**
     * Initialization failed.
     */
    public static final int ERR_NOT_READY = 1;
    /**
     * Initialization failed. Invalid token.
     */
    public static final int ERR_INVALID_TOKEN = 2;
    /**
     * Network Error
     */
    public static final int ERR_CONNECTION_LOST = 3;
    /**
     * Initialization failed. Room number and APP ID and token does not match.
     */
    public static final int ERR_CONFIG_PARAMETER_NOT_MATCH = 4;

    public ErrorCode() {
    }

}
