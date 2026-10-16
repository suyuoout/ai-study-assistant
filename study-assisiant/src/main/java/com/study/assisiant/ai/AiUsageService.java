package com.study.assisiant.ai;

import com.study.assisiant.mapper.AiUsageMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * AI 用量记录与统计。记录失败不影响 AI 主流程（best-effort）。
 */
@Service
public class AiUsageService {

    private static final Logger log =
            LoggerFactory.getLogger(AiUsageService.class);

    private final AiUsageMapper usageMapper;

    public AiUsageService(AiUsageMapper usageMapper) {
        this.usageMapper = usageMapper;
    }

    public void record(Long userId, String kind, String model,
                       Integer promptTokens, Integer completionTokens,
                       boolean ownKey, boolean success) {
        if (userId == null) {
            return;
        }
        try {
            AiUsage usage = new AiUsage();
            usage.setUserId(userId);
            usage.setKind(kind);
            usage.setModel(model);
            usage.setPromptTokens(promptTokens);
            usage.setCompletionTokens(completionTokens);
            usage.setOwnKey(ownKey);
            usage.setSuccess(success);
            usageMapper.insert(usage);
        } catch (Exception e) {
            log.warn("记录 AI 用量失败 userId={} kind={}", userId, kind, e);
        }
    }

    /** 今天使用系统默认 Key 的次数（用于额度判断） */
    public long countTodaySystemKey(Long userId) {
        return usageMapper.countSince(userId, false, startOfToday());
    }

    public long countToday(Long userId) {
        return usageMapper.countAllSince(userId, startOfToday());
    }

    public List<Map<String, Object>> countByKindToday(Long userId) {
        return usageMapper.countByKindSince(userId, startOfToday());
    }

    public List<Map<String, Object>> dailyTotals(int days) {
        return usageMapper.dailyTotals(
                startOfToday().minusDays(Math.max(1, days) - 1L));
    }

    public List<Map<String, Object>> topUsers(int days, int limit) {
        return usageMapper.topUsers(
                startOfToday().minusDays(Math.max(1, days) - 1L), limit);
    }

    public long countTotal(int days) {
        return usageMapper.countTotalSince(
                startOfToday().minusDays(Math.max(1, days) - 1L));
    }

    private LocalDateTime startOfToday() {
        return LocalDate.now().atStartOfDay();
    }
}
