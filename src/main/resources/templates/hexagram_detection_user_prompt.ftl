以下是需要检测的古书文本，共约 ${contentLength} 个字符。
偏移量 0 对应 ===TEXT_START=== 标记后的第一个字符：

===TEXT_START===
${windowContent}===TEXT_END===

请检测上述文本中包含几个完整六爻占例单元，对每个检测到的占例单元给出：
- start_offset：占例起始字符偏移（相对于 ===TEXT_START=== 后第一个字符，从 0 开始计数）
- end_offset：占例结束字符偏移（同上）
- confidence：该占例完整程度的置信度（0~1）
- hexagram_number：该占例单元内包含的六爻卦例数量（整数，最少为1）
- preview：占例正文的前 20 个字符，必须原文照抄
- postview：占例正文的后 20 个字符，必须原文照抄
