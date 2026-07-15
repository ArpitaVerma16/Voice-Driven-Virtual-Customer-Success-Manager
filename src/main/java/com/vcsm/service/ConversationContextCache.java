package com.vcsm.service;

import org.springframework.stereotype.Service;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ConversationContextCache {
    // In-memory cache tied to CustomerSession (using email as sessionId for now)
    private final Map<String, LinkedList<String>> cache = new ConcurrentHashMap<>();
    private static final int MAX_TURNS = 5;

    public void addContext(String sessionId, String text) {
        cache.compute(sessionId, (k, v) -> {
            if (v == null) v = new LinkedList<>();
            v.add(text);
            if (v.size() > MAX_TURNS) {
                v.removeFirst();
            }
            return v;
        });
    }

    public String getContext(String sessionId) {
        LinkedList<String> list = cache.get(sessionId);
        return list == null ? "" : String.join(" ", list);
    }
    
    public void clearContext(String sessionId) {
        cache.remove(sessionId);
    }
}
