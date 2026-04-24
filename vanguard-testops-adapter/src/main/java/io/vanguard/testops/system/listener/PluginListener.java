package io.vanguard.testops.system.listener;


import io.vanguard.testops.sdk.constants.KafkaTopicConstants;
import io.vanguard.testops.sdk.util.JSON;
import io.vanguard.testops.sdk.util.LogUtils;
import io.vanguard.testops.system.service.PluginLoadService;
import io.vanguard.testops.system.support.plugin.PluginNotifiedDTO;
import jakarta.annotation.Resource;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PluginListener {

    public static final String PLUGIN_CONSUMER = "plugin_consumer";

    @Resource
    private PluginLoadService pluginLoadService;

    // groupId 必须是每个实例唯一
    @KafkaListener(id = PLUGIN_CONSUMER, topics = KafkaTopicConstants.PLUGIN, groupId = PLUGIN_CONSUMER + "_" + "${random.uuid}")
    public void handlePluginChange(ConsumerRecord<?, String> record) {
        LogUtils.info("Service consume platform_plugin message: " + record.value());
        PluginNotifiedDTO pluginNotifiedDTO = JSON.parseObject(record.value(), PluginNotifiedDTO.class);
        String operate = pluginNotifiedDTO.getOperate();
        String pluginId = pluginNotifiedDTO.getPluginId();
        String fileName = pluginNotifiedDTO.getFileName();
        switch (operate) {
            case KafkaTopicConstants.TYPE.ADD:
                pluginLoadService.handlePluginAddNotified(pluginId, fileName);
                break;
            case KafkaTopicConstants.TYPE.DELETE:
                pluginLoadService.handlePluginDeleteNotified(pluginId, fileName);
                break;
            default:
                break;
        }
    }
}
