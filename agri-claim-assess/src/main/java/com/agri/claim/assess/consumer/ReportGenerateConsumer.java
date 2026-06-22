package com.agri.claim.assess.consumer;

import com.agri.claim.common.constant.Constants;
import com.agri.claim.assess.service.ReportService;
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
        topic = Constants.MQ_TOPIC_REPORT_GENERATE,
        consumerGroup = Constants.MQ_CONSUMER_GROUP_ASSESS
)
public class ReportGenerateConsumer implements RocketMQListener<String> {

    private final ReportService reportService;

    @Override
    public void onMessage(String message) {
        try {
            JSONObject data = JSON.parseObject(message);
            Long missionId = data.getLong("missionId");
            String action = data.getString("action");
            log.info("收到报告处理消息 | missionId: {} | action: {}", missionId, action);

            if ("GENERATE".equals(action) || action == null) {
                reportService.generatePdf(missionId);
            }

            if ("PUSH".equals(action)) {
                reportService.pushToCoreSystem(missionId);
            }

            if ("FULL".equals(action)) {
                reportService.generatePdf(missionId);
                Thread.sleep(1000);
                reportService.pushToCoreSystem(missionId);
            }
        } catch (Exception e) {
            log.error("报告处理消费失败 | msg: {}", message, e);
        }
    }
}
