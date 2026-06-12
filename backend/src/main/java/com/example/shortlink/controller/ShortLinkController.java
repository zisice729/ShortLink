
package com.example.shortlink.controller;

import com.example.shortlink.common.dto.request.ShortLinkCreateRequest;
import com.example.shortlink.common.dto.response.ApiResponse;
import com.example.shortlink.common.dto.response.ShortLinkCreateResponse;
import com.example.shortlink.common.dto.response.ShortLinkStatsResponse;
import com.example.shortlink.common.util.ObjectUtil;
import com.example.shortlink.service.ShortLinkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 短链接控制器
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ShortLinkController {

    private final ShortLinkService shortLinkService;

    @PostMapping("/shorten")
    public ApiResponse<ShortLinkCreateResponse> createShortLink(@Valid @RequestBody ShortLinkCreateRequest request) {
        log.info("创建短链接请求: longUrl={}", request.getLongUrl());
        ShortLinkCreateResponse response = shortLinkService.createShortLink(request);
        return ApiResponse.success(response);
    }

    @GetMapping("/shorten/{code}/stats")
    public ApiResponse<ShortLinkStatsResponse> getStats(@PathVariable("code") String shortCode) {
        log.info("查询短链接统计: shortCode={}", shortCode);
        ShortLinkStatsResponse response = shortLinkService.getStats(shortCode);
        return ApiResponse.success(response);
    }

    @PutMapping("/shorten/{code}/disable")
    public ApiResponse<Void> disableShortLink(@PathVariable("code") String shortCode) {
        log.info("禁用短链接: shortCode={}", shortCode);
        shortLinkService.disableShortLink(shortCode);
        return ApiResponse.success(null);
    }

    @PutMapping("/shorten/{code}/enable")
    public ApiResponse<Void> enableShortLink(@PathVariable("code") String shortCode) {
        log.info("启用短链接: shortCode={}", shortCode);
        shortLinkService.enableShortLink(shortCode);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/shorten/{code}")
    public ApiResponse<Void> deleteShortLink(@PathVariable("code") String shortCode) {
        log.info("删除短链接: shortCode={}", shortCode);
        shortLinkService.deleteShortLink(shortCode);
        return ApiResponse.success(null);
    }

    @GetMapping("/shorten/{code}/redirect")
    public ResponseEntity<Void> redirect(@PathVariable("code") String shortCode) {
        log.info("兜底跳转请求: shortCode={}", shortCode);
        String longUrl = shortLinkService.getLongUrl(shortCode);
        
        if (ObjectUtil.isBlank(longUrl)) {
            return ResponseEntity.notFound().build();
        }
        
        shortLinkService.incrementClick(shortCode);
        
        HttpHeaders headers = new HttpHeaders();
        headers.add("Location", longUrl);
        headers.add("Cache-Control", "no-cache");
        
        return ResponseEntity.status(HttpStatus.FOUND).headers(headers).build();
    }
}
