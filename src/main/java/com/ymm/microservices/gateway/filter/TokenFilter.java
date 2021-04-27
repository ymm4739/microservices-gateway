package com.ymm.microservices.gateway.filter;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.nimbusds.jose.JWSObject;
import com.ymm.microservices.gateway.config.ApiResult;
import com.ymm.microservices.gateway.config.IgnoreUrlsConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;


@Component
@RefreshScope
public class TokenFilter implements GlobalFilter, Ordered {
    private static final Logger log = LoggerFactory.getLogger(TokenFilter.class);
    @Autowired
    private IgnoreUrlsConfig ignoreUrlsConfig;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        ServerHttpRequest serverHttpRequest = exchange.getRequest();
        String uri = serverHttpRequest.getURI().getPath();


        String token = serverHttpRequest.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        ServerHttpResponse response = exchange.getResponse();
        PathMatcher pathMatcher = new AntPathMatcher();

        for (String url : ignoreUrlsConfig.getUrls()) {
            if (pathMatcher.match(url, uri)) {
                log.info("ignored url, {}", uri);
                return chain.filter(exchange);
            }
        }

        String realToken = token.replace("Bearer ", "");
        try {
            JWSObject jwsObject = JWSObject.parse(realToken);
            String jwtStr = jwsObject.getPayload().toString();
            JSONObject jwtObject = JSONObject.parseObject(jwtStr);

            String uid = jwtObject.getString("uid");

            JSONArray jsonArray = jwtObject.getJSONArray("authorities");
            Set<String> authorities = new HashSet<>();

            Iterator<Object> it = jsonArray.iterator();
            while (it.hasNext()) {
                JSONObject obj = (JSONObject) it.next();
                authorities.add(obj.getString("authority"));
            }
            for (String authority : authorities) {
                if (pathMatcher.match(authority, uri)) {

                    ServerHttpRequest req = exchange.getRequest().mutate()
                            .header("uid", uid).build();
                    return chain.filter(exchange.mutate().request(req).build());

                }
            }

            log.info("authorization failed, authorities={}, uri={}", authorities, uri);
            return authError(response, 403, "no authority");
            //return chain.filter(exchange);

        } catch (Exception e) {
            e.printStackTrace();
            return authError(response, 500, "server error, " + e.getMessage());
        }

        //  exchange = exchange.mutate().request(req).build();
        //   ServerHttpRequest req = exchange.getRequest().mutate()
        //           .header(HttpHeaders.AUTHORIZATION, token).build();
        //  return chain.filter(exchange);
    }


    @Override
    public int getOrder() {
        return -100;
    }

    private Mono<Void> authError(ServerHttpResponse resp, int code, String message) {
        resp.setStatusCode(HttpStatus.OK);
        resp.getHeaders().add("Content-Type", "application/json;charset=UTF-8");
        ApiResult returnData = new ApiResult(code, message, null);
        String returnStr = "";
        returnStr = JSONObject.toJSONString(returnData);

        DataBuffer buffer = resp.bufferFactory().wrap(returnStr.getBytes(StandardCharsets.UTF_8));
        return resp.writeWith(Flux.just(buffer));
    }

}
