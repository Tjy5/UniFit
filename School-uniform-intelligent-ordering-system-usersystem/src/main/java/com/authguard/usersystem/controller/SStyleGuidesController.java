package com.authguard.usersystem.controller;

import com.authguard.usersystem.entity.SStyleGuides;
import com.authguard.usersystem.service.SStyleGuidesService;
// ⭐ 1. Import
import com.authguard.usersystem.service.ISUserActivityLogService;
import jakarta.servlet.http.HttpServletRequest;
// End Import
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/s-style-guides")
public class SStyleGuidesController {
    @Autowired
    private SStyleGuidesService sStyleGuidesService;

    // ⭐ 2. Inject
    @Autowired
    private ISUserActivityLogService activityLogService;

    @GetMapping("/active")
    public List<SStyleGuides> getAllActiveStyleGuides(
            HttpServletRequest request // ⭐ 3.
    ) {
        // Consider if this needs userId if only active ones are public
        // For now, logging general API call
        activityLogService.recordActivityAsync(null, "VIEW_ACTIVE_STYLE_GUIDES", null, null, null, request);
        return sStyleGuidesService.getAllActiveStyleGuides();
    }

    @GetMapping("/{id}")
    public SStyleGuides getStyleGuideById(
            @PathVariable("id") Long id,
            HttpServletRequest request // ⭐ 3.
    ) {
        // userId might be relevant if there's a "my recently viewed" feature later
        // Or if different users see different details (unlikely for guides)
        activityLogService.recordActivityAsync(null, "VIEW_STYLE_GUIDE_DETAIL", "STYLE_GUIDE", id.toString(), null, request);
        return sStyleGuidesService.getStyleGuideById(id);
    }
}
