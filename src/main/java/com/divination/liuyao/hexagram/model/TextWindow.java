package com.divination.liuyao.hexagram.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 滑动窗口文本片段
 * <p>
 * 记录一个窗口在原始文本中的位置信息，以及窗口内容，
 * 便于后续将 AI 返回的局部偏移量映射回原文坐标。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TextWindow {

    /** 窗口序号（从 0 开始，按滑动顺序递增） */
    private int windowIndex;

    /** 在原始全文中的起始字符下标（含） */
    private int startIndex;

    /** 在原始全文中的结束字符下标（不含） */
    private int endIndex;

    /** 窗口内容（截取自原始全文的子字符串） */
    private String content;
}
