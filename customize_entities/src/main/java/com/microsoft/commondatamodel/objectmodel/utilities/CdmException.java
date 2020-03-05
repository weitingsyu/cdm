package com.microsoft.commondatamodel.objectmodel.utilities;

public class CdmException extends Exception {
    private static final long serialVersionUID = -2327313620869310516L;

    public CdmException(final String message) {
        super(message);
    }

    public CdmException(final String message, final Throwable throwable) {
        super(message, throwable);
    }
}