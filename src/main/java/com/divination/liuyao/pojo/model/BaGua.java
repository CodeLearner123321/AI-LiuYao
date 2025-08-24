package com.divination.liuyao.pojo.model;

import com.divination.liuyao.assemblies.enums.DiZhi;
import com.divination.liuyao.assemblies.enums.LiuQin;
import com.divination.liuyao.assemblies.enums.ShiOrYing;
import com.divination.liuyao.assemblies.enums.TianGan;
import com.divination.liuyao.assemblies.enums.YouHunGuiHun;
import com.divination.liuyao.util.ConstantUtil;
import com.divination.liuyao.util.WuXinUtil;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BaGua {

    private static final Map<String, BaGua> BAGUA_MAP = new HashMap<>();
    //宫位对应五行的Map
    public static final Map<String, String> GONG_WEI_TO_SHU_XIN = new HashMap<>();

    /**
     * 该卦对应的数字
     */
    private String id;

    /**
     * 本卦的卦名
     */
    private String name;

    /**
     * 卦的宫位
     */
    private String gongWei;

    /**
     * 是何爻包卦
     */
    private String baoGua;

    /**
     * 是否是游魂归魂卦
     */
    private YouHunGuiHun youHunGuiOrGuiHunGua;

    /**
     * 是否是六合六冲卦
     */
    private String liuHeLiuChong;

    private Yao[] yaos;


    /**
     * 返回世爻地支
     * @return
     */
    public DiZhi getShiYaoDiZhi(){
        for (int i = 0; i < yaos.length; i++) {
            if(yaos[i].isShiYao()){
                return yaos[i].getDiZhi();
            }
        }
        return null;
    }


    /**
     * 根据数字创建六爻卦
     */
    public static BaGua createBaGua(byte[] isChangeMap, String number){
        BaGua originalBaGua  = BAGUA_MAP.get(number);
        BaGua baGua = deepCopy(originalBaGua);

        for (int i = 0; i < 6; i++) {
            Yao yao = baGua.getYaos()[i];
            if(yao.isNull()){
                continue;
            }
            yao.setIsChange(isChangeMap[i] == 1);
        }
        return baGua;
    }

    /**
     * 根据卦名创建六爻卦
     */
    public static BaGua createBaGuaName(String guaName){
        return BAGUA_MAP.values().stream()
                .filter(baGua -> Objects.equals(baGua.getName(), guaName))
                .findFirst().orElse(null);
    }

    /**
     * 根据卦名创建六爻卦
     */
    public static String findNumberByGuaName(String guaName){
        return BAGUA_MAP.entrySet().stream()
                .filter(baGua -> Objects.equals(baGua.getValue().getName(), guaName))
                .map(Map.Entry::getKey)
                .findFirst().orElse("");
    }


    /**
     * 返回本卦的更多信息
     * @return
     */
    public String getString() {
        return Stream.of(
                (gongWei != null && !gongWei.isEmpty()) ? gongWei : null,
                (youHunGuiOrGuiHunGua != null) ? youHunGuiOrGuiHunGua.getName() : null,
                (liuHeLiuChong != null && !liuHeLiuChong.isEmpty()) ? liuHeLiuChong : null
            )
            .filter(Objects::nonNull)
            .collect(Collectors.collectingAndThen(
                Collectors.joining(","),
                s -> s.isEmpty() ? "" : "(" + s + ")"
            ));
    }


    /**
     * 创建BaGua对象的深拷贝
     * @param original 要拷贝的原始BaGua对象
     * @return 拷贝后的新BaGua对象
     */
    public static BaGua deepCopy(BaGua original) {
        if (original == null) {
            return null;
        }
        
        BaGua copy = new BaGua();
        copy.setId(original.getId());
        copy.setName(original.getName());
        copy.setGongWei(original.getGongWei());
        copy.setBaoGua(original.getBaoGua());
        copy.setYouHunGuiOrGuiHunGua(original.getYouHunGuiOrGuiHunGua());
        copy.setLiuHeLiuChong(original.getLiuHeLiuChong());
        
        // 深拷贝爻数组
        if (original.getYaos() != null) {
            Yao[] originalYaos = original.getYaos();
            Yao[] copyYaos = new Yao[originalYaos.length];
            
            for (int i = 0; i < originalYaos.length; i++) {
                if (originalYaos[i] != null) {
                    Yao originalYao = originalYaos[i];
                    Yao copyYao = new Yao(
                        originalYao.getValue(),
                        originalYao.getPosition(),
                        originalYao.getTianGan(),
                        originalYao.getDiZhi(),
                        originalYao.getLiuQin(),
                        originalYao.getShiOrYing(),
                        originalYao.getFuCang(),
                        originalYao.getLiuShen()
                    );
                    copyYao.setIsChange(originalYao.getIsChange());
                    copyYaos[i] = copyYao;
                }
            }
            
            copy.setYaos(copyYaos);
        }
        
        return copy;
    }

    /**
     * 根据数字创建变卦的六爻卦（因为变卦的六亲由主卦的宫位属性决定）
     * @param originalShuXin 本卦的属性，用来确定变卦的六亲
     * @param number    变卦的数字
     * @return
     */
    public static BaGua createChangeBaGua(String originalShuXin, String number){
        // 获取原始BaGua对象
        BaGua originalBaGua = BAGUA_MAP.get(number);
        // 创建深拷贝
        BaGua baGua = deepCopy(originalBaGua);
        
        for (int i = 0; i < 6; i++) {
            Yao yao = baGua.getYaos()[i];
            if(yao.isNull()){
                continue;
            }
            String s = WuXinUtil.relationLiuYao(originalShuXin, yao.getDiZhi().getShuXin());
            yao.setLiuQin(LiuQin.getLiuQinByName(s));
        }
        return baGua;
    }

    public void formatting() {
        this.id = null;
    }


    static {
        GONG_WEI_TO_SHU_XIN.put("乾宫", "金");
        GONG_WEI_TO_SHU_XIN.put("坎宫", "水");
        GONG_WEI_TO_SHU_XIN.put("震宫", "木");
        GONG_WEI_TO_SHU_XIN.put("离宫", "火");
        GONG_WEI_TO_SHU_XIN.put("艮宫", "土");
        GONG_WEI_TO_SHU_XIN.put("巽宫", "木");
        GONG_WEI_TO_SHU_XIN.put("坤宫", "土");
        GONG_WEI_TO_SHU_XIN.put("兑宫", "金");

        //乾宫
       BAGUA_MAP.put("111111", new BaGua("111111","乾为天","乾宫",null,null, ConstantUtil.LIU_CHONG,new Yao[]{
           new Yao(1,1, TianGan.JIA, DiZhi.ZI,  LiuQin.ZISUN,  null,null,null),
           new Yao(1,2, TianGan.JIA, DiZhi.YIN, LiuQin.QICAI,  null,null,null),
           new Yao(1,3, TianGan.JIA, DiZhi.CHEN,LiuQin.FUMU,   ShiOrYing.YING,null,null),
           new Yao(1,4, TianGan.REN, DiZhi.WU,  LiuQin.GUANGUI,null,null,null),
           new Yao(1,5, TianGan.REN, DiZhi.SHEN,LiuQin.XIONGDI,null,null,null),
           new Yao(1,6, TianGan.REN, DiZhi.XU,  LiuQin.FUMU,   ShiOrYing.SHI,null,null)}));
       BAGUA_MAP.put("011111", new BaGua("011111","天风姤","乾宫",null,null,null,new Yao[]{
           new Yao(0,1, TianGan.XIN, DiZhi.CHOU,  LiuQin.FUMU,  ShiOrYing.SHI,null,null),
           new Yao(1,2, TianGan.XIN, DiZhi.HAI, LiuQin.ZISUN,  null,"伏寅木妻财",null),
           new Yao(1,3, TianGan.XIN, DiZhi.YOU,LiuQin.XIONGDI,   null,null,null),
           new Yao(1,4, TianGan.REN, DiZhi.WU,  LiuQin.GUANGUI,ShiOrYing.YING,null,null),
           new Yao(1,5, TianGan.REN, DiZhi.SHEN,LiuQin.XIONGDI,null,null,null),
           new Yao(1,6, TianGan.REN, DiZhi.XU,  LiuQin.FUMU,   null,null,null)}));
       BAGUA_MAP.put("001111", new BaGua("001111","天山遁","乾宫",null,null,null,new Yao[]{
           
           new Yao(0,1, TianGan.BING, DiZhi.CHEN,  LiuQin.FUMU,  null,"伏子水子孙",null),
           new Yao(0,2, TianGan.BING, DiZhi.WU, LiuQin.GUANGUI,  ShiOrYing.SHI,"伏寅木妻财",null),
           new Yao(1,3, TianGan.BING, DiZhi.SHEN,LiuQin.XIONGDI,   null,null,null),
           new Yao(1,4, TianGan.REN, DiZhi.WU,  LiuQin.GUANGUI,null,null,null),
           new Yao(1,5, TianGan.REN, DiZhi.SHEN,LiuQin.XIONGDI,ShiOrYing.YING,null,null),
           new Yao(1,6, TianGan.REN, DiZhi.XU,  LiuQin.FUMU,   null,null,null)}));
       BAGUA_MAP.put("000111", new BaGua("000111","天地否","乾宫",null,null,ConstantUtil.LIU_HE,new Yao[]{
           
           new Yao(0,1, TianGan.YI, DiZhi.WEI,  LiuQin.FUMU,  null,"伏子水子孙",null),
           new Yao(0,2, TianGan.YI, DiZhi.SI, LiuQin.GUANGUI,  null,null,null),
           new Yao(0,3, TianGan.YI, DiZhi.MAO,LiuQin.QICAI,   ShiOrYing.SHI,null,null),
           new Yao(1,4, TianGan.REN, DiZhi.WU,  LiuQin.GUANGUI,null,null,null),
           new Yao(1,5, TianGan.REN, DiZhi.SHEN,LiuQin.XIONGDI,null,null,null),
           new Yao(1,6, TianGan.REN, DiZhi.XU,  LiuQin.FUMU,   ShiOrYing.YING,null,null)}));
       BAGUA_MAP.put("000011", new BaGua("000011","风地观","乾宫",null,null,null,new Yao[]{
           
           new Yao(0,1, TianGan.YI, DiZhi.WEI,  LiuQin.FUMU,  ShiOrYing.YING,"伏子水子孙",null),
           new Yao(0,2, TianGan.YI, DiZhi.SI, LiuQin.GUANGUI,  null,null,null),
           new Yao(0,3, TianGan.YI, DiZhi.MAO,LiuQin.QICAI,   null,null,null),
           new Yao(0,4, TianGan.XIN, DiZhi.WEI,  LiuQin.FUMU,ShiOrYing.SHI,null,null),
           new Yao(1,5, TianGan.XIN, DiZhi.SI,LiuQin.GUANGUI,null,"伏申金兄弟",null),
           new Yao(1,6, TianGan.XIN, DiZhi.MAO,  LiuQin.QICAI,   null,null,null)}));
       BAGUA_MAP.put("000001", new BaGua("000001","山地剥","乾宫",null,null,null,new Yao[]{
           
           new Yao(0,1, TianGan.YI, DiZhi.WEI,  LiuQin.FUMU,  null,null,null),
           new Yao(0,2, TianGan.YI, DiZhi.SI, LiuQin.GUANGUI,  ShiOrYing.YING,null,null),
           new Yao(0,3, TianGan.YI, DiZhi.MAO,LiuQin.QICAI,   null,null,null),
           new Yao(0,4, TianGan.BING, DiZhi.XU,  LiuQin.FUMU,null,null,null),
           new Yao(0,5, TianGan.BING, DiZhi.ZI,LiuQin.ZISUN,ShiOrYing.SHI,"伏申金兄弟",null),
           new Yao(1,6, TianGan.BING, DiZhi.YIN,  LiuQin.QICAI,   null,null,null)}));
       BAGUA_MAP.put("000101", new BaGua("000101","火地晋","乾宫",null,YouHunGuiHun.YOUHUNGUA,null,new Yao[]{
           
           new Yao(0,1, TianGan.YI, DiZhi.WEI,  LiuQin.FUMU,    ShiOrYing.YING,"伏子水子孙",null),
           new Yao(0,2, TianGan.YI, DiZhi.SI, LiuQin.GUANGUI, null,null,null),
           new Yao(0,3, TianGan.YI, DiZhi.MAO,LiuQin.QICAI,   null,null,null),
           new Yao(1,4, TianGan.JI, DiZhi.YOU,  LiuQin.XIONGDI,ShiOrYing.SHI,null,null),
           new Yao(0,5, TianGan.JI, DiZhi.WEI,LiuQin.FUMU,null,null,null),
           new Yao(1,6, TianGan.JI, DiZhi.SI,  LiuQin.GUANGUI,   null,null,null)}));
       BAGUA_MAP.put("111101", new BaGua("111101","火天大有","乾宫",null,YouHunGuiHun.GUIHUNGUA,null,new Yao[]{
           
           new Yao(1,1, TianGan.JIA, DiZhi.ZI,  LiuQin.ZISUN,  null,null,null),
           new Yao(1,2, TianGan.JIA, DiZhi.YIN, LiuQin.QICAI,  null,null,null),
           new Yao(1,3, TianGan.JIA, DiZhi.CHEN,LiuQin.FUMU,   ShiOrYing.SHI,null,null),
           new Yao(1,4, TianGan.JI, DiZhi.YOU,  LiuQin.XIONGDI,null,null,null),
           new Yao(0,5, TianGan.JI, DiZhi.WEI,LiuQin.FUMU,null,null,null),
           new Yao(1,6, TianGan.JI, DiZhi.SI,  LiuQin.GUANGUI,   ShiOrYing.YING,null,null)}));


       //兑宫
        BAGUA_MAP.put("110110", new BaGua("110110","兑为泽","兑宫",null,null,ConstantUtil.LIU_CHONG,new Yao[]{
            
            new Yao(1,1, TianGan.DIN, DiZhi.SI,  LiuQin.GUANGUI,  null,null,null),
            new Yao(1,2, TianGan.DIN, DiZhi.MAO, LiuQin.QICAI,  null,null,null),
            new Yao(0,3, TianGan.DIN, DiZhi.CHOU,LiuQin.FUMU,   ShiOrYing.YING,null,null),
            new Yao(1,4, TianGan.DIN, DiZhi.HAI,  LiuQin.ZISUN,null,null,null),
            new Yao(1,5, TianGan.DIN, DiZhi.YOU,LiuQin.XIONGDI,null,null,null),
            new Yao(0,6, TianGan.DIN, DiZhi.WEI,  LiuQin.FUMU,   ShiOrYing.SHI,null,null)}));
        BAGUA_MAP.put("010110", new BaGua("010110","泽水困","兑宫",null,null,ConstantUtil.LIU_HE,new Yao[]{
            
            new Yao(0,1, TianGan.WU, DiZhi.YIN,  LiuQin.QICAI,  ShiOrYing.SHI,null,null),
            new Yao(1,2, TianGan.WU, DiZhi.CHEN, LiuQin.FUMU,  null,null,null),
            new Yao(0,3, TianGan.WU, DiZhi.WU,LiuQin.GUANGUI,   null,null,null),
            new Yao(1,4, TianGan.DIN, DiZhi.HAI,  LiuQin.ZISUN,ShiOrYing.YING,null,null),
            new Yao(1,5, TianGan.DIN, DiZhi.YOU,LiuQin.XIONGDI,null,null,null),
            new Yao(0,6, TianGan.DIN, DiZhi.WEI,  LiuQin.FUMU,   null,null,null)}));
        BAGUA_MAP.put("000110", new BaGua("000110","泽地萃","兑宫",null,null,null,new Yao[]{
            
            new Yao(0,1, TianGan.YI, DiZhi.WEI,  LiuQin.FUMU,  null,null,null),
            new Yao(0,2, TianGan.YI, DiZhi.SI, LiuQin.GUANGUI,  ShiOrYing.SHI,null,null),
            new Yao(0,3, TianGan.YI, DiZhi.MAO,LiuQin.QICAI,   null,null,null),
            new Yao(1,4, TianGan.DIN, DiZhi.HAI,  LiuQin.ZISUN,null,null,null),
            new Yao(1,5, TianGan.DIN, DiZhi.YOU,LiuQin.XIONGDI,ShiOrYing.YING,null,null),
            new Yao(0,6, TianGan.DIN, DiZhi.WEI,  LiuQin.FUMU,   null,null,null)}));
        BAGUA_MAP.put("001110", new BaGua("001110","泽山咸","兑宫",null,null,null,new Yao[]{
            
            new Yao(0,1, TianGan.BING, DiZhi.CHEN,  LiuQin.FUMU,  null,null,null),
            new Yao(0,2, TianGan.BING, DiZhi.WU, LiuQin.GUANGUI,  null,"伏卯木妻财",null),
            new Yao(1,3, TianGan.BING, DiZhi.SHEN,LiuQin.XIONGDI,   ShiOrYing.SHI,null,null),
            new Yao(1,4, TianGan.DIN, DiZhi.HAI,  LiuQin.ZISUN,null,null,null),
            new Yao(1,5, TianGan.DIN, DiZhi.YOU,LiuQin.XIONGDI,null,null,null),
            new Yao(0,6, TianGan.DIN, DiZhi.WEI,  LiuQin.FUMU,   ShiOrYing.YING,null,null)}));
        BAGUA_MAP.put("001010", new BaGua("001010","水山蹇","兑宫",null,null,null,new Yao[]{
            
            new Yao(0,1, TianGan.BING, DiZhi.CHEN,  LiuQin.FUMU,  ShiOrYing.YING,null,null),
            new Yao(0,2, TianGan.BING, DiZhi.WU, LiuQin.GUANGUI,  null,"伏卯木妻财",null),
            new Yao(1,3, TianGan.BING, DiZhi.SHEN,LiuQin.XIONGDI,   null,null,null),
            new Yao(0,4, TianGan.WU, DiZhi.SHEN,  LiuQin.XIONGDI,ShiOrYing.SHI,null,null),
            new Yao(1,5, TianGan.WU, DiZhi.XU,LiuQin.FUMU,null,"伏酉金兄弟",null),
            new Yao(0,6, TianGan.WU, DiZhi.ZI,  LiuQin.ZISUN,   null,null,null)}));
        BAGUA_MAP.put("001000", new BaGua("001000","地山谦","兑宫",null,null,null,new Yao[]{
            
            new Yao(0,1, TianGan.BING, DiZhi.CHEN,  LiuQin.FUMU,  null,null,null),
            new Yao(0,2, TianGan.BING, DiZhi.WU, LiuQin.GUANGUI,  ShiOrYing.YING,"伏卯木妻财",null),
            new Yao(1,3, TianGan.BING, DiZhi.SHEN,LiuQin.XIONGDI,   null,null,null),
            new Yao(0,4, TianGan.GUI, DiZhi.CHOU,  LiuQin.FUMU,null,null,null),
            new Yao(0,5, TianGan.GUI, DiZhi.HAI,LiuQin.ZISUN,ShiOrYing.SHI,null,null),
            new Yao(0,6, TianGan.GUI, DiZhi.YOU,  LiuQin.XIONGDI,   null,null,null)}));
        BAGUA_MAP.put("001100", new BaGua("001100","雷山小过","兑宫",null,YouHunGuiHun.YOUHUNGUA,null,new Yao[]{
            
            new Yao(0,1, TianGan.BING, DiZhi.CHEN,  LiuQin.FUMU,  ShiOrYing.YING,null,null),
            new Yao(0,2, TianGan.BING, DiZhi.WU, LiuQin.GUANGUI,  null,"伏卯木妻财",null),
            new Yao(1,3, TianGan.BING, DiZhi.SHEN,LiuQin.XIONGDI,  null,null,null),
            new Yao(1,4, TianGan.GENG, DiZhi.WU,  LiuQin.GUANGUI,ShiOrYing.SHI,"伏亥水子孙",null),
            new Yao(0,5, TianGan.GENG, DiZhi.SHEN,LiuQin.XIONGDI,null,null,null),
            new Yao(0,6, TianGan.GENG, DiZhi.XU,  LiuQin.FUMU,   null,null,null)}));
        BAGUA_MAP.put("110100", new BaGua("110100","雷泽归妹","兑宫",null,YouHunGuiHun.GUIHUNGUA,null,new Yao[]{
            
            new Yao(1,1, TianGan.DIN, DiZhi.SI,  LiuQin.GUANGUI,  null,null,null),
            new Yao(1,2, TianGan.DIN, DiZhi.MAO, LiuQin.QICAI,  null,null,null),
            new Yao(0,3, TianGan.DIN, DiZhi.CHOU,LiuQin.FUMU,   ShiOrYing.SHI,null,null),
            new Yao(1,4, TianGan.GENG, DiZhi.WU,  LiuQin.GUANGUI,null,"伏亥水子孙",null),
            new Yao(0,5, TianGan.GENG, DiZhi.SHEN,LiuQin.XIONGDI,null,null,null),
            new Yao(0,6, TianGan.GENG, DiZhi.XU,  LiuQin.FUMU,   ShiOrYing.YING,null,null)}));



        //离宫
        BAGUA_MAP.put("101101", new BaGua("101101","离为火","离宫",null,null,ConstantUtil.LIU_CHONG,new Yao[]{
            new Yao(1,1, TianGan.JI, DiZhi.MAO,  LiuQin.FUMU,  null,null,null),
            new Yao(0,2, TianGan.JI, DiZhi.CHOU, LiuQin.ZISUN,  null,null,null),
            new Yao(1,3, TianGan.JI, DiZhi.HAI,LiuQin.GUANGUI,   ShiOrYing.YING,null,null),
            new Yao(1,4, TianGan.JI, DiZhi.YOU,  LiuQin.QICAI,null,null,null),
            new Yao(0,5, TianGan.JI, DiZhi.WEI,LiuQin.ZISUN,null,null,null),
            new Yao(1,6, TianGan.JI, DiZhi.SI,  LiuQin.XIONGDI,   ShiOrYing.SHI,null,null)}));
        BAGUA_MAP.put("001101", new BaGua("001101","火山旅","离宫",null,null,ConstantUtil.LIU_HE,new Yao[]{
            new Yao(0,1, TianGan.BING, DiZhi.CHEN,  LiuQin.ZISUN,  ShiOrYing.SHI,"伏卯木父母",null),
            new Yao(0,2, TianGan.BING, DiZhi.WU, LiuQin.XIONGDI,  null,null,null),
            new Yao(1,3, TianGan.BING, DiZhi.SHEN,LiuQin.QICAI,   null,"伏亥水官鬼",null),
            new Yao(1,4, TianGan.JI, DiZhi.YOU,  LiuQin.QICAI,ShiOrYing.YING,null,null),
            new Yao(0,5, TianGan.JI, DiZhi.WEI,LiuQin.ZISUN,null,null,null),
            new Yao(1,6, TianGan.JI, DiZhi.SI,  LiuQin.XIONGDI,   null,null,null)}));
        BAGUA_MAP.put("011101", new BaGua("011101","火风鼎","离宫",null,null,null,new Yao[]{
            new Yao(0,1, TianGan.XIN, DiZhi.CHOU,  LiuQin.ZISUN,  null,"伏卯木兄弟",null),
            new Yao(1,2, TianGan.XIN, DiZhi.HAI, LiuQin.GUANGUI,  ShiOrYing.SHI,null,null),
            new Yao(1,3, TianGan.XIN, DiZhi.YOU,LiuQin.QICAI,   null,null,null),
            new Yao(1,4, TianGan.JI, DiZhi.YOU,  LiuQin.QICAI,null,null,null),
            new Yao(0,5, TianGan.JI, DiZhi.WEI,LiuQin.ZISUN,ShiOrYing.YING,null,null),
            new Yao(1,6, TianGan.JI, DiZhi.SI,  LiuQin.XIONGDI,   null,null,null)}));
        BAGUA_MAP.put("010101", new BaGua("010101","火水未济","离宫",null,null,null,new Yao[]{
            new Yao(0,1, TianGan.WU, DiZhi.YIN,  LiuQin.FUMU,  null,null,null),
            new Yao(1,2, TianGan.WU, DiZhi.CHEN, LiuQin.ZISUN,  null,null,null),
            new Yao(0,3, TianGan.WU, DiZhi.WU,LiuQin.XIONGDI,   ShiOrYing.SHI,"伏亥水官鬼",null),
            new Yao(1,4, TianGan.JI, DiZhi.YOU,  LiuQin.QICAI,null,null,null),
            new Yao(0,5, TianGan.JI, DiZhi.WEI,LiuQin.ZISUN,null,null,null),
            new Yao(1,6, TianGan.JI, DiZhi.SI,  LiuQin.XIONGDI,   ShiOrYing.YING,null,null)}));
        BAGUA_MAP.put("010001", new BaGua("010001","山水蒙","离宫",null,null,null,new Yao[]{
            new Yao(0,1, TianGan.WU, DiZhi.YIN,  LiuQin.FUMU,  ShiOrYing.YING,null,null),
            new Yao(1,2, TianGan.WU, DiZhi.CHEN, LiuQin.ZISUN,  null,null,null),
            new Yao(0,3, TianGan.WU, DiZhi.WU,LiuQin.XIONGDI,   null,null,null),
            new Yao(0,4, TianGan.BING, DiZhi.XU,  LiuQin.ZISUN,ShiOrYing.SHI,"伏酉金兄弟",null),
            new Yao(0,5, TianGan.BING, DiZhi.ZI,LiuQin.GUANGUI,null,null,null),
            new Yao(1,6, TianGan.BING, DiZhi.YIN,  LiuQin.FUMU,   null,null,null)}));
        BAGUA_MAP.put("010011", new BaGua("010011","风水涣","离宫",null,null,null,new Yao[]{
            new Yao(0,1, TianGan.WU, DiZhi.YIN,  LiuQin.FUMU,  null,null,null),
            new Yao(1,2, TianGan.WU, DiZhi.CHEN, LiuQin.ZISUN,  ShiOrYing.YING,null,null),
            new Yao(0,3, TianGan.WU, DiZhi.WU,LiuQin.XIONGDI,   null,"伏亥水官鬼",null),
            new Yao(0,4, TianGan.XIN, DiZhi.WEI,  LiuQin.ZISUN,null,"伏酉金妻财",null),
            new Yao(1,5, TianGan.XIN, DiZhi.SI,LiuQin.XIONGDI,ShiOrYing.SHI,null,null),
            new Yao(1,6, TianGan.XIN, DiZhi.MAO,  LiuQin.FUMU,   null,null,null)}));
        BAGUA_MAP.put("010111", new BaGua("010111","天水讼","离宫",null,YouHunGuiHun.YOUHUNGUA,null,new Yao[]{
            new Yao(0,1, TianGan.WU, DiZhi.YIN,  LiuQin.FUMU,  ShiOrYing.YING,"伏卯木父母",null),
            new Yao(1,2, TianGan.WU, DiZhi.CHEN, LiuQin.ZISUN,  null,null,null),
            new Yao(0,3, TianGan.WU, DiZhi.WU,LiuQin.XIONGDI,   null,"伏亥水官鬼",null),
            new Yao(1,4, TianGan.REN, DiZhi.WU,  LiuQin.XIONGDI,ShiOrYing.SHI,null,null),
            new Yao(1,5, TianGan.REN, DiZhi.SHEN,LiuQin.QICAI,null,null,null),
            new Yao(1,6, TianGan.REN, DiZhi.XU,  LiuQin.ZISUN,   null,null,null)}));
        BAGUA_MAP.put("101111", new BaGua("101111","天火同人","离宫",null,YouHunGuiHun.GUIHUNGUA,null,new Yao[]{
            new Yao(1,1, TianGan.JI, DiZhi.MAO,  LiuQin.FUMU,  null,null,null),
            new Yao(0,2, TianGan.JI, DiZhi.CHOU, LiuQin.ZISUN,  null,null,null),
            new Yao(1,3, TianGan.JI, DiZhi.HAI,LiuQin.GUANGUI,   ShiOrYing.SHI,null,null),
            new Yao(1,4, TianGan.REN, DiZhi.WU,  LiuQin.XIONGDI,null,null,null),
            new Yao(1,5, TianGan.REN, DiZhi.SHEN,LiuQin.QICAI,null,null,null),
            new Yao(1,6, TianGan.REN, DiZhi.XU,  LiuQin.ZISUN,   ShiOrYing.YING,null,null)}));


        //震宫
        BAGUA_MAP.put("100100", new BaGua("100100","震为雷","震宫",null,null,ConstantUtil.LIU_CHONG,new Yao[]{
            
            new Yao(1,1, TianGan.GENG, DiZhi.ZI,  LiuQin.FUMU,  null,null,null),
            new Yao(0,2, TianGan.GENG, DiZhi.YIN, LiuQin.XIONGDI,  null,null,null),
            new Yao(0,3, TianGan.GENG, DiZhi.CHEN,LiuQin.QICAI,   ShiOrYing.YING,null,null),
            new Yao(1,4, TianGan.GENG, DiZhi.WU,  LiuQin.ZISUN,null,null,null),
            new Yao(0,5, TianGan.GENG, DiZhi.SHEN,LiuQin.GUANGUI,null,null,null),
            new Yao(0,6, TianGan.GENG, DiZhi.XU,  LiuQin.QICAI,   ShiOrYing.SHI,null,null)}));
        BAGUA_MAP.put("000100", new BaGua("000100","雷地豫","震宫",null,null,ConstantUtil.LIU_HE,new Yao[]{
            
            new Yao(0,1, TianGan.YI, DiZhi.WEI,  LiuQin.QICAI,  ShiOrYing.SHI,"伏子水父母",null),
            new Yao(0,2, TianGan.YI, DiZhi.SI, LiuQin.ZISUN,  null,null,null),
            new Yao(0,3, TianGan.YI, DiZhi.MAO,LiuQin.XIONGDI,   null,null,null),
            new Yao(1,4, TianGan.GENG, DiZhi.WU,  LiuQin.ZISUN,ShiOrYing.YING,null,null),
            new Yao(0,5, TianGan.GENG, DiZhi.SHEN,LiuQin.GUANGUI,null,null,null),
            new Yao(0,6, TianGan.GENG, DiZhi.XU,  LiuQin.QICAI,   null,null,null)}));
        BAGUA_MAP.put("010100", new BaGua("010100","雷水解","震宫",null,null,null,new Yao[]{
            
            new Yao(0,1, TianGan.WU, DiZhi.YIN,  LiuQin.XIONGDI,  null,"伏子水父母",null),
            new Yao(1,2, TianGan.WU, DiZhi.CHEN, LiuQin.QICAI,  ShiOrYing.SHI,null,null),
            new Yao(0,3, TianGan.WU, DiZhi.WU,LiuQin.ZISUN,   null,null,null),
            new Yao(1,4, TianGan.GENG, DiZhi.WU,  LiuQin.ZISUN,null,null,null),
            new Yao(0,5, TianGan.GENG, DiZhi.SHEN,LiuQin.GUANGUI,ShiOrYing.YING,null,null),
            new Yao(0,6, TianGan.GENG, DiZhi.XU,  LiuQin.QICAI,   null,null,null)}));
        BAGUA_MAP.put("011100", new BaGua("011100","雷风恒","震宫",null,null,null,new Yao[]{
            
            new Yao(0,1, TianGan.XIN, DiZhi.CHOU,  LiuQin.QICAI,  null,null,null),
            new Yao(1,2, TianGan.XIN, DiZhi.HAI, LiuQin.FUMU,  null,"伏寅木兄弟",null),
            new Yao(1,3, TianGan.XIN, DiZhi.YOU,LiuQin.GUANGUI,   ShiOrYing.SHI,null,null),
            new Yao(1,4, TianGan.GENG, DiZhi.WU,  LiuQin.ZISUN,null,null,null),
            new Yao(0,5, TianGan.GENG, DiZhi.SHEN,LiuQin.GUANGUI,null,null,null),
            new Yao(0,6, TianGan.GENG, DiZhi.XU,  LiuQin.QICAI,   ShiOrYing.YING,null,null)}));
        BAGUA_MAP.put("011000", new BaGua("011000","地风升","震宫",null,null,null,new Yao[]{
            
            new Yao(0,1, TianGan.XIN, DiZhi.CHOU,  LiuQin.QICAI,  ShiOrYing.YING,null,null),
            new Yao(1,2, TianGan.XIN, DiZhi.HAI, LiuQin.FUMU,  null,"伏寅木兄弟",null),
            new Yao(1,3, TianGan.XIN, DiZhi.YOU,LiuQin.GUANGUI,   null,null,null),
            new Yao(0,4, TianGan.GUI, DiZhi.CHOU,  LiuQin.QICAI,ShiOrYing.SHI,"伏午火子孙",null),
            new Yao(0,5, TianGan.GUI, DiZhi.HAI,LiuQin.FUMU,null,null,null),
            new Yao(0,6, TianGan.GUI, DiZhi.YOU,  LiuQin.GUANGUI,   null,null,null)}));
        BAGUA_MAP.put("011010", new BaGua("011010","水风井","震宫",null,null,null,new Yao[]{
            
            new Yao(0,1, TianGan.XIN, DiZhi.CHOU,  LiuQin.QICAI,  null,null,null),
            new Yao(1,2, TianGan.XIN, DiZhi.HAI, LiuQin.FUMU,  ShiOrYing.YING,"伏寅木兄弟",null),
            new Yao(1,3, TianGan.XIN, DiZhi.YOU,LiuQin.GUANGUI,   null,"伏辰土妻财",null),
            new Yao(0,4, TianGan.WU, DiZhi.SHEN,  LiuQin.GUANGUI,null,"伏午火子孙",null),
            new Yao(1,5, TianGan.WU, DiZhi.XU,LiuQin.QICAI, ShiOrYing.SHI,null,null),
            new Yao(0,6, TianGan.WU, DiZhi.ZI,  LiuQin.FUMU,   null,null,null)}));
        BAGUA_MAP.put("011110", new BaGua("011110","泽风大过","震宫",null,YouHunGuiHun.YOUHUNGUA,null,new Yao[]{
            
            new Yao(0,1, TianGan.XIN, DiZhi.CHOU,  LiuQin.QICAI,  ShiOrYing.YING,null,null),
            new Yao(1,2, TianGan.XIN, DiZhi.HAI, LiuQin.FUMU,  null,"伏寅木兄弟",null),
            new Yao(1,3, TianGan.XIN, DiZhi.YOU,LiuQin.GUANGUI,   null,null,null),
            new Yao(1,4, TianGan.DIN, DiZhi.HAI,  LiuQin.FUMU,ShiOrYing.SHI,"伏午火子孙",null),
            new Yao(1,5, TianGan.DIN, DiZhi.YOU,LiuQin.GUANGUI,null,null,null),
            new Yao(0,6, TianGan.DIN, DiZhi.WEI,  LiuQin.QICAI,   null,null,null)}));
        BAGUA_MAP.put("100110", new BaGua("100110","泽雷随","震宫",null,YouHunGuiHun.GUIHUNGUA,null,new Yao[]{
            
            new Yao(1,1, TianGan.GENG, DiZhi.ZI,  LiuQin.FUMU,  null,null,null),
            new Yao(0,2, TianGan.GENG, DiZhi.YIN, LiuQin.XIONGDI,  null,null,null),
            new Yao(0,3, TianGan.GENG, DiZhi.CHEN,LiuQin.QICAI,   ShiOrYing.SHI,null,null),
            new Yao(1,4, TianGan.DIN, DiZhi.HAI,  LiuQin.FUMU,null,"伏午火子孙",null),
            new Yao(1,5, TianGan.DIN, DiZhi.YOU,LiuQin.GUANGUI,null,"伏申金官鬼",null),
            new Yao(0,6, TianGan.DIN, DiZhi.WEI,  LiuQin.QICAI,   ShiOrYing.YING,null,null)}));


        //巽宫
        BAGUA_MAP.put("011011", new BaGua("011011","巽为风","巽宫",null,null,ConstantUtil.LIU_CHONG,new Yao[]{
            
            new Yao(0,1, TianGan.XIN, DiZhi.CHOU,  LiuQin.QICAI,  null,null,null),
            new Yao(1,2, TianGan.XIN, DiZhi.HAI, LiuQin.FUMU,  null,null,null),
            new Yao(1,3, TianGan.XIN, DiZhi.YOU,LiuQin.GUANGUI,   ShiOrYing.YING,null,null),
            new Yao(0,4, TianGan.XIN, DiZhi.WEI,  LiuQin.QICAI,null,null,null),
            new Yao(1,5, TianGan.XIN, DiZhi.SI,LiuQin.ZISUN,null,null,null),
            new Yao(1,6, TianGan.XIN, DiZhi.MAO,  LiuQin.XIONGDI,  ShiOrYing.SHI,null,null)}));
        BAGUA_MAP.put("111011", new BaGua("111011","风天小蓄","巽宫",null,null,null,new Yao[]{
            
            new Yao(1,1, TianGan.JIA, DiZhi.ZI,  LiuQin.FUMU,  ShiOrYing.SHI,null,null),
            new Yao(1,2, TianGan.JIA, DiZhi.YIN, LiuQin.XIONGDI,  null,null,null),
            new Yao(1,3, TianGan.JIA, DiZhi.CHEN,LiuQin.QICAI,   null,"伏酉金官鬼",null),
            new Yao(0,4, TianGan.XIN, DiZhi.WEI,  LiuQin.QICAI,ShiOrYing.YING,null,null),
            new Yao(1,5, TianGan.XIN, DiZhi.SI,LiuQin.ZISUN,null,null,null),
            new Yao(1,6, TianGan.XIN, DiZhi.MAO,  LiuQin.XIONGDI,  null,null,null)}));
        BAGUA_MAP.put("101011", new BaGua("101011","风火家人","巽宫",null,null,null,new Yao[]{
            
            new Yao(1,1, TianGan.JI, DiZhi.MAO,  LiuQin.XIONGDI,  null,null,null),
            new Yao(0,2, TianGan.JI, DiZhi.CHOU, LiuQin.QICAI,  ShiOrYing.SHI,null,null),
            new Yao(1,3, TianGan.JI, DiZhi.HAI,LiuQin.FUMU,   null,"伏酉金官鬼",null),
            new Yao(0,4, TianGan.XIN, DiZhi.WEI,  LiuQin.QICAI,null,null,null),
            new Yao(1,5, TianGan.XIN, DiZhi.SI,LiuQin.ZISUN,ShiOrYing.YING,null,null),
            new Yao(1,6, TianGan.XIN, DiZhi.MAO,  LiuQin.XIONGDI,  null,null,null)}));
        BAGUA_MAP.put("100011", new BaGua("100011","风雷益","巽宫",null,null,null,new Yao[]{
            
            new Yao(1,1, TianGan.GENG, DiZhi.ZI,  LiuQin.FUMU,  null,null,null),
            new Yao(0,2, TianGan.GENG, DiZhi.YIN, LiuQin.XIONGDI,  null,null,null),
            new Yao(0,3, TianGan.GENG, DiZhi.CHEN,LiuQin.QICAI,   ShiOrYing.SHI,"伏酉金官鬼",null),
            new Yao(0,4, TianGan.XIN, DiZhi.WEI,  LiuQin.QICAI,null,null,null),
            new Yao(1,5, TianGan.XIN, DiZhi.SI,LiuQin.ZISUN,null,null,null),
            new Yao(1,6, TianGan.XIN, DiZhi.MAO,  LiuQin.XIONGDI,  ShiOrYing.YING,null,null)}));
        BAGUA_MAP.put("100111", new BaGua("100111","天雷无妄","巽宫",null,null,ConstantUtil.LIU_CHONG,new Yao[]{
            
            new Yao(1,1, TianGan.GENG, DiZhi.ZI,  LiuQin.FUMU,  ShiOrYing.YING,null,null),
            new Yao(0,2, TianGan.GENG, DiZhi.YIN, LiuQin.XIONGDI,  null,null,null),
            new Yao(0,3, TianGan.GENG, DiZhi.CHEN,LiuQin.QICAI,   null,null,null),
            new Yao(1,4, TianGan.REN, DiZhi.WU,  LiuQin.ZISUN,ShiOrYing.SHI,null,null),
            new Yao(1,5, TianGan.REN, DiZhi.SHEN,LiuQin.GUANGUI,null,null,null),
            new Yao(1,6, TianGan.REN, DiZhi.XU,  LiuQin.QICAI,  null,"伏卯木兄弟",null)}));
        BAGUA_MAP.put("100101", new BaGua("100101","火雷噬嗑","巽宫",null,null,null,new Yao[]{
            
            new Yao(1,1, TianGan.GENG, DiZhi.ZI,  LiuQin.FUMU,  null,null,null),
            new Yao(0,2, TianGan.GENG, DiZhi.YIN, LiuQin.XIONGDI,  ShiOrYing.YING,null,null),
            new Yao(0,3, TianGan.GENG, DiZhi.CHEN,LiuQin.QICAI,   null,null,null),
            new Yao(1,4, TianGan.JI, DiZhi.YOU,  LiuQin.GUANGUI,null,null,null),
            new Yao(0,5, TianGan.JI, DiZhi.WEI,LiuQin.QICAI,ShiOrYing.SHI,null,null),
            new Yao(1,6, TianGan.JI, DiZhi.SI,  LiuQin.ZISUN,  null,null,null)}));
        BAGUA_MAP.put("100001", new BaGua("100001","山雷颐","巽宫",null,YouHunGuiHun.YOUHUNGUA,null,new Yao[]{
            
            new Yao(1,1, TianGan.GENG, DiZhi.ZI,  LiuQin.FUMU,  ShiOrYing.YING,null,null),
            new Yao(0,2, TianGan.GENG, DiZhi.YIN, LiuQin.XIONGDI,  null,null,null),
            new Yao(0,3, TianGan.GENG, DiZhi.CHEN,LiuQin.QICAI,   null,"伏酉金官鬼",null),
            new Yao(0,4, TianGan.BING, DiZhi.XU,  LiuQin.QICAI,ShiOrYing.SHI,null,null),
            new Yao(0,5, TianGan.BING, DiZhi.ZI,LiuQin.FUMU,null,"伏巳火子孙",null),
            new Yao(1,6, TianGan.BING, DiZhi.YIN,  LiuQin.XIONGDI,  null,null,null)}));
        BAGUA_MAP.put("011001", new BaGua("011001","山风蛊","巽宫",null,YouHunGuiHun.GUIHUNGUA,null,new Yao[]{
            
            new Yao(0,1, TianGan.XIN, DiZhi.CHOU,  LiuQin.QICAI,  null,null,null),
            new Yao(1,2, TianGan.XIN, DiZhi.HAI, LiuQin.FUMU,  null,null,null),
            new Yao(1,3, TianGan.XIN, DiZhi.YOU,LiuQin.GUANGUI,   ShiOrYing.SHI,null,null),
            new Yao(0,4, TianGan.BING, DiZhi.XU,  LiuQin.QICAI,null,null,null),
            new Yao(0,5, TianGan.BING, DiZhi.ZI,LiuQin.FUMU,null,"伏巳火子孙",null),
            new Yao(1,6, TianGan.BING, DiZhi.YIN,  LiuQin.XIONGDI,  ShiOrYing.YING,null,null)}));


        //坎宫
        BAGUA_MAP.put("010010", new BaGua("010010","坎为水","坎宫",null,null,ConstantUtil.LIU_CHONG,new Yao[]{
            
            new Yao(0,1, TianGan.WU, DiZhi.YIN,  LiuQin.ZISUN,  null,null,null),
            new Yao(1,2, TianGan.WU, DiZhi.CHEN, LiuQin.GUANGUI,  null,null,null),
            new Yao(0,3, TianGan.WU, DiZhi.WU,LiuQin.QICAI,   ShiOrYing.YING,null,null),
            new Yao(0,4, TianGan.WU, DiZhi.SHEN,  LiuQin.FUMU,null,null,null),
            new Yao(1,5, TianGan.WU, DiZhi.XU,LiuQin.GUANGUI,null,null,null),
            new Yao(0,6, TianGan.WU, DiZhi.ZI,  LiuQin.XIONGDI,  ShiOrYing.SHI,null,null)}));
        BAGUA_MAP.put("110010", new BaGua("110010","水泽节","坎宫",null,null,ConstantUtil.LIU_HE,new Yao[]{
            
            new Yao(1,1, TianGan.DIN, DiZhi.SI,  LiuQin.QICAI,  ShiOrYing.SHI,null,null),
            new Yao(1,2, TianGan.DIN, DiZhi.MAO, LiuQin.ZISUN,  null,null,null),
            new Yao(0,3, TianGan.DIN, DiZhi.CHOU,LiuQin.GUANGUI,   null,null,null),
            new Yao(0,4, TianGan.WU, DiZhi.SHEN,  LiuQin.FUMU,ShiOrYing.YING,null,null),
            new Yao(1,5, TianGan.WU, DiZhi.XU,LiuQin.GUANGUI,null,null,null),
            new Yao(0,6, TianGan.WU, DiZhi.ZI,  LiuQin.XIONGDI,  null,null,null)}));
        BAGUA_MAP.put("100010", new BaGua("100010","水雷屯","坎宫",null,null,null,new Yao[]{
            
            new Yao(1,1, TianGan.GENG, DiZhi.ZI,  LiuQin.XIONGDI,  null,null,null),
            new Yao(0,2, TianGan.GENG, DiZhi.YIN, LiuQin.ZISUN,  ShiOrYing.SHI,null,null),
            new Yao(0,3, TianGan.GENG, DiZhi.CHEN,LiuQin.GUANGUI,   null,"伏午火妻财",null),
            new Yao(0,4, TianGan.WU, DiZhi.SHEN,  LiuQin.FUMU,null,null,null),
            new Yao(1,5, TianGan.WU, DiZhi.XU,LiuQin.GUANGUI,ShiOrYing.YING,null,null),
            new Yao(0,6, TianGan.WU, DiZhi.ZI,  LiuQin.XIONGDI,  null,null,null)}));
        BAGUA_MAP.put("101010", new BaGua("101010","水火即济","坎宫",null,null,null,new Yao[]{
            
            new Yao(1,1, TianGan.JI, DiZhi.MAO,  LiuQin.ZISUN,  null,"伏寅木子孙",null),
            new Yao(0,2, TianGan.JI, DiZhi.CHOU, LiuQin.GUANGUI,  null,null,null),
            new Yao(1,3, TianGan.JI, DiZhi.HAI,LiuQin.XIONGDI,   ShiOrYing.SHI,"伏午火妻财",null),
            new Yao(0,4, TianGan.WU, DiZhi.SHEN,  LiuQin.FUMU,null,null,null),
            new Yao(1,5, TianGan.WU, DiZhi.XU,LiuQin.GUANGUI,null,null,null),
            new Yao(0,6, TianGan.WU, DiZhi.ZI,  LiuQin.XIONGDI,  ShiOrYing.YING,null,null)}));
        BAGUA_MAP.put("101110", new BaGua("101110","泽火革","坎宫",null,null,null,new Yao[]{
            
            new Yao(1,1, TianGan.JI, DiZhi.MAO,  LiuQin.ZISUN,  ShiOrYing.YING,null,null),
            new Yao(0,2, TianGan.JI, DiZhi.CHOU, LiuQin.GUANGUI,  null,null,null),
            new Yao(1,3, TianGan.JI, DiZhi.HAI,LiuQin.XIONGDI,   null,"伏午火妻财",null),
            new Yao(1,4, TianGan.DIN, DiZhi.HAI, LiuQin.XIONGDI, ShiOrYing.SHI,null,null),
            new Yao(1,5, TianGan.DIN, DiZhi.YOU,LiuQin.FUMU,null,null,null),
            new Yao(0,6, TianGan.DIN, DiZhi.WEI,  LiuQin.GUANGUI,  null,null,null)}));
        BAGUA_MAP.put("101100", new BaGua("101100","雷火丰","坎宫",null,null,null,new Yao[]{
            
            new Yao(1,1, TianGan.JI, DiZhi.MAO,  LiuQin.ZISUN,  null,null,null),
            new Yao(0,2, TianGan.JI, DiZhi.CHOU, LiuQin.GUANGUI,  ShiOrYing.YING,null,null),
            new Yao(1,3, TianGan.JI, DiZhi.HAI,LiuQin.XIONGDI,   null,null,null),
            new Yao(1,4, TianGan.GENG, DiZhi.WU,  LiuQin.QICAI,null,null,null),
            new Yao(0,5, TianGan.GENG, DiZhi.SHEN,LiuQin.FUMU,ShiOrYing.SHI,null,null),
            new Yao(0,6, TianGan.GENG, DiZhi.XU,  LiuQin.GUANGUI,  null,null,null)}));
        BAGUA_MAP.put("101000", new BaGua("101000","地火明夷","坎宫",null,YouHunGuiHun.YOUHUNGUA,null,new Yao[]{
            
            new Yao(1,1, TianGan.JI, DiZhi.MAO,  LiuQin.ZISUN,  ShiOrYing.YING,null,null),
            new Yao(0,2, TianGan.JI, DiZhi.CHOU, LiuQin.GUANGUI,  null,null,null),
            new Yao(1,3, TianGan.JI, DiZhi.HAI,LiuQin.XIONGDI,   null,"伏午火妻财",null),
            new Yao(0,4, TianGan.GUI, DiZhi.CHOU,  LiuQin.GUANGUI,ShiOrYing.SHI,null,null),
            new Yao(0,5, TianGan.GUI, DiZhi.HAI,LiuQin.XIONGDI,null,null,null),
            new Yao(0,6, TianGan.GUI, DiZhi.YOU,  LiuQin.FUMU,  null,null,null)}));
        BAGUA_MAP.put("010000", new BaGua("010000","地水师","坎宫",null,YouHunGuiHun.GUIHUNGUA,null,new Yao[]{
            
            new Yao(0,1, TianGan.WU, DiZhi.YIN,  LiuQin.ZISUN,  null,null,null),
            new Yao(1,2, TianGan.WU, DiZhi.CHEN, LiuQin.GUANGUI,  null,null,null),
            new Yao(0,3, TianGan.WU, DiZhi.WU,LiuQin.QICAI,   ShiOrYing.SHI,null,null),
            new Yao(0,4, TianGan.GUI, DiZhi.CHOU,  LiuQin.GUANGUI,null,"伏申金父母",null),
            new Yao(0,5, TianGan.GUI, DiZhi.HAI,LiuQin.XIONGDI,null,null,null),
            new Yao(0,6, TianGan.GUI, DiZhi.YOU,  LiuQin.FUMU,  ShiOrYing.YING,null,null)}));



        //艮宫
        BAGUA_MAP.put("001001", new BaGua("001001","艮为山","艮宫",null,null,ConstantUtil.LIU_CHONG,new Yao[]{
            
            new Yao(0,1, TianGan.BING, DiZhi.CHEN,  LiuQin.XIONGDI,  null,null,null),
            new Yao(0,2, TianGan.BING, DiZhi.WU, LiuQin.FUMU,  null,null,null),
            new Yao(1,3, TianGan.BING, DiZhi.SHEN,LiuQin.ZISUN,   ShiOrYing.YING,null,null),
            new Yao(0,4, TianGan.BING, DiZhi.XU,  LiuQin.XIONGDI,null,null,null),
            new Yao(0,5, TianGan.BING, DiZhi.ZI,LiuQin.QICAI,null,null,null),
            new Yao(1,6, TianGan.BING, DiZhi.YIN,  LiuQin.GUANGUI,  ShiOrYing.SHI,null,null)}));
        BAGUA_MAP.put("101001", new BaGua("101001","山火贲","艮宫",null,null,ConstantUtil.LIU_HE,new Yao[]{
            
            new Yao(1,1, TianGan.JI, DiZhi.MAO,  LiuQin.GUANGUI,  ShiOrYing.SHI,null,null),
            new Yao(0,2, TianGan.JI, DiZhi.CHOU, LiuQin.XIONGDI,  null,"伏午火父母",null),
            new Yao(1,3, TianGan.JI, DiZhi.HAI,LiuQin.QICAI,   null,"伏申金子孙",null),
            new Yao(0,4, TianGan.BING, DiZhi.XU,  LiuQin.XIONGDI,ShiOrYing.YING,null,null),
            new Yao(0,5, TianGan.BING, DiZhi.ZI,LiuQin.QICAI,null,null,null),
            new Yao(1,6, TianGan.BING, DiZhi.YIN,  LiuQin.GUANGUI,  null,null,null)}));
        BAGUA_MAP.put("111001", new BaGua("111001","山天大蓄","艮宫",null,null,null,new Yao[]{
            
            new Yao(1,1, TianGan.JIA, DiZhi.ZI,  LiuQin.QICAI,  null,null,null),
            new Yao(1,2, TianGan.JIA, DiZhi.YIN, LiuQin.GUANGUI,  ShiOrYing.SHI,"伏午火父母",null),
            new Yao(1,3, TianGan.JIA, DiZhi.CHEN,LiuQin.XIONGDI,   null,"伏申金子孙",null),
            new Yao(0,4, TianGan.BING, DiZhi.XU,  LiuQin.XIONGDI,null,null,null),
            new Yao(0,5, TianGan.BING, DiZhi.ZI,LiuQin.QICAI,ShiOrYing.YING,null,null),
            new Yao(1,6, TianGan.BING, DiZhi.YIN,  LiuQin.GUANGUI,  null,null,null)}));
        BAGUA_MAP.put("110001", new BaGua("110001","山泽损","艮宫",null,null,null,new Yao[]{
            
            new Yao(1,1, TianGan.DIN, DiZhi.SI,  LiuQin.FUMU,  null,null,null),
            new Yao(1,2, TianGan.DIN, DiZhi.MAO, LiuQin.GUANGUI,  null,null,null),
            new Yao(0,3, TianGan.DIN, DiZhi.CHOU,LiuQin.XIONGDI,   ShiOrYing.SHI,"伏申金子孙",null),
            new Yao(0,4, TianGan.BING, DiZhi.XU,  LiuQin.XIONGDI,null,null,null),
            new Yao(0,5, TianGan.BING, DiZhi.ZI,LiuQin.QICAI,null,null,null),
            new Yao(1,6, TianGan.BING, DiZhi.YIN,  LiuQin.GUANGUI,  ShiOrYing.YING,null,null)}));
        BAGUA_MAP.put("110101", new BaGua("110101","火泽暌","艮宫",null,null,null,new Yao[]{
            
            new Yao(1,1, TianGan.DIN, DiZhi.SI,  LiuQin.FUMU,  ShiOrYing.YING,null,null),
            new Yao(1,2, TianGan.DIN, DiZhi.MAO, LiuQin.GUANGUI,  null,null,null),
            new Yao(0,3, TianGan.DIN, DiZhi.CHOU,LiuQin.XIONGDI,   null,null,null),
            new Yao(1,4, TianGan.JI, DiZhi.YOU,  LiuQin.ZISUN,ShiOrYing.SHI,null,null),
            new Yao(0,5, TianGan.JI, DiZhi.WEI,LiuQin.XIONGDI,null,"伏子水妻财",null),
            new Yao(1,6, TianGan.JI, DiZhi.SI,  LiuQin.FUMU,  null,null,null)}));
        BAGUA_MAP.put("110111", new BaGua("110111","天泽履","艮宫",null,null,null,new Yao[]{
            
            new Yao(1,1, TianGan.DIN, DiZhi.SI,  LiuQin.FUMU,  null,"伏辰土兄弟",null),
            new Yao(1,2, TianGan.DIN, DiZhi.MAO, LiuQin.GUANGUI,  ShiOrYing.YING,null,null),
            new Yao(0,3, TianGan.DIN, DiZhi.CHOU,LiuQin.XIONGDI,   null,null,null),
            new Yao(1,4, TianGan.REN, DiZhi.WU,  LiuQin.FUMU,null,null,null),
            new Yao(1,5, TianGan.REN, DiZhi.SHEN,LiuQin.ZISUN,ShiOrYing.SHI,"伏子水妻财",null),
            new Yao(1,6, TianGan.REN, DiZhi.XU,  LiuQin.XIONGDI,  null,null,null)}));
        BAGUA_MAP.put("110011", new BaGua("110011","风泽中孚","艮宫",null,YouHunGuiHun.YOUHUNGUA,null,new Yao[]{
            
            new Yao(1,1, TianGan.DIN, DiZhi.SI,  LiuQin.FUMU,  ShiOrYing.YING,null,null),
            new Yao(1,2, TianGan.DIN, DiZhi.MAO, LiuQin.GUANGUI,  null,null,null),
            new Yao(0,3, TianGan.DIN, DiZhi.CHOU,LiuQin.XIONGDI,   null,"伏申金子孙",null),
            new Yao(0,4, TianGan.XIN, DiZhi.WEI,  LiuQin.XIONGDI,ShiOrYing.SHI,null,null),
            new Yao(1,5, TianGan.XIN, DiZhi.SI,LiuQin.FUMU,null,"伏子水妻财",null),
            new Yao(1,6, TianGan.XIN, DiZhi.MAO,  LiuQin.GUANGUI,  null,null,null)}));
        BAGUA_MAP.put("001011", new BaGua("001011","风山渐","艮宫",null,YouHunGuiHun.GUIHUNGUA,null,new Yao[]{
            
            new Yao(0,1, TianGan.BING, DiZhi.CHEN,  LiuQin.XIONGDI,  null,null,null),
            new Yao(0,2, TianGan.BING, DiZhi.WU, LiuQin.FUMU,  null,null,null),
            new Yao(1,3, TianGan.BING, DiZhi.SHEN,LiuQin.ZISUN,   ShiOrYing.SHI,null,null),
            new Yao(0,4, TianGan.XIN, DiZhi.WEI,  LiuQin.XIONGDI,null,null,null),
            new Yao(1,5, TianGan.XIN, DiZhi.SI,LiuQin.FUMU,null,"伏子水妻财",null),
            new Yao(1,6, TianGan.XIN, DiZhi.MAO,  LiuQin.GUANGUI,  ShiOrYing.YING,"伏寅木官鬼",null)}));

        //坤宫
        BAGUA_MAP.put("000000", new BaGua("000000","坤为地","坤宫",null,null,ConstantUtil.LIU_CHONG,new Yao[]{
            
            new Yao(0,1, TianGan.YI, DiZhi.WEI,  LiuQin.XIONGDI,  null,null,null),
            new Yao(0,2, TianGan.YI, DiZhi.SI, LiuQin.FUMU,  null,null,null),
            new Yao(0,3, TianGan.YI, DiZhi.MAO,LiuQin.GUANGUI,   ShiOrYing.YING,null,null),
            new Yao(0,4, TianGan.GUI, DiZhi.CHOU,  LiuQin.XIONGDI,null,null,null),
            new Yao(0,5, TianGan.GUI, DiZhi.HAI,LiuQin.QICAI,null,null,null),
            new Yao(0,6, TianGan.GUI, DiZhi.YOU,  LiuQin.ZISUN,  ShiOrYing.SHI,null,null)}));
        BAGUA_MAP.put("100000", new BaGua("100000","地雷复","坤宫",null,null,ConstantUtil.LIU_HE,new Yao[]{
            
            new Yao(1,1, TianGan.GENG, DiZhi.ZI,  LiuQin.QICAI,  ShiOrYing.SHI,null,null),
            new Yao(0,2, TianGan.GENG, DiZhi.YIN, LiuQin.GUANGUI,  null,"伏巳火父母",null),
            new Yao(0,3, TianGan.GENG, DiZhi.CHEN,LiuQin.XIONGDI,   null,null,null),
            new Yao(0,4, TianGan.GUI, DiZhi.CHOU,  LiuQin.XIONGDI,ShiOrYing.YING,null,null),
            new Yao(0,5, TianGan.GUI, DiZhi.HAI,LiuQin.QICAI,null,null,null),
            new Yao(0,6, TianGan.GUI, DiZhi.YOU,  LiuQin.ZISUN,  null,null,null)}));
        BAGUA_MAP.put("110000", new BaGua("110000","地泽临","坤宫",null,null,null,new Yao[]{
            
            new Yao(1,1, TianGan.DIN, DiZhi.SI,  LiuQin.FUMU,  null,null,null),
            new Yao(1,2, TianGan.DIN, DiZhi.MAO, LiuQin.GUANGUI,  ShiOrYing.SHI,null,null),
            new Yao(0,3, TianGan.DIN, DiZhi.CHOU,LiuQin.XIONGDI,   null,null,null),
            new Yao(0,4, TianGan.GUI, DiZhi.CHOU,  LiuQin.XIONGDI,null,null,null),
            new Yao(0,5, TianGan.GUI, DiZhi.HAI,LiuQin.QICAI,ShiOrYing.YING,null,null),
            new Yao(0,6, TianGan.GUI, DiZhi.YOU,  LiuQin.ZISUN,  null,null,null)}));
        BAGUA_MAP.put("111000", new BaGua("111000","地天泰","坤宫",null,null,ConstantUtil.LIU_HE,new Yao[]{
            
            new Yao(1,1, TianGan.JIA, DiZhi.ZI,  LiuQin.QICAI,  null,null,null),
            new Yao(1,2, TianGan.JIA, DiZhi.YIN, LiuQin.GUANGUI,  null,"伏巳火父母",null),
            new Yao(1,3, TianGan.JIA, DiZhi.CHEN,LiuQin.XIONGDI,   ShiOrYing.SHI,null,null),
            new Yao(0,4, TianGan.GUI, DiZhi.CHOU,  LiuQin.XIONGDI,null,null,null),
            new Yao(0,5, TianGan.GUI, DiZhi.HAI,LiuQin.QICAI,null,null,null),
            new Yao(0,6, TianGan.GUI, DiZhi.YOU,  LiuQin.ZISUN,  ShiOrYing.YING,null,null)}));
        BAGUA_MAP.put("111100", new BaGua("111100","雷天大壮","坤宫",null,null,ConstantUtil.LIU_CHONG,new Yao[]{
            
            new Yao(1,1, TianGan.JIA, DiZhi.ZI,  LiuQin.QICAI,  ShiOrYing.YING,null,null),
            new Yao(1,2, TianGan.JIA, DiZhi.YIN, LiuQin.GUANGUI,  null,null,null),
            new Yao(1,3, TianGan.JIA, DiZhi.CHEN,LiuQin.XIONGDI,   null,null,null),
            new Yao(1,4, TianGan.GENG, DiZhi.WU,  LiuQin.FUMU,ShiOrYing.SHI,null,null),
            new Yao(0,5, TianGan.GENG, DiZhi.SHEN,LiuQin.ZISUN,null,null,null),
            new Yao(0,6, TianGan.GENG, DiZhi.XU,  LiuQin.XIONGDI,  null,null,null)}));
        BAGUA_MAP.put("111110", new BaGua("111110","泽天夬","坤宫",null,null,null,new Yao[]{
            
            new Yao(1,1, TianGan.JIA, DiZhi.ZI,  LiuQin.QICAI,  null,null,null),
            new Yao(1,2, TianGan.JIA, DiZhi.YIN, LiuQin.GUANGUI,  ShiOrYing.YING,"伏巳火父母",null),
            new Yao(1,3, TianGan.JIA, DiZhi.CHEN,LiuQin.XIONGDI,   null,null,null),
            new Yao(1,4, TianGan.DIN, DiZhi.HAI,  LiuQin.QICAI,null,null,null),
            new Yao(1,5, TianGan.DIN, DiZhi.YOU,LiuQin.ZISUN,ShiOrYing.SHI,null,null),
            new Yao(0,6, TianGan.DIN, DiZhi.WEI,  LiuQin.XIONGDI,  null,null,null)}));
        BAGUA_MAP.put("111010", new BaGua("111010","水天需","坤宫",null,YouHunGuiHun.YOUHUNGUA,null,new Yao[]{
            
            new Yao(1,1, TianGan.JIA, DiZhi.ZI,  LiuQin.QICAI,  ShiOrYing.YING,null,null),
            new Yao(1,2, TianGan.JIA, DiZhi.YIN, LiuQin.GUANGUI,  null,"伏巳火父母",null),
            new Yao(1,3, TianGan.JIA, DiZhi.CHEN,LiuQin.XIONGDI,   null,null,null),
            new Yao(0,4, TianGan.WU, DiZhi.SHEN,  LiuQin.ZISUN,ShiOrYing.SHI,null,null),
            new Yao(1,5, TianGan.WU, DiZhi.XU,LiuQin.XIONGDI,null,null,null),
            new Yao(0,6, TianGan.WU, DiZhi.ZI,  LiuQin.QICAI,  null,"伏酉金子孙",null)}));
        BAGUA_MAP.put("000010", new BaGua("000010","水地比","坤宫",null,YouHunGuiHun.GUIHUNGUA,null,new Yao[]{
            
            new Yao(0,1, TianGan.YI, DiZhi.WEI,  LiuQin.XIONGDI,  null,null,null),
            new Yao(0,2, TianGan.YI, DiZhi.SI, LiuQin.FUMU,  null,null,null),
            new Yao(0,3, TianGan.YI, DiZhi.MAO,LiuQin.GUANGUI,   ShiOrYing.SHI,null,null),
            new Yao(0,4, TianGan.WU, DiZhi.SHEN,  LiuQin.ZISUN,null,null,null),
            new Yao(1,5, TianGan.WU, DiZhi.XU,LiuQin.XIONGDI,null,null,null),
            new Yao(0,6, TianGan.WU, DiZhi.ZI,  LiuQin.QICAI,  ShiOrYing.YING,null,null)}));
    }

}
