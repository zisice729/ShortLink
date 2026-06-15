
package com.shortlink.controller;

import com.shortlink.entity.dto.GenerateReqDTO;
import com.shortlink.entity.dto.GenerateRespDTO;
import com.shortlink.service.GenerateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 短链生成控制器（写链路核心接口）
 *
 * 功能说明：
 * - 提供短链接生成HTTP接口
 * - 接收长链接，返回生成的短链接
 * - 属于写链路接口，访问频率相对跳转接口较低
 *
 * 接口说明：
 * - POST /api/link/generate
 * - Content-Type: application/json
 * - 请求体：{"rawUrl": "https://example.com/xxx", "expireSecond": 86400}
 *
 * 处理流程：
 * 1. 参数校验（@Valid注解自动校验）
 * 2. 调用GenerateService生成短链
 * 3. 返回短链信息（短码、完整短链接、过期时间）
 */
@Slf4j
@RestController
@RequestMapping("/api/link")
@RequiredArgsConstructor
public class GenerateController {

    /** 短链生成服务 */
    private final GenerateService generateService;

    /**
     * 生成短链接接口
     *
     * @param request 生成请求DTO，包含原始长链接和过期时间
     * @return ResponseEntity<GenerateRespDTO> 生成的短链响应信息
     */
    @PostMapping("/generate")
    public ResponseEntity<GenerateRespDTO> generateShortUrl(@Valid @RequestBody GenerateReqDTO request) {
        log.info("收到短链生成请求: rawUrl={}", request.getRawUrl());
        // 调用服务层生成短链
        GenerateRespDTO response = generateService.generateShortUrl(request);
        return ResponseEntity.ok(response);
    }
}
