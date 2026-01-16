package com.findoraai.giftfinder.config.exception;

import lombok.Getter;

@Getter
public class DuplicateEmailException extends RuntimeException {
    private final String email;

    public DuplicateEmailException(String message) {
        super(message);
        this.email = null;
    }

    public DuplicateEmailException(String message, String email) {
        super(message);
        this.email = email;
    }
}
