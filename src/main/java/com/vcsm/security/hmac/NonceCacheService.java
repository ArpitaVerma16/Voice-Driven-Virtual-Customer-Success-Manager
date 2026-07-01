package com.vcsm.security.hmac;

import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Service
public class NonceCacheService {

    private final ConcurrentHashMap<String, Long> nonceStore =
            new ConcurrentHashMap<>();

    public boolean exists(String nonce) {
        return nonceStore.containsKey(nonce);
    }

    public void save(String nonce) {
        nonceStore.put(nonce, System.currentTimeMillis());
    }
}