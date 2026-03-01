package com.divination.liuyao.hexagram;

import com.divination.liuyao.hexagram.model.HexagramDetectionResult;
import com.divination.liuyao.hexagram.model.TextWindow;
import com.divination.liuyao.hexagram.runner.HexagramDetectionRunner;
import com.divination.liuyao.hexagram.service.HexagramDetectionService;
import com.divination.liuyao.hexagram.service.SlidingWindowService;
import com.divination.liuyao.hexagram.util.FileTextExtractor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.net.URL;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 滑动窗口卦例检测集成测试
 * <p>
 * 使用 @SpringBootTest 加载完整 Spring 上下文（包含 LLMService Bean），
 * 对 test/resources/hexagram/test-hexagram.txt 执行端到端流程验证。
 * <p>
 * 运行前须确保 application.properties 中 ai.dashscope.api.key 已配置有效密钥。
 */
@Slf4j
@SpringBootTest
class HexagramDetectionRunnerTest {

    @Autowired
    private HexagramDetectionRunner runner;

    @Autowired
    private SlidingWindowService slidingWindowService;

    @Autowired
    private HexagramDetectionService hexagramDetectionService;

    // ------------------------------------------------------------------ //
    //  测试一：完整端到端流程（真实 AI 调用）
    // ------------------------------------------------------------------ //

    /**
     * 完整流程测试：文件 → 文本 → 窗口 → AI检测 → 打印
     * <p>
     * 测试文件内包含 2 个卦例，期望至少检测到 1 个。
     */
    @Test
    void testFullDetectionFlow() throws Exception {
        String filePath = "D:/GitHubCode/aiLiuYao/AI-LiuYao/target/test-classes/hexagram/1.doc";
        log.info("测试文件路径: {}", filePath);

        List<HexagramDetectionResult> hits = runner.run(filePath);

        log.info("检测结果数量: {}", hits.size());
        // 测试文件含 2 个明显卦例，至少应命中 1 个
        assertFalse(hits.isEmpty(), "测试文件中应至少检测到 1 个卦例");

        for (HexagramDetectionResult hit : hits) {
            assertNotNull(hit.getSourceWindow(), "命中结果必须携带原始窗口信息");
            assertNotNull(hit.getStartOffset(), "startOffset 不应为 null");
            assertNotNull(hit.getEndOffset(), "endOffset 不应为 null");
            assertNotNull(hit.getAbsoluteStartIndex(), "原文绝对起始位置不应为 null");
            assertNotNull(hit.getAbsoluteEndIndex(), "原文绝对结束位置不应为 null");
            assertTrue(hit.getAbsoluteStartIndex() >= 0, "起始位置应 >= 0");
            assertTrue(hit.getAbsoluteEndIndex() > hit.getAbsoluteStartIndex(), "结束位置应 > 起始 位置");
        }
    }

    // ------------------------------------------------------------------ //
    //  测试二：FileTextExtractor 单元测试（无需 AI）
    // ------------------------------------------------------------------ //

    @Test
    void testFileTextExtractorTxt() throws Exception {
        String filePath = "D:/GitHubCode/aiLiuYao/AI-LiuYao/target/test-classes/hexagram/1.doc";
        String text = FileTextExtractor.extract(new File(filePath));

        assertNotNull(text);
        assertFalse(text.isBlank(), "提取文本不应为空");
        log.info("TXT 提取成功，字符数={}", text.length());
        log.info("TXT 提取成功，文本内容={}", text);

    }

    // ------------------------------------------------------------------ //
    //  测试三：SlidingWindowService 单元测试（无需 AI）
    // ------------------------------------------------------------------ //

    @Test
    void testSlidingWindowGeneration() {
        // 构造一段长度 5000 的文本
        String text = "六爻".repeat(2500); // 每个汉字2字节，但Java按char计，此处共5000字符
        List<TextWindow> windows = slidingWindowService.generateWindows(text, 3000, 1500);

        assertFalse(windows.isEmpty());

        // 验证第一个窗口
        TextWindow first = windows.get(0);
        assertEquals(0, first.getWindowIndex());
        assertEquals(0, first.getStartIndex());
        assertEquals(3000, first.getEndIndex());
        assertEquals(3000, first.getContent().length());

        // 验证第二个窗口起点
        TextWindow second = windows.get(1);
        assertEquals(1, second.getWindowIndex());
        assertEquals(1500, second.getStartIndex());

        // 验证最后一个窗口结束不超过文本长度
        TextWindow last = windows.get(windows.size() - 1);
        assertTrue(last.getEndIndex() <= text.length());

        log.info("窗口生成测试通过，共 {} 个窗口", windows.size());
    }

    @Test
    void testSlidingWindowWithShortText() {
        // 文本短于 windowSize，应只生成 1 个窗口
        String text = "这是一段很短的测试文本，不足一个窗口大小。";
        List<TextWindow> windows = slidingWindowService.generateWindows(text, 3000, 1500);

        assertEquals(1, windows.size());
        assertEquals(0, windows.get(0).getStartIndex());
        assertEquals(text.length(), windows.get(0).getEndIndex());
    }

    @Test
    void testSlidingWindowWithEmptyText() {
        List<TextWindow> windows = slidingWindowService.generateWindows("", 3000, 1500);
        assertTrue(windows.isEmpty(), "空文本应返回空窗口列表");
    }

    // ------------------------------------------------------------------ //
    //  测试四：自定义文件路径（手动测试时替换路径）
    // ------------------------------------------------------------------ //

    /**
     * 手动测试入口：替换 YOUR_FILE_PATH 为真实古籍文件路径后运行。
     * 默认跳过以防止 CI 环境失败。
     */
    @Test
    void testCustomFilePath() {
        String customPath = "YOUR_FILE_PATH_HERE"; // ← 替换为实际文件路径
        if ("YOUR_FILE_PATH_HERE".equals(customPath)) {
            log.info("testCustomFilePath 已跳过（未设置真实文件路径）");
            return;
        }
        List<HexagramDetectionResult> hits = runner.run(customPath);
        log.info("自定义文件检测完成，共命中 {} 个卦例", hits.size());
    }

    // ------------------------------------------------------------------ //
    //  工具方法
    // ------------------------------------------------------------------ //

    private String getTestFilePath(String resourcePath) {
        URL resource = getClass().getClassLoader().getResource(resourcePath);
        assertNotNull(resource, "测试资源文件不存在: " + resourcePath);
        return resource.getPath();
    }
}
