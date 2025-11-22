package com.example.wordgame.utility;

import java.util.Set;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * A wrapper class for a Set that is emptied after the specified time interval.
 * Expiration is checked during each access method to the container.
 * Expiration timer can be reset with resetExpiration.
 * This data structure is not thread safe - access should be synchronized externally.
 */
@NotThreadSafe
public class ExpiringSet<T> {
    private final Set<T> wrappedSet;
    private final long expireTimeMs;
    private long lastRefreshTime;

    public ExpiringSet(Set<T> wrappedSet, long expireTimeMs) {
        this.wrappedSet = wrappedSet;
        this.expireTimeMs = expireTimeMs;
        this.lastRefreshTime = System.currentTimeMillis();
    }

    public boolean contains(T e) {
        expireIfNecessary();
        return this.wrappedSet.contains(e);
    }

    public boolean add(T e) {
        expireIfNecessary();
        return this.wrappedSet.add(e);
    }

    public boolean remove(T e) {
        final boolean removed = this.wrappedSet.remove(e);
        expireIfNecessary();
        return removed;
    }

    public void clear() {
        this.wrappedSet.clear();
        lastRefreshTime = System.currentTimeMillis();
    }

    public int size() {
        expireIfNecessary();
        return this.wrappedSet.size();
    }

    /**
     * Resets the expiration timer to start from the current time.
     */
    public void resetExpiration() {
        this.lastRefreshTime = System.currentTimeMillis();
    }

    public Set<T> getWrappedSet() {
        return this.wrappedSet;
    }

    private void expireIfNecessary() {
        final long now = System.currentTimeMillis();
        if (now - lastRefreshTime > expireTimeMs) {
            this.wrappedSet.clear();
            lastRefreshTime = now;
        }
    }
}
