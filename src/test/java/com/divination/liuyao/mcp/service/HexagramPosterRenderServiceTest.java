package com.divination.liuyao.mcp.service;

import com.divination.liuyao.assemblies.enums.DiZhi;
import com.divination.liuyao.assemblies.enums.TianGan;
import com.divination.liuyao.pojo.dto.BaZi;
import com.divination.liuyao.pojo.entity.Prediction;
import com.divination.liuyao.pojo.model.BaGua;
import com.divination.liuyao.pojo.model.Hexagram;
import com.divination.liuyao.pojo.vo.BaGuaVo;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static com.divination.liuyao.pojo.model.Hexagram.createShenSha;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HexagramPosterRenderServiceTest {

    private static final Path DEFAULT_BACKGROUND = Paths.get(
        "package", "status", "images", "default-poster-background.png"
    );

    @Test
    void shouldRenderMultiplePosterPreviewsFromBaguaMap() throws Exception {
        HexagramPosterRenderService service = new HexagramPosterRenderService(
            new HexagramPosterHtmlService(),
            new BrowserScreenshotService()
        );

        List<Map.Entry<String, BaGua>> entries = loadBaguaMapEntries();
        int caseCount = Math.min(6, entries.size());
        assertTrue(caseCount > 0, "BAGUA_MAP 中没有可用卦象");

        for (int i = 0; i < caseCount; i++) {
            String originalName = entries.get(i).getValue().getName();
            boolean staticCase = i % 2 == 1;
            String changedName = staticCase
                ? originalName
                : entries.get((i + 11) % entries.size()).getValue().getName();

            Hexagram hexagram = buildHexagram(originalName, changedName, staticCase, i);
            Prediction prediction = buildPrediction(originalName, changedName, staticCase, i);
            Path output = Paths.get("target", "test-output", String.format("hexagram-poster-preview-%02d.png", i + 1));

            Path actual = service.renderToFile(hexagram, prediction, DEFAULT_BACKGROUND, output);
            assertTrue(Files.exists(actual), "预览图未生成: " + actual);
            assertTrue(Files.size(actual) > 0, "预览图为空文件: " + actual);
            System.out.println("Poster preview generated at: " + actual.toAbsolutePath() + ", staticCase=" + staticCase);
        }
    }

    private Hexagram buildHexagram(String originalName, String changedName, boolean staticCase, int index) {
        BaZi baZi = new BaZi();
        baZi.setYearTianGan(TianGan.JIA);
        baZi.setYearDiZhi(DiZhi.CHEN);
        baZi.setMonthTianGan(TianGan.DIN);
        baZi.setMonthDiZhi(DiZhi.MAO);
        baZi.setDayTianGan(TianGan.JI);
        baZi.setDayDiZhi(DiZhi.YOU);
        baZi.setHourTianGan(TianGan.GUI);
        baZi.setHourDiZhi(DiZhi.WEI);
        baZi.initXunKong();

        BaGua original = BaGua.createBaGuaName(originalName);
        BaGua changed = BaGua.createBaGuaName(changedName);
        BaGuaVo baGuaVo = Hexagram.createBaGuaVoByBagua(original, changed, baZi);

        Hexagram hexagram = new Hexagram();
        hexagram.setOriginalBaGua(baGuaVo.getOriginalBaGua());
        hexagram.setChangedBaGua(baGuaVo.getChangedBaGua());
        hexagram.setExistChanged(baGuaVo.getExistChanged());
        hexagram.setBaZi(baZi);
        hexagram.setLocalDateTime(LocalDateTime.of(2024, 12, 11, 9, 0).plusDays(index));
        String question = staticCase
            ? "测试案例" + (index + 1) + "：关于" + originalName + "静卦的占断。"
            : "测试案例" + (index + 1) + "：关于" + originalName + "变" + changedName + "的占断。";
        hexagram.setQuestionDescription(question);
        hexagram.setQuestionBackground("此案例用于验证六爻海报排版在不同卦例下的稳定性。");
        hexagram.setShenSha(createShenSha(
            baZi.getYearDiZhi(),
            baZi.getMonthDiZhi(),
            baZi.getDayDiZhi(),
            baZi.getDayTianGan()
        ));
        if (baGuaVo.getChangedBaGua() == null) {
            hexagram.setNumber(BaGua.getStringByBaGua(baGuaVo.getOriginalBaGua(), baGuaVo.getOriginalBaGua()));
        } else {
            hexagram.setNumber(BaGua.getStringByBaGua(baGuaVo.getOriginalBaGua(), baGuaVo.getChangedBaGua()));
        }
        return hexagram;
    }

    private Prediction buildPrediction(String originalName, String changedName, boolean staticCase, int index) {
        Prediction prediction = new Prediction();

        Prediction.Description description = new Prediction.Description();
        description.setQuestion(staticCase
            ? "测试案例" + (index + 1) + "：关于" + originalName + "静卦的占断。"
            : "测试案例" + (index + 1) + "：关于" + originalName + "变" + changedName + "的占断。");
        description.setBackground("此案例用于验证六爻海报排版在不同卦例下的稳定性。");
        prediction.setDescription(description);

        Prediction.Gua gua = new Prediction.Gua();
        gua.setZhuGua(originalName);
        gua.setBianGua(changedName);
        prediction.setGua(gua);

        Prediction.Time time = new Prediction.Time();
        time.setYear(buildDatePart("2024", "甲", "辰"));
        time.setMonth(buildDatePart("12", "丁", "卯"));
        time.setDay(buildDatePart(String.valueOf(11 + index), "己", "酉"));
        time.setHour(buildDatePart("9", "癸", "未"));
        prediction.setTime(time);

        return prediction;
    }

    private Prediction.DatePart buildDatePart(String timeValue, String tiangan, String dizhi) {
        Prediction.DatePart datePart = new Prediction.DatePart();
        datePart.setTime(timeValue);
        Prediction.Ganzhi ganzhi = new Prediction.Ganzhi();
        ganzhi.setTiangan(tiangan);
        ganzhi.setDizhi(dizhi);
        datePart.setGanzhi(ganzhi);
        return datePart;
    }

    @SuppressWarnings("unchecked")
    private List<Map.Entry<String, BaGua>> loadBaguaMapEntries() throws Exception {
        Field field = BaGua.class.getDeclaredField("BAGUA_MAP");
        field.setAccessible(true);
        Map<String, BaGua> map = (Map<String, BaGua>) field.get(null);

        List<Map.Entry<String, BaGua>> result = new ArrayList<>(map.entrySet());
        result.removeIf(entry -> entry.getValue() == null || entry.getValue().getName() == null);
        result.sort(Comparator.comparing(Map.Entry::getKey));
        return result;
    }
}

