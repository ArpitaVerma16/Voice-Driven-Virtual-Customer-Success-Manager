package com.vcsm.service;

import com.vcsm.model.User;
import com.vcsm.model.UserActivity;
import com.vcsm.repository.UserActivityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserActivityService {
    
    @Autowired
    private UserActivityRepository userActivityRepository;
    
    public void logActivity(User user, String actionType, String description, Long referenceId) {
        UserActivity activity = new UserActivity(user, actionType, description, referenceId);
        userActivityRepository.save(activity);
        
        // Update last active timestamp
        user.setLastActive(LocalDateTime.now());
    }
    
    public List<UserActivity> getUserActivities(User user) {
        return userActivityRepository.findByUserOrderByCreatedAtDesc(user);
    }
    
    public Page<UserActivity> getUserActivities(User user, Pageable pageable) {
        return userActivityRepository.findByUser(user, pageable);
    }
    
    public List<UserActivity> getUserActivitiesByType(User user, String actionType) {
        return userActivityRepository.findByUserAndActionTypeOrderByCreatedAtDesc(user, actionType);
    }
    
    public long getActivityCount(User user) {
        return userActivityRepository.countByUser(user);
    }
}