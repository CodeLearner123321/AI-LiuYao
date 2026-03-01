package com.divination.liuyao.service;

import com.divination.liuyao.pojo.vo.SourceFileUploadVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 原始文件上传服务接口。
 */
public interface SourceFileService {

    /**
     * 上传文件并触发后台卦例检测。
     * <p>
     * 同步部分：MD5 去重 → OSS 上传 → 文本提取 → source_file 入库。<br>
     * 异步部分：滑动窗口 AI 检测 → hexagram_case 批量入库 → 更新 source_file 状态。
     *
     * @param file 用户上传的文件（MultipartFile）
     * @return 上传结果 VO，包含 sourceFileId 和是否重复标志
     */
    SourceFileUploadVO upload(MultipartFile file);
}
