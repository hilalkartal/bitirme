package com.bitirme.demo_bitirme.config;

import java.lang.annotation.*;

/**
 * Inject the current user's ID into a controller parameter.
 * Reads the X-User-Id request header and verifies the user exists.
 * No real auth yet — this is the test-mode user switcher.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CurrentUserId {
}
