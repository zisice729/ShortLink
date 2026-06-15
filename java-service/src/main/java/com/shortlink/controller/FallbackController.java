
package com.shortlink.controller;

import com.shortlink.entity.ShortLinkDO;
import com.shortlink.service.FallbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;

/**
 * 跳转降级兜底控制器
 *
 * 功能说明：
 * - 当Redis服务不可用时，作为跳转兜底接口
 * - 直接从MySQL查询短链数据，支持降级跳转
 * - 查询完成后自动回写Redis缓存，恢复正常流程
 *
 * 调用场景：
 * - Nginx探测到Redis不可用时，将请求转发到此接口
 * - 适用于Redis故障期间的低频跳转请求
 *
 * HTTP状态码说明：
 * - 200：跳转成功
 * - 404：短链不存在
 * - 410：短链已过期或已禁用
 */
@Slf4j
@RestController
@RequestMapping("/api/link")
@RequiredArgsConstructor
public class FallbackController {

    /** 降级兜底服务 */
    private final FallbackService fallbackService;

    /**
     * 降级跳转接口
     * 从URL路径中提取短码，查询数据库进行跳转
     *
     * @param req  HTTP请求对象
     * @param resp HTTP响应对象
     * @throws IOException 响应写入异常
     */
    @GetMapping("/fallback")
    public void fallbackRedirect(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // 从请求URI中提取短码
        String uri = req.getRequestURI();
        String shortCode = uri.substring(uri.lastIndexOf("/") + 1);

        log.info("降级跳转请求: shortCode={}", shortCode);

        // Step 1: 从MySQL查询短链数据
        ShortLinkDO data = fallbackService.getByShortCode(shortCode);

        // Step 2: 短链不存在，返回404
        if (data == null) {
            resp.setStatus(404);
            resp.getWriter().write("Not Found");
            return;
        }

        // Step 3: 检查短链是否被禁用，返回410 Gone
        if (data.getStatus() != 1) {
            resp.setStatus(410);
            resp.getWriter().write("Gone");
            return;
        }

        // Step 4: 检查短链是否过期，返回410 Gone
        if (data.getExpireAt() != null && LocalDateTime.now().isAfter(data.getExpireAt())) {
            resp.setStatus(410);
            resp.getWriter().write("Gone");
            return;
        }

        // Step 5: 回写Redis缓存，恢复正常流程
        fallbackService.refreshCache(data);

        // Step 6: 执行302重定向到原始长链接
        resp.sendRedirect(data.getLongUrl());
    }
}
