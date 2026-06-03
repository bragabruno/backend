package com.bragdev.frauddetection.common.security;

import org.springframework.stereotype.Service;

/**
 * PII masking and tokenization service.
 * Masks sensitive fields for logging/display while preserving format for validation.
 */
@Service
public class PiiMaskingService {

    public String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 2) {
            return "***" + email.substring(atIndex);
        }
        return email.substring(0, 2) + "***" + email.substring(atIndex);
    }

    public String maskIp(String ip) {
        if (ip == null) {
            return "***";
        }
        int lastDot = ip.lastIndexOf('.');
        if (lastDot < 0) {
            return "***";
        }
        return ip.substring(0, lastDot + 1) + "***";
    }

    public String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "***";
        }
        return "****-****-****-" + cardNumber.substring(cardNumber.length() - 4);
    }

    public String maskString(String value, int visibleChars) {
        if (value == null || value.length() <= visibleChars) {
            return "***";
        }
        return value.substring(0, visibleChars) + "***";
    }
}
