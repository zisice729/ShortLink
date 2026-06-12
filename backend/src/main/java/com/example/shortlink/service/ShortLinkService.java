
package com.example.shortlink.service;

import com.example.shortlink.common.dto.request.ShortLinkCreateRequest;
import com.example.shortlink.common.dto.response.ShortLinkCreateResponse;
import com.example.shortlink.common.dto.response.ShortLinkStatsResponse;

/**
 * 短链接服务接口
 */
public interface ShortLinkService {

    ShortLinkCreateResponse createShortLink(ShortLinkCreateRequest request);

    String getLongUrl(String shortCode);

    ShortLinkStatsResponse getStats(String shortCode);

    void disableShortLink(String shortCode);

    void enableShortLink(String shortCode);

    void deleteShortLink(String shortCode);

    void incrementClick(String shortCode);
}
