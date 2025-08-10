package com.divination.liuyao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.divination.liuyao.pojo.entity.FileInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 文件信息数据访问层接口
 * 继承BaseMapper获得通用的CRUD方法
 */
@Mapper
public interface FileInfoMapper extends BaseMapper<FileInfo> {
    
    /**
     * 根据上传者用户ID查询文件列表
     * @param uploaderUserId 上传者用户ID
     * @return 文件列表
     */
    List<FileInfo> selectByUploaderUserId(@Param("uploaderUserId") Long uploaderUserId);
    
    /**
     * 根据文件类型查询文件列表
     * @param type 文件类型
     * @return 文件列表
     */
    List<FileInfo> selectByType(@Param("type") String type);
    
    /**
     * 根据文件格式查询文件列表
     * @param fileFormat 文件格式
     * @return 文件列表
     */
    List<FileInfo> selectByFileFormat(@Param("fileFormat") String fileFormat);
    
    /**
     * 根据文件名模糊查询
     * @param fileName 文件名（支持模糊查询）
     * @return 文件列表
     */
    List<FileInfo> selectByFileNameLike(@Param("fileName") String fileName);
    
    /**
     * 根据作者查询文件列表
     * @param author 作者
     * @return 文件列表
     */
    List<FileInfo> selectByAuthor(@Param("author") String author);
    
    /**
     * 根据条件查询文件列表
     * @param fileInfo 查询条件
     * @return 文件列表
     */
    List<FileInfo> selectByCondition(FileInfo fileInfo);
    
    /**
     * 根据条件统计文件数量
     * @param fileInfo 查询条件
     * @return 文件数量
     */
    long countByCondition(FileInfo fileInfo);
    
    /**
     * 根据文件大小范围查询
     * @param minSize 最小文件大小（字节）
     * @param maxSize 最大文件大小（字节）
     * @return 文件列表
     */
    List<FileInfo> selectByFileSizeRange(@Param("minSize") Long minSize, @Param("maxSize") Long maxSize);
    
    /**
     * 根据创建时间范围查询
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 文件列表
     */
    List<FileInfo> selectByCreateTimeRange(@Param("startTime") String startTime, @Param("endTime") String endTime);
    
    /**
     * 根据文件大小排序查询
     * @param orderBy 排序方式：asc-升序，desc-降序
     * @param limit 限制数量
     * @return 文件列表
     */
    List<FileInfo> selectByFileSizeOrder(@Param("orderBy") String orderBy, @Param("limit") int limit);
    
    /**
     * 根据创建时间排序查询
     * @param orderBy 排序方式：asc-升序，desc-降序
     * @param limit 限制数量
     * @return 文件列表
     */
    List<FileInfo> selectByCreateTimeOrder(@Param("orderBy") String orderBy, @Param("limit") int limit);
    
    /**
     * 统计各文件格式的数量
     * @return 格式统计列表
     */
    List<FileInfo> countByFileFormat();
    
    /**
     * 统计各文件类型的数量
     * @return 类型统计列表
     */
    List<FileInfo> countByFileType();
    
    /**
     * 统计用户上传文件数量
     * @param uploaderUserId 上传者用户ID
     * @return 文件数量
     */
    long countByUploaderUserId(@Param("uploaderUserId") Long uploaderUserId);
    
    /**
     * 查询用户最近上传的文件
     * @param uploaderUserId 上传者用户ID
     * @param limit 限制数量
     * @return 文件列表
     */
    List<FileInfo> selectRecentByUploaderUserId(@Param("uploaderUserId") Long uploaderUserId, @Param("limit") int limit);
    
    /**
     * 更新文件名称
     * @param id 文件ID
     * @param fileName 新文件名
     * @return 影响行数
     */
    int updateFileName(@Param("id") Long id, @Param("fileName") String fileName);
    
    /**
     * 更新文件类型
     * @param id 文件ID
     * @param type 新文件类型
     * @return 影响行数
     */
    int updateFileType(@Param("id") Long id, @Param("type") String type);
    
    /**
     * 更新作者信息
     * @param id 文件ID
     * @param author 新作者
     * @return 影响行数
     */
    int updateAuthor(@Param("id") Long id, @Param("author") String author);
    
    /**
     * 恢复已删除的文件
     * @param id 文件ID
     * @return 影响行数
     */
    int restoreById(@Param("id") Long id);
    
    /**
     * 查询已删除的文件列表
     * @return 已删除的文件列表
     */
    List<FileInfo> selectDeleted();
}
