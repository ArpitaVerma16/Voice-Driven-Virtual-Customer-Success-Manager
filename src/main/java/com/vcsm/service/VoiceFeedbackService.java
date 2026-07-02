package com.vcsm.service;

import com.vcsm.model.User;
import com.vcsm.model.VoiceCommand;
import com.vcsm.model.VoiceFeedback;
import com.vcsm.repository.VoiceCommandRepository;
import com.vcsm.repository.VoiceFeedbackRepository;
import com.vcsm.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@lombok.RequiredArgsConstructor
public class VoiceFeedbackService {
    
    private final VoiceFeedbackRepository voiceFeedbackRepository;
    
    private final VoiceCommandRepository voiceCommandRepository;
    
    private final UserRepository userRepository;
    
    public VoiceFeedback submitFeedback(Long commandId, Long userId, String feedback, String comment) {
        VoiceCommand command = voiceCommandRepository.findById(commandId)
                .orElseThrow(() -> new RuntimeException("Voice command not found"));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        VoiceFeedback voiceFeedback = new VoiceFeedback(command, user, feedback.toUpperCase(), comment);
        return voiceFeedbackRepository.save(voiceFeedback);
    }
    
    public Map<String, Object> getFeedbackStats() {
        Map<String, Object> stats = new HashMap<>();
        
        long upvotes = voiceFeedbackRepository.countUpvotes();
        long downvotes = voiceFeedbackRepository.countDownvotes();
        long total = upvotes + downvotes;
        
        stats.put("upvotes", upvotes);
        stats.put("downvotes", downvotes);
        stats.put("total", total);
        stats.put("satisfactionRate", total > 0 ? (upvotes * 100.0 / total) : 0);
        
        return stats;
    }
}