package com.divination.liuyao.hexagram.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 单个六爻卦例的检测结果
 * <p>
 * 对应 AI 返回 JSON 中 cases 数组内的一个元素，记录该卦例在窗口文本内的位置与置信度。
 * AI 现在支持一个窗口返回多个卦例，因此上层使用 {@code List<HexagramDetectionResult>}。
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class HexagramDetectionResult {

    /**
     * 卦例在当前窗口文本内的起始字符偏移（相对于窗口首字符，从 0 计数）
     */
    @JsonProperty("start_offset")
    private Integer startOffset;

    /**
     * 卦例在当前窗口文本内的结束字符偏移（相对于窗口首字符，从 0 计数）
     */
    @JsonProperty("end_offset")
    private Integer endOffset;

    /** AI 对该卦例完整程度的置信度，范围 0~1 */
    @JsonProperty("confidence")
    private Double confidence;

    /**
     * 卦例正文的前 20 个字符（AI 原文照抄）。
     * 用于代码侧锚点定位：通过在 windowContent 中 indexOf(preview) 找到真实起始位置，
     * 避免直接依赖 AI 的数字偏移量带来的误差。
     */
    @JsonProperty("preview")
    private String preview;

    /**
     * 卦例正文的后 20 个字符（AI 原文照抄）。
     * 与 preview 配合，用于校验和修正卦例结束位置。
     */
    @JsonProperty("postview")
    private String postview;

    /**
     * 该占例单元中包含的六爻卦例数量（AI 返回）。
     * 一个占例背景下可能记录了多次起卦，AI 在语义扩展时负责计数。
     * 默认 1；若 AI 未返回则由代码保持默认值。
     */
    @JsonProperty("hexagram_number")
    private Integer hexagramNumber = 1;

    // ---- 以下字段由程序填充，非 AI JSON 字段 ----

    /** 对应的原始窗口，用于将局部偏移映射回全文坐标 */
    private transient TextWindow sourceWindow;

    /**
     * 该卦例在窗口列表中的序号（同一窗口可能有多个卦例）。
     * 由 HexagramDetectionService 填充。
     */
    private transient int caseIndexInWindow;

    /** 调试用：AI 原始回复文本 */
    private transient String rawAiResponse;

    // ---- 计算属性：将窗口内偏移换算回原文绝对位置 ----

    /**
     * 卦例在原始全文中的绝对起始字符下标。
     * 仅当 sourceWindow 和 startOffset 均不为 null 时有效。
     */
    public Integer getAbsoluteStartIndex() {
        if (sourceWindow == null || startOffset == null) return null;
        return sourceWindow.getStartIndex() + startOffset;
    }

    /**
     * 卦例在原始全文中的绝对结束字符下标。
     * 仅当 sourceWindow 和 endOffset 均不为 null 时有效。
     */
    public Integer getAbsoluteEndIndex() {
        if (sourceWindow == null || endOffset == null) return null;
        return sourceWindow.getStartIndex() + endOffset;
    }
}
