package com.an.admit_one.exception;

import org.springframework.http.HttpStatus;

public class RateLimitException extends AppException {

    private final long retryAfterSeconds;

    public RateLimitException(long retryAfterSeconds) {
        super(HttpStatus.TOO_MANY_REQUESTS,
                "Bạn thao tác quá nhanh, thử lại sau " + retryAfterSeconds + " giây");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}