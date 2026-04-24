package io.vanguard.testops.system.controller;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.page.PageMethod;
import io.vanguard.testops.sdk.constants.PermissionConstants;
import io.vanguard.testops.sdk.dto.pool.ResourcePoolNodeMetric;
import io.vanguard.testops.sdk.util.BeanUtils;
import io.vanguard.testops.system.dto.pool.TestResourcePoolCapacityRequest;
import io.vanguard.testops.system.dto.pool.TestResourcePoolDTO;
import io.vanguard.testops.system.dto.pool.TestResourcePoolRequest;
import io.vanguard.testops.system.dto.pool.TestResourcePoolReturnDTO;
import io.vanguard.testops.system.dto.sdk.QueryResourcePoolRequest;
import io.vanguard.testops.system.dto.taskhub.TaskHubItemDTO;
import io.vanguard.testops.system.dto.taskhub.request.TaskHubItemRequest;
import io.vanguard.testops.system.log.annotation.Log;
import io.vanguard.testops.system.log.constants.OperationLogType;
import io.vanguard.testops.system.service.BaseTaskHubService;
import io.vanguard.testops.system.service.TestResourcePoolService;
import io.vanguard.testops.system.support.page.PageUtils;
import io.vanguard.testops.system.dto.page.Pager;
import io.vanguard.testops.validation.groups.Updated;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Tag(name = "系统设置-系统-资源池")
@RequestMapping("/test/resource/pool")
@RestController
public class TestResourcePoolController {

    @Resource
    private TestResourcePoolService testResourcePoolService;
    @Resource
    private BaseTaskHubService baseTaskHubService;

    @PostMapping("/add")
    @Operation(summary = "系统设置-系统-资源池-添加资源池")
    @RequiresPermissions(PermissionConstants.SYSTEM_TEST_RESOURCE_POOL_READ_UPDATE)
    @Log(type = OperationLogType.ADD, expression = "#msClass.addLog(#request)", msClass = TestResourcePoolService.class)
    public void addTestResourcePool(@Validated @RequestBody TestResourcePoolRequest request) {
        TestResourcePoolDTO testResourcePool = new TestResourcePoolDTO();
        BeanUtils.copyBean(testResourcePool, request);
        testResourcePoolService.addTestResourcePool(testResourcePool);
    }

    @PostMapping("/update")
    @Operation(summary = "系统设置-系统-资源池-更新资源池")
    @RequiresPermissions(PermissionConstants.SYSTEM_TEST_RESOURCE_POOL_READ_UPDATE)
    @Log(type = OperationLogType.UPDATE, expression = "#msClass.updateLog(#request)", msClass = TestResourcePoolService.class)
    public void updateTestResourcePool(@Validated({Updated.class}) @RequestBody TestResourcePoolRequest request) {
        TestResourcePoolDTO testResourcePool = new TestResourcePoolDTO();
        BeanUtils.copyBean(testResourcePool, request);
        testResourcePool.setCreateUser(null);
        testResourcePool.setCreateTime(null);
        testResourcePoolService.updateTestResourcePool(testResourcePool);
    }

    @PostMapping("/delete")
    @Operation(summary = "系统设置-系统-资源池-删除资源池")
    @RequiresPermissions(PermissionConstants.SYSTEM_TEST_RESOURCE_POOL_READ_UPDATE)
    @Log(type = OperationLogType.DELETE, expression = "#msClass.deleteLog(#request)", msClass = TestResourcePoolService.class)
    public void deleteTestResourcePool(@RequestBody TestResourcePoolRequest request) {
        testResourcePoolService.deleteTestResourcePool(request.getId());
    }

    @PostMapping("/page")
    @Operation(summary = "系统设置-系统-资源池-获取资源池列表")
    @RequiresPermissions(PermissionConstants.SYSTEM_TEST_RESOURCE_POOL_READ)
    public Pager<List<TestResourcePoolDTO>> listResourcePools(@Validated @RequestBody QueryResourcePoolRequest request) {
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize(), true);
        return PageUtils.setPageInfo(page, testResourcePoolService.listResourcePools(request));
    }

    @GetMapping("/detail/{poolId}")
    @Operation(summary = "系统-资源池-查看资源池详细")
    @RequiresPermissions(PermissionConstants.SYSTEM_TEST_RESOURCE_POOL_READ)
    public TestResourcePoolReturnDTO getTestResourcePoolDetail(@PathVariable(value = "poolId") String testResourcePoolId) {
        return testResourcePoolService.getTestResourcePoolDetail(testResourcePoolId);
    }

    @PostMapping("/capacity/detail")
    @Operation(summary = "系统-资源池-查看资源池容量内存详细")
    @RequiresPermissions(PermissionConstants.SYSTEM_TEST_RESOURCE_POOL_READ)
    public ResourcePoolNodeMetric getTestResourcePoolCapacityDetail(@Validated @RequestBody TestResourcePoolCapacityRequest request) {
        return testResourcePoolService.getTestResourcePoolCapacityDetail(request);
    }

    @PostMapping("/capacity/task/list")
    @Operation(summary = "系统-资源池-查看资源池节点任务列表")
    @RequiresPermissions(PermissionConstants.SYSTEM_TEST_RESOURCE_POOL_READ)
    public Pager<List<TaskHubItemDTO>> getCaseTaskItemList(@Validated @RequestBody TestResourcePoolCapacityRequest request) {
        TaskHubItemRequest taskHubItemRequest = new TaskHubItemRequest();
        BeanUtils.copyBean(taskHubItemRequest, request);
        taskHubItemRequest.setResourcePoolIds(List.of(request.getPoolId()));
        if (StringUtils.isNotBlank(request.getIp()) && StringUtils.isNotBlank(request.getPort())) {
            String node = request.getIp() + ":" + request.getPort();
            taskHubItemRequest.setResourcePoolNodes(List.of(node));
        }
        if (!testResourcePoolService.isPoolAvailable(request.getPoolId())) {
            Page<Object> page = PageMethod.startPage(request.getCurrent(), request.getPageSize(),
                    StringUtils.isNotBlank(request.getSortString()) ? request.getSortString() : "id asc");
            return PageUtils.setPageInfo(page, new ArrayList<>());
        }
        return baseTaskHubService.getCaseTaskItemList(taskHubItemRequest, null, null);
    }


}
