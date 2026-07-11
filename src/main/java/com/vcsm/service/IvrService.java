package com.vcsm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vcsm.dto.IvrNode;
import com.vcsm.model.IvrFlowConfig;
import com.vcsm.model.IvrSession;
import com.vcsm.repository.IvrFlowConfigRepository;
import com.vcsm.repository.IvrSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@Service
@lombok.RequiredArgsConstructor
public class IvrService {
    private static final Logger log = LoggerFactory.getLogger(IvrService.class);

    private final IvrFlowConfigRepository configRepository;

    private final IvrSessionRepository sessionRepository;

    private final ObjectMapper objectMapper;

    private final ConversationContextCache contextCache;

    private static final String DEFAULT_FLOW = "{\n" +
            "  \"id\": \"root\",\n" +
            "  \"prompt\": \"Welcome to VCSM voice support. Say '1' for complaints, or '2' for community events.\",\n" +
            "  \"options\": [\n" +
            "    {\n" +
            "      \"id\": \"complaints_menu\",\n" +
            "      \"pattern\": \"1|one|complaint|complaints\",\n" +
            "      \"prompt\": \"You selected complaints. Say 'new' to file a new complaint, or 'status' to check existing complaints.\",\n" +
            "      \"options\": [\n" +
            "        {\n" +
            "          \"id\": \"file_complaint\",\n" +
            "          \"pattern\": \"new|file|create\",\n" +
            "          \"prompt\": \"Redirecting you to the file complaint screen. Please describe your issue.\",\n" +
            "          \"action\": \"action_file_complaint\"\n" +
            "        },\n" +
            "        {\n" +
            "          \"id\": \"view_complaints\",\n" +
            "          \"pattern\": \"status|check|existing\",\n" +
            "          \"prompt\": \"Opening your active complaints board.\",\n" +
            "          \"action\": \"action_view_complaints\"\n" +
            "        }\n" +
            "      ]\n" +
            "    },\n" +
            "    {\n" +
            "      \"id\": \"events_menu\",\n" +
            "      \"pattern\": \"2|two|event|events\",\n" +
            "      \"prompt\": \"You selected events. Say 'book' to book a new event ticket, or 'list' to view available events.\",\n" +
            "      \"options\": [\n" +
            "        {\n" +
            "          \"id\": \"book_event\",\n" +
            "          \"pattern\": \"book|reserve|register\",\n" +
            "          \"prompt\": \"Redirecting to the event registration section.\",\n" +
            "          \"action\": \"action_book_event\"\n" +
            "        },\n" +
            "        {\n" +
            "          \"id\": \"view_events\",\n" +
            "          \"pattern\": \"list|view|show\",\n" +
            "          \"prompt\": \"Opening the community events board.\",\n" +
            "          \"action\": \"action_view_events\"\n" +
            "        }\n" +
            "      ]\n" +
            "    }\n" +
            "  ]\n" +
            "}";

    public IvrNode getActiveFlow() {
        IvrFlowConfig config = configRepository.findFirstByIsActiveTrueOrderByUpdatedAtDesc()
                .orElseGet(() -> {
                    IvrFlowConfig defaultCfg = new IvrFlowConfig(DEFAULT_FLOW, true);
                    return configRepository.save(defaultCfg);
                });
        try {
            return objectMapper.readValue(config.getFlowJson(), IvrNode.class);
        } catch (Exception e) {
            System.err.println("Error parsing IVR flow JSON: " + e.getMessage());
            try {
                return objectMapper.readValue(DEFAULT_FLOW, IvrNode.class);
            } catch (Exception ex) {
                throw new CustomDomainException("Fallback default flow is invalid: " + ex.getMessage());
            }
        }
    }

    @Transactional
    public void saveFlow(String flowJson) {
        List<IvrFlowConfig> configs = configRepository.findAll();
        for (IvrFlowConfig cfg : configs) {
            cfg.setActive(false);
        }
        configRepository.saveAll(configs);

        IvrFlowConfig newConfig = new IvrFlowConfig(flowJson, true);
        configRepository.save(newConfig);
    }

    @Transactional
    public Map<String, Object> processInteraction(String userEmail, String transcript) {
        IvrNode rootNode = getActiveFlow();
        String lower = transcript.toLowerCase().trim();

        // 1. Reset check
        if (lower.equals("reset") || lower.equals("restart") || lower.equals("menu") || lower.equals("hello") || lower.equals("hi")) {
            sessionRepository.deleteByUserEmail(userEmail);
            contextCache.clearContext(userEmail); // Reset conversational memory
            IvrSession session = new IvrSession(userEmail, rootNode.getId());
            sessionRepository.save(session);

            Map<String, Object> response = new HashMap<>();
            response.put("prompt", rootNode.getPrompt());
            response.put("currentNodeId", rootNode.getId());
            response.put("action", "none");
            response.put("success", true);
            return response;
        }

        // 2. Fetch or create session
        IvrSession session = sessionRepository.findByUserEmail(userEmail)
                .orElseGet(() -> {
                    IvrSession newSession = new IvrSession(userEmail, rootNode.getId());
                    return sessionRepository.save(newSession);
                });

        IvrNode currentNode = findNodeById(rootNode, session.getCurrentNodeId());
        if (currentNode == null) {
            currentNode = rootNode;
            session.setCurrentNodeId(rootNode.getId());
            sessionRepository.save(session);
        }

        // Add current turn to conversation context cache and get combined context
        contextCache.addContext(userEmail, lower);
        String combinedContext = contextCache.getContext(userEmail);

        // 3. Match transcript against children (options) using multi-turn context
        IvrNode matchedChild = null;
        if (currentNode.getOptions() != null) {
            for (IvrNode child : currentNode.getOptions()) {
                String patternStr = "(?i).*(" + child.getPattern() + ").*";
                try {
                    if (Pattern.matches(patternStr, combinedContext)) {
                        matchedChild = child;
                        break;
                    }
                } catch (Exception e) {
                    if (combinedContext.contains(child.getPattern().toLowerCase())) {
                        matchedChild = child;
                        break;
                    }
                }
            }
        }

        Map<String, Object> response = new HashMap<>();

        if (matchedChild != null) {
            session.setCurrentNodeId(matchedChild.getId());
            session.setUpdatedAt(LocalDateTime.now());
            sessionRepository.save(session);

            response.put("prompt", matchedChild.getPrompt());
            response.put("currentNodeId", matchedChild.getId());
            response.put("action", matchedChild.getAction() != null ? matchedChild.getAction() : "none");
            response.put("success", true);

            if (matchedChild.getAction() != null || matchedChild.getOptions() == null || matchedChild.getOptions().isEmpty()) {
                sessionRepository.deleteByUserEmail(userEmail);
                contextCache.clearContext(userEmail);
            }
        } else {
            response.put("prompt", "Sorry, I didn't catch that. " + currentNode.getPrompt());
            response.put("currentNodeId", currentNode.getId());
            response.put("action", "none");
            response.put("success", false);
        }

        return response;
    }

    private IvrNode findNodeById(IvrNode current, String id) {
        if (current.getId().equals(id)) {
            return current;
        }
        if (current.getOptions() != null) {
            for (IvrNode option : current.getOptions()) {
                IvrNode found = findNodeById(option, id);
                if (found != null) return found;
            }
        }
        return null;
    }
}
