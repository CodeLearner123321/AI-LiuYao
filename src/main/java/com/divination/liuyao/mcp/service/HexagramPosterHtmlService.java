package com.divination.liuyao.mcp.service;

import com.divination.liuyao.pojo.entity.Prediction;
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
public class HexagramPosterHtmlService {

    private static final DateTimeFormatter CAST_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy年M月d号");
    public static final int POSTER_WIDTH = 1500;
    public static final int POSTER_HEIGHT = 1600;

    public String render(Hexagram hexagram, Prediction prediction, Path backgroundImagePath) {
        Map<String, Object> data = new HashMap<>();
        data.put("title", "六爻卦象");
        data.put("posterWidth", POSTER_WIDTH);
        data.put("posterHeight", POSTER_HEIGHT);
        data.put("backgroundImageUrl", backgroundImagePath.toAbsolutePath().normalize().toUri().toString());
        data.put("question", safeText(hexagram.getQuestionDescription(), "未识别"));
        data.put("background", blankToNull(hexagram.getQuestionBackground()));
        data.put("castTimeText", buildDisplayCastTimeText(hexagram));
        data.put("ganzhiTokens", buildGanzhiTokens(prediction));
        data.put("shenShaList", hexagram.getShenSha() == null ? List.of() : hexagram.getShenSha());

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

        return FreemarkerUtil.render("hexagram_poster.ftl", data);
    }

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
}
