package com.vcsm.controller;

import com.vcsm.service.CommandTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/voice/templates")
@CrossOrigin(origins = "*")
public class CommandTemplateController {

    @Autowired
    private CommandTemplateService commandTemplateService;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllTemplates() {
        return ResponseEntity.ok(commandTemplateService.getAllTemplates());
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<Map<String, Object>>> getByCategory(@PathVariable String category) {
        return ResponseEntity.ok(commandTemplateService.getTemplatesByCategory(category));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable int id) {
        Map<String, Object> template = commandTemplateService.getTemplateById(id);
        if (template == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(template);
    }

    @GetMapping("/categories")
    public ResponseEntity<List<String>> getCategories() {
        return ResponseEntity.ok(commandTemplateService.getAllCategories());
    }

    @GetMapping("/suggestion")
    public ResponseEntity<Map<String, String>> getSuggestion(@RequestParam(required = false) String context) {
        String suggestion = commandTemplateService.getSuggestion(context);
        Map<String, String> response = new HashMap<>();
        response.put("suggestion", suggestion);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/random")
    public ResponseEntity<List<Map<String, Object>>> getRandomTemplates(
            @RequestParam(defaultValue = "3") int count) {
        return ResponseEntity.ok(commandTemplateService.getRandomTemplates(count));
    }
}