
package com.trustvault.controller;

import com.trustvault.model.ActivityLog;
import com.trustvault.repository.ActivityLogRepository;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/activity")
public class ActivityLogController {

    private final ActivityLogRepository activityLogRepository;

    public ActivityLogController(
            ActivityLogRepository activityLogRepository) {
        this.activityLogRepository = activityLogRepository;
    }

    @GetMapping("/my")
    public List<Map<String, Object>> getMyActivity(
            Authentication authentication) {

        List<ActivityLog> logs =
                activityLogRepository
                    .findByUser_UsernameOrderByCreatedAtDesc(
                        authentication.getName());

        return logs.stream().map(log -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", log.getId());
            item.put("action", log.getAction());
            item.put("filename", log.getFilename());
            item.put("createdAt", log.getCreatedAt());
            return item;
        }).toList();
    }
}
