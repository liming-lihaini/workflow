package com.flow.engine;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验收门禁 3：全局异常处理器链路 — 业务异常、系统异常、404 均返回统一 Result。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CommonResponseTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate rest;

    @Test
    @DisplayName("业务异常 → code=1000，message 包含异常描述")
    void businessExceptionHandled() {
        ResponseEntity<String> resp = rest.getForEntity("/api/ping/fail", String.class);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).contains("\"code\":1000");
        assertThat(resp.getBody()).contains("演示业务异常");
    }

    @Test
    @DisplayName("系统异常 → code=500，message 为'系统异常'")
    void systemExceptionHandled() {
        ResponseEntity<String> resp = rest.getForEntity("/api/ping/boom", String.class);
        assertThat(resp.getStatusCode().value()).isEqualTo(500);
        assertThat(resp.getBody()).contains("\"code\":500");
        assertThat(resp.getBody()).contains("系统异常");
    }

    @Test
    @DisplayName("404 未匹配路由 → code=404，message 为'资源不存在'")
    void notFoundHandled() {
        ResponseEntity<String> resp = rest.getForEntity("/api/not-exist-route", String.class);
        assertThat(resp.getStatusCode().value()).isEqualTo(404);
        assertThat(resp.getBody()).contains("\"code\":404");
        assertThat(resp.getBody()).contains("资源不存在");
    }
}
