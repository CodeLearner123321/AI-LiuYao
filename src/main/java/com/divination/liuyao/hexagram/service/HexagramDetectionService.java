package com.divination.liuyao.hexagram.service;

import com.divination.liuyao.assemblies.enums.LLMServiceType;
import com.divination.liuyao.assemblies.enums.ModelType;
import com.divination.liuyao.hexagram.model.AiDetectionResponse;
import com.divination.liuyao.hexagram.model.HexagramDetectionResult;
import com.divination.liuyao.hexagram.model.TextWindow;
import com.divination.liuyao.pojo.model.AiResult;
import com.divination.liuyao.service.LLMService;
import com.divination.liuyao.service.factory.LLMServiceFactory;
import com.divination.liuyao.util.FreemarkerUtil;
import com.divination.liuyao.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 六爻卦例 AI 检测服务
 * <p>
 * 对单个滑动窗口调用大语言模型，检测其中所有完整六爻卦例，
 * 支持一个窗口内返回多个卦例（{@code List<HexagramDetectionResult>}）。
 * <p>
 * 提示词通过 Freemarker 模板渲染，与代码解耦，便于独立调整。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HexagramDetectionService {

    private static final String SYSTEM_PROMPT_TEMPLATE = "hexagram_detection_system_prompt.ftl";
    private static final String USER_PROMPT_TEMPLATE   = "hexagram_detection_user_prompt.ftl";

    /** preview / postview 精确匹配时使用的最大字符数 */
    private static final int PREVIEW_MATCH_MAX_LEN = 20;

    /** 逐步缩短匹配时的候选长度序列 */
    private static final int[] FALLBACK_LENGTHS = {15, 12, 10, 8, 6};

    private final LLMServiceFactory llmServiceFactory;

    // ------------------------------------------------------------------ //
    //  核心检测方法
    // ------------------------------------------------------------------ //

    public List<HexagramDetectionResult> detectHexagramCases(TextWindow window) {
        String windowContent = window.getContent();
        log.info("[HexagramDetectionService] 开始检测窗口#{} (startIndex={}, 字符数={})",
                window.getWindowIndex(), window.getStartIndex(), windowContent.length());

        String systemPrompt = buildSystemPrompt();
        String userPrompt   = buildUserPrompt(windowContent);

        String rawResponse = null;
        try {
            LLMService llmService = llmServiceFactory.getLLMService(LLMServiceType.DASHSCOPE);
            AiResult aiResult = llmService.generateText(
                    systemPrompt, userPrompt, ModelType.qwenPlus, null, false);

            rawResponse = aiResult.getText();
            log.debug("[HexagramDetectionService] 窗口#{} AI 原始回复: {}",
                    window.getWindowIndex(), rawResponse);

            List<HexagramDetectionResult> cases = parseAndFillResults(rawResponse, window);
            log.info("[HexagramDetectionService] 窗口#{} 检测完成，共发现 {} 个卦例",
                    window.getWindowIndex(), cases.size());
            return cases;

        } catch (Exception e) {
            log.error("[HexagramDetectionService] 窗口#{} 处理异常: {}",
                    window.getWindowIndex(), e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    // ------------------------------------------------------------------ //
    //  提示词构建
    // ------------------------------------------------------------------ //

    private String buildSystemPrompt() {
        return FreemarkerUtil.render(SYSTEM_PROMPT_TEMPLATE, new HashMap<>());
    }

    private String buildUserPrompt(String windowContent) {
        Map<String, Object> params = new HashMap<>();
        params.put("windowContent", windowContent);
        params.put("contentLength", windowContent.length());
        return FreemarkerUtil.render(USER_PROMPT_TEMPLATE, params);
    }

    // ------------------------------------------------------------------ //
    //  AI 响应解析（含多级兜底）
    // ------------------------------------------------------------------ //

    /**
     * 从 AI 原始回复中提取并解析检测结果。
     * <p>
     * 解析策略（三级降级）：
     * <ol>
     *   <li>正常 JSON 解析</li>
     *   <li>JSON 截断修复后解析（AI preview 过长导致 JSON 被截断的常见场景）</li>
     *   <li>正则逐字段提取（JSON 结构严重损坏时的最终兜底）</li>
     * </ol>
     */
    private List<HexagramDetectionResult> parseAndFillResults(
            String rawResponse, TextWindow window) throws Exception {

        if (rawResponse == null || rawResponse.isBlank()) {
            log.warn("[HexagramDetectionService] 窗口#{} AI 回复为空", window.getWindowIndex());
            return Collections.emptyList();
        }

        String cleaned = LLMServiceFactory.cleanJson(rawResponse);
        String jsonStr  = extractJsonObject(cleaned);

        // 级别 1：正常解析
        AiDetectionResponse response = JsonUtil.fromJson(jsonStr, AiDetectionResponse.class);

        // 级别 2：JSON 被截断时尝试修复
        if (response == null || !response.hasCases()) {
            log.warn("[HexagramDetectionService] 窗口#{} 正常解析失败，尝试修复截断 JSON",
                    window.getWindowIndex());
            String repaired = repairTruncatedJson(jsonStr);
            if (repaired != null) {
                response = JsonUtil.fromJson(repaired, AiDetectionResponse.class);
            }
        }

        // 级别 3：正则兜底提取
        if (response == null || !response.hasCases()) {
            log.warn("[HexagramDetectionService] 窗口#{} JSON 修复仍失败，使用正则提取",
                    window.getWindowIndex());
            response = extractByRegex(jsonStr);
        }

        if (response == null || !response.hasCases()) {
            return Collections.emptyList();
        }

        String windowContent = window.getContent();
        List<HexagramDetectionResult> cases = response.getCases();
        for (int i = 0; i < cases.size(); i++) {
            HexagramDetectionResult result = cases.get(i);
            result.setSourceWindow(window);
            result.setCaseIndexInWindow(i);
            result.setRawAiResponse(rawResponse);
            correctOffsetsByPreview(result, windowContent, i);
            clampOffsets(result, windowContent.length());
        }
        return cases;
    }

    /**
     * 修复因 preview 字段内容过长导致 JSON 在字符串中间截断的情况。
     * <p>
     * 策略：找到最后一个完整的 case 对象（以 {@code },} 或 {@code }} 结尾），
     * 在其后追加 {@code ]} 和 {@code }} 使 JSON 合法。
     */
    private String repairTruncatedJson(String jsonStr) {
        if (jsonStr == null) return null;

        // 找到 cases 数组中最后一个完整 case 对象的结束位置
        // 完整 case 对象以 "}," 或 "}" + 空白 + "]" 结尾
        int lastClose = -1;
        for (int i = jsonStr.length() - 1; i >= 0; i--) {
            if (jsonStr.charAt(i) == '}') {
                lastClose = i;
                break;
            }
        }

        if (lastClose <= 0) return null;

        String repaired = jsonStr.substring(0, lastClose + 1) + "]}";
        // 快速校验：修复后必须包含 "cases"
        if (!repaired.contains("\"cases\"")) return null;

        log.debug("[HexagramDetectionService] JSON 修复完成，截断位置={}", lastClose);
        return repaired;
    }

    /**
     * 当 JSON 结构损坏无法解析时，用正则从原始字符串中逐字段提取卦例信息。
     * 仅提取数值字段（start_offset / end_offset / confidence），preview 可选提取。
     */
    private AiDetectionResponse extractByRegex(String jsonStr) {
        if (jsonStr == null) return null;

        // 匹配每组连续出现的三个数值字段，容忍字段顺序不同
        Pattern startP          = Pattern.compile("\"start_offset\"\\s*:\\s*(\\d+)");
        Pattern endP            = Pattern.compile("\"end_offset\"\\s*:\\s*(\\d+)");
        Pattern confidenceP     = Pattern.compile("\"confidence\"\\s*:\\s*([0-9.]+)");
        Pattern hexagramNumberP = Pattern.compile("\"hexagram_number\"\\s*:\\s*(\\d+)");
        // preview / postview 只取第一行非空白内容（避免捕获大段换行）
        Pattern previewP  = Pattern.compile("\"preview\"\\s*:\\s*\"([^\"\\n\\r]{3,25})");
        Pattern postviewP = Pattern.compile("\"postview\"\\s*:\\s*\"([^\"\\n\\r]{3,25})");

        Matcher startMatcher = startP.matcher(jsonStr);
        List<HexagramDetectionResult> cases = new ArrayList<>();

        while (startMatcher.find()) {
            int blockStart = Math.max(0, startMatcher.start() - 10);
            int blockEnd   = Math.min(jsonStr.length(), startMatcher.end() + 400);
            String block   = jsonStr.substring(blockStart, blockEnd);

            Matcher endM  = endP.matcher(block);
            Matcher confM = confidenceP.matcher(block);
            if (!endM.find() || !confM.find()) continue;

            HexagramDetectionResult r = new HexagramDetectionResult();
            r.setStartOffset(Integer.parseInt(startMatcher.group(1)));
            r.setEndOffset(Integer.parseInt(endM.group(1)));
            r.setConfidence(Double.parseDouble(confM.group(1)));

            Matcher hexNumM = hexagramNumberP.matcher(block);
            if (hexNumM.find()) {
                r.setHexagramNumber(Integer.parseInt(hexNumM.group(1)));
            }
            Matcher previewM = previewP.matcher(block);
            if (previewM.find()) {
                r.setPreview(previewM.group(1));
            }
            Matcher postviewM = postviewP.matcher(block);
            if (postviewM.find()) {
                r.setPostview(postviewM.group(1));
            }

            cases.add(r);
            log.info("[HexagramDetectionService] 正则提取到卦例: start={}, end={}, confidence={}, hexagram_number={}, preview=[{}], postview=[{}]",
                    r.getStartOffset(), r.getEndOffset(), r.getConfidence(),
                    r.getHexagramNumber(), r.getPreview(), r.getPostview());
        }

        AiDetectionResponse resp = new AiDetectionResponse();
        resp.setCases(cases);
        return resp;
    }

    // ------------------------------------------------------------------ //
    //  preview 锚点定位（多级匹配）
    // ------------------------------------------------------------------ //

    /**
     * 基于 preview（起始锚点）和 postview（结束锚点）对 AI 返回的偏移量进行校正。
     * <p>
     * 起始位置：取 preview 前 20 字做多级 indexOf 匹配。<br>
     * 结束位置：优先用 postview 后 20 字反向锚定；若 postview 匹配失败则降级为 AI 长度推算。
     * <p>
     * 匹配策略（逐级降级）：
     * <ol>
     *   <li>取前 20 字精确 indexOf</li>
     *   <li>归一化空白后再次匹配</li>
     *   <li>逐步缩短至 15/12/10/8/6 字，每步重复①②</li>
     *   <li>全部失败则保留 AI 原始偏移量</li>
     * </ol>
     */
    private void correctOffsetsByPreview(HexagramDetectionResult result,
                                         String windowContent, int caseIndex) {
        String preview = result.getPreview();
        if (preview == null || preview.isBlank()) {
            log.warn("[HexagramDetectionService] 卦例#{} 未返回 preview，保留 AI 原始偏移量", caseIndex);
            return;
        }

        // ---- 1. 用 preview 定位起始位置 ----
        String startAnchor = preview.length() > PREVIEW_MATCH_MAX_LEN
                ? preview.substring(0, PREVIEW_MATCH_MAX_LEN)
                : preview;

        int trueStart = findInContent(startAnchor, windowContent);
        if (trueStart < 0) {
            for (int len : FALLBACK_LENGTHS) {
                if (startAnchor.length() <= len) continue;
                trueStart = findInContent(startAnchor.substring(0, len), windowContent);
                if (trueStart >= 0) {
                    log.debug("[HexagramDetectionService] 卦例#{} 使用缩短 preview anchor({}字) 匹配成功",
                            caseIndex, len);
                    break;
                }
            }
        }

        if (trueStart < 0) {
            log.warn("[HexagramDetectionService] 卦例#{} preview 全部匹配失败，保留 AI 原始偏移量。"
                    + "preview anchor=[{}]", caseIndex, startAnchor);
            return;
        }

        // ---- 2. 用 postview 定位结束位置 ----
        int trueEnd = resolveEndByPostview(result, windowContent, trueStart, caseIndex);

        log.debug("[HexagramDetectionService] 卦例#{} 偏移量校正: AI({}, {}) → 锚点({}, {})  "
                + "preview=[{}] postview=[{}]",
                caseIndex, result.getStartOffset(), result.getEndOffset(),
                trueStart, trueEnd, preview, result.getPostview());

        result.setStartOffset(trueStart);
        result.setEndOffset(trueEnd);
    }

    /**
     * 用 postview 反向定位卦例的结束位置。
     * <p>
     * 在 windowContent 中找到 postview 末尾出现的位置，从而精确确定 endOffset。
     * 若匹配失败则降级使用 AI 给的长度推算。
     *
     * @param trueStart 已确认的起始位置（用于兜底推算）
     * @return 确认后的结束位置
     */
    private int resolveEndByPostview(HexagramDetectionResult result,
                                     String windowContent, int trueStart, int caseIndex) {
        String postview = result.getPostview();

        if (postview != null && !postview.isBlank()) {
            String endAnchor = postview.length() > PREVIEW_MATCH_MAX_LEN
                    ? postview.substring(postview.length() - PREVIEW_MATCH_MAX_LEN)
                    : postview;

            int anchorPos = findInContent(endAnchor, windowContent);
            if (anchorPos < 0) {
                for (int len : FALLBACK_LENGTHS) {
                    if (endAnchor.length() <= len) continue;
                    String shorter = endAnchor.substring(endAnchor.length() - len);
                    anchorPos = findInContent(shorter, windowContent);
                    if (anchorPos >= 0) {
                        log.debug("[HexagramDetectionService] 卦例#{} 使用缩短 postview anchor({}字) 匹配成功",
                                caseIndex, len);
                        endAnchor = shorter;
                        break;
                    }
                }
            }

            if (anchorPos >= 0 && anchorPos > trueStart) {
                // postview 匹配成功：结束位置 = postview 锚点位置 + 锚点长度
                return Math.min(anchorPos + endAnchor.length(), windowContent.length());
            }
            log.debug("[HexagramDetectionService] 卦例#{} postview 匹配失败，降级使用 AI 长度推算。"
                    + "postview=[{}]", caseIndex, postview);
        }

        // postview 不可用时：用 AI 的 end-start 长度从真实起始推算
        int aiLength = (result.getStartOffset() != null && result.getEndOffset() != null)
                ? result.getEndOffset() - result.getStartOffset()
                : 0;
        int trueLength = Math.max(aiLength, PREVIEW_MATCH_MAX_LEN);
        return Math.min(trueStart + trueLength, windowContent.length());
    }

    /**
     * 在 windowContent 中搜索 anchor：
     * 先精确匹配，若失败则对空白符归一化后再匹配。
     *
     * @return 匹配起始位置，未找到返回 -1
     */
    private int findInContent(String anchor, String windowContent) {
        // 精确匹配
        int pos = windowContent.indexOf(anchor);
        if (pos >= 0) return pos;

        // 空白归一化匹配：把连续空白（空格/换行/制表）都压缩为单个空格
        String normalizedAnchor  = normalizeWhitespace(anchor);
        String normalizedContent = normalizeWhitespace(windowContent);
        int normalizedPos = normalizedContent.indexOf(normalizedAnchor);
        if (normalizedPos < 0) return -1;

        // 将归一化后的位置映射回原始字符串的位置
        return mapNormalizedPosToOriginal(normalizedPos, windowContent);
    }

    /** 将所有连续空白压缩为单个空格 */
    private String normalizeWhitespace(String text) {
        return text.replaceAll("[\\s　]+", " ").trim();
    }

    /**
     * 将归一化字符串中的位置 normalizedPos 映射回原始字符串中对应的位置。
     * 遍历原始字符串同步计数，跳过被合并的多余空白。
     */
    private int mapNormalizedPosToOriginal(int normalizedPos, String original) {
        int normCount = 0;
        boolean lastWasSpace = false;
        for (int i = 0; i < original.length(); i++) {
            char c = original.charAt(i);
            boolean isSpace = Character.isWhitespace(c) || c == '　';
            if (isSpace) {
                if (!lastWasSpace) {
                    normCount++; // 连续空白只计一次
                }
                lastWasSpace = true;
            } else {
                normCount++;
                lastWasSpace = false;
            }
            if (normCount - 1 == normalizedPos) {
                return i - (isSpace ? 0 : 0);
            }
        }
        return 0;
    }

    // ------------------------------------------------------------------ //
    //  工具方法
    // ------------------------------------------------------------------ //

    private void clampOffsets(HexagramDetectionResult result, int contentLength) {
        if (result.getStartOffset() != null) {
            result.setStartOffset(Math.max(0, Math.min(result.getStartOffset(), contentLength)));
        }
        if (result.getEndOffset() != null) {
            result.setEndOffset(Math.max(0, Math.min(result.getEndOffset(), contentLength)));
        }
    }

    private String extractJsonObject(String text) {
        text = text.trim();
        if (text.startsWith("{")) return text;

        Pattern pattern = Pattern.compile("\\{[\\s\\S]*}");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) return matcher.group();

        throw new IllegalStateException(
                "无法从 AI 回复中提取 JSON 对象，原文片段: "
                        + (text.length() > 200 ? text.substring(0, 200) + "..." : text));
    }
}
