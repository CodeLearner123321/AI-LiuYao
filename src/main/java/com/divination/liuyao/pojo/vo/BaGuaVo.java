package com.divination.liuyao.pojo.vo;

import com.divination.liuyao.pojo.dto.BaZi;
import com.divination.liuyao.pojo.model.BaGua;
import com.divination.liuyao.pojo.model.Yao;
import com.divination.liuyao.util.LiuYaoUtil;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BaGuaVo {

    /**
     * 本卦卦象
     */
    private BaGua originalBaGua;

    /**
     * 变卦卦象
     */
    private BaGua changedBaGua;

    /**
     * 是否存在变卦
     * true 存在
     */
    private Boolean existChanged;

    /**
     * 神煞
     */
    private List<String> shenSha;

    /**
     * 起卦时间
     */
    private LocalDateTime localDateTime;

    /**
     * 当前六爻卦八字信息
     */
    private BaZi baZi;

    /**
     * 根据爻位返回对应爻位的信息
     * 初爻：0 二爻：1.。。。。。上爻：5
     */
    public String getYaoStringByPosition(Integer number, Boolean isChanged){
        Yao yao = originalBaGua.getYaos()[number];
        String str = LiuYaoUtil.positionMap.get(number) + ":(临" + yao.getLiuShen().getName() + ")"
            + yao.getLiuQin().getName() + yao.getDiZhi().getNameAndShuXin()
            + (yao.getShiOrYing() != null ? "(" + yao.getShiOrYing().getName() + "爻) " : "       ")
            + (yao.getFuCang() != null && !yao.getFuCang().isEmpty() ? "(" + yao.getFuCang() + ") " : "         ")
            + (isChanged ? (yao.getIsChange() ? "动变:  " : "同位爻: ") : "       ")
            + (isChanged ? changedBaGua.getYaos()[number].getLiuQin().getName() + changedBaGua.getYaos()[number].getDiZhi().getNameAndShuXin() : "");
        return str;
    }

    /**
     * 根据爻位返回对应爻位的信息
     * 初爻：0 二爻：1.。。。。。上爻：5  14
     */
    public String getGuaStringByPosition(Boolean isChanged){
        return "主卦: " + originalBaGua.getName() + originalBaGua.getString()
            + (isChanged ? ("       变->    " + changedBaGua.getName() + changedBaGua.getString()) : "") ;
    }

    public String getShenShaString(){
        return "神煞：" + shenSha.stream().filter(Objects::nonNull)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.joining(", "));
    }

    /**
     * 是否存在变卦
     */
    public Boolean isExistChanged(){
        return existChanged != null && existChanged;
    }


    /**
     * 格式化
     */
    public void formatting() {
        if (this.originalBaGua != null){
            originalBaGua.formatting();
        }
    }
}
