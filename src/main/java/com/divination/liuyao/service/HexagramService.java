package com.divination.liuyao.service;

import com.alibaba.dashscope.utils.JsonUtils;
import com.divination.liuyao.assemblies.enums.DiZhi;
import com.divination.liuyao.assemblies.enums.TianGan;
import com.divination.liuyao.pojo.dto.BaGuaDto;
import com.divination.liuyao.pojo.dto.BaZi;
import com.divination.liuyao.pojo.dto.CastDto;
import com.divination.liuyao.pojo.entity.Prediction;
import com.divination.liuyao.pojo.entity.User;
import com.divination.liuyao.pojo.enums.AITaskType;
import com.divination.liuyao.pojo.enums.PaymentType;
import com.divination.liuyao.pojo.model.AiResult;
import com.divination.liuyao.pojo.model.BaGua;
import com.divination.liuyao.pojo.model.Hexagram;
import com.divination.liuyao.pojo.vo.BaGuaVo;
import com.divination.liuyao.pojo.vo.RecognizeImageVo;
import com.divination.liuyao.service.factory.LLMServiceFactory;
import com.divination.liuyao.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static com.divination.liuyao.pojo.model.Hexagram.createShenSha;


@Slf4j
@Service
public class HexagramService {

    @Autowired
    private LLMServiceFactory llmServiceFactory;
    @Autowired
    private PaymentService paymentService;
    // 爻值常量
    public static final int LAO_YIN = 0;  // 老阴
    public static final int SHAO_YANG = 1;  // 少阳
    public static final int SHAO_YIN = 2;  // 少阴
    public static final int LAO_YANG = 3;  // 老阳


    /**
     *  根据前端信息生成卦象
     * @param baGuaDto
     * @return
     */
    public BaGuaVo calculateLiuYao(BaGuaDto baGuaDto) {
        switch (baGuaDto.getCastType()) {
            case TIME:
                // 时间起卦
                return Hexagram.createBaGuaVoByTimestamp(baGuaDto);
            case RANDOM:
            case MANUAL:
                // 数字起卦
                return Hexagram.createBaGuaVoByNumber(baGuaDto);
            case IMAGE:
                return Hexagram.createBaGuaVoByNumber2(baGuaDto);
            default:
                throw new IllegalArgumentException("卦象创建失败，参数错误,baGuaDto:" + JsonUtil.toJson(baGuaDto));
        }
    }

    /**
     * 上传图片，识别文字
     */
    @Transactional(rollbackFor = Exception.class)
    public RecognizeImageVo recognizeTextByImage(@RequestParam("file") MultipartFile file) {
        if(file.isEmpty()) {
            return new RecognizeImageVo();
        }
        String fileName = file.getOriginalFilename();
        User user = UserContextHolder.getUser();
        String username = user.getUserName();
        String ossPath = "recognizeImage/" + username;
        String ossUrl;
        try {
            ossUrl = OSSUtil.uploadFile(ossPath, fileName, file.getInputStream());
        } catch (Exception e) {
            log.error("文件上传OSS失败", e);
            return new RecognizeImageVo();
        }

        String jsonTemplate = AIDocJsonBuilder.generateJsonWithNotes(Prediction.class);
        Map<String, Object> data = new HashMap<>();
        data.put("guaList", ConstantUtil.GUA_LIST);
        data.put("jsonTemplate", jsonTemplate);
        String finalPrompt = FreemarkerUtil.render("prediction_prompt.ftl", data);


        RecognizeImageVo recognizeImageVo = new RecognizeImageVo();
        AiResult aiResult = llmServiceFactory.generateTextByImage(ConstantUtil.IMAGE_SYSTEM_PROMPT ,
                finalPrompt, ossUrl);
        log.info("AI图片分析结果：" + aiResult.getText());
        BigDecimal price = paymentService.confirmPay(PaymentType.BALANCE_PAYMENT, AITaskType.IMAGE, user.getId(), aiResult);
        Prediction prediction = JsonUtils.fromJson(aiResult.getText(), Prediction.class);
        Hexagram hexagram = calculateLiuYaoByImage(prediction);
        recognizeImageVo.setHexagram(hexagram);
        recognizeImageVo.setPrice(price.setScale(2, RoundingMode.HALF_UP));

        return recognizeImageVo;
    }

    public Hexagram castHexagram(CastDto castDto) {
        BaGuaVo baGuaVo = calculateLiuYao(castDto);
        
        // 创建新的Hexagram对象，并复制属性
        Hexagram hexagram = new Hexagram();
        hexagram.setOriginalBaGua(baGuaVo.getOriginalBaGua());
        hexagram.setChangedBaGua(baGuaVo.getChangedBaGua());
        hexagram.setExistChanged(baGuaVo.getExistChanged());
        hexagram.setShenSha(baGuaVo.getShenSha());
        hexagram.setLocalDateTime(baGuaVo.getLocalDateTime());
        hexagram.setCustomTime(baGuaVo.getCustomTime());

        return hexagram;
    }

    /**
     * 根据识别出的图像内容生成卦象
     */
    public Hexagram calculateLiuYaoByImage(Prediction prediction) {
        if(prediction == null || prediction.getGua() == null || prediction.getGua().getZhuGua() == null || !prediction.check()) {
            throw new IllegalArgumentException("参数错误：{}" + JsonUtil.toJson(prediction));
        }
        BaZi baZi = new BaZi();
        safeDiZhi(() -> prediction.getTime().getYear().getGanzhi().getDizhi())
                .ifPresent(baZi::setYearDiZhi);
        safeDiZhi(() -> prediction.getTime().getMonth().getGanzhi().getDizhi())
                .ifPresent(baZi::setMonthDiZhi);
        safeDiZhi(() -> prediction.getTime().getDay().getGanzhi().getDizhi())
                .ifPresent(baZi::setDayDiZhi);
        safeDiZhi(() -> prediction.getTime().getHour().getGanzhi().getDizhi())
                .ifPresent(baZi::setHourDiZhi);
        safeTianGan(() -> prediction.getTime().getYear().getGanzhi().getTiangan())
                .ifPresent(baZi::setYearTianGan);
        safeTianGan(() -> prediction.getTime().getMonth().getGanzhi().getTiangan())
                .ifPresent(baZi::setMonthTianGan);
        safeTianGan(() -> prediction.getTime().getDay().getGanzhi().getTiangan())
                .ifPresent(baZi::setDayTianGan);
        safeTianGan(() -> prediction.getTime().getHour().getGanzhi().getTiangan())
                .ifPresent(baZi::setHourTianGan);

        baZi.initXunKong();

        BaGua originalBaGua = BaGua.createBaGuaName(prediction.getGua().getZhuGua());
        String changedGuaName = prediction.getGua().getBianGua();
        if (changedGuaName == null || changedGuaName.isBlank()) {
            changedGuaName = prediction.getGua().getZhuGua();
        }
        BaGua changedBaGua = BaGua.createBaGuaName(changedGuaName);
        if (originalBaGua == null || changedBaGua == null) {
            throw new IllegalArgumentException("未能匹配识别出的卦名");
        }

        String customTime = buildImageCustomTime(prediction.getTime());
        String number = buildImageModeNumber(originalBaGua, changedBaGua);

        BaGuaDto baGuaDto = new BaGuaDto();
        baGuaDto.setCastType(com.divination.liuyao.assemblies.enums.CastType.IMAGE);
        baGuaDto.setCustomTime(customTime);
        baGuaDto.setNumber(number);

        BaGuaVo baGuaVo = Hexagram.createBaGuaVoByNumber2(baGuaDto);

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
                    baZi.getDayTianGan()));
        } else {
            hexagram.setShenSha(null);
        }
        hexagram.setBaZi(baZi);
        hexagram.setCustomTime(customTime);
        hexagram.setQuestionDescription(prediction.getDescription() == null ? null : prediction.getDescription().getQuestion());
        hexagram.setQuestionBackground(prediction.getDescription() == null ? null : prediction.getDescription().getBackground());
        hexagram.setNumber(number);
        return hexagram;
    }

    private String buildImageModeNumber(BaGua originalBaGua, BaGua changedBaGua) {
        String originalNumber = originalBaGua.getId();
        String changedNumber = changedBaGua.getId();
        if (originalNumber == null || changedNumber == null
                || originalNumber.length() != 6 || changedNumber.length() != 6) {
            throw new IllegalArgumentException("识别出的卦象编号无效");
        }

        StringBuilder number = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            char original = originalNumber.charAt(i);
            char changed = changedNumber.charAt(i);
            if (original == changed) {
                number.append(original);
            } else if (original == '0' && changed == '1') {
                number.append('2');
            } else if (original == '1' && changed == '0') {
                number.append('3');
            } else {
                throw new IllegalArgumentException("识别出的卦象编号包含非法爻值");
            }
        }
        return number.toString();
    }

    private String buildImageCustomTime(Prediction.Time time) {
        if (time == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        appendGanzhiTime(builder, time.getYear(), "年");
        appendGanzhiTime(builder, time.getMonth(), "月");
        appendGanzhiTime(builder, time.getDay(), "日");
        appendGanzhiTime(builder, time.getHour(), "时");
        String text = builder.toString().trim();
        return text.isEmpty() ? null : text;
    }

    private void appendGanzhiTime(StringBuilder builder, Prediction.DatePart datePart, String suffix) {
        if (datePart == null || datePart.getGanzhi() == null) {
            return;
        }
        String tiangan = datePart.getGanzhi().getTiangan();
        String dizhi = datePart.getGanzhi().getDizhi();
        if ((tiangan == null || tiangan.isBlank()) && (dizhi == null || dizhi.isBlank())) {
            return;
        }
        if (builder.length() > 0) {
            builder.append(' ');
        }
        builder.append(tiangan == null ? "" : tiangan)
                .append(dizhi == null ? "" : dizhi)
                .append(suffix);
    }

    private Optional<DiZhi> safeDiZhi(Supplier<String> dizhiSupplier) {
        try {
            return Optional.ofNullable(dizhiSupplier.get())
                    .map(DiZhi::formatDiZhiByName);
        } catch (NullPointerException e) {
            return Optional.empty();
        }
    }

    private Optional<TianGan> safeTianGan(Supplier<String> dizhiSupplier) {
        try {
            return Optional.ofNullable(dizhiSupplier.get())
                    .map(TianGan::getTianGanByName);
        } catch (NullPointerException e) {
            return Optional.empty();
        }
    }
}
