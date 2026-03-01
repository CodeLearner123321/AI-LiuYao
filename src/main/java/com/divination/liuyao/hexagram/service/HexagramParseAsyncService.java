package com.divination.liuyao.hexagram.service;

import com.divination.liuyao.hexagram.model.HexagramDetectionResult;
import com.divination.liuyao.hexagram.runner.HexagramDetectionRunner;
import com.divination.liuyao.mapper.HexagramCaseMapper;
import com.divination.liuyao.mapper.SourceFileMapper;
import com.divination.liuyao.pojo.entity.HexagramCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 六爻卦例异步解析服务。
 * <p>
 * 由 {@link com.divination.liuyao.service.impl.SourceFileServiceImpl} 在文件入库后调用，
 * 在独立线程中完成：滑动窗口 → AI 检测 → hexagram_case 批量入库 → 更新 source_file 状态。
 * <p>
 * 由于 Spring AOP 代理限制，{@code @Async} 方法必须在独立 Bean 中声明（不能与调用方同类）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HexagramParseAsyncService {

    /** 用于 AI 检测的模型标识，写入 hexagram_case.ai_model */
    private static final String AI_MODEL = "qwenPlus";

    private final HexagramDetectionRunner hexagramDetectionRunner;
    private final SourceFileMapper        sourceFileMapper;
    private final HexagramCaseMapper      hexagramCaseMapper;
    private final ObjectMapper            objectMapper;

    /**
     * 异步执行卦例检测并将结果入库。
     * <p>
     * 使用 {@link AsyncConfig}（taskExecutor 线程池）在后台运行，不阻塞上传接口返回。
     *
     * @param sourceFileId     source_file 表主键
     * @param fullText         已提取的完整文本
     * @param structureVersion 结构版本号（写入 hexagram_case.structure_version）
     */
    @Async("taskExecutor")
    public void asyncDetectAndSave(Long sourceFileId, String fullText, String structureVersion) {
        log.info("[HexagramParseAsync] 开始异步检测，sourceFileId={}, 文本长度={}",
                sourceFileId, fullText.length());

        try {
            // ① 滑动窗口 + AI 检测（NMS 去重后的最终结果）
            List<HexagramDetectionResult> hits = hexagramDetectionRunner.runOnText(fullText);
            log.info("[HexagramParseAsync] 检测完成，共命中 {} 个卦例，sourceFileId={}", hits.size(), sourceFileId);

            // ② 批量保存 hexagram_case
            int caseIndex = 1;
            for (HexagramDetectionResult hit : hits) {
                HexagramCase hexagramCase = buildHexagramCase(
                        sourceFileId, caseIndex++, hit, fullText, structureVersion);
                hexagramCaseMapper.insert(hexagramCase);
            }
            log.info("[HexagramParseAsync] hexagram_case 入库完成，共 {} 条，sourceFileId={}", hits.size(), sourceFileId);

            // ③ 更新 source_file：parse_status=1，total_cases=命中数
            sourceFileMapper.updateParseResult(sourceFileId, 1, hits.size());
            log.info("[HexagramParseAsync] source_file 状态已更新为已解析，sourceFileId={}", sourceFileId);

        } catch (Exception e) {
            log.error("[HexagramParseAsync] 异步检测失败，sourceFileId={}, error={}",
                    sourceFileId, e.getMessage(), e);
            // 更新 parse_status=2（失败），total_cases 保持 0
            try {
                sourceFileMapper.updateParseResult(sourceFileId, 2, 0);
            } catch (Exception updateEx) {
                log.error("[HexagramParseAsync] 更新失败状态也失败了，sourceFileId={}", sourceFileId, updateEx);
            }
        }
    }

    // ------------------------------------------------------------------ //
    //  私有辅助方法
    // ------------------------------------------------------------------ //

    /**
     * 将 {@link HexagramDetectionResult} 转换为 {@link HexagramCase} 实体，
     * 并从原文截取卦例全文。
     */
    private HexagramCase buildHexagramCase(Long sourceFileId,
                                           int caseIndex,
                                           HexagramDetectionResult hit,
                                           String fullText,
                                           String structureVersion) {
        Integer absStart = hit.getAbsoluteStartIndex();
        Integer absEnd   = hit.getAbsoluteEndIndex();

        // 从全文中截取卦例文本（做边界安全处理）
        String caseText = "";
        if (absStart != null && absEnd != null) {
            int s = Math.max(0, absStart);
            int e = Math.min(fullText.length(), absEnd);
            if (s < e) {
                caseText = fullText.substring(s, e);
            }
        }

        // 将 AI 原始回复序列化为 JSON 字符串（存入 raw_ai_json 列）
        String rawAiJson = serializeRawJson(hit);

        // confidence 转 BigDecimal，scale=3
        BigDecimal confidence = hit.getConfidence() != null
                ? BigDecimal.valueOf(hit.getConfidence()).setScale(3, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        HexagramCase entity = new HexagramCase();
        entity.setSourceFileId(sourceFileId);
        entity.setCaseIndex(caseIndex);
        entity.setStartOffset(absStart);
        entity.setEndOffset(absEnd);
        entity.setCaseText(caseText);
        entity.setAiConfidence(confidence);
        entity.setHexagramNumber(hit.getHexagramNumber() != null ? hit.getHexagramNumber() : 1);
        entity.setRawAiJson(rawAiJson);
        entity.setAiModel(AI_MODEL);
        entity.setStructureVersion(structureVersion);
        return entity;
    }

    /**
     * 将检测结果中的关键字段序列化为 JSON 字符串，用于存入 raw_ai_json。
     * 只保留 AI 返回的字段（start/end/confidence/hexagram_number/preview/postview + rawAiResponse），
     * transient 的内部字段不存储。
     */
    private String serializeRawJson(HexagramDetectionResult hit) {
        try {
            java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
            map.put("start_offset", hit.getStartOffset());
            map.put("end_offset", hit.getEndOffset());
            map.put("confidence", hit.getConfidence());
            map.put("hexagram_number", hit.getHexagramNumber());
            map.put("preview", hit.getPreview());
            map.put("postview", hit.getPostview());
            map.put("raw_ai_response", hit.getRawAiResponse());
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            log.warn("[HexagramParseAsync] raw_ai_json 序列化失败: {}", e.getMessage());
            return "{}";
        }
    }
}
