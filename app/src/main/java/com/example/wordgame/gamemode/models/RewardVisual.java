package com.example.wordgame.gamemode.models;

import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

/**
 * Optional text visual to display when a word is found.
 */
@Immutable
public class RewardVisual {
    public static final int UNINITIALIZED_ID = Integer.MIN_VALUE;
    private final int viewId;
    private final int animationId;
    private final String text;

    public RewardVisual(int viewId, final int animationId, @Nonnull String text) {
        this.viewId = viewId;
        this.animationId = animationId;
        this.text = Objects.requireNonNull(text);
    }

    public int getViewId() {
        return this.viewId;
    }

    public int getAnimationId() {
        return this.animationId;
    }

    public String getText() {
        return this.text;
    }
}
