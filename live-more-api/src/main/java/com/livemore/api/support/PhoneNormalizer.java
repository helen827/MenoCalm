package com.livemore.api.support;

import java.util.Optional;

public final class PhoneNormalizer {

    private PhoneNormalizer() {
    }

    /**
     * China mainland mobile: 11 digits, starts with 1.
     */
    public static Optional<String> normalizeCnMobile(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String digits = raw.replaceAll("\\D", "");
        if (digits.length() == 11 && digits.startsWith("1")) {
            return Optional.of(digits);
        }
        return Optional.empty();
    }
}
