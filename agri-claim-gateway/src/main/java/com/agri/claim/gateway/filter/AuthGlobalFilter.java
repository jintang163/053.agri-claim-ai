package com.agri.claim.gateway.filter;

import cn.hutool.core.util.StrUtil;
import com.agri.claim.common.constant.Constants;
import com.agri.claim.common.result.R;
import com.agri.claim.common.result.ResultCode;
import com.agri.claim.common.utils.JwtUtils;
import com.alibaba.fastjson2.JSON;
import io.jsonwebtoken.Claims;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Data
@Component
@ConfigurationProperties(prefix = "gateway")
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private List<String> whitelist;
    private final StringRedisTemplate stringRedisTemplate;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public AuthGlobalFilter(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        if (isWhitelisted(path)) {
            return chain.filter(exchange);
        }

        String token = request.getHeaders().getFirst(Constants.TOKEN_HEADER);
        if (StrUtil.isNotBlank(token) && token.startsWith(Constants.TOKEN_PREFIX)) {
            token = token.substring(Constants.TOKEN_PREFIX.length());
        }

        if (StrUtil.isBlank(token)) {
            return unauthorizedResponse(exchange, ResultCode.UNAUTHORIZED);
        }

        try {
            Claims claims = JwtUtils.parseToken(token);
            if (JwtUtils.isTokenExpired(claims)) {
                return unauthorizedResponse(exchange, ResultCode.TOKEN_EXPIRED);
            }

            Long userId = JwtUtils.getUserId(claims);
            String userName = JwtUtils.getUserName(claims);
            Long deptId = JwtUtils.getDeptId(claims);
            String roleKey = JwtUtils.getRoleKey(claims);

            String redisKey = Constants.REDIS_USER_KEY + userId;
            String cachedToken = stringRedisTemplate.opsForValue().get(redisKey);
            if (StrUtil.isBlank(cachedToken) || !cachedToken.equals(token)) {
                return unauthorizedResponse(exchange, ResultCode.LOGIN_EXPIRED);
            }

            ServerHttpRequest modifiedRequest = request.mutate()
                    .header(Constants.USER_ID, userId != null ? userId.toString() : "")
                    .header(Constants.USER_NAME, StrUtil.nullToDefault(userName, ""))
                    .header(Constants.DEPT_ID, deptId != null ? deptId.toString() : "")
                    .header(Constants.ROLE_KEY, StrUtil.nullToDefault(roleKey, ""))
                    .build();

            return chain.filter(exchange.mutate().request(modifiedRequest).build());

        } catch (Exception e) {
            log.warn("Token解析失败 | token: {} | error: {}", token, e.getMessage());
            return unauthorizedResponse(exchange, ResultCode.TOKEN_INVALID);
        }
    }

    private boolean isWhitelisted(String path) {
        if (whitelist == null || whitelist.isEmpty()) {
            return false;
        }
        for (String pattern : whitelist) {
            if (pathMatcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange, ResultCode resultCode) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.OK);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        R<Void> body = R.fail(resultCode);
        String json = JSON.toJSONString(body);
        DataBuffer buffer = response.bufferFactory()
                .wrap(json.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
