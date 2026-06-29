package com.bucket4j.local;

import java.io.Serializable;

public class LocalBucket implements Serializable {
    private static final long serialVersionUID = 1L;

    public boolean tryConsume(int tokens) {
        return true;
    }

    public long getAvailableTokens() {
        return 10L;
    }
}
