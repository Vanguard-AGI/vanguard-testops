package io.vanguard.testops.api.controller;

import io.vanguard.testops.api.dto.share.ApiReportShareDTO;
import io.vanguard.testops.api.dto.share.ApiReportShareRequest;
import io.vanguard.testops.api.dto.share.ShareInfoDTO;
import io.vanguard.testops.api.service.ApiReportShareService;
import io.vanguard.testops.sdk.constants.PermissionConstants;
import io.vanguard.testops.system.utils.SessionUtils;
import io.vanguard.testops.validation.groups.Created;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/report/share")
@Tag(name = "接口测试-接口报告-分享")
public class ApiReportShareController {

    @Resource
    private ApiReportShareService apiReportShareService;

    @PostMapping("/gen")
    @Operation(summary = "接口测试-接口报告-生成分享链接")
    @RequiresPermissions(PermissionConstants.PROJECT_API_REPORT_SHARE)
    public ShareInfoDTO generateShareInfo(@Validated(Created.class) @RequestBody ApiReportShareRequest request) {
        return apiReportShareService.gen(request, SessionUtils.getUserId());
    }

    @GetMapping("/get/{id}")
    @Operation(summary = "接口测试-接口报告-获取分享链接")
    public ApiReportShareDTO get(@PathVariable String id) {
        return apiReportShareService.get(id);
    }

    @GetMapping("/get-share-time/{id}")
    @Operation(summary = "接口测试-接口报告-获取分享链接的有效时间")
    public String getShareTime(@PathVariable String id) {
        return apiReportShareService.getShareTime(id);
    }


}
