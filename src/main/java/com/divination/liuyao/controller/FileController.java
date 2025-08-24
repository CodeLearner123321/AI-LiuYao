package com.divination.liuyao.controller;

import com.divination.liuyao.annotation.RateLimit;
import com.divination.liuyao.pojo.dto.BookInfoDTO;
import com.divination.liuyao.pojo.dto.PageRequestDTO;
import com.divination.liuyao.pojo.dto.PageResultDTO;
import com.divination.liuyao.pojo.entity.FileInfo;
import com.divination.liuyao.result.RespEntity;
import com.divination.liuyao.service.AiAnalysisService;
import com.divination.liuyao.service.FileService;
import com.divination.liuyao.util.UserContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/api/file")
public class FileController {

    @Autowired
    private FileService fileService;
    @Autowired
    private AiAnalysisService aiAnalysisService;

    /**
     * 文件上传接口
     */
    @PostMapping("/upload")
    public RespEntity<Long> uploadFile(@RequestParam("file") MultipartFile file, @ModelAttribute FileInfo fileInfo) {
        if (!UserContextHolder.isRoot()) {
            return RespEntity.error("无权限上传文件");
        }
        return RespEntity.ok(fileService.uploadFile(file, fileInfo));
    }

    /**
     * 文件下载接口
     */
    @GetMapping("/download/{fileId}")
    @RateLimit(period = 1, timeUnit = TimeUnit.DAYS, maxRequests = 2, message = "一天最多只能下载两次文件")
    public ResponseEntity<String> downloadFile(@PathVariable("fileId") Long fileId) {
        return ResponseEntity.ok(fileService.downloadFile(fileId));
    }


    /**
     * 获取系统书籍列表（分页）
     * @return 分页结果，包含书籍信息列表和分页信息
     */
    @PostMapping("/system/books")
    public RespEntity<PageResultDTO<BookInfoDTO>> getSystemBooks(@RequestBody PageRequestDTO pageRequest) {
        try {
            // 调用服务层方法进行分页查询
            PageResultDTO<BookInfoDTO> pageResult = fileService.getSystemBooks(pageRequest);
            return RespEntity.ok(pageResult);
        } catch (Exception e) {
            log.error("获取系统书籍列表失败", e);
            return RespEntity.error("获取系统书籍列表失败");
        }
    }

    /**
     * 上传图片，识别文字
     */
    @PostMapping("/recognize")
    public ResponseEntity<String> recognizeText(@RequestParam("file") MultipartFile file) {
        try {
            aiAnalysisService.recognizeTextByImage(file);

            return ResponseEntity.ok("1");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("识别失败: " + e.getMessage());
        }
    }


}
