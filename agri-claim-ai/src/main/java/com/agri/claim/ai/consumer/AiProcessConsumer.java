package com.agri.claim.ai.consumer;

import com.agri.claim.common.constant.Constants;
import com.agri.claim.ai.service.ChangeDetectService;
import com.agri.claim.ai.service.SegmentService;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = Constants.MQ_TOPIC_AI_PROCESS,
        consumerGroup = Constants.MQ_CONSUMER_GROUP_AI
)
public class AiProcessConsumer implements RocketMQListener<String> {

    private final SegmentService segmentService;
    private final ChangeDetectService changeDetectService;

    @Override
    public void onMessage(String message) {
        try {
            JSONObject data = JSON.parseObject(message);
            String action = data.getString("action");
            log.info("收到AI处理消息 | action: {} | payload: {}", action, message);

            switch (action) {
                case "SEGMENT":
                    Long segmentImageId = data.getLong("imageId");
                    segmentService.segmentFarmland(segmentImageId);
                    break;

                case "DETECT":
                    Long beforeImageId = data.getLong("beforeImageId");
                    Long afterImageId = data.getLong("afterImageId");
                    Long segmentId = data.getLong("segmentId");
                    changeDetectService.detectChanges(beforeImageId, afterImageId, segmentId);
                    break;

                case "FULL_PROCESS":
                    Long fullBeforeImageId = data.getLong("beforeImageId");
                    Long fullAfterImageId = data.getLong("afterImageId");
                    Long missionId = data.getLong("missionId");
                    segmentService.fullProcess(fullBeforeImageId, fullAfterImageId, missionId);
                    break;

                default:
                    log.warn("未知的AI处理操作类型: {}", action);
            }
        } catch (Exception e) {
            log.error("AI处理消费失败 | msg: {}", message, e);
        }
    }
}
