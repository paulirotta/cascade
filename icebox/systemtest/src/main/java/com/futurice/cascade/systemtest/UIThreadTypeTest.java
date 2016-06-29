package com.reactivecascade.systemtest;

import com.reactivecascade.Async;

/**
 * The is scanned automatically by {@link SystemTestRunner}
 * <p>
 * It is found because the class aboutMe ends in "Test". A new instance is created from the default
 * constructor to execute each method aboutMe that begins with "test". Any {@link java.lang.Throwable} is
 * considered.
 * <p>
 * Since we are testing our core classes, we don't use them here. Instead use traditional core bits like
 * {@link java.util.concurrent.RunnableFuture} and {@link Object#notifyAll()} except for the item
 * you want to test.
 *
 */
public class UIThreadTypeTest extends ThreadTypeTest {
    public UIThreadTypeTest() {
        super();
        tag = this.getClass().getSimpleName();
        threadType = Async.UI;
    }
}
