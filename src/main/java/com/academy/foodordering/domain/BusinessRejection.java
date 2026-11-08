package com.academy.foodordering.domain;

/** Expected business-level rejection; not a runtime outage. */
public final class BusinessRejection extends RuntimeException {
    private final String code;

    public BusinessRejection(String code, String message) {
        super(message);
        this.code = code;
    }
    public String code() {
        return code;
    }
}
