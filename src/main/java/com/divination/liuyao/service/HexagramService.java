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
import com.divination.liuyao.service.factory.LLMServiceFactory;
import com.divination.liuyao.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;
import java.util.function.Supplier;

import static com.divination.liuyao.util.ConstantUtil.IMAGE_PROCESSING_PROMPT_WORDS2;

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
            case IMAGE:
                // 数字起卦
                return Hexagram.createBaGuaVoByNumber(baGuaDto);
            default:
                throw new IllegalArgumentException("卦象创建失败，参数错误,baGuaDto:" + JsonUtil.toJson(baGuaDto));
        }
    }

    /**
     * 上传图片，识别文字
     */
    @Transactional(rollbackFor = Exception.class)
    public Hexagram recognizeTextByImage(@RequestParam("file") MultipartFile file) {
        if(file.isEmpty()) {
            return new Hexagram();
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
            return null;
        }
        AiResult aiResult = llmServiceFactory.generateTextByImage(ConstantUtil.IMAGE_SYSTEM_PROMPT + AIDocJsonBuilder.generateJsonWithNotes(Prediction.class),
                IMAGE_PROCESSING_PROMPT_WORDS2, ossUrl);
        log.info("AI图片分析结果：" + aiResult.getText());
        paymentService.confirmPay(PaymentType.BALANCE_PAYMENT, AITaskType.IMAGE, user.getId(),aiResult);
        Prediction prediction = JsonUtils.fromJson(aiResult.getText(), Prediction.class);
        return calculateLiuYaoByImage(prediction);
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

        BaGuaVo baGuaVo = Hexagram.createBaGuaVoByBagua(
                BaGua.createBaGuaName(prediction.getGua().getZhuGua()),
                BaGua.createBaGuaName(prediction.getGua().getBianGua()),
                baZi);

        Hexagram hexagram = new Hexagram();
        hexagram.setOriginalBaGua(baGuaVo.getOriginalBaGua());
        hexagram.setChangedBaGua(baGuaVo.getChangedBaGua());
        hexagram.setExistChanged(baGuaVo.getExistChanged());
        hexagram.setShenSha(null);
        hexagram.setBaZi(baZi);
        hexagram.setQuestionDescription(prediction.getDescription() == null ? null : prediction.getDescription().getQuestion());
        hexagram.setQuestionBackground(prediction.getDescription() == null ? null : prediction.getDescription().getBackground());
        hexagram.setNumber(BaGua.getStringByBaGua(baGuaVo.getOriginalBaGua(), baGuaVo.getChangedBaGua()));
        return hexagram;
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