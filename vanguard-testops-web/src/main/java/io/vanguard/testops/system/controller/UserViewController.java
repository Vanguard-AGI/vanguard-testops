package io.vanguard.testops.system.controller;

import io.vanguard.testops.system.constants.UserViewType;
import io.vanguard.testops.system.domain.UserView;
import io.vanguard.testops.system.dto.UserViewDTO;
import io.vanguard.testops.system.dto.UserViewListGroupedDTO;
import io.vanguard.testops.system.dto.request.UserViewAddRequest;
import io.vanguard.testops.system.dto.request.UserViewUpdateRequest;
import io.vanguard.testops.system.service.UserViewService;
import io.vanguard.testops.system.utils.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @Author: Jan
 * @CreateTime: 2024-08-30  14:43
 */
@Tag(name = "视图")
@RestController
@RequestMapping("/user-view")
public class UserViewController {
    @Resource
    private UserViewService userViewService;

    @GetMapping("/{viewType}/list")
    @Operation(summary = "视图列表")
    public List<UserView> list(@RequestParam String scopeId, @PathVariable String viewType) {
        UserViewType userViewType = UserViewType.getByValue(viewType);
        return userViewService.list(scopeId, userViewType, SessionUtils.getUserId());
    }

    @GetMapping("/{viewType}/grouped/list")
    @Operation(summary = "视图列表")
    public UserViewListGroupedDTO groupedList(@RequestParam String scopeId, @PathVariable String viewType) {
        UserViewType userViewType = UserViewType.getByValue(viewType);
        return userViewService.groupedList(scopeId, userViewType, SessionUtils.getUserId());
    }

    @GetMapping("/{viewType}/get/{id}")
    @Operation(summary = "视图详情")
    public UserViewDTO get(@PathVariable String id, @PathVariable String viewType) {
        UserViewType userViewType = UserViewType.getByValue(viewType);
        return userViewService.get(id, userViewType, SessionUtils.getUserId());
    }

    @PostMapping("/{viewType}/add")
    @Operation(summary = "新增视图")
    public UserViewDTO add(@Validated @RequestBody UserViewAddRequest request, @PathVariable String viewType) {
        UserViewType userViewType = UserViewType.getByValue(viewType);
        return userViewService.add(request, userViewType.name(), SessionUtils.getUserId());
    }

    @PostMapping("/{viewType}/update")
    @Operation(summary = "编辑视图")
    public UserViewDTO update(@Validated @RequestBody UserViewUpdateRequest request, @PathVariable String viewType) {
        UserViewType userViewType = UserViewType.getByValue(viewType);
        return userViewService.update(request, userViewType.name(), SessionUtils.getUserId());
    }

    @GetMapping("/{viewType}/delete/{id}")
    @Operation(summary = "删除视图")
    public void delete(@PathVariable String id, @PathVariable String viewType) {
        userViewService.delete(id, SessionUtils.getUserId());
    }
}