package com.composum.ai.backend.base.service;

/**
 * Any kind of error when accessing GPT.
 */
// TODO(hps,06.04.23) add more specific exceptions, possibly details for the user
public class GPTException extends RuntimeException {

    public GPTException(String message) {
        super(message);
    }

    public GPTException(String message, Throwable cause) {
        super(message, cause);
    }

    public GPTException(Throwable cause) {
        super(cause);
    }

}
