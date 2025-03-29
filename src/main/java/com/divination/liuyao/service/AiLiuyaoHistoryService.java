package com.divination.liuyao.service;

import com.divination.liuyao.mapper.AiLiuyaoHistoryMapper;
import com.divination.liuyao.pojo.dto.BaGuaDto;
import com.divination.liuyao.pojo.entity.AiLiuyaoHistory;
import com.divination.liuyao.pojo.vo.AiLiuyaoHistoryVO;
import com.divination.liuyao.pojo.vo.BaGuaVo;
import com.divination.liuyao.util.UserContextHolder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

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
    public List<AiLiuyaoHistoryVO> getCurrentUserHistory() {
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            return List.of();
        }
        
        return getUserHistory(userId);
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
        BaGuaDto baGuaDto = aiLiuyaoHistory.convertBaGuaDto();
        //处理卦象问题

        BaGuaVo baGuaVo = hexagramService.calculateLiuYao(baGuaDto);


        return convertToVO(aiLiuyaoHistory, baGuaVo);
    }

    /**
     * 返回的只包含：卦象的所有信息 + 问题 + 问题背景 + 判辞 + 卦象结果
     */
    private AiLiuyaoHistoryVO convertToVO(AiLiuyaoHistory history, BaGuaVo baGuaVo) {
        AiLiuyaoHistoryVO vo = new AiLiuyaoHistoryVO(baGuaVo);
        vo.setUserId(history.getUserId());
        vo.setQuestion(history.getQuestion());
        vo.setBackground(history.getBackground());
        vo.setKeyOutcome(history.getKeyOutcome());
        vo.setCastType(history.getCastType().getDescription());
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
        return vo;
    }
} 