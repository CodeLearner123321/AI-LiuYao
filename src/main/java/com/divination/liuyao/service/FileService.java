package com.divination.liuyao.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.divination.liuyao.pojo.dto.BookInfoDTO;
import com.divination.liuyao.pojo.dto.PageRequestDTO;
import com.divination.liuyao.pojo.dto.PageResultDTO;
import com.divination.liuyao.pojo.entity.FileInfo;
import com.divination.liuyao.result.RespEntity;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileService {

    /**
     * 上传文件
     *
     * @param file 要上传的文件
     * @param fileInfo
     * @return 返回上传结果
     */
    Long uploadFile(MultipartFile file, FileInfo fileInfo);


    /**
     * 下载文件
     *
     * @param fileId 文件ID
     * @return 返回文件资源
     */
    String downloadFile(Long fileId);
    
    /**
     * 分页查询系统书籍
     *
     * @param pageRequest 分页请求参数
     * @return 分页结果，包含书籍信息列表和分页信息
     */
    PageResultDTO<BookInfoDTO> getSystemBooks(PageRequestDTO pageRequest);

}

