package com.divination.liuyao.hexagram.service;

import com.divination.liuyao.hexagram.model.HexagramDetectionResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 六爻卦例检测结果去重器（Non-Maximum Suppression）
 * <p>
 * 由于滑动窗口存在 50% 重叠区，同一个卦例可能被相邻窗口各检测一次，
 * 产生位置高度重叠的重复结果。本类使用 NMS 算法消除重复，
 * 对两两重叠率（IoU）超过阈值的结果只保留置信度更高的那个。
 *
 * <p><b>算法说明（贪心 NMS）：</b>
 * <ol>
 *   <li>按原文绝对起始位置升序排列所有候选结果</li>
 *   <li>从置信度最高的结果开始，依次与后续结果计算 IoU</li>
 *   <li>若 IoU &gt; 阈值，则将后续结果标记为"被抑制"</li>
 *   <li>保留所有未被抑制的结果</li>
 * </ol>
 */
@Slf4j
@Component
public class HexagramResultDeduplicator {

    /** 默认 IoU 阈值：两个区间重叠超过 50% 则认为是同一卦例的重复检测 */
    private static final double DEFAULT_IOU_THRESHOLD = 0.5;

    /**
     * 对多窗口汇总的卦例检测结果进行 NMS 去重（使用默认阈值 0.5）。
     *
     * @param results 所有窗口汇总的原始结果（允许含重复）
     * @return 去重后的结果列表，按原文起始位置升序排列
     */
    public List<HexagramDetectionResult> deduplicate(List<HexagramDetectionResult> results) {
        return deduplicate(results, DEFAULT_IOU_THRESHOLD);
    }

    /**
     * 对多窗口汇总的卦例检测结果进行 NMS 去重（自定义阈值）。
     *
     * @param results      所有窗口汇总的原始结果
     * @param iouThreshold IoU 阈值，超过此值的重叠结果将被抑制
     * @return 去重后的结果列表
     */
    public List<HexagramDetectionResult> deduplicate(
            List<HexagramDetectionResult> results, double iouThreshold) {

        if (results == null || results.size() <= 1) {
            return results == null ? new ArrayList<>() : new ArrayList<>(results);
        }

        // ① 过滤掉位置信息不完整的结果（防御性处理）
        List<HexagramDetectionResult> valid = filterValid(results);
        if (valid.isEmpty()) return new ArrayList<>();

        // ② 按置信度降序排列（保证每轮优先保留置信度高的结果）
        valid.sort(Comparator.comparingDouble(
                r -> -(r.getConfidence() == null ? 0.0 : r.getConfidence())));

        // ③ 贪心 NMS
        boolean[] suppressed = new boolean[valid.size()];
        int suppressedCount = 0;

        for (int i = 0; i < valid.size(); i++) {
            if (suppressed[i]) continue;

            HexagramDetectionResult current = valid.get(i);
            for (int j = i + 1; j < valid.size(); j++) {
                if (suppressed[j]) continue;

                double iou = computeIoU(current, valid.get(j));
                if (iou > iouThreshold) {
                    // current 置信度 >= valid[j]（因为已按置信度降序），抑制 j
                    suppressed[j] = true;
                    suppressedCount++;
                    log.debug("[NMS] 抑制重复结果: 窗口#{}-卦例#{} (IoU={:.2f} > {:.2f}，被窗口#{}-卦例#{}覆盖)",
                            valid.get(j).getSourceWindow().getWindowIndex(),
                            valid.get(j).getCaseIndexInWindow(),
                            iou, iouThreshold,
                            current.getSourceWindow().getWindowIndex(),
                            current.getCaseIndexInWindow());
                }
            }
        }

        // ④ 收集保留结果，并按原文起始位置重新排序，便于调用方按顺序处理
        List<HexagramDetectionResult> retained = new ArrayList<>(valid.size() - suppressedCount);
        for (int i = 0; i < valid.size(); i++) {
            if (!suppressed[i]) {
                retained.add(valid.get(i));
            }
        }
        retained.sort(Comparator.comparingInt(r -> r.getAbsoluteStartIndex() == null ? 0 : r.getAbsoluteStartIndex()));

        log.info("[NMS] 去重完成: 原始 {} 个 → 保留 {} 个（抑制 {} 个重复）",
                valid.size(), retained.size(), suppressedCount);
        return retained;
    }

    // ------------------------------------------------------------------ //
    //  私有方法
    // ------------------------------------------------------------------ //

    /**
     * 计算两个检测结果在原文中的区间重叠率（Intersection over Union）。
     *
     * <pre>
     *   IoU = |A ∩ B| / |A ∪ B|
     *       = intersection_length / (len_A + len_B - intersection_length)
     * </pre>
     *
     * @return IoU 值，范围 [0, 1]；若任一区间长度为 0，返回 0
     */
    private double computeIoU(HexagramDetectionResult a, HexagramDetectionResult b) {
        int s1 = a.getAbsoluteStartIndex();
        int e1 = a.getAbsoluteEndIndex();
        int s2 = b.getAbsoluteStartIndex();
        int e2 = b.getAbsoluteEndIndex();

        int lenA = e1 - s1;
        int lenB = e2 - s2;
        if (lenA <= 0 || lenB <= 0) return 0.0;

        int interStart  = Math.max(s1, s2);
        int interEnd    = Math.min(e1, e2);
        int intersection = Math.max(0, interEnd - interStart);

        if (intersection == 0) return 0.0;

        int union = lenA + lenB - intersection;
        return (double) intersection / union;
    }

    /** 过滤掉绝对坐标为 null 或起始 >= 结束的无效结果 */
    private List<HexagramDetectionResult> filterValid(List<HexagramDetectionResult> results) {
        List<HexagramDetectionResult> valid = new ArrayList<>();
        for (HexagramDetectionResult r : results) {
            Integer s = r.getAbsoluteStartIndex();
            Integer e = r.getAbsoluteEndIndex();
            if (s != null && e != null && e > s) {
                valid.add(r);
            } else {
                log.warn("[NMS] 过滤掉无效结果（坐标异常）: 窗口#{} 卦例#{} start={} end={}",
                        r.getSourceWindow() != null ? r.getSourceWindow().getWindowIndex() : "?",
                        r.getCaseIndexInWindow(), s, e);
            }
        }
        return valid;
    }
}
