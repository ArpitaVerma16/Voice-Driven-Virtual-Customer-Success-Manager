package com.vcsm.controller;

import com.vcsm.service.ChatbotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/chatbot")
@lombok.RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;

    @PostMapping("/ask")
    public ResponseEntity<Map<String, Object>> ask(@Valid @RequestBody Map<String, String> request) {
        String message = request.get("message");
        String response = chatbotService.getResponse(message);

        Map<String, Object> result = new HashMap<>();
        result.put("question", message);
        result.put("answer", response);
        result.put("success", true);

        return ResponseEntity.ok(result);
    }
}