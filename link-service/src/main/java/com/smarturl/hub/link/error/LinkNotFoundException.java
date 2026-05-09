package com.smarturl.hub.link.error;

public class LinkNotFoundException extends RuntimeException {

    public LinkNotFoundException(String reference) {
        super("Link not found: " + reference);
    }
}
