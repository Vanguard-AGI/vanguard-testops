package io.vanguard.testops.functional.controller;

import io.vanguard.testops.functional.dto.request.TrackModificationTimeRequest;
import io.vanguard.testops.functional.dto.request.TrackWriteTimeRequest;
import io.vanguard.testops.functional.service.MetricTrackService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/metrics/track")
public class MetricController {

    @Resource
    private MetricTrackService metricTrackService;

    @PostMapping("/write")
    public void trackWriteTime(@RequestBody TrackWriteTimeRequest request) {
        metricTrackService.trackWriteTime(request);
    }

    @PostMapping("/modification")
    public void trackModificationTime(@RequestBody TrackModificationTimeRequest request) {
        metricTrackService.trackModificationTime(request);
    }
}
