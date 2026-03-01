package com.divination.liuyao.hexagram.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * AI 检测接口返回的顶层 JSON 包装对象
 * <p>
 * 结构示例：
 * <pre>
 * {
 *   "cases": [
 *     { "start_offset": 100, "end_offset": 500, "confidence": 0.95 },
 *     { "start_offset": 620, "end_offset": 980, "confidence": 0.88 }
 *   ]
 * }
 * </pre>
 * 若文本中无完整卦例，AI 返回 {@code "cases": []}。
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiDetectionResponse {

    /** 检测到的卦例列表，无卦例时为空列表 */
    private List<HexagramDetectionResult> cases = Collections.emptyList();

    /** 快捷判断：当前窗口是否包含任何卦例 */
    public boolean hasCases() {
        return cases != null && !cases.isEmpty();
    }
}
