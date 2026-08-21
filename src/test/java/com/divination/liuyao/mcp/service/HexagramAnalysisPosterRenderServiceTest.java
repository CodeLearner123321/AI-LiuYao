package com.divination.liuyao.mcp.service;

import com.divination.liuyao.assemblies.enums.DiZhi;
import com.divination.liuyao.assemblies.enums.TianGan;
import com.divination.liuyao.pojo.dto.BaZi;
import com.divination.liuyao.pojo.entity.Prediction;
import com.divination.liuyao.pojo.model.AiResult;
import com.divination.liuyao.pojo.model.BaGua;
import com.divination.liuyao.pojo.model.Hexagram;
import com.divination.liuyao.pojo.vo.BaGuaVo;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import static com.divination.liuyao.pojo.model.Hexagram.createShenSha;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("integration")
class HexagramAnalysisPosterRenderServiceTest {

    @Test
    void shouldRenderAnalysisPosterPreviewsWithDifferentTextLengths() throws Exception {
        HexagramAnalysisPosterHtmlService htmlService = new HexagramAnalysisPosterHtmlService();
        HexagramAnalysisPosterRenderService service = new HexagramAnalysisPosterRenderService(
            htmlService,
            new BrowserScreenshotService()
        );
        PosterBackgroundService backgroundService = new PosterBackgroundService();

        List<Map.Entry<String, BaGua>> entries = loadBaguaMapEntries();
        assertTrue(entries.size() > 10, "BAGUA_MAP 中可用卦象不足");

        String originalName = entries.get(2).getValue().getName();
        String changedName = entries.get(11).getValue().getName();
        Hexagram hexagram = buildHexagram(originalName, changedName);
        Prediction prediction = buildPrediction(originalName, changedName);

        List<TestCase> cases = List.of(
            new TestCase("short", buildShortAnalysisResult()),
            new TestCase("medium", buildMediumAnalysisResult()),
            new TestCase("long", buildLongAnalysisResult()),
            new TestCase("xlong", buildExtraLongAnalysisResult()),
            new TestCase("xxlong", buildVeryLongAnalysisResult()),
            new TestCase("mega", buildMegaLongAnalysisResult())
        );

        Path backgroundPath = backgroundService.prepareDefaultBackgroundPath();
        try {
            for (TestCase testCase : cases) {
                HexagramAnalysisPosterHtmlService.RenderedPoster renderedPoster =
                    htmlService.render(hexagram, prediction, testCase.analysisResult, backgroundPath.getFileName().toString());
                Path output = Path.of("target", "test-output", "hexagram-analysis-poster-" + testCase.name + ".png");
                Path actual = service.renderToFile(hexagram, prediction, testCase.analysisResult, backgroundPath, output);
                assertTrue(Files.exists(actual), "结果图未生成: " + actual);
                assertTrue(Files.size(actual) > 0, "结果图为空文件: " + actual);
                System.out.println(
                    "case=" + testCase.name
                        + ", textLength=" + testCase.analysisResult.getText().length()
                        + ", estimatedHeight=" + renderedPoster.getHeight()
                        + ", output=" + actual.toAbsolutePath()
                );
            }
        } finally {
            Files.deleteIfExists(backgroundPath);
        }
    }

    private Hexagram buildHexagram(String originalName, String changedName) {
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
        hexagram.setLocalDateTime(LocalDateTime.of(2026, 3, 21, 9, 30));
        hexagram.setQuestionDescription("测试案例：工作项目推进是否顺利，以及接下来应如何调整节奏？");
        hexagram.setQuestionBackground("此测试用于验证带分析结论的卦象结果图是否能稳定生成，便于后续持续调样式、调字号和调内容分区。当前项目处于功能扩展阶段，希望评估近期推进效率、协作阻力与节奏把控。现实情况下，用户给出的背景描述往往也不会太短，因此这里刻意保留一段较长背景用于压测。 ");
        hexagram.setShenSha(createShenSha(
            baZi.getYearDiZhi(),
            baZi.getMonthDiZhi(),
            baZi.getDayDiZhi(),
            baZi.getDayTianGan()
        ));
        hexagram.setNumber(BaGua.getStringByBaGua(baGuaVo.getOriginalBaGua(), baGuaVo.getChangedBaGua()));
        return hexagram;
    }

    private Prediction buildPrediction(String originalName, String changedName) {
        Prediction prediction = new Prediction();

        Prediction.Description description = new Prediction.Description();
        description.setQuestion("测试案例：工作项目推进是否顺利，以及接下来应如何调整节奏？");
        description.setBackground("此测试用于验证带分析结论的卦象结果图是否能稳定生成，便于后续持续调样式。当前项目处于功能扩展阶段，希望评估近期推进效率、协作阻力与节奏把控。现实情况下，用户给出的背景描述往往也不会太短，因此这里刻意保留一段较长背景用于压测。");
        prediction.setDescription(description);

        Prediction.Gua gua = new Prediction.Gua();
        gua.setZhuGua(originalName);
        gua.setBianGua(changedName);
        prediction.setGua(gua);

        Prediction.Time time = new Prediction.Time();
        time.setYear(buildDatePart("2026", "甲", "辰"));
        time.setMonth(buildDatePart("3", "丁", "卯"));
        time.setDay(buildDatePart("21", "己", "酉"));
        time.setHour(buildDatePart("9", "癸", "未"));
        prediction.setTime(time);

        return prediction;
    }

    private AiResult buildShortAnalysisResult() {
        return new AiResult(
            "可成。",
            "事情可以推进，阻力不算大。先稳住节奏，把关键节点盯紧，近期会有起色。",
            0L,
            0L,
            0L,
            true
        );
    }

    private AiResult buildMediumAnalysisResult() {
        return new AiResult(
            "事有转机，宜先稳后进。",
            "从卦象看，当前事情并非完全受阻，而是处在可以推进但不宜冒进的阶段。你手里其实有一定主动权，只是节奏还不够稳，容易因为外部变化打乱原本安排。\n\n短期内适合先梳理主线，把最关键的任务、人和时间点明确下来。只要不急于一步到位，反而更容易把事情做成。",
            0L,
            0L,
            0L,
            true
        );
    }

    private AiResult buildLongAnalysisResult() {
        return new AiResult(
            "压力可化，事仍可成。",
            "从卦象来看，事情虽然有波动，但整体仍有推进空间，并不是已经走到无路可走的局面。主卦反映的是当前状态下资源、关系和节奏之间并不完全顺畅，局部会有掣肘；变卦则提示你后续需要进入一个重新协调、不断修正的过程。\n\n具体来看，问题不完全在执行力，而更像是在优先级安排、协作分工和外部预期之间存在错位。你越想一次把所有环节同时推进，越容易出现局部阻滞。相反，如果把主线压缩清楚，把边缘问题阶段性放下，整体推进感会明显增强。\n\n因此这件事不是不能成，而是需要你换一种推进方式。先处理最关键的一两个卡点，再去扩展整体面，节奏上宁可稳一点，也不要急着求快。只要持续调整，事情最终仍偏向可成。",
            0L,
            0L,
            0L,
            true
        );
    }

    private AiResult buildExtraLongAnalysisResult() {
        return new AiResult(
            "压力化转事能成",
            "1、用神\n主用神：官鬼申金（应爻），代表项目本身，因官鬼象征事业、项目推进。\n辅用神：世爻妻财辰土，象征自身掌控的资源与能力；子孙午火，象征解决压力与阻碍的助力。\n\n2、理法\n官鬼申金得日生，又受回头生，虽然月令对其有泄，但整体仍然能够站得住。世爻妻财辰土旺而能生应爻，说明你本人的资源、执行力、调配能力并不差，对项目有实际支撑。子孙午火虽动，但休囚偏弱，对官鬼的克制有限，因此化解问题的力量有，但不够强。\n\n3、象法\n从象意上看，主卦雷水解，本身就有解困、疏通、化压之象，说明事情不是彻底卡死，而是处于可通过调整获得突破的状态。变卦坎为水，提示后续仍会有反复、波动和心理压力，尤其在进度推进过程中，容易因为外界变化或内部沟通不到位而出现新的焦虑。\n\n4、结论\n项目并不是不能推进，相反，整体是有机会走通的，只是推进过程中会感觉压力大、节奏乱、协作不够顺手。真正的问题不在于有没有能力，而在于是否能把主线收紧，把反复横跳的部分压住。若能减少无效沟通、提前锁定关键节点、把容易拖延的事项前置处理，那么事情会逐步从被动转主动。短期有扰，中期可成。",
            0L,
            0L,
            0L,
            true
        );
    }

    private AiResult buildVeryLongAnalysisResult() {
        return new AiResult(
            "事多反复，但主线仍可成。",
            "1、整体判断\n从整个卦象的组合来看，这件事不是立即顺畅见效的类型，而是带着明显的波动、等待和反复。你会在推进过程中不断感受到局部阻力，有时像是外部环境不配合，有时又像是内部协作与节奏没有踩准。真正的问题并不在于有没有机会，而在于机会出现时是否能接住。\n\n2、当前状态\n主卦反映的是当前局面中主线尚在，但边缘变量较多。你对事情本身不是完全失控，甚至可以说还有不少可调度资源，只不过这些资源没有在同一时间形成合力，因此看上去总像差一口气。事情之所以让人焦躁，往往不是因为完全推不动，而是因为推进一下就会遇到新的小阻点。\n\n3、阻力来源\n阻力主要来自三个层面。第一是信息不同步，很多时候你认为已经明确的事项，在别人那里仍然停留在模糊状态。第二是预期管理不到位，有些节点没有真正达成共识，只是暂时默认往下推，后面自然还会反复。第三是节奏问题，事情本该分层推进，却容易被并行处理成一团，最后谁都在忙，但真正最重要的部分反而没有得到足够聚焦。\n\n4、如何应对\n最有效的办法不是继续加码推进，而是先压缩主线。你需要先明确：这一阶段必须达成的目标是什么，哪个环节最关键，谁的动作对结果影响最大。把这些点钉住以后，再去处理边缘问题，整体效率会明显提升。若继续试图所有问题一起解决，反而更容易陷入疲于奔命。\n\n5、时间节奏\n短期看仍有压力，尤其容易在沟通和排期上出现卡顿；中段会逐渐见到转机，但前提是你能把资源重新排布，而不是延续旧的推进方式；后期则偏向稳中有成，只是这个成不是一步到位，而是持续修正之后慢慢落地。\n\n6、最终结论\n这件事可以做成，但过程一定不是线性的。你越能接受它需要分阶段推进、需要不断修正，就越容易把握主动。真正决定结果的不是某一次冲刺，而是你是否能持续稳定地主导节奏、筛选重点、减少无效消耗。",
            0L,
            0L,
            0L,
            true
        );
    }

    private AiResult buildMegaLongAnalysisResult() {
        return new AiResult(
            "势可成，惟忌乱。",
            "1、卦意总览\n这类问题在实战里最常见的误区，是看到局部压力就误判成全局失利。实际上，从卦象整体关系来看，事情仍然保有相当强的可塑性，核心主线并没有断，只是推进路径不够平直，导致人在体感上会觉得很累、很乱、很难一口气走通。\n\n2、局势结构\n主卦更多是在说现阶段的基础盘面并不差，说明资源、能力和机会三者并不是完全缺失的。变卦则透露出一个重要信息：未来会经历一次比较明显的节奏切换，也就是说你不能完全按当前方法一路推到底。旧的推进方式可能能把你带到某一个节点，但未必能把你带到最终结果。\n\n3、内外关系\n从人与事的关系看，你现在处在一个既需要主动，又不能只靠蛮力主动的阶段。外部环境并不是完全敌对，但它不会自动配合你；内部资源也不是完全不足，但它们不会天然整合在一起。于是就形成了一个很典型的局面：每一块单看都还行，但一旦拼起来，就总有一处接不上。\n\n4、执行层面\n执行上的问题，最容易出现在以下几个地方：第一，关键节点和普通节点没有真正区分；第二，对外沟通停留在表面确认，没有落实到可执行动作；第三，缺少对波动的预案，一旦某个环节晚一点、慢一点、变一点，整体节奏就开始乱。长远看，这不是能力问题，而是系统问题。\n\n5、资源层面\n资源其实并不算差，只是配置方式需要调整。你手上可能已经有足够推进事情的人、时间或条件，但如果投放顺序不对，资源就会白白消耗。真正高效的方式，是先用资源保障主线，等主线稳住以后，再把余量分配给辅助事项。顺序错了，就会出现边缘事情做了很多，主干事情却没有明显进展。\n\n6、协作层面\n协作是这件事里很容易被低估的变量。很多卡点表面看像是任务本身难，实际上是协作节奏错位。有人提早一步，有人滞后两步，有人理解的是目标，有人理解的只是动作，最后就会出现表面都在推进，实际没有真正形成闭环的情况。你要特别警惕这种“看起来很忙、实际上很散”的假性推进。\n\n7、风险层面\n风险并不是突然的大失败，而更像是连续的小偏差累积成的大问题。最常见的是：一个节点延后，一条信息没同步，一个条件没确认，一个人临时改口，单件看都不算大，但叠起来就会让整体推进效率明显下降。因此，真正需要补的不是信心，而是约束机制和校验机制。\n\n8、节奏建议\n接下来最好的方式是分三段走。第一段先锁定最重要的结果，不求面面俱到；第二段围绕结果反推资源和节奏，把必须协同的人和信息串起来；第三段再处理那些表面看起来很急、但实际上可以后置的问题。你越能控制顺序，越能减少无效波动。\n\n9、心理层面\n心理压力会贯穿这段过程，原因并不是你看不到希望，而是希望总在波动中出现。最消耗人的地方，不是彻底失败，而是不断看到可能性、又不断遇到反复。所以这类事情特别考验稳定性。你要做的不是等一个完全没有阻力的时机，而是在有阻力时依然保持推进秩序。\n\n10、最终判断\n综合来看，事情仍属可成之局，但成在有序，不成在混乱。若继续维持当前比较分散、被动响应的推进方式，结果会拖、会反复、会消耗；若能收束主线、重建节奏、明确协作边界，后续会越来越顺。这个卦最重要的提醒不是“要不要做”，而是“怎么做才不会白白消耗”。",
            0L,
            0L,
            0L,
            true
        );
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

    private static class TestCase {
        private final String name;
        private final AiResult analysisResult;

        private TestCase(String name, AiResult analysisResult) {
            this.name = name;
            this.analysisResult = analysisResult;
        }
    }
}
