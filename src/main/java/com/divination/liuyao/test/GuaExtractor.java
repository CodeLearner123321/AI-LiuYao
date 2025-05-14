package com.divination.liuyao.test;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.regex.*;
import java.util.*;
import java.sql.*;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.ooxml.POIXMLDocument;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.exceptions.OLE2NotOfficeXmlFileException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

public class GuaExtractor {

    public static void main(String[] args) throws Exception {
        String text = readDocument("D:\\GitHubCode\\aiLiuYao\\AI-LiuYao\\docs\\《增删卜易》下 白话版(副本) 中国古代占卜经典 (清)野鹤老人著  - 副本.doc");

        // 按【原文】【今译】分割
        String[] sections = text.split("【原文】");

        List<GuaExample> examples = new ArrayList<>();

        for (String section : sections) {
            if (section.contains("【今译】")) {
                String[] parts = section.split("【今译】");
                String originalText = parts[0];
                String translatedText = parts[1];

                GuaExample example = new GuaExample();
                example.originalText = originalText.trim();
                example.translatedText = translatedText.trim();

                // 提取得卦信息
                Pattern guaPattern = Pattern.compile("得(.*?)之(.*?)卦");
                Matcher guaMatcher = guaPattern.matcher(originalText);
                if (guaMatcher.find()) {
                    example.originalGua = guaMatcher.group(1);
                    example.changedGua = guaMatcher.group(2);
                }

                // 提取占卜问题
                Pattern questionPattern = Pattern.compile("占(.*?)，得");
                Matcher questionMatcher = questionPattern.matcher(originalText);
                if (questionMatcher.find()) {
                    example.question = questionMatcher.group(1);
                }

                // 提取动爻排盘（包含●●或×的行）
                List<String> yaoLines = new ArrayList<>();
                Pattern yaoPattern = Pattern.compile(".*?[●×].*");
                Matcher yaoMatcher = yaoPattern.matcher(originalText);
                while (yaoMatcher.find()) {
                    yaoLines.add(yaoMatcher.group());
                }
                example.yaoLines = yaoLines;

                // 其他字段可以再提取分析

                examples.add(example);
            }
        }

        // 保存到数据库
        insertToMySQL(examples);
    }

    static void insertToMySQL(List<GuaExample> examples) throws Exception {
//        Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/yourdb", "user", "password");
//        PreparedStatement stmt = conn.prepareStatement("INSERT INTO gua_cases (question, original_gua, changed_gua, original_text, translated_text) VALUES (?, ?, ?, ?, ?)");
//
//        for (GuaExample ex : examples) {
//            stmt.setString(1, ex.question);
//            stmt.setString(2, ex.originalGua);
//            stmt.setString(3, ex.changedGua);
//            stmt.setString(4, ex.originalText);
//            stmt.setString(5, ex.translatedText);
//            stmt.addBatch();
//        }
//        stmt.executeBatch();
//        conn.close();
    }

    static String readDocument(String path) {
        StringBuilder content = new StringBuilder();
        try (FileInputStream fis = new FileInputStream(path)) {
            try {
                // 先尝试以docx打开
                OPCPackage opcPackage = OPCPackage.open(fis);
                XWPFDocument document = new XWPFDocument(opcPackage);
                XWPFWordExtractor extractor = new XWPFWordExtractor(document);
                System.out.println("检测到DOCX格式");
                content.append(extractor.getText());
            } catch (OLE2NotOfficeXmlFileException e) {
                // 如果失败，说明是doc
                System.out.println("检测到DOC格式");
                try (FileInputStream fis2 = new FileInputStream(path);
                    HWPFDocument document = new HWPFDocument(fis2);
                    WordExtractor extractor = new WordExtractor(document)) {
                    String[] paragraphs = extractor.getParagraphText();
                    for (String para : paragraphs) {
                        content.append(para.trim()).append("\n");
                    }
                }
            } catch (InvalidFormatException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return content.toString();
    }
}

class GuaExample {
    String question;
    String originalGua;
    String changedGua;
    List<String> yaoLines;
    String originalText;
    String translatedText;
}
