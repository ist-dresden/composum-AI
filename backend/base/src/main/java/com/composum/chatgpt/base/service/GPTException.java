package com.composum.chatgpt.base.service;

/**
 * Any kind of error when accessing GPT.
 */
// FIXME(hps,06.04.23) add more specific exceptions, possibly details for the user
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
