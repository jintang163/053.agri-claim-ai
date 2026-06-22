package com.agri.claim.gateway.handler;

import com.alibaba.csp.sentinel.adapter.spring.webflux.callback.BlockRequestHandler;
import com.agri.claim.common.result.R;
import com.agri.claim.common.result.ResultCode;
import com.alibaba.fastjson2.JSON;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Configuration
public class SentinelConfig {

    @Bean
    public BlockRequestHandler blockRequestHandler() {
        return (ServerWebExchange exchange, Throwable t) -> {
            ServerHttpRequest request = exchange.getRequest();
            ServerHttpResponse response = exchange.getResponse();
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            R<Void> body = R.fail(ResultCode.TOO_MANY_REQUESTS);
            String json = JSON.toJSONString(body);
            DataBuffer buffer = response.bufferFactory()
                    .wrap(json.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(buffer));
        };
    }
}
