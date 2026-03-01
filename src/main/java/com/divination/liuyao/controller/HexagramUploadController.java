package com.divination.liuyao.controller;

import com.divination.liuyao.mapper.HexagramCaseMapper;
import com.divination.liuyao.mapper.SourceFileMapper;
import com.divination.liuyao.pojo.vo.HexagramCaseListVO;
import com.divination.liuyao.pojo.vo.SourceFileStatusVO;
import com.divination.liuyao.pojo.vo.SourceFileUploadVO;
import com.divination.liuyao.result.RespEntity;
import com.divination.liuyao.service.SourceFileService;
import com.divination.liuyao.util.UserContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 六爻卦例文件上传与查询接口。
 * <p>
 * 当前权限：仅管理员（isRoot）可操作，后续开放时去掉权限校验即可。
 *
 * <pre>
 * POST  /api/hexagram/upload          上传文件并触发异步卦例检测
 * GET   /api/hexagram/file/{id}       查询 source_file 解析状态
 * GET   /api/hexagram/cases/{fileId}  查询某文件下的所有卦例
 * </pre>
 */
@Slf4j
@RestController
@RequestMapping("/api/hexagram")
@RequiredArgsConstructor
public class HexagramUploadController {

    private final SourceFileService   sourceFileService;
    private final SourceFileMapper    sourceFileMapper;
    private final HexagramCaseMapper  hexagramCaseMapper;

    // ------------------------------------------------------------------ //
    //  上传文件
    // ------------------------------------------------------------------ //

    /**
     * 上传文件并触发异步 AI 卦例检测。
     * <p>
     * 同步返回 {@link SourceFileUploadVO}；AI 检测在后台运行，
     * 前端可凭 {@code sourceFileId} 轮询 {@code GET /api/hexagram/file/{id}} 的 {@code parseStatus}。
     *
     * @param file 上传的文件（multipart/form-data 字段名 "file"）
     * @return 上传结果，含 sourceFileId 及是否重复标志
     */
    @PostMapping("/upload")
    public RespEntity<SourceFileUploadVO> upload(@RequestParam("file") MultipartFile file) {
        if (!UserContextHolder.isRoot()) {
            return RespEntity.error("暂无权限，该功能仅管理员可用");
        }
        if (file == null || file.isEmpty()) {
            return RespEntity.error("上传文件不能为空");
        }
        try {
            SourceFileUploadVO vo = sourceFileService.upload(file);
            return RespEntity.ok(vo);
        } catch (Exception e) {
            log.error("[HexagramUploadController] 文件上传失败: {}", e.getMessage(), e);
            return RespEntity.error("文件上传失败：" + e.getMessage());
        }
    }


    /**
     * 查询 source_file 的解析状态（按需查询，仅返回轮询所需字段）。
     * <p>
     * {@code parseStatus}：0=检测中 1=已完成 2=失败
     *
     * @param id source_file 主键
     * @return 状态 VO，不含 full_text、oss_url、file_md5 等
     */
    @GetMapping("/file/{id}")
    public RespEntity<SourceFileStatusVO> getFileStatus(@PathVariable Long id) {
        if (!UserContextHolder.isRoot()) {
            return RespEntity.error("暂无权限");
        }
        SourceFileStatusVO vo = sourceFileMapper.selectStatusById(id);
        if (vo == null) {
            return RespEntity.error("记录不存在，id=" + id);
        }
        return RespEntity.ok(vo);
    }

    /**
     * 查询指定文件下所有已识别的卦例（按需查询，仅返回展示所需字段）。
     * <p>
     * 需 parse_status=1 后才有数据；状态为 0 或 2 时返回空列表。
     *
     * @param fileId source_file 主键
     * @return 卦例列表 VO，不含 raw_ai_json、ai_model 等
     */
    @GetMapping("/cases/{fileId}")
    public RespEntity<List<HexagramCaseListVO>> getCases(@PathVariable Long fileId) {
        if (!UserContextHolder.isRoot()) {
            return RespEntity.error("暂无权限");
        }
        List<HexagramCaseListVO> cases = hexagramCaseMapper.selectListBySourceFileId(fileId);
        return RespEntity.ok(cases);
    }
}
