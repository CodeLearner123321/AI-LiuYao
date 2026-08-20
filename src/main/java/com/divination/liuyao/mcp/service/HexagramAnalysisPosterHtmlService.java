package com.divination.liuyao.mcp.service;

import com.divination.liuyao.pojo.entity.Prediction;
import com.divination.liuyao.pojo.model.AiResult;
import com.divination.liuyao.pojo.model.BaGua;
import com.divination.liuyao.pojo.model.Hexagram;
import com.divination.liuyao.pojo.model.Yao;
import com.divination.liuyao.util.FreemarkerUtil;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class HexagramAnalysisPosterHtmlService {

    private static final DateTimeFormatter CAST_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy年M月d号");
    public static final int POSTER_WIDTH = 1500;
    private static final int MIN_POSTER_HEIGHT = 1100;
    private static final int MAX_POSTER_HEIGHT = 5000;

    /**
     * 组装带断卦结果的海报模板数据，并输出最终 HTML。
     */
    public RenderedPoster render(Hexagram hexagram, Prediction prediction, AiResult analysisResult, String backgroundImageUrl) {
        int posterHeight = estimatePosterHeight(hexagram, analysisResult);

        Map<String, Object> data = new HashMap<>();
        data.put("title", "六爻卦象");
        data.put("posterWidth", POSTER_WIDTH);
        data.put("posterHeight", posterHeight);
        data.put("backgroundImageUrl", backgroundImageUrl);
        data.put("question", safeText(hexagram.getQuestionDescription(), "未识别"));
        data.put("background", blankToNull(hexagram.getQuestionBackground()));
        data.put("castTimeText", buildDisplayCastTimeText(hexagram));
        data.put("ganzhiTokens", buildGanzhiTokens(prediction));
        data.put("shenShaList", hexagram.getShenSha() == null ? List.of() : hexagram.getShenSha());
        data.put("keyOutcome", safeText(analysisResult == null ? null : analysisResult.getKeyOutcome(), "未生成"));
        data.put("analysisText", safeText(analysisResult == null ? null : analysisResult.getText(), "未生成"));

        String originalName = hexagram.getOriginalBaGua() == null ? "" : hexagram.getOriginalBaGua().getName();
        String changedName = hexagram.getChangedBaGua() == null ? originalName : hexagram.getChangedBaGua().getName();
        boolean hasChanged = hexagram.isExistChanged() && hexagram.getChangedBaGua() != null;

        data.put("originalName", originalName);
        data.put("changedName", changedName);
        data.put("hasChanged", hasChanged);
        data.put("originalYaoRows", buildYaoRows(
            hexagram.getOriginalBaGua(),
            hexagram.getOriginalBaGua(),
            hexagram.getOriginalBaGua(),
            true
        ));
        data.put("changedYaoRows", buildYaoRows(
            hasChanged ? hexagram.getChangedBaGua() : hexagram.getOriginalBaGua(),
            null,
            hexagram.getOriginalBaGua(),
            false
        ));

        return new RenderedPoster(
            FreemarkerUtil.render("hexagram_analysis_poster.ftl", data),
            posterHeight
        );
    }

    /**
     * 生成海报中展示的爻位行数据，供主卦和变卦模板循环渲染。
     */
    private List<Map<String, Object>> buildYaoRows(
        BaGua displayBaGua,
        BaGua movingRefBaGua,
        BaGua liuShenRefBaGua,
        boolean includeRelations
    ) {
        List<Map<String, Object>> rows = new ArrayList<>();
        if (displayBaGua == null || displayBaGua.getYaos() == null) {
            return rows;
        }

        for (int idx = 5; idx >= 0; idx--) {
            Yao displayYao = displayBaGua.getYaos()[idx];
            if (displayYao == null || displayYao.isNull()) {
                continue;
            }

            Yao movingRefYao = pickYaoAt(movingRefBaGua, idx);
            Yao liuShenRefYao = pickYaoAt(liuShenRefBaGua, idx);

            Map<String, Object> row = new HashMap<>();
            row.put("yang", isYang(displayYao));
            row.put("liuShen", resolveLiuShenName(displayYao, liuShenRefYao));
            row.put("liuQin", displayYao.getLiuQin() == null ? "" : displayYao.getLiuQin().getName());
            row.put("tianGanDiZhi", buildGanZhiText(displayYao));
            row.put("fuShen", blankToNull(displayYao.getFuCang()));

            boolean shi = includeRelations && displayYao.getShiOrYing() != null && displayYao.isShiYao();
            boolean ying = includeRelations && displayYao.getShiOrYing() != null && !displayYao.isShiYao();
            boolean moving = includeRelations && movingRefYao != null && Boolean.TRUE.equals(movingRefYao.getIsChange());
            String shiMarker = shi ? "世" : (ying ? "应" : "");
            String movingMarker = "";
            if (moving) {
                movingMarker = isYang(movingRefYao) ? "○" : "×";
            }
            row.put("shiMarker", shiMarker);
            row.put("movingMarker", movingMarker);
            row.put("moving", moving);
            rows.add(row);
        }
        return rows;
    }

    /**
     * 根据卦象结构和文本长度估算海报高度，避免结果图固定高度导致内容过短或被截断。
     */
    private int estimatePosterHeight(Hexagram hexagram, AiResult analysisResult) {
        int height = MIN_POSTER_HEIGHT;
        if (hexagram.isExistChanged() && hexagram.getChangedBaGua() != null) {
            height += 280;
        }
        height += estimateTextHeight(hexagram.getQuestionDescription(), 34, 60);
        height += estimateTextHeight(hexagram.getQuestionBackground(), 34, 60);
        height += estimateTextHeight(analysisResult == null ? null : analysisResult.getKeyOutcome(), 26, 60);
        height += estimateTextHeight(analysisResult == null ? null : analysisResult.getText(), 44, 50);
        return Math.max(MIN_POSTER_HEIGHT, Math.min(MAX_POSTER_HEIGHT, height));
    }

    /**
     * 按“每行可容纳字符数”和“每行高度”估算一段文本大约占用的垂直空间。
     */
    private int estimateTextHeight(String text, int charsPerLine, int lineHeight) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        String normalized = text.replace("\r", "");
        String[] paragraphs = normalized.split("\n", -1);
        int lines = 0;
        for (String paragraph : paragraphs) {
            int length = Math.max(1, paragraph.length());
            lines += Math.max(1, (int) Math.ceil((double) length / charsPerLine));
        }
        return lines * lineHeight;
    }

    private Yao pickYaoAt(BaGua baGua, int index) {
        if (baGua == null || baGua.getYaos() == null || index < 0 || index >= baGua.getYaos().length) {
            return null;
        }
        return baGua.getYaos()[index];
    }

    private boolean isYang(Yao yao) {
        if (yao.getValue() == null) {
            return false;
        }
        return yao.getValue() == 1 || yao.getValue() == 3;
    }

    private String resolveLiuShenName(Yao displayYao, Yao fallbackYao) {
        if (displayYao.getLiuShen() != null) {
            return displayYao.getLiuShen().getName();
        }
        if (fallbackYao != null && fallbackYao.getLiuShen() != null) {
            return fallbackYao.getLiuShen().getName();
        }
        return "";
    }

    private String buildDisplayCastTimeText(Hexagram hexagram) {
        if (hexagram.getLocalDateTime() != null) {
            return CAST_TIME_FORMATTER.format(hexagram.getLocalDateTime());
        }
        if (hexagram.getCustomTime() != null && !hexagram.getCustomTime().isBlank()) {
            return hexagram.getCustomTime();
        }
        return "未识别";
    }

    private List<String> buildGanzhiTokens(Prediction prediction) {
        if (prediction == null || prediction.getTime() == null) {
            return List.of("未识别");
        }
        List<String> tokens = new ArrayList<>();
        addGanzhiPart(tokens, prediction.getTime().getYear());
        addGanzhiPart(tokens, prediction.getTime().getMonth());
        addGanzhiPart(tokens, prediction.getTime().getDay());
        addGanzhiPart(tokens, prediction.getTime().getHour());
        return tokens.isEmpty() ? List.of("未识别") : tokens;
    }

    private void addGanzhiPart(List<String> parts, Prediction.DatePart datePart) {
        if (datePart == null || datePart.getGanzhi() == null) {
            return;
        }
        String tg = datePart.getGanzhi().getTiangan();
        String dz = datePart.getGanzhi().getDizhi();
        if ((tg == null || tg.isBlank()) && (dz == null || dz.isBlank())) {
            return;
        }
        parts.add((tg == null ? "" : tg) + (dz == null ? "" : dz));
    }

    private String buildGanZhiText(Yao yao) {
        StringBuilder builder = new StringBuilder();
        if (yao.getTianGan() != null) {
            builder.append(yao.getTianGan().getName());
        }
        if (yao.getDiZhi() != null) {
            builder.append(yao.getDiZhi().getNameAndShuXin());
        }
        return builder.toString();
    }

    private String safeText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    /**
     * 动态海报渲染结果，包含最终 HTML 以及截图时应使用的高度。
     */
    public static class RenderedPoster {
        private final String html;
        private final int height;

        /**
         * @param html 渲染后的完整 HTML
         * @param height 根据内容估算出的截图高度
         */
        public RenderedPoster(String html, int height) {
            this.html = html;
            this.height = height;
        }

        public String getHtml() {
            return html;
        }

        public int getHeight() {
            return height;
        }
    }
}

