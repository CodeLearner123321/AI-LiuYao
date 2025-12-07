package com.divination.liuyao.service;

import cn.hutool.core.util.NumberUtil;
import com.divination.liuyao.assemblies.enums.CastType;
import com.divination.liuyao.exception.BusinessException;
import com.divination.liuyao.mapper.AiLiuyaoHistoryMapper;
import com.divination.liuyao.pojo.dto.BaGuaDto;
import com.divination.liuyao.pojo.entity.AiLiuyaoHistory;
import com.divination.liuyao.pojo.vo.AiLiuyaoHistoryAllVO;
import com.divination.liuyao.pojo.vo.AiLiuyaoHistoryVO;
import com.divination.liuyao.pojo.vo.BaGuaVo;
import com.divination.liuyao.util.UserContextHolder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.divination.liuyao.util.ConstantUtil.AI_ERROR_RESULT;

/**
 * AI六爻历史记录服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiLiuyaoHistoryService {

    private final AiLiuyaoHistoryMapper historyMapper;
    private final ObjectMapper objectMapper;
    private final HexagramService hexagramService;

    /**
     * 获取当前用户的历史记录
     * @return 历史记录列表
     */
    public AiLiuyaoHistoryAllVO getCurrentUserHistory() {
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            return new AiLiuyaoHistoryAllVO();
        }
        AiLiuyaoHistoryAllVO aiLiuyaoHistoryAllVO = new AiLiuyaoHistoryAllVO();
        List<AiLiuyaoHistoryVO> aiLiuyaoHistoryVOS = getUserHistory(userId);
        //将消费1的置为0(消费1代表使用的是一次免费额度)
        aiLiuyaoHistoryVOS.forEach(o -> {
            if(o.getAmount() != null
                    && NumberUtil.isNumber(o.getAmount())
                    && BigDecimal.valueOf(1).compareTo(new BigDecimal(o.getAmount())) == 0){
                o.setAmount(0 + "");
            }
        });
        aiLiuyaoHistoryAllVO.setAiLiuyaoHistoryVOS(aiLiuyaoHistoryVOS);
        List<AiLiuyaoHistoryVO> validList = aiLiuyaoHistoryVOS.stream()
                .filter(vo -> vo.getIsAccurate() != null)
                .collect(Collectors.toList());

        long total = validList.stream()
                .filter(h -> h.getIsAccurate() != null)
                .count();

        if (total == 0) {
            aiLiuyaoHistoryAllVO.setAccuracyRate(BigDecimal.ZERO);
            return aiLiuyaoHistoryAllVO;
        }
        long accurate = validList.stream()
                .filter(h -> Integer.valueOf(1).equals(h.getIsAccurate()))
                .count();
        aiLiuyaoHistoryAllVO.setAccuracyRate(
                BigDecimal.valueOf((double) accurate / total)
                        .setScale(4, BigDecimal.ROUND_HALF_UP)
        );
        return aiLiuyaoHistoryAllVO;
    }



    /**
     * 根据用户ID获取历史记录
     * @param userId 用户ID
     * @return 历史记录列表
     */
    public List<AiLiuyaoHistoryVO> getUserHistory(Long userId) {
        List<AiLiuyaoHistory> histories = historyMapper.findQuestionListByUserId(userId);
        
        return histories.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }
    
    /**
     * 根据历史记录ID获取历史记录
     * @param historyId 历史记录ID
     * @return 历史记录，如果不存在则返回null
     */
    public AiLiuyaoHistoryVO getHistoryById(Long historyId) {
        AiLiuyaoHistory aiLiuyaoHistory = historyMapper.findById(historyId).orElse(null);
        if(aiLiuyaoHistory == null) {
            return null;
        }

        BaGuaDto baGuaDto = aiLiuyaoHistory.convertBaGuaDto();
        //处理卦象问题

        BaGuaVo baGuaVo = hexagramService.calculateLiuYao(baGuaDto);


        return convertToVO(aiLiuyaoHistory, baGuaVo);
    }

    /**
     * 返回的只包含：卦象的所有信息 + 问题 + 问题背景 + 判辞 + 卦象结果
     */
    private AiLiuyaoHistoryVO convertToVO(AiLiuyaoHistory history, BaGuaVo baGuaVo) {
        AiLiuyaoHistoryVO vo = convertToVO(history);
        AiLiuyaoHistoryVO.mergeFromBaGua(vo, baGuaVo);
        // 处理结果数据
        try {
            if (history.getResultData() != null) {
                vo.setResultData(objectMapper.readTree(history.getResultData()));
            }
        } catch (JsonProcessingException e) {
            log.error("解析结果数据失败: {}", e.getMessage(), e);
            vo.setResultData(null);
        }

        return vo;
    }
    
    /**
     * 返回的只有：历史记录ID
     */
    private AiLiuyaoHistoryVO convertToVO(AiLiuyaoHistory history) {
        AiLiuyaoHistoryVO vo = new AiLiuyaoHistoryVO();
        vo.setId(history.getId());
        vo.setQuestion(history.getQuestion());
        vo.setKeyOutcome(history.getKeyOutcome());
        vo.setDurationSeconds(history.getDurationSeconds());
        vo.setUserId(history.getUserId());
        vo.setBackground(history.getBackground());
        vo.setCastType(history.getCastType() != null ? history.getCastType().getDescription() : "");
        vo.setAmount(history.getAmount());
        vo.setIsAccurate(history.getIsAccurate());
        vo.setCustomTime(history.getCustomTime());
        return vo;
    }

    public Boolean deleteById(Long historyId) {
        historyMapper.deleteById(historyId);
        return true;
    }
    
    /**
     * 更新历史记录的准确性反馈
     * @param historyId 历史记录ID
     * @param isAccurate 准确性反馈：0-不准确，1-准确
     * @return 是否更新成功
     */
    public Boolean updateFeedback(Long historyId, Integer isAccurate) {
        // 验证参数
        if (isAccurate == null || (isAccurate != 0 && isAccurate != 1)) {
            throw new BusinessException("准确性反馈参数错误，只能是0（不准确）或1（准确）");
        }
        
        // 验证历史记录是否存在
        AiLiuyaoHistory history = historyMapper.findById(historyId).orElse(null);
        if (history == null) {
            throw new BusinessException("历史记录不存在");
        }
        
        // 验证记录是否属于当前用户
        Long userId = UserContextHolder.getUserId();
        if (userId == null || !history.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该历史记录");
        }

        if(!CastType.IMAGE.equals(history.getCastType()) && !CastType.MANUAL.equals(history.getCastType())){
            throw new BusinessException("为确保准确性，只支持反馈手摇卦和图片起卦");
        }
        if(history.getResultData() != null && history.getResultData().contains(AI_ERROR_RESULT)){
            throw new BusinessException("问题或背景有误,不允许反馈");
        }
        
        // 更新准确性反馈
        int updated = historyMapper.updateIsAccurate(historyId, isAccurate);
        return updated > 0;
    }
}