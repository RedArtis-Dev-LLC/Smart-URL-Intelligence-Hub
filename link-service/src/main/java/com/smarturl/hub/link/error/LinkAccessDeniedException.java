package com.smarturl.hub.link.error;

public class LinkAccessDeniedException extends RuntimeException {

    public LinkAccessDeniedException() {
        super("You do not have access to this link");
    }
}
