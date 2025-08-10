package com.divination.liuyao.controller;

import com.divination.liuyao.pojo.dto.BookInfoDTO;
import com.divination.liuyao.pojo.dto.PageRequestDTO;
import com.divination.liuyao.pojo.dto.PageResultDTO;
import com.divination.liuyao.pojo.entity.FileInfo;
import com.divination.liuyao.result.RespEntity;
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

@Slf4j
@RestController
@RequestMapping("/api/file")
public class FileController {

    @Autowired
    private FileService fileService;

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
    public ResponseEntity<Resource> downloadFile(@PathVariable("fileId") Long fileId) {
        return fileService.downloadFile(fileId);
    }



    private final Path fileStorageLocation;
    private final Path systemBooksLocation;

    public FileController() {
        // 设置文件存储位置，这里使用项目根目录下的uploads文件夹
        this.fileStorageLocation = Paths.get("books","user").toAbsolutePath().normalize();
        // 设置系统书籍位置
        this.systemBooksLocation = Paths.get("books", "system").toAbsolutePath().normalize();
        try {
            // 如果目录不存在则创建
            if (!this.fileStorageLocation.toFile().exists()) {
                this.fileStorageLocation.toFile().mkdirs();
            }
            if (!this.systemBooksLocation.toFile().exists()) {
                this.systemBooksLocation.toFile().mkdirs();
            }
        } catch (Exception ex) {
            throw new RuntimeException("无法创建文件存储目录", ex);
        }
    }

    /**
     * 获取系统书籍列表（分页）
     * @param current 当前页码，默认为1
     * @param size 每页大小，默认为10
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


}
