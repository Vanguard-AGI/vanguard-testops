package io.vanguard.testops.api.service.schedule;


import io.vanguard.testops.api.constants.ApiImportPlatform;
import io.vanguard.testops.api.dto.definition.ApiScheduleDTO;
import io.vanguard.testops.api.dto.request.ImportRequest;
import io.vanguard.testops.api.service.definition.ApiDefinitionImportService;
import io.vanguard.testops.api.service.definition.ApiDefinitionScheduleService;
import io.vanguard.testops.sdk.util.BeanUtils;
import io.vanguard.testops.sdk.util.CommonBeanFactory;
import io.vanguard.testops.system.runtime.schedule.BaseScheduleJob;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.TriggerKey;

public class SwaggerUrlImportJob extends BaseScheduleJob {
    private final ApiDefinitionImportService apiDefinitionImportService;
    private final ApiDefinitionScheduleService apiDefinitionScheduleService;

    public SwaggerUrlImportJob() {
        apiDefinitionImportService = CommonBeanFactory.getBean(ApiDefinitionImportService.class);
        apiDefinitionScheduleService = CommonBeanFactory.getBean(ApiDefinitionScheduleService.class);
    }

    @Override
    protected void businessExecute(JobExecutionContext context) {
        JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();
        String resourceId = jobDataMap.getString("resourceId");
        ApiScheduleDTO swaggerInfo = apiDefinitionScheduleService.getSchedule(resourceId);
        ImportRequest request = new ImportRequest();
        BeanUtils.copyBean(request, swaggerInfo);
        request.setPlatform(ApiImportPlatform.Swagger3.name());
        request.setUserId(jobDataMap.getString("userId"));
        request.setType("SCHEDULE");
        request.setResourceId(resourceId);
        apiDefinitionImportService.apiDefinitionImport(null, request, request.getProjectId());
    }

    public static JobKey getJobKey(String resourceId) {
        return new JobKey(resourceId, SwaggerUrlImportJob.class.getName());
    }

    public static TriggerKey getTriggerKey(String resourceId) {
        return new TriggerKey(resourceId, SwaggerUrlImportJob.class.getName());
    }

}
