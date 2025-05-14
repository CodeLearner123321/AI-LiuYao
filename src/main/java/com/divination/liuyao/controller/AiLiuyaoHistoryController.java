import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;

import java.io.FileInputStream;
import java.io.IOException;

class GuaExtractor {

    public static void main(String[] args) throws Exception {
        String text = readDocument("path/to/your/file.docx"); // 不管是.doc还是.docx都行

        System.out.println(text.substring(0, Math.min(1000, text.length()))); // 打印前1000字符
    }

    static String readDocument(String path) {
        StringBuilder content = new StringBuilder();
        try (FileInputStream fis = new FileInputStream(path)) {
            if (path.toLowerCase().endsWith(".doc")) {
                HWPFDocument document = new HWPFDocument(fis);
                WordExtractor extractor = new WordExtractor(document);
                String[] paragraphs = extractor.getParagraphText();
                for (String para : paragraphs) {
                    content.append(para.trim()).append("\n");
                }
            } else if (path.toLowerCase().endsWith(".docx")) {
                XWPFDocument document = new XWPFDocument(fis);
                XWPFWordExtractor extractor = new XWPFWordExtractor(document);
                content.append(extractor.getText());
            } else {
                throw new IllegalArgumentException("Unsupported file format!");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return content.toString();
    }
}