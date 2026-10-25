
package com.trustvault.service;

import com.trustvault.model.ActivityLog;
import com.trustvault.model.User;
import com.trustvault.repository.ActivityLogRepository;
import com.trustvault.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;
    private final UserRepository userRepository;

    public ActivityLogService(
            ActivityLogRepository activityLogRepository,
            UserRepository userRepository) {
        this.activityLogRepository = activityLogRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void logActivity(
            String username,
            String action,
            String filename) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));

        ActivityLog log = new ActivityLog(user, action, filename);

        activityLogRepository.save(log);
    }
}
