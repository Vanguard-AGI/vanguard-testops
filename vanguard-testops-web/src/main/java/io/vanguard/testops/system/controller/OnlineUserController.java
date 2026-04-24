package io.vanguard.testops.system.controller;

import io.vanguard.testops.system.controller.handler.ResultHolder;
import io.vanguard.testops.system.dto.OnlineUserStats;
import io.vanguard.testops.system.service.OnlineUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 在线用户：数量、明细、在线时长（数据来自 Redis Session）
 */
@RestController
@RequestMapping("/system/online")
@Tag(name = "系统设置 - 在线用户")
public class OnlineUserController {

    @Resource
    private OnlineUserService onlineUserService;

    @GetMapping("/users")
    @Operation(summary = "获取在线用户数量与明细（含在线时长）")
    public ResultHolder getOnlineUsers() {
        return ResultHolder.success(onlineUserService.getOnlineUserStats());
    }
}
