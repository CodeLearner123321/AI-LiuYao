package com.divination.liuyao.hexagram.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 文件文本提取工具（静态工具类，无 Spring 依赖，便于单独测试）
 * <p>
 * 支持格式：
 * <ul>
 *   <li>.txt  — 直接读取，默认 UTF-8，若失败自动降级 GBK（兼容古籍常见编码）</li>
 *   <li>.pdf  — Apache PDFBox 2.x 提取</li>
 *   <li>.doc  — Apache POI HWPFDocument 提取（Word 97-2003 格式）</li>
 *   <li>.docx — Apache POI XWPFDocument 提取（Word 2007+ 格式）</li>
 * </ul>
 */
@Slf4j
public class FileTextExtractor {

    private FileTextExtractor() {
        // 静态工具类，禁止实例化
    }

    /**
     * 根据文件扩展名自动选择解析策略，返回纯文本字符串。
     *
     * @param file 待提取的文件
     * @return 提取到的纯文本内容
     * @throws IOException              文件读取失败
     * @throws IllegalArgumentException 文件格式不受支持
     */
    public static String extract(File file) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("文件参数不能为 null");
        }
        if (!file.exists()) {
            throw new IOException("文件不存在: " + file.getAbsolutePath());
        }
        if (!file.isFile()) {
            throw new IOException("路径不是普通文件: " + file.getAbsolutePath());
        }

        String fileName = file.getName().toLowerCase();
        if (fileName.endsWith(".txt")) {
            return extractTxt(file);
        } else if (fileName.endsWith(".pdf")) {
            return extractPdf(file);
        } else if (fileName.endsWith(".doc")) {
            return extractDoc(file);
        } else if (fileName.endsWith(".docx")) {
            return extractDocx(file);
        } else {
            throw new IllegalArgumentException(
                    "不支持的文件格式，仅支持 .txt / .pdf / .doc / .docx，当前文件: " + file.getName());
        }
    }

    // ------------------------------------------------------------------ //
    //  私有方法：各格式具体实现
    // ------------------------------------------------------------------ //

    private static String extractTxt(File file) throws IOException {
        log.info("[FileTextExtractor] 读取 TXT: {}", file.getName());
        // 优先 UTF-8；若出现乱码字符则降级 GBK（古籍扫描文件常用）
        try {
            String text = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            // 简单校验：若包含大量替换字符 (U+FFFD) 则说明编码不对
            if (containsExcessiveReplacementChars(text)) {
                log.warn("[FileTextExtractor] UTF-8 解码疑似乱码，改用 GBK 重试");
                text = new String(Files.readAllBytes(file.toPath()), Charset.forName("GBK"));
            }
            return text;
        } catch (Exception e) {
            log.warn("[FileTextExtractor] UTF-8 读取失败，降级 GBK: {}", e.getMessage());
            return new String(Files.readAllBytes(file.toPath()), Charset.forName("GBK"));
        }
    }

    private static String extractPdf(File file) throws IOException {
        log.info("[FileTextExtractor] 读取 PDF: {}", file.getName());
        try (PDDocument doc = PDDocument.load(file)) {
            if (doc.isEncrypted()) {
                throw new IOException("PDF 已加密，无法提取文本: " + file.getName());
            }
            PDFTextStripper stripper = new PDFTextStripper();
            // 按页顺序提取，保留原始段落换行
            stripper.setSortByPosition(true);
            String text = stripper.getText(doc);
            log.info("[FileTextExtractor] PDF 提取完成，字符数={}", text.length());
            return text;
        }
    }

    private static String extractDoc(File file) throws IOException {
        log.info("[FileTextExtractor] 读取 DOC: {}", file.getName());
        try (FileInputStream fis = new FileInputStream(file);
             HWPFDocument doc = new HWPFDocument(fis);
             WordExtractor extractor = new WordExtractor(doc)) {
            String text = extractor.getText();
            log.info("[FileTextExtractor] DOC 提取完成，字符数={}", text.length());
            return text;
        } catch (IllegalArgumentException e) {
            // 部分用户将 .docx 文件后缀改为 .doc，实际内容是 OOXML 格式
            // HWPFDocument 会抛出 "The document is really a OOXML file"，此处自动降级
            if (e.getMessage() != null && e.getMessage().contains("OOXML")) {
                log.warn("[FileTextExtractor] {} 实际为 OOXML 格式，自动切换 DOCX 解析器", file.getName());
                return extractDocx(file);
            }
            throw new IOException("DOC 文件解析失败: " + e.getMessage(), e);
        }
    }

    private static String extractDocx(File file) throws IOException {
        log.info("[FileTextExtractor] 读取 DOCX: {}", file.getName());
        try (FileInputStream fis = new FileInputStream(file);
             XWPFDocument doc = new XWPFDocument(fis)) {
            List<XWPFParagraph> paragraphs = doc.getParagraphs();
            String text = paragraphs.stream()
                    .map(XWPFParagraph::getText)
                    .collect(Collectors.joining("\n"));
            log.info("[FileTextExtractor] DOCX 提取完成，段落数={}，字符数={}",
                    paragraphs.size(), text.length());
            return text;
        }
    }

    /** 检测文本中替换字符（乱码标志）的比例是否超过阈值 */
    private static boolean containsExcessiveReplacementChars(String text) {
        if (text == null || text.isEmpty()) return false;
        long count = text.chars().filter(c -> c == '\uFFFD').count();
        return (double) count / text.length() > 0.01; // 超过 1% 认为编码有误
    }
}
