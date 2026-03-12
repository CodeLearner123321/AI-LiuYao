package com.divination.liuyao.mcp.service;

import com.divination.liuyao.assemblies.enums.DiZhi;
import com.divination.liuyao.assemblies.enums.TianGan;
import com.divination.liuyao.mcp.model.RecognizedHexagramResult;
import com.divination.liuyao.pojo.dto.BaZi;
import com.divination.liuyao.pojo.entity.Prediction;
import com.divination.liuyao.pojo.model.AiResult;
import com.divination.liuyao.pojo.model.BaGua;
import com.divination.liuyao.pojo.model.Hexagram;
import com.divination.liuyao.pojo.vo.BaGuaVo;
import com.divination.liuyao.service.factory.LLMServiceFactory;
import com.divination.liuyao.util.AIDocJsonBuilder;
import com.divination.liuyao.util.ConstantUtil;
import com.divination.liuyao.util.FreemarkerUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;

import static com.divination.liuyao.pojo.model.Hexagram.createShenSha;

@Service
public class HexagramImageRecognitionService {

    private static final java.nio.file.Path DEFAULT_BACKGROUND_IMAGE = Paths.get(
        "package", "status", "images", "default-poster-background.png"
    );

    private final LLMServiceFactory llmServiceFactory;
    private final HexagramTextFormatter hexagramTextFormatter;
    private final HexagramPosterRenderService hexagramPosterRenderService;
    private final ObjectMapper objectMapper;

    public HexagramImageRecognitionService(
        LLMServiceFactory llmServiceFactory,
        HexagramTextFormatter hexagramTextFormatter,
        HexagramPosterRenderService hexagramPosterRenderService,
        ObjectMapper objectMapper
    ) {
        this.llmServiceFactory = llmServiceFactory;
        this.hexagramTextFormatter = hexagramTextFormatter;
        this.hexagramPosterRenderService = hexagramPosterRenderService;
        this.objectMapper = objectMapper;
    }

    public RecognizedHexagramResult recognize(String imageUrl) throws IOException, InterruptedException {
        return recognize(imageUrl, true);
    }

    public RecognizedHexagramResult recognize(String imageUrl, boolean renderImage) throws IOException, InterruptedException {
        String normalizedImageUrl = normalizeImageUrl(imageUrl);

        String prompt = buildPredictionPrompt();
        AiResult aiResult = llmServiceFactory.generateTextByImage(ConstantUtil.IMAGE_SYSTEM_PROMPT, prompt, normalizedImageUrl);
        String cleaned = LLMServiceFactory.cleanJson(aiResult.getText());
        Prediction prediction = objectMapper.readValue(cleaned, Prediction.class);
        Hexagram hexagram = convertPredictionToHexagram(prediction);

        String renderedImageUrl = null;
        boolean imageRenderingImplemented = false;
        String imageRenderingStatus = "已跳过图片渲染";
        if (renderImage) {
            renderedImageUrl = hexagramPosterRenderService.renderAndUpload(
                hexagram,
                prediction,
                deriveSourceFileName(normalizedImageUrl),
                DEFAULT_BACKGROUND_IMAGE
            );
            imageRenderingImplemented = true;
            imageRenderingStatus = "卦象图片渲染完成";
        }

        return new RecognizedHexagramResult(
            normalizedImageUrl,
            normalizedImageUrl,
            renderedImageUrl,
            prediction,
            hexagram,
            hexagramTextFormatter.format(hexagram),
            imageRenderingImplemented,
            imageRenderingStatus
        );
    }

    private String normalizeImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new IllegalArgumentException("imageUrl is required");
        }
        try {
            URI uri = new URI(imageUrl.trim());
            String scheme = uri.getScheme();
            if (scheme == null || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
                throw new IllegalArgumentException("imageUrl must be an http/https URL");
            }
            if (uri.getHost() == null || uri.getHost().isBlank()) {
                throw new IllegalArgumentException("imageUrl must include a valid host");
            }
            return uri.toString();
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException("imageUrl is invalid: " + imageUrl, ex);
        }
    }

    private String deriveSourceFileName(String imageUrl) {
        try {
            URI uri = new URI(imageUrl);
            String path = uri.getPath();
            if (path == null || path.isBlank() || path.endsWith("/")) {
                return "remote-image.png";
            }
            String fileName = Paths.get(path).getFileName().toString();
            return fileName.isBlank() ? "remote-image.png" : fileName;
        } catch (Exception ex) {
            return "remote-image.png";
        }
    }

    private String buildPredictionPrompt() {
        String jsonTemplate = AIDocJsonBuilder.generateJsonWithNotes(Prediction.class);
        Map<String, Object> data = new HashMap<>();
        data.put("guaList", ConstantUtil.GUA_LIST);
        data.put("jsonTemplate", jsonTemplate);
        return FreemarkerUtil.render("prediction_prompt.ftl", data);
    }

    private Hexagram convertPredictionToHexagram(Prediction prediction) {
        if (prediction == null || prediction.getGua() == null || prediction.getGua().getZhuGua() == null || !prediction.check()) {
            throw new IllegalArgumentException("图片识别结果无效");
        }

        BaZi baZi = new BaZi();
        safeDiZhi(() -> prediction.getTime().getYear().getGanzhi().getDizhi()).ifPresent(baZi::setYearDiZhi);
        safeDiZhi(() -> prediction.getTime().getMonth().getGanzhi().getDizhi()).ifPresent(baZi::setMonthDiZhi);
        safeDiZhi(() -> prediction.getTime().getDay().getGanzhi().getDizhi()).ifPresent(baZi::setDayDiZhi);
        safeDiZhi(() -> prediction.getTime().getHour().getGanzhi().getDizhi()).ifPresent(baZi::setHourDiZhi);
        safeTianGan(() -> prediction.getTime().getYear().getGanzhi().getTiangan()).ifPresent(baZi::setYearTianGan);
        safeTianGan(() -> prediction.getTime().getMonth().getGanzhi().getTiangan()).ifPresent(baZi::setMonthTianGan);
        safeTianGan(() -> prediction.getTime().getDay().getGanzhi().getTiangan()).ifPresent(baZi::setDayTianGan);
        safeTianGan(() -> prediction.getTime().getHour().getGanzhi().getTiangan()).ifPresent(baZi::setHourTianGan);
        baZi.initXunKong();

        String originName = prediction.getGua().getZhuGua();
        String changedName = prediction.getGua().getBianGua();
        if (changedName == null || changedName.isBlank()) {
            changedName = originName;
        }

        BaGua originBaGua = BaGua.createBaGuaName(originName);
        BaGua changedBaGua = BaGua.createBaGuaName(changedName);
        if (originBaGua == null || changedBaGua == null) {
            throw new IllegalArgumentException("未能匹配识别出的卦名");
        }

        BaGuaVo baGuaVo = Hexagram.createBaGuaVoByBagua(originBaGua, changedBaGua, baZi);

        Hexagram hexagram = new Hexagram();
        hexagram.setOriginalBaGua(baGuaVo.getOriginalBaGua());
        hexagram.setChangedBaGua(baGuaVo.getChangedBaGua());
        hexagram.setExistChanged(baGuaVo.getExistChanged());
        if (baZi.getYearDiZhi() != null
            && baZi.getMonthDiZhi() != null
            && baZi.getDayDiZhi() != null
            && baZi.getDayTianGan() != null) {
            hexagram.setShenSha(createShenSha(
                baZi.getYearDiZhi(),
                baZi.getMonthDiZhi(),
                baZi.getDayDiZhi(),
                baZi.getDayTianGan()
            ));
        }
        hexagram.setBaZi(baZi);
        hexagram.setCustomTime(buildCustomTimeText(prediction.getTime()));
        hexagram.setQuestionDescription(prediction.getDescription() == null ? null : prediction.getDescription().getQuestion());
        hexagram.setQuestionBackground(prediction.getDescription() == null ? null : prediction.getDescription().getBackground());
        hexagram.setNumber(BaGua.getStringByBaGua(baGuaVo.getOriginalBaGua(), baGuaVo.getChangedBaGua()));
        return hexagram;
    }

    private String buildCustomTimeText(Prediction.Time time) {
        if (time == null) {
            return null;
        }

        StringBuilder builder = new StringBuilder();
        appendDatePart(builder, "年", time.getYear());
        appendDatePart(builder, "月", time.getMonth());
        appendDatePart(builder, "日", time.getDay());
        appendDatePart(builder, "时", time.getHour());
        String text = builder.toString().trim();
        return text.isEmpty() ? null : text;
    }

    private void appendDatePart(StringBuilder builder, String label, Prediction.DatePart datePart) {
        if (datePart == null) {
            return;
        }

        String ganzhi = null;
        if (datePart.getGanzhi() != null) {
            String tiangan = datePart.getGanzhi().getTiangan();
            String dizhi = datePart.getGanzhi().getDizhi();
            if (tiangan != null || dizhi != null) {
                ganzhi = (tiangan == null ? "" : tiangan) + (dizhi == null ? "" : dizhi);
            }
        }

        if ((ganzhi == null || ganzhi.isBlank()) && (datePart.getTime() == null || datePart.getTime().isBlank())) {
            return;
        }

        if (builder.length() > 0) {
            builder.append(' ');
        }
        builder.append(label).append(':');
        if (ganzhi != null && !ganzhi.isBlank()) {
            builder.append(ganzhi);
        }
        if (datePart.getTime() != null && !datePart.getTime().isBlank()) {
            builder.append('(').append(datePart.getTime()).append(')');
        }
    }

    private Optional<DiZhi> safeDiZhi(Supplier<String> supplier) {
        try {
            return Optional.ofNullable(supplier.get()).map(DiZhi::formatDiZhiByName);
        } catch (NullPointerException ex) {
            return Optional.empty();
        }
    }

    private Optional<TianGan> safeTianGan(Supplier<String> supplier) {
        try {
            return Optional.ofNullable(supplier.get()).map(TianGan::getTianGanByName);
        } catch (NullPointerException ex) {
            return Optional.empty();
        }
    }
}
