package com.divination.liuyao.pojo.model;

import cn.hutool.core.date.LocalDateTimeUtil;
import com.divination.liuyao.assemblies.enums.CastType;
import com.divination.liuyao.assemblies.enums.DiZhi;
import com.divination.liuyao.assemblies.enums.LiuShen;
import com.divination.liuyao.assemblies.enums.TianGan;
import com.divination.liuyao.pojo.dto.BaGuaDto;
import com.divination.liuyao.pojo.dto.BaZi;
import com.divination.liuyao.pojo.dto.CastDto;
import com.divination.liuyao.pojo.vo.BaGuaVo;
import com.divination.liuyao.util.BaZiUtil;
import com.divination.liuyao.util.ConstantUtil;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 六爻卦象模型
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class Hexagram extends BaGuaVo{

    //六神
    private static final LiuShen[] LIU_SHENS = new LiuShen[]{LiuShen.QINGLONG,LiuShen.ZHUQUE,LiuShen.GOUCHEN,LiuShen.TENGSHE,LiuShen.BAIHU,LiuShen.XUANWU};

    private static final Map<TianGan,LiuShen> TIAN_GAN_LIU_SHEN_MAP = new HashMap<>();

    /**
     * 用于存储AI分析结果
     */
    private String aiAnalysis;


    /**
     * 0-老阴，1-少阳，2-少阴，3-老阳
     * 0:阴 1：阳
     */
    public static BaGuaVo createBaGuaVoByNumber(BaGuaDto baGuaDto) {
        Boolean isChange = false;
        StringBuilder originNumber = new StringBuilder();
        StringBuilder changeNumber = new StringBuilder();
        byte[] isChangeMap = new byte[]{0,0,0,0,0,0};
        String number = baGuaDto.getNumber();

        for (int i = 0; i < number.length(); i++) {
            char c = number.charAt(i);
            if(c == '0' || c == '3'){
                isChange = true;
                changeNumber.append(c == '0' ? '1' : '0');
                isChangeMap[i] = 1;
            } else {
                changeNumber.append(c == '2' ? '0' : '1');
            }
            originNumber.append(c == '0' || c == '2' ? '0' : '1');
        }

        BaGuaVo baGuaVo = new BaGuaVo();
        BaGua originBaGua = BaGua.createBaGua(isChangeMap, originNumber.toString());
        baGuaVo.setOriginalBaGua(BaGua.createBaGua(isChangeMap, originNumber.toString()));
        baGuaVo.setExistChanged(isChange);
        baGuaVo.setChangedBaGua(isChange
            ? BaGua.createChangeBaGua(BaGua.GONG_WEI_TO_SHU_XIN.get(originBaGua.getGongWei()), changeNumber.toString())
            : null);
        baGuaVo.setLocalDateTime(baGuaDto.getCastTime());

        BaZi baZi = BaZiUtil.baziConvertByTime(baGuaDto.getCastTime());
        compositionLiuShen(baGuaVo.getOriginalBaGua(), baZi.getDayTianGan());
        baGuaVo.setShenSha(createShenSha(baZi.getYearDiZhi(), baZi.getMonthDiZhi(), baZi.getDayDiZhi(), baZi.getDayTianGan()));
        baGuaVo.setBaZi(baZi);
        baGuaVo.formatting();
        return baGuaVo;
    }

    /**
     * 构造神煞
     */
    private static List<String> createShenSha(DiZhi yearDiZhi,DiZhi monthDiZhi, DiZhi dayDiZhi, TianGan dayTianGan) {
        List<String> str = new ArrayList<>();
        str.add(ConstantUtil.YI_MA + "-" + dayDiZhi.getYiMa());
        str.add(ConstantUtil.JIAN_XING + "-" + dayDiZhi.getJiangXin());
        str.add(ConstantUtil.XIAN_CHI + "-" + dayDiZhi.getXianChi());
        str.add(ConstantUtil.HUA_GAI + "-" + dayDiZhi.getHuaGai());
        str.add(ConstantUtil.TIAN_XI + "-" + yearDiZhi.getTianXi());
        str.add(ConstantUtil.TIAN_YI + "-" + monthDiZhi.getTianYi());


        str.add(ConstantUtil.LU_SHE + "-" + dayTianGan.getLuShen());
        str.add(ConstantUtil.WEN_CHANG + "-" + dayTianGan.getWenChang());
        str.add(ConstantUtil.YANG_REN + "-" + dayTianGan.getYangRen());
        str.add(ConstantUtil.GUI_REN + "-" + dayTianGan.getGuiRen());

        return str;
    }


    /**
     * 根据日干 构造六神 放入originalBaGua中的yao中
     * @return
     */
    private static void compositionLiuShen(BaGua originalBaGua, TianGan tianGan) {
        Yao[] yaos = originalBaGua.getYaos();

        LiuShen liuShen = TIAN_GAN_LIU_SHEN_MAP.get(tianGan);
        Integer num = 0;
        for (int i = 0; i < LIU_SHENS.length; i++) {
            if(LIU_SHENS[i] == liuShen){
                num = i;
            }
        }
        for (int i = 0; i <LIU_SHENS.length; i++) {
            Yao yao = yaos[i];
            if( !yao.isNull() ){
                yao.setLiuShen(LIU_SHENS[(num + i) % LIU_SHENS.length]);
            }
        }
    }

    public static BaGuaVo createBaGuaVoByTimestamp(BaGuaDto baGuaDto) {
        Long timestamp = baGuaDto.getTimestamp();
        if (timestamp == null) {
            timestamp = System.currentTimeMillis();
            log.warn("获取前端返回时间戳失败！BaGuaDto：" + baGuaDto.toString());
        }

        // 与4095取余
        int remainder = (int)(timestamp % 4095);

        // 转换为二进制字符串，确保至少有12位
        String binary = String.format("%12s", Integer.toBinaryString(remainder)).replace(' ', '0');


        StringBuilder stringBuilder = new StringBuilder();
        // 时间戳对应的高位为上爻，低位为初爻
        for (int i = 5; i >= 0; i--) {
            int startIndex = i * 2;
            String twoBits = binary.substring(startIndex, startIndex + 2);
            int value = Integer.parseInt(twoBits, 2);
            stringBuilder.append(value);
        }
        baGuaDto.setNumber(stringBuilder.toString());
        return createBaGuaVoByNumber(baGuaDto);
    }


    static {
        TIAN_GAN_LIU_SHEN_MAP.put(TianGan.JIA,LiuShen.QINGLONG);
        TIAN_GAN_LIU_SHEN_MAP.put(TianGan.YI,LiuShen.QINGLONG);
        TIAN_GAN_LIU_SHEN_MAP.put(TianGan.BING,LiuShen.ZHUQUE);
        TIAN_GAN_LIU_SHEN_MAP.put(TianGan.DIN,LiuShen.ZHUQUE);
        TIAN_GAN_LIU_SHEN_MAP.put(TianGan.WU,LiuShen.GOUCHEN);
        TIAN_GAN_LIU_SHEN_MAP.put(TianGan.JI,LiuShen.TENGSHE);
        TIAN_GAN_LIU_SHEN_MAP.put(TianGan.GENG,LiuShen.BAIHU);
        TIAN_GAN_LIU_SHEN_MAP.put(TianGan.XIN,LiuShen.BAIHU);
        TIAN_GAN_LIU_SHEN_MAP.put(TianGan.REN,LiuShen.XUANWU);
        TIAN_GAN_LIU_SHEN_MAP.put(TianGan.GUI,LiuShen.XUANWU);
    }
} 