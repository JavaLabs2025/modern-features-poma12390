package org.lab.app;

/**
 * Non-null replacement for Void in Result<T>.
 * Use Unit.INSTANCE when operation succeeds but returns no payload.
 */
public enum Unit {
    INSTANCE
}
