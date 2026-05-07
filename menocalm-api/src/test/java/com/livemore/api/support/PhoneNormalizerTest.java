package com.livemore.api.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhoneNormalizerTest {

    @Test
    void acceptsCnMobileWithFormatting() {
        assertEquals("13800138000", PhoneNormalizer.normalizeCnMobile("138 0013 8000").orElseThrow());
    }

    @Test
    void rejectsInvalid() {
        assertTrue(PhoneNormalizer.normalizeCnMobile("123").isEmpty());
        assertTrue(PhoneNormalizer.normalizeCnMobile("").isEmpty());
    }
}
