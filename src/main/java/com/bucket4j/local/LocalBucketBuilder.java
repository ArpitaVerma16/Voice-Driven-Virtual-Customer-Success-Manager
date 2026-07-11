package com.bucket4j.local;

import java.util.function.Consumer;

public class LocalBucketBuilder {
    public LocalBucketBuilder addLimit(Consumer<LimitBuilder> consumer) {
        return this;
    }

    public LocalBucket build() {
        return new LocalBucket();
    }

    public static class LimitBuilder {
        public LimitBuilder capacity(long capacity) {
            return this;
        }
        public LimitBuilder refillIntervally(long tokens, java.time.Duration duration) {
            return this;
        }
    }
}
