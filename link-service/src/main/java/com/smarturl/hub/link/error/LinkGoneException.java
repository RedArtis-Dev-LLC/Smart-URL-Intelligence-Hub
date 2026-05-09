package com.smarturl.hub.link.error;

public class LinkGoneException extends RuntimeException {

    public LinkGoneException(String reason) {
        super(reason);
    }
}
