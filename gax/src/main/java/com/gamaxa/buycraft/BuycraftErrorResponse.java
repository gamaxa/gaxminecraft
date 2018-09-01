package com.gamaxa.buycraft;

/**
 * @author PaulBGD
 */
public class BuycraftErrorResponse {
    public int error_code;
    public String error_message;

    @Override
    public String toString() {
        return "BuycraftErrorResponse{" +
                "error_code=" + error_code +
                ", error_message='" + error_message + '\'' +
                '}';
    }
}
