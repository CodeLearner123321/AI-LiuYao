package com.divination.liuyao.hexagram.runner;

import com.divination.liuyao.hexagram.model.HexagramDetectionResult;
import com.divination.liuyao.hexagram.model.TextWindow;
import com.divination.liuyao.hexagram.service.HexagramDetectionService;
import com.divination.liuyao.hexagram.service.HexagramResultDeduplicator;
import com.divination.liuyao.hexagram.service.SlidingWindowService;
import com.divination.liuyao.hexagram.util.FileTextExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 滑动窗口卦例检测主流程编排器
 * <p>
 * 将"文件提取 → 滑动窗口 → AI 检测 → 打印结果"四个步骤串联起来。
 * 支持单个窗口内检测到多个卦例，所有命中结果汇总后返回。
 * <p>
 * 本类不暴露 HTTP 接口，可直接被测试类或后续 Controller 注入调用。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HexagramDetectionRunner {

    private static final int DEFAULT_WINDOW_SIZE = 3000;
    private static final int DEFAULT_STEP_SIZE   = 2000;

    private final SlidingWindowService       slidingWindowService;
    private final HexagramDetectionService   hexagramDetectionService;
    private final HexagramResultDeduplicator deduplicator;

    // ------------------------------------------------------------------ //
    //  公开入口
    // ------------------------------------------------------------------ //

    /**
     * 对指定文件执行完整检测流程（使用默认窗口参数）。
     *
     * @param filePath 文件路径（支持 .txt / .pdf / .docx）
     * @return 所有检测到的卦例列表（跨窗口汇总）
     */
    public List<HexagramDetectionResult> run(String filePath) {
        return run(filePath, DEFAULT_WINDOW_SIZE, DEFAULT_STEP_SIZE);
    }

    /**
     * 对指定文件执行完整检测流程（自定义窗口参数）。
     *
     * @param filePath   文件路径
     * @param windowSize 窗口字符数
     * @param stepSize   步进字符数
     * @return 所有检测到的卦例列表（跨窗口汇总）
     */
    public List<HexagramDetectionResult> run(String filePath, int windowSize, int stepSize) {
        log.info("========== 开始滑动窗口卦例检测 ==========");
        log.info("文件: {}  windowSize={}  stepSize={}", filePath, windowSize, stepSize);

        // ① 文件 → 文本
        String fullText = extractText(filePath);
        if (fullText == null || fullText.isBlank()) {
            log.warn("文件内容为空，终止检测");
            return new ArrayList<>();
        }
        log.info("文本提取完成，总字符数={}", fullText.length());

        // ② 文本 → 滑动窗口
        List<TextWindow> windows = slidingWindowService.generateWindows(fullText, windowSize, stepSize);
        log.info("共生成 {} 个窗口，开始逐窗口 AI 检测...", windows.size());

        // ③ 逐窗口检测，收集所有原始结果（每个窗口可能返回多个，含重叠区重复检测）
        List<HexagramDetectionResult> rawHits = new ArrayList<>();
        for (TextWindow window : windows) {
            List<HexagramDetectionResult> casesInWindow =
                    hexagramDetectionService.detectHexagramCases(window);

            if (casesInWindow.isEmpty()) {
                log.info("[窗口#{}] 未检测到卦例", window.getWindowIndex());
            } else {
                log.info("[窗口#{}] 检测到 {} 个卦例（去重前）", window.getWindowIndex(), casesInWindow.size());
                rawHits.addAll(casesInWindow);
            }
        }

        // ④ NMS 去重：消除重叠窗口对同一卦例的重复检测（IoU 阈值 0.5）
        List<HexagramDetectionResult> allHits = deduplicator.deduplicate(rawHits);

        // ⑤ 打印去重后的最终结果
        allHits.forEach(this::printHit);
        printSummary(windows.size(), rawHits.size(), allHits);
        return allHits;
    }

    // ------------------------------------------------------------------ //
    //  仅传入已提取文本的入口（供 Service 层调用，跳过文件读取步骤）
    // ------------------------------------------------------------------ //

    /**
     * 在已提取的全文上执行检测（使用默认窗口参数）。
     * <p>
     * 与 {@link #run(String)} 的区别：不再从文件读取，直接接受文本字符串。
     * 适用于文件已在外部解析并存储的场景，避免重复 I/O。
     *
     * @param fullText 待检测的完整文本
     * @return 去重后的卦例检测结果列表，按原文顺序排列
     */
    public List<HexagramDetectionResult> runOnText(String fullText) {
        return runOnText(fullText, DEFAULT_WINDOW_SIZE, DEFAULT_STEP_SIZE);
    }

    /**
     * 在已提取的全文上执行检测（自定义窗口参数）。
     *
     * @param fullText   待检测的完整文本
     * @param windowSize 窗口字符数
     * @param stepSize   步进字符数
     * @return 去重后的卦例检测结果列表
     */
    public List<HexagramDetectionResult> runOnText(String fullText, int windowSize, int stepSize) {
        log.info("========== 开始滑动窗口卦例检测（文本模式）==========");
        log.info("文本长度={}  windowSize={}  stepSize={}", fullText.length(), windowSize, stepSize);

        if (fullText == null || fullText.isBlank()) {
            log.warn("传入文本为空，终止检测");
            return new ArrayList<>();
        }

        List<TextWindow> windows = slidingWindowService.generateWindows(fullText, windowSize, stepSize);
        log.info("共生成 {} 个窗口，开始逐窗口 AI 检测...", windows.size());

        List<HexagramDetectionResult> rawHits = new ArrayList<>();
        for (TextWindow window : windows) {
            List<HexagramDetectionResult> casesInWindow =
                    hexagramDetectionService.detectHexagramCases(window);
            if (casesInWindow.isEmpty()) {
                log.info("[窗口#{}] 未检测到卦例", window.getWindowIndex());
            } else {
                log.info("[窗口#{}] 检测到 {} 个卦例（去重前）", window.getWindowIndex(), casesInWindow.size());
                rawHits.addAll(casesInWindow);
            }
        }

        List<HexagramDetectionResult> allHits = deduplicator.deduplicate(rawHits);
        log.info("========== 文本检测结束，共命中 {} 个卦例（去重后，原始 {}）==========",
                allHits.size(), rawHits.size());
        return allHits;
    }

    // ------------------------------------------------------------------ //
    //  私有辅助方法
    // ------------------------------------------------------------------ //

    private String extractText(String filePath) {
        try {
            File file = new File(filePath);
            String text = FileTextExtractor.extract(file);
            log.info("文件读取成功: {} ({} 字符)", file.getName(), text.length());
            return text;
        } catch (Exception e) {
            log.error("文件提取失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /** 打印单个命中卦例的详细信息 */
    private void printHit(HexagramDetectionResult result) {
        TextWindow w = result.getSourceWindow();
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════╗");
        System.out.printf ("║  【检测到卦例】窗口#%d  卦例序号#%d%n",
                w.getWindowIndex(), result.getCaseIndexInWindow());
        System.out.println("╠══════════════════════════════════════════════════╣");
        System.out.printf ("║  窗口在原文位置    : [%d, %d)%n",
                w.getStartIndex(), w.getEndIndex());
        System.out.printf ("║  卦例窗口内偏移    : start=%d  end=%d%n",
                result.getStartOffset(), result.getEndOffset());
        System.out.printf ("║  卦例原文绝对位置  : [%d, %d)%n",
                result.getAbsoluteStartIndex(), result.getAbsoluteEndIndex());
        System.out.printf ("║  置信度           : %.2f%n", result.getConfidence());
        System.out.printf ("║  AI preview       : %s%n",
                result.getPreview() != null ? result.getPreview() : "(无)");
        System.out.printf ("║  AI postview      : %s%n",
                result.getPostview() != null ? result.getPostview() : "(无)");
        System.out.println("╠══════════════════════════════════════════════════╣");

        // 截取卦例内容预览（最多 500 字）
        printSnippet(w.getContent(), result.getStartOffset(), result.getEndOffset());
        System.out.println("╚══════════════════════════════════════════════════╝");
        System.out.println();
    }

    /** 截取并打印卦例片段 */
    private void printSnippet(String windowContent, Integer startOffset, Integer endOffset) {
        if (startOffset == null || endOffset == null) return;
        int s = Math.max(0, startOffset);
        int e = Math.min(windowContent.length(), endOffset);
        if (s >= e) return;

        String snippet = windowContent.substring(s, e);
        if (snippet.length() > 500) {
            snippet = snippet.substring(0, 500) + "...(截断)";
        }
        System.out.println("║  卦例内容预览:");
        // 每行前加边框前缀
        for (String line : snippet.split("\n")) {
            System.out.println("║    " + line);
        }
    }

    /** 打印全局检测汇总（含去重前后对比） */
    private void printSummary(int totalWindows, int rawHitCount,
                              List<HexagramDetectionResult> allHits) {
        System.out.println();
        System.out.println("══════════════════ 检测完成 ══════════════════");
        System.out.printf ("  总窗口数          : %d%n", totalWindows);
        System.out.printf ("  原始检测数（去重前）: %d%n", rawHitCount);
        System.out.printf ("  最终卦例数（去重后）: %d%n", allHits.size());
        System.out.printf ("  NMS 抑制数         : %d%n", rawHitCount - allHits.size());
        if (!allHits.isEmpty()) {
            System.out.println("  命中明细（按原文顺序）:");
            for (int i = 0; i < allHits.size(); i++) {
                HexagramDetectionResult r = allHits.get(i);
                System.out.printf("    %d. 原文位置 [%d, %d)  置信度=%.2f  (来自窗口#%d)  "
                        + "preview=[%s]  postview=[%s]%n",
                        i + 1,
                        r.getAbsoluteStartIndex(),
                        r.getAbsoluteEndIndex(),
                        r.getConfidence(),
                        r.getSourceWindow().getWindowIndex(),
                        r.getPreview() != null ? r.getPreview() : "",
                        r.getPostview() != null ? r.getPostview() : "");
            }
        }
        System.out.println("══════════════════════════════════════════════");
        log.info("========== 检测流程结束，共命中 {} 个卦例（去重后） ==========", allHits.size());
    }
}
