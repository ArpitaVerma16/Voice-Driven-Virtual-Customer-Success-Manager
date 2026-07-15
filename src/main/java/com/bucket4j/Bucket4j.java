package com.bucket4j;

import com.bucket4j.local.LocalBucketBuilder;

public class Bucket4j {
    public static LocalBucketBuilder builder() {
        return new LocalBucketBuilder();
    }
}
