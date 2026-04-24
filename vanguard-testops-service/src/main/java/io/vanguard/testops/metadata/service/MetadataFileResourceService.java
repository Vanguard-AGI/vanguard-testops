package io.vanguard.testops.metadata.service;

import com.aliyun.oss.OSS;
import io.vanguard.testops.metadata.domain.MetadataFileResource;
import io.vanguard.testops.metadata.mapper.MetadataFileResourceMapper;
import io.vanguard.testops.sdk.constants.StorageType;
import io.vanguard.testops.sdk.exception.MSException;
import io.vanguard.testops.sdk.file.FileCenter;
import io.vanguard.testops.sdk.file.FileRequest;
import io.vanguard.testops.sdk.file.FileRepository;
import io.vanguard.testops.sdk.util.LogUtils;
import io.vanguard.testops.system.infrastructure.storage.config.OssProperties;
import io.vanguard.testops.system.uid.IDGenerator;
import jakarta.annotation.Resource;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 元数据文件资源服务
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class MetadataFileResourceService {

    @Resource
    private MetadataFileResourceMapper metadataFileResourceMapper;

    @Resource(name = "ossClient")
    private Object ossClient;

    @Resource
    private OssProperties ossProperties;

    /**
     * 上传文件
     * 
     * @param file 文件
     * @param projectId 项目ID
     * @param storageType 存储类型: LOCAL/MINIO/OSS/S3
     * @param category 分类: DATA(测试数据)/CERT(证书)/ATTACHMENT(附件)
     * @param userId 用户ID
     * @return 文件资源ID
     */
    public String upload(MultipartFile file, String projectId, String storageType, String category, String userId) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new MSException("文件不能为空");
        }

        // 计算文件校验和（MD5）
        byte[] fileBytes = file.getBytes();
        String checksum = DigestUtils.md5Hex(fileBytes);
        
        // 检查是否已存在相同文件（根据校验和）
        List<MetadataFileResource> existing = metadataFileResourceMapper.selectByChecksum(checksum);
        if (!existing.isEmpty()) {
            // 如果已存在相同文件，返回已存在的文件ID
            MetadataFileResource existingFile = existing.get(0);
            if (StringUtils.equals(existingFile.getProjectId(), projectId)) {
                return existingFile.getId();
            }
        }

        // 生成文件ID
        String fileId = IDGenerator.nextStr();
        
        // 生成文件存储名称：filename_fileid
        String originalFilename = file.getOriginalFilename();
        String baseFilename = originalFilename != null ? originalFilename : "file";
        // 移除扩展名（如果有）
        if (baseFilename.contains(".")) {
            baseFilename = baseFilename.substring(0, baseFilename.lastIndexOf("."));
        }
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String storageName = baseFilename + "_" + fileId + extension;

        // 构建文件存储路径（按日期维度：metadata/2025-03/文件名.jpg）
        String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        String folder = "metadata/" + dateDir;
        // 统一使用阿里云OSS存储
        String storage = StorageType.OSS.name();
        
        // 检查OSS客户端是否已初始化
        if (!(ossClient instanceof OSS)) {
            throw new MSException("OSS客户端未初始化");
        }
        
        // 上传文件到OSS（确保路径使用正斜杠）
        String filePath = folder + "/" + storageName;
        filePath = filePath.replace("\\", "/"); // 统一使用正斜杠
        
        LogUtils.info("准备上传文件到OSS - Bucket: " + ossProperties.getBucketName() + ", Path: " + filePath + ", Size: " + fileBytes.length + " bytes");
        
        try (java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(fileBytes)) {
            if (!putOssFile(filePath, bais)) {
                throw new MSException("上传文件到OSS失败: " + filePath);
            }
            LogUtils.info("文件上传到OSS成功 - Path: " + filePath);
        }

        // 保存文件资源记录
        long currentTime = System.currentTimeMillis();
        MetadataFileResource fileResource = new MetadataFileResource();
        fileResource.setId(fileId);
        fileResource.setProjectId(projectId);
        fileResource.setStorageName(storageName);
        fileResource.setStorageType(storage);
        fileResource.setPath(filePath);
        fileResource.setFileSize((long) fileBytes.length);
        fileResource.setExtension(extension);
        fileResource.setContentType(file.getContentType());
        fileResource.setChecksum(checksum);
        fileResource.setCategory(StringUtils.isNotBlank(category) ? category : "ATTACHMENT");
        fileResource.setCreateUser(userId);
        fileResource.setCreateTime(currentTime);
        // deleted_time 不设置，默认为 NULL（表示未删除）

        metadataFileResourceMapper.insert(fileResource);

        return fileResource.getId();
    }

    /**
     * 下载文件
     * 
     * @param fileResourceId 文件资源ID
     * @return 文件字节数组
     */
    public byte[] download(String fileResourceId) throws Exception {
        MetadataFileResource fileResource = get(fileResourceId);
        
        // 统一使用阿里云OSS存储
        if (!(ossClient instanceof OSS)) {
            throw new MSException("OSS客户端未初始化");
        }
        
        String filePath = fileResource.getPath();
        try (InputStream inputStream = getOssFile(filePath)) {
            if (inputStream == null) {
                throw new MSException("文件不存在: " + filePath);
            }
            return inputStream.readAllBytes();
        }
    }

    /**
     * 获取文件流
     * 
     * @param fileResourceId 文件资源ID
     * @return 文件输入流
     */
    public InputStream getFileAsStream(String fileResourceId) throws Exception {
        MetadataFileResource fileResource = get(fileResourceId);
        
        // 统一使用阿里云OSS存储
        if (!(ossClient instanceof OSS)) {
            throw new MSException("OSS客户端未初始化");
        }
        
        String filePath = fileResource.getPath();
        InputStream inputStream = getOssFile(filePath);
        if (inputStream == null) {
            throw new MSException("文件不存在: " + filePath);
        }
        return inputStream;
    }

    /**
     * 获取文件资源信息
     * 
     * @param fileResourceId 文件资源ID
     * @return 文件资源信息
     */
    public MetadataFileResource get(String fileResourceId) {
        MetadataFileResource fileResource = metadataFileResourceMapper.selectByIdWithTimestamp(fileResourceId);
        if (fileResource == null) {
            throw new MSException("文件资源不存在");
        }
        return fileResource;
    }

    /**
     * 上传文件流到OSS（参考 Python 代码的 put_oss_file）
     * 
     * @param filePath OSS文件路径
     * @param inputStream 文件输入流
     * @return 是否上传成功
     */
    public boolean putOssFile(String filePath, InputStream inputStream) {
        if (!(ossClient instanceof OSS)) {
            throw new MSException("当前不是阿里云OSS客户端");
        }
        OSS aliyunOss = (OSS) ossClient;
        String bucketName = ossProperties.getBucketName();
        
        // 确保路径格式正确（统一使用正斜杠，去除开头的斜杠）
        filePath = filePath.replace("\\", "/").replaceAll("^/+", "");
        
        LogUtils.info("开始上传到OSS - Bucket: " + bucketName + ", ObjectKey: " + filePath);
        
        try {
            // 检查桶是否存在
            if (!aliyunOss.doesBucketExist(bucketName)) {
                LogUtils.error("OSS存储桶不存在: " + bucketName);
                return false;
            }
            
            com.aliyun.oss.model.ObjectMetadata metadata = new com.aliyun.oss.model.ObjectMetadata();
            byte[] bytes = inputStream.readAllBytes();
            metadata.setContentLength(bytes.length);
            
            try (java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(bytes)) {
                com.aliyun.oss.model.PutObjectRequest put = new com.aliyun.oss.model.PutObjectRequest(
                    bucketName, filePath, bais, metadata);
                com.aliyun.oss.model.PutObjectResult result = aliyunOss.putObject(put);
                LogUtils.info("OSS上传成功 - Bucket: " + bucketName + ", ObjectKey: " + filePath + ", ETag: " + result.getETag());
            }
            return true;
        } catch (Exception e) {
            LogUtils.error("上传文件到OSS失败 - Bucket: " + bucketName + ", ObjectKey: " + filePath + ", Error: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * 从OSS下载文件流（参考 Python 代码的 get_oss_file）
     * 
     * @param filePath OSS文件路径
     * @return 文件输入流，如果文件不存在返回null
     */
    public InputStream getOssFile(String filePath) {
        if (!(ossClient instanceof OSS)) {
            throw new MSException("当前不是阿里云OSS客户端");
        }
        OSS aliyunOss = (OSS) ossClient;
        String bucketName = ossProperties.getBucketName();
        
        // 确保路径格式正确（统一使用正斜杠，去除开头的斜杠）
        filePath = filePath.replace("\\", "/").replaceAll("^/+", "");
        
        try {
            if (!aliyunOss.doesObjectExist(bucketName, filePath)) {
                LogUtils.warn("OSS文件不存在 - Bucket: " + bucketName + ", ObjectKey: " + filePath);
                return null;
            }
            LogUtils.info("从OSS获取文件 - Bucket: " + bucketName + ", ObjectKey: " + filePath);
            return aliyunOss.getObject(bucketName, filePath).getObjectContent();
        } catch (Exception e) {
            LogUtils.error("从OSS下载文件失败 - Bucket: " + bucketName + ", ObjectKey: " + filePath + ", Error: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * 删除OSS上的文件（参考 Python 代码的 delete_oss_file）
     * 
     * @param filePath OSS文件路径
     * @return 是否删除成功
     */
    public boolean deleteOssFile(String filePath) {
        if (!(ossClient instanceof OSS)) {
            throw new MSException("当前不是阿里云OSS客户端");
        }
        OSS aliyunOss = (OSS) ossClient;
        String bucketName = ossProperties.getBucketName();
        
        try {
            aliyunOss.deleteObject(bucketName, filePath);
            return true;
        } catch (Exception e) {
            LogUtils.error("删除OSS文件失败: " + filePath, e);
            return false;
        }
    }

    /**
     * 检查存储桶是否存在（参考 Python 代码的 get_bucket_info）
     * 
     * @param bucketName 存储桶名称，如果为空则使用配置的bucketName
     * @return 存储桶是否存在
     */
    public boolean checkBucketExists(String bucketName) {
        if (!(ossClient instanceof OSS)) {
            throw new MSException("当前不是阿里云OSS客户端");
        }
        OSS aliyunOss = (OSS) ossClient;
        String bucket = StringUtils.isNotBlank(bucketName) ? bucketName : ossProperties.getBucketName();
        
        try {
            return aliyunOss.doesBucketExist(bucket);
        } catch (Exception e) {
            LogUtils.error("检查存储桶是否存在失败: " + bucket, e);
            return false;
        }
    }

    /**
     * 递归搜索本地文件（参考 Python 代码的 recursive_search_files）
     * 
     * @param directory 目录路径
     * @return 文件列表
     */
    public List<File> recursiveSearchFiles(String directory) {
        List<File> fileList = new ArrayList<>();
        try {
            Path dirPath = Paths.get(directory);
            if (!Files.exists(dirPath) || !Files.isDirectory(dirPath)) {
                return fileList;
            }
            
            Files.walk(dirPath)
                .filter(Files::isRegularFile)
                .forEach(path -> fileList.add(path.toFile()));
        } catch (Exception e) {
            LogUtils.error("递归搜索本地文件失败: " + directory, e);
        }
        return fileList;
    }

    /**
     * 批量上传文件夹中的所有文件（参考 Python 代码的 process）
     * 
     * @param localDir 本地目录路径
     * @param ossPrefix OSS路径前缀
     * @return 上传成功的文件数量
     */
    public int batchUploadFolder(String localDir, String ossPrefix) {
        List<File> files = recursiveSearchFiles(localDir);
        int successCount = 0;
        
        for (File file : files) {
            try {
                String relativePath = Paths.get(localDir).relativize(Paths.get(file.getAbsolutePath())).toString();
                String ossPath = StringUtils.isNotBlank(ossPrefix) 
                    ? ossPrefix + "/" + relativePath.replace("\\", "/")
                    : relativePath.replace("\\", "/");
                
                try (FileInputStream fis = new FileInputStream(file)) {
                    if (putOssFile(ossPath, fis)) {
                        successCount++;
                        LogUtils.info("上传文件成功: " + file.getAbsolutePath() + " -> " + ossPath);
                    }
                }
            } catch (Exception e) {
                LogUtils.error("上传文件失败: " + file.getAbsolutePath(), e);
            }
        }
        
        return successCount;
    }

    /**
     * 下载单个文件到本地（参考 Python 代码的 download_one_file）
     * 
     * @param ossPath OSS文件路径
     * @param localPath 本地保存路径
     * @return 是否下载成功
     */
    public boolean downloadOneFile(String ossPath, String localPath) {
        try (InputStream inputStream = getOssFile(ossPath)) {
            if (inputStream == null) {
                LogUtils.error("OSS文件不存在: " + ossPath);
                return false;
            }
            
            File localFile = new File(localPath);
            File parentDir = localFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            
            try (FileOutputStream fos = new FileOutputStream(localFile)) {
                inputStream.transferTo(fos);
            }
            LogUtils.info("下载文件成功: " + ossPath + " -> " + localPath);
            return true;
        } catch (Exception e) {
            LogUtils.error("下载文件失败: " + ossPath + " -> " + localPath, e);
            return false;
        }
    }

    /**
     * 批量下载文件夹内容（参考 Python 代码的 download_many_file）
     * 
     * @param ossPrefix OSS路径前缀
     * @param localDir 本地保存目录
     * @return 下载成功的文件数量
     */
    public int batchDownloadFolder(String ossPrefix, String localDir) {
        if (!(ossClient instanceof OSS)) {
            throw new MSException("当前不是阿里云OSS客户端");
        }
        OSS aliyunOss = (OSS) ossClient;
        String bucketName = ossProperties.getBucketName();
        
        int successCount = 0;
        try {
            com.aliyun.oss.model.ListObjectsRequest request = new com.aliyun.oss.model.ListObjectsRequest();
            request.setBucketName(bucketName);
            request.setPrefix(ossPrefix);
            request.setMaxKeys(1000);
            
            com.aliyun.oss.model.ObjectListing listing = aliyunOss.listObjects(request);
            
            for (com.aliyun.oss.model.OSSObjectSummary objectSummary : listing.getObjectSummaries()) {
                String ossPath = objectSummary.getKey();
                if (ossPath.endsWith("/")) {
                    continue;
                }
                
                String relativePath = ossPath.substring(ossPrefix.length()).replaceAll("^/+", "");
                String localPath = Paths.get(localDir, relativePath).toString();
                
                if (downloadOneFile(ossPath, localPath)) {
                    successCount++;
                }
            }
        } catch (Exception e) {
            LogUtils.error("批量下载文件夹失败: " + ossPrefix, e);
        }
        
        return successCount;
    }
}
