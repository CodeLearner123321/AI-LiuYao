package com.divination.liuyao.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.divination.liuyao.exception.YsyjException;
import com.divination.liuyao.mapper.FileInfoMapper;
import com.divination.liuyao.pojo.dto.BookInfoDTO;
import com.divination.liuyao.pojo.dto.PageRequestDTO;
import com.divination.liuyao.pojo.dto.PageResultDTO;
import com.divination.liuyao.pojo.enums.DeleteType;
import com.divination.liuyao.pojo.enums.FileFormatEnum;
import com.divination.liuyao.pojo.entity.FileInfo;
import com.divination.liuyao.service.FileService;
import com.divination.liuyao.util.OSSUtil;
import com.divination.liuyao.util.UserContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FileServiceImpl implements FileService {
    private static final Integer MAX_FILE_SIZE = 100 * 1024 * 1024; // 100MB
    private static final String BOOK = "book"; // 100MB
    private static final String IMAGE = "image"; // 100MB
    private static final String FILE_NAME_PREFIX = "book_";

    @Autowired
    private FileInfoMapper fileInfoMapper;


    /**
     * 校验文件合法性（空、大小、名称、格式）
     * @param file MultipartFile
     * @return 校验通过返回格式字符串，否则返回null
     */
    private Boolean validateFile(MultipartFile file, FileInfo fileInfo) {
        if(file == null || fileInfo == null
                || (!Objects.equals(fileInfo.getType(), BOOK) && !Objects.equals(fileInfo.getType(), IMAGE))) {
            throw new YsyjException("上传文件信息有误");
        }
        if (file == null || file.isEmpty()) {
            throw new YsyjException("上传文件为空");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new YsyjException("文件大小超过限制，最大允许100MB");
        }
        String fileName = file.getOriginalFilename();
        if (StringUtils.isBlank(fileName)) {
            throw new YsyjException("文件名不允许");
        }
        String fileFormat = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        if (!FileFormatEnum.isSupported(fileFormat)) {
            String supportedFormats = Arrays.stream(FileFormatEnum.values()).map(Enum::name).collect(Collectors.joining(", "));
            throw new YsyjException("文件格式不支持，支持格式：" + supportedFormats);
        }
        return true;
    }

    @Override
    public Long uploadFile(MultipartFile file, FileInfo fileInfo) {
        validateFile(file, fileInfo);

        String fileName = file.getOriginalFilename();
        // 构造OSS路径
        String ossPath = fileInfo.getType() + "/" + UserContextHolder.getUsername();
        String ossUrl;
        try {
            ossUrl = OSSUtil.uploadFile(ossPath, fileName, file.getInputStream());
        } catch (Exception e) {
            log.error("文件上传OSS失败", e);
            return null;
        }
        // 填充文件信息
        fileInfo.setFileName(fileInfo.isBook() ? fileName : "");
        fileInfo.setImageName(fileInfo.isImage() ? fileName : "");
        fileInfo.setFileSize(file.getSize());
        fileInfo.setFileFormat(fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase());
        fileInfo.setUpdateTime(new Date());
        fileInfo.setDeleted(DeleteType.NOT_DELETED.getCode());
        fileInfo.setUploaderUserId(UserContextHolder.getUserId());

        if(fileInfo.getId() == null){
            fileInfo.setCreateTime(new Date());
            fileInfoMapper.insert(fileInfo);
        }else {
            fileInfoMapper.updateById(fileInfo);
        }
        
        return fileInfo.getId();
    }



    @Override
    public ResponseEntity<Resource> downloadFile(Long fileId) {
        return null;
    }
    
    @Override
    public PageResultDTO<BookInfoDTO> getSystemBooks(PageRequestDTO pageRequest) {
        // 创建分页对象
        Page<FileInfo> page = new Page<>(pageRequest.getCurrent(), pageRequest.getSize());
        
        // 构建查询条件：type为book且未删除的记录
        LambdaQueryWrapper<FileInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FileInfo::getDeleted, DeleteType.NOT_DELETED.getCode())
                    .isNotNull(FileInfo::getFileName)
                    .orderByDesc(FileInfo::getCreateTime);
        
        // 执行分页查询
        Page<FileInfo> resultPage = fileInfoMapper.selectPage(page, queryWrapper);
        
        // 将FileInfo转换为BookInfoDTO
        List<BookInfoDTO> books = new ArrayList<>();
        for (FileInfo fileInfo : resultPage.getRecords()) {
            BookInfoDTO book = new BookInfoDTO();
            book.setId(fileInfo.getId());
            book.setTitle(fileInfo.getFileName());
            book.setDescription("常用六爻术语解释与对照");
            book.setSize(formatFileSize(fileInfo.getFileSize()));
            book.setFormat(fileInfo.getFileFormat());
            books.add(book);
        }
        
        // 构建分页结果DTO
        PageResultDTO<BookInfoDTO> pageResult = new PageResultDTO<>();
        pageResult.setCurrent(resultPage.getCurrent());
        pageResult.setSize(resultPage.getSize());
        pageResult.setTotal(resultPage.getTotal());
        pageResult.setPages(resultPage.getPages());
        pageResult.setRecords(books);
        pageResult.setHasPrevious(resultPage.hasPrevious());
        pageResult.setHasNext(resultPage.hasNext());
        
        return pageResult;
    }
    
    /**
     * 格式化文件大小
     */
    private String formatFileSize(long size) {
        if (size < 1024) {
            return size + "B";
        } else if (size < 1024 * 1024) {
            return String.format("%.1fKB", size / 1024.0);
        } else {
            return String.format("%.1fMB", size / (1024.0 * 1024));
        }
    }

}
