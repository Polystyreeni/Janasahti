package com.example.wordgame.utility;

import android.content.Context;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Cache used to store view animations that have been loaded.
 * NOTE: The cache will never expire, so this should not be used for persistently active components.
 */
public class AnimationCache {
    private final Map<Integer, Animation> animationMap = new HashMap<>();

    public Animation get(Context ctx, int animationId) {
        final Animation existing = animationMap.get(animationId);
        if (existing != null) {
            return existing;
        } else {
            final Animation loaded = AnimationUtils.loadAnimation(ctx, animationId);
            animationMap.put(animationId, loaded);
            return loaded;
        }
    }
}