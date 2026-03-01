package com.divination.liuyao.hexagram.service;

import com.divination.liuyao.hexagram.model.TextWindow;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 滑动窗口生成服务
 * <p>
 * 将长文本按固定 windowSize 切分为若干重叠窗口，相邻窗口起点间距为 stepSize。
 * 当 stepSize &lt; windowSize 时，窗口之间存在重叠区域，有助于避免卦例被切断。
 */
@Slf4j
@Service
public class SlidingWindowService {

    /**
     * 对文本进行滑动窗口切分
     *
     * @param text       原始全文（不能为 null）
     * @param windowSize 每个窗口的最大字符数（建议 3000）
     * @param stepSize   相邻窗口的起点间距（建议 1500，产生 50% 重叠）
     * @return 按顺序排列的窗口列表；文本为空时返回空列表
     */
    public List<TextWindow> generateWindows(String text, int windowSize, int stepSize) {
        Assert.isTrue(windowSize > 0, "windowSize 必须大于 0");
        Assert.isTrue(stepSize > 0, "stepSize 必须大于 0");

        if (text == null || text.isEmpty()) {
            log.warn("[SlidingWindowService] 输入文本为空，返回空窗口列表");
            return Collections.emptyList();
        }

        List<TextWindow> windows = new ArrayList<>();
        int textLength = text.length();
        int windowIndex = 0;
        int start = 0;

        while (start < textLength) {
            int end = Math.min(start + windowSize, textLength);
            String content = text.substring(start, end);
            windows.add(new TextWindow(windowIndex, start, end, content));
            windowIndex++;

            // 到达文本末尾时终止，避免产生空内容窗口
            if (end >= textLength) {
                break;
            }
            start += stepSize;
        }

        log.info("[SlidingWindowService] 文本长度={}字符，windowSize={}，stepSize={}，共生成{}个窗口",
                textLength, windowSize, stepSize, windows.size());
        return windows;
    }
}
