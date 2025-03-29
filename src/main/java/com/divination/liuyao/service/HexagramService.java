package com.divination.liuyao.service;

import com.divination.liuyao.pojo.dto.BaGuaDto;
import com.divination.liuyao.pojo.dto.CastDto;
import com.divination.liuyao.pojo.model.Hexagram;
import com.divination.liuyao.pojo.vo.BaGuaVo;
import org.springframework.stereotype.Service;

@Service
public class HexagramService {
    
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
            default:
                throw new IllegalArgumentException("卦象创建失败");
        }
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
        
        return hexagram;
    }
}