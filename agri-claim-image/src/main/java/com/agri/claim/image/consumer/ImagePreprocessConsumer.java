package com.agri.claim.image.consumer;

import com.agri.claim.common.constant.Constants;
import com.agri.claim.image.service.ImageService;
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
        topic = Constants.MQ_TOPIC_IMAGE_PREPROCESS,
        consumerGroup = Constants.MQ_CONSUMER_GROUP_IMAGE
)
public class ImagePreprocessConsumer implements RocketMQListener<String> {

    private final ImageService imageService;

    @Override
    public void onMessage(String message) {
        try {
            JSONObject data = JSON.parseObject(message);
            Long imageId = data.getLong("imageId");
            log.info("收到影像预处理消息 | imageId: {}", imageId);
            imageService.preprocessImage(imageId);
        } catch (Exception e) {
            log.error("影像预处理消费失败 | msg: {}", message, e);
        }
    }
}
