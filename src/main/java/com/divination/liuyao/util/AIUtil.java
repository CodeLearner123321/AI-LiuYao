package com.divination.liuyao.util;

import com.divination.liuyao.pojo.model.AiResult;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AIUtil {
    /**
     * 用于处理AI返回的结果
     * @param aiResponse AI返回的原始文本
     * @return 处理后的AiResult对象，包含判辞和完整文本
     */
    public static AiResult conductAIResults(String aiResponse){
        // 如果AI返回了错误代码，直接返回错误信息
        if (aiResponse != null && aiResponse.contains(ConstantUtil.AI_ERROR_RESULT_CODE)) {
            log.info("AI返回了错误代码，问题或背景可能有误");
            AiResult errorResult = new AiResult();
            errorResult.setText(ConstantUtil.AI_ERROR_RESULT);
            errorResult.setKeyOutcome(ConstantUtil.AI_ERROR_RESULT_KEY);
            return errorResult;
        }

        AiResult result = new AiResult();

        try {
            // 尝试使用多种可能的判辞标记格式
            String[] possiblePrefixes = {
                    "5、判辞", "5、 判辞", "5.判辞", "5. 判辞",
                    "5、判词", "5、 判词", "5.判词", "5. 判词",
                    "5.判断", "5、判断", "5. 判断", "5、 判断",
                    "判辞：", "判辞:", "判词：", "判词:"
            };

            int keyOutcomeStartIndex = -1;
            String foundPrefix = null;

            // 查找判辞部分的开始位置
            for (String prefix : possiblePrefixes) {
                int index = aiResponse.indexOf(prefix);
                if (index != -1) {
                    keyOutcomeStartIndex = index;
                    foundPrefix = prefix;
                    break;
                }
            }

            // 处理找不到判辞的情况
            if (keyOutcomeStartIndex == -1) {
                log.warn("无法通过标准格式在AI响应中找到判辞部分，尝试识别最后一段...");

                // 尝试查找最后一个分隔符后的内容作为判辞
                String[] possibleSeparators = {"\\---", "---", "***", "\\*\\*\\*", "##", "\\n\\n"};

                for (String separator : possibleSeparators) {
                    int lastSepIndex = aiResponse.lastIndexOf(separator);
                    if (lastSepIndex != -1 && lastSepIndex < aiResponse.length() - 10) { // 确保分隔符后还有足够的内容
                        keyOutcomeStartIndex = lastSepIndex + separator.length();
                        foundPrefix = "";
                        break;
                    }
                }

                // 如果仍然找不到，将整个文本作为text返回，提取最后一段作为keyOutcome
                if (keyOutcomeStartIndex == -1) {
                    log.warn("无法在AI响应中找到判辞部分，使用最后一段作为判辞");
                    // 查找最后一个换行符
                    int lastNewline = aiResponse.lastIndexOf("\n\n");
                    if (lastNewline != -1 && lastNewline < aiResponse.length() - 10) {
                        String lastParagraph = aiResponse.substring(lastNewline).trim();
                        result.setKeyOutcome(cleanKeyOutcome(lastParagraph));
                        result.setText(aiResponse); // 设置全文
                    } else {
                        // 如果没有找到适合的最后一段，则设置默认判辞
                        result.setKeyOutcome("请参考完整分析结果");
                        result.setText(aiResponse); // 设置全文
                    }
                    return result;
                }
            }

            log.debug("找到判辞标记: {}, 位置: {}", foundPrefix, keyOutcomeStartIndex);

            // 提取判辞部分
            String keyOutcomePart = aiResponse.substring(keyOutcomeStartIndex);

            // 查找判辞部分的结束位置（如果有下一个段落的话）
            int nextSectionIndex = keyOutcomePart.indexOf("\n\n");
            if (nextSectionIndex != -1) {
                keyOutcomePart = keyOutcomePart.substring(0, nextSectionIndex);
            }

            // 清理判辞文本
            String keyOutcome = cleanKeyOutcome(keyOutcomePart);

            // 如果清理后的判辞是空的，或者太短，可能是处理有误
            if (keyOutcome.isEmpty() || keyOutcome.length() < 5) {
                log.warn("清理后的判辞内容异常短: [{}]，尝试使用原始文本", keyOutcome);
                keyOutcome = keyOutcomePart.replace(foundPrefix, "").trim();
            }

            log.debug("提取的判辞: {}", keyOutcome);

            // 设置keyOutcome
            result.setKeyOutcome(keyOutcome);

            // 设置text（不包含判辞部分）
            if (keyOutcomeStartIndex > 0) {
                result.setText(aiResponse.substring(0, keyOutcomeStartIndex).trim());
            } else {
                result.setText(""); // 如果判辞在开头，text为空
            }

        } catch (Exception e) {
            log.error("处理AI响应时出错: ", e);
            // 出错时将整个响应设为text，keyOutcome留空
            result.setText(aiResponse);
            result.setKeyOutcome("处理出错，请参考完整分析");
        }

        return result;
    }




    /**
     * 清理判辞文本，移除标记和格式
     */
    private static String cleanKeyOutcome(String rawText) {
        if (rawText == null) return "";

        // 移除判辞标记
        String cleaned = rawText
                .replace("5、判辞", "")
                .replace("5、 判辞", "")
                .replace("5.判辞", "")
                .replace("5. 判辞", "")
                .replace("5、判词", "")
                .replace("5、 判词", "")
                .replace("5.判词", "")
                .replace("5. 判词", "")
                .replace("5.判断", "")
                .replace("5、判断", "")
                .replace("5. 判断", "")
                .replace("5、 判断", "")
                .replace("判辞：", "")
                .replace("判辞:", "")
                .replace("判词：", "")
                .replace("判词:", "");

        // 移除Markdown标记
        cleaned = cleaned
                .replaceAll("\\*\\*", "")
                .replaceAll("\\\\-", "")
                .replaceAll("\\\\n", " ")
                .replaceAll("#+", "");

        // 移除多余空白
        cleaned = cleaned.trim();

        // 如果判辞过长，可能不是真正的判辞，截取适当长度
        if (cleaned.length() > 200) {
            log.warn("判辞过长 ({} 字符)，可能包含了额外内容，进行截断", cleaned.length());
            // 尝试在一个合理的位置截断
            int cutPoint = cleaned.indexOf("。", 50);
            if (cutPoint == -1) {
                cutPoint = cleaned.indexOf("，", 50);
            }
            if (cutPoint == -1) {
                cutPoint = cleaned.indexOf("\n", 50);
            }
            if (cutPoint == -1 && cleaned.length() > 100) {
                cutPoint = 100;
            }

            if (cutPoint != -1) {
                cleaned = cleaned.substring(0, cutPoint + 1);
            }
        }

        return cleaned;
    }
}
