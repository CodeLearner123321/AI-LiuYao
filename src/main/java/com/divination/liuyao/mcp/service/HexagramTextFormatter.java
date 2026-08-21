package com.divination.liuyao.mcp.service;

import com.divination.liuyao.pojo.dto.BaZi;
import com.divination.liuyao.pojo.model.Hexagram;
import com.divination.liuyao.util.BaZiUtil;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class HexagramTextFormatter {

    public String format(Hexagram hexagram) {
        StringBuilder builder = new StringBuilder();

        appendIfPresent(builder, "问题", hexagram.getQuestionDescription());
        appendIfPresent(builder, "背景", hexagram.getQuestionBackground());

        String timeText = buildTimeText(hexagram);
        appendIfPresent(builder, "时间", timeText);
        appendIfPresent(builder, "卦象", hexagram.getGuaStringByPosition(hexagram.isExistChanged()));
        appendIfPresent(builder, "卦码", hexagram.getNumber());

        if (hexagram.getShenSha() != null && !hexagram.getShenSha().isEmpty()) {
            appendIfPresent(builder, "神煞", hexagram.getShenShaString());
        }

        builder.append("六爻:\n");
        for (int i = 5; i >= 0; i--) {
            builder.append("- ").append(hexagram.getYaoStringByPosition(i, hexagram.isExistChanged())).append("\n");
        }

        return builder.toString().trim();
    }

    private String buildTimeText(Hexagram hexagram) {
        if (hexagram.getCustomTime() != null && !hexagram.getCustomTime().isBlank()) {
            return hexagram.getCustomTime();
        }

        BaZi baZi = hexagram.getBaZi();
        if (baZi == null) {
            if (hexagram.getLocalDateTime() != null) {
                return BaZiUtil.getAllByLocalDateTime(hexagram.getLocalDateTime());
            }
            return null;
        }

        String baZiText = baZi.toString();
        if (Objects.equals("", baZiText == null ? null : baZiText.trim())) {
            return null;
        }
        return baZiText.trim();
    }

    private void appendIfPresent(StringBuilder builder, String label, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        builder.append(label).append(": ").append(value.trim()).append("\n");
    }
}
