package com.company;

public enum StatusCodes {
    SUCCESS(200),
    BAD_PASSWORD(412),
    BAD_USERNAME(413);

    private int statusCode;

    StatusCodes(int statusCode) {
        this.statusCode = statusCode;
    }

}
