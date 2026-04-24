package io.vanguard.testops.sdk.file;

import io.vanguard.testops.sdk.exception.MSException;
import io.vanguard.testops.sdk.util.LogUtils;
import io.minio.*;
import io.minio.messages.Item;
import com.aliyun.oss.OSS;
import com.aliyun.oss.model.*;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class OssRepository implements FileRepository {

    private MinioClient client;
    private OSS aliyunClient;
    private String bucketName = "spotter-aegis-test";

    private static final int BUFFER_SIZE = 8192;
    public static final String DEFAULT_BUCKET = "spotter-aegis-test";
    public static final String ENDPOINT = "endpoint";
    public static final String ACCESS_KEY = "accessKey";
    public static final String SECRET_KEY = "secretKey";
    public static final String BUCKET_NAME = "bucketName";

    public void init(MinioClient client) {
        if (this.client == null) {
            this.client = client;
        }
    }

    public void init(MinioClient client, String bucketName) {
        if (this.client == null) {
            this.client = client;
        }
        if (StringUtils.isNotBlank(bucketName)) {
            this.bucketName = bucketName;
        }
    }

    public void init(OSS client) {
        if (this.aliyunClient == null) {
            this.aliyunClient = client;
        }
    }

    public void init(OSS client, String bucketName) {
        if (this.aliyunClient == null) {
            this.aliyunClient = client;
        }
        if (StringUtils.isNotBlank(bucketName)) {
            this.bucketName = bucketName;
        }
    }

    public void init(Map<String, Object> ossConfig) {
        if (ossConfig == null || ossConfig.isEmpty()) {
            LogUtils.info("OSS初始化失败，参数[ossConfig]为空");
            return;
        }

        try {
            Object serverUrl = ossConfig.get(ENDPOINT);
            if (ObjectUtils.isNotEmpty(serverUrl)) {
                String endpoint = ossConfig.get(ENDPOINT).toString();
                Object bucketNameConfig = ossConfig.get(BUCKET_NAME);
                if (ObjectUtils.isNotEmpty(bucketNameConfig)) {
                    this.bucketName = bucketNameConfig.toString();
                }

                // 简单以域名包含 aliyuncs.com 判断为阿里云
                if (StringUtils.containsIgnoreCase(endpoint, "aliyuncs.com")) {
                    this.aliyunClient = new com.aliyun.oss.OSSClientBuilder().build(
                            endpoint,
                            ossConfig.get(ACCESS_KEY).toString(),
                            ossConfig.get(SECRET_KEY).toString());
                    boolean exist = aliyunClient.doesBucketExist(this.bucketName);
                    if (!exist) {
                        LogUtils.info("存储桶 {} 不存在，正在创建...", this.bucketName);
                        aliyunClient.createBucket(this.bucketName);
                        LogUtils.info("存储桶 {} 创建成功", this.bucketName);
                    }
                } else {
                    client = MinioClient.builder()
                            .endpoint(endpoint)
                            .credentials(ossConfig.get(ACCESS_KEY).toString(), ossConfig.get(SECRET_KEY).toString())
                            .build();
                    boolean exist = client.bucketExists(BucketExistsArgs.builder().bucket(this.bucketName).build());
                    if (!exist) {
                        LogUtils.info("存储桶 {} 不存在，正在创建...", this.bucketName);
                        client.makeBucket(MakeBucketArgs.builder().bucket(this.bucketName).build());
                        LogUtils.info("存储桶 {} 创建成功", this.bucketName);
                    }
                }
            }
        } catch (Exception e) {
            LogUtils.error("OSS客户端初始化失败！", e);
        }
    }

    private String getPath(FileRequest request) {
        String folder = request.getFolder();
        if (!StringUtils.startsWithAny(folder, "system", "project", "organization")) {
            throw new MSException("folder.error");
        }
        return StringUtils.join(folder, "/", request.getFileName());
    }

    @Override
    public String saveFile(MultipartFile file, FileRequest request) throws Exception {
        String filePath = getPath(request);
        if (aliyunClient != null) {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            PutObjectRequest put = new PutObjectRequest(this.bucketName, filePath, file.getInputStream(), metadata);
            aliyunClient.putObject(put);
        } else {
            client.putObject(PutObjectArgs.builder()
                    .bucket(this.bucketName)
                    .object(filePath)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .build());
        }
        return filePath;
    }

    @Override
    public String saveFile(byte[] bytes, FileRequest request) throws Exception {
        String filePath = getPath(request);
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes)) {
            if (aliyunClient != null) {
                ObjectMetadata metadata = new ObjectMetadata();
                metadata.setContentLength(bytes.length);
                PutObjectRequest put = new PutObjectRequest(this.bucketName, filePath, inputStream, metadata);
                aliyunClient.putObject(put);
            } else {
                client.putObject(PutObjectArgs.builder()
                        .bucket(this.bucketName)
                        .object(filePath)
                        .stream(inputStream, bytes.length, -1)
                        .build());
            }
        }
        return request.getFileName();
    }

    @Override
    public String saveFile(InputStream inputStream, FileRequest request) throws Exception {
        String filePath = getPath(request);

        if (aliyunClient != null) {
            // 读取到内存确定长度后再上传（简化实现）
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }
            byte[] bytes = bos.toByteArray();
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(bytes.length);
            try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
                PutObjectRequest put = new PutObjectRequest(this.bucketName, filePath, bais, metadata);
                aliyunClient.putObject(put);
            }
        } else {
            client.putObject(PutObjectArgs.builder()
                    .bucket(this.bucketName)
                    .object(filePath)
                    .stream(inputStream, -1, 5242880)
                    .build());
        }
        return filePath;
    }

    @Override
    public void delete(FileRequest request) throws Exception {
        String filePath = getPath(request);
        removeObject(this.bucketName, filePath);
    }

    @Override
    public void deleteFolder(FileRequest request) throws Exception {
        String filePath = getPath(request);
        removeObjects(this.bucketName, filePath);
    }

    @Override
    public List<String> getFolderFileNames(FileRequest request) throws Exception {
        return listObjects(this.bucketName, getPath(request));
    }

    @Override
    public void copyFile(FileCopyRequest request) throws Exception {
        String sourcePath = StringUtils.join(request.getCopyFolder(), "/", request.getCopyfileName());
        String targetPath = getPath(request);
        
        if (aliyunClient != null) {
            // 使用阿里云OSS客户端
            aliyunClient.copyObject(this.bucketName, sourcePath, this.bucketName, targetPath);
        } else {
            // 使用MinIO客户端（兼容模式）
            client.copyObject(CopyObjectArgs.builder()
                    .bucket(this.bucketName)
                    .object(targetPath)
                    .source(CopySource.builder()
                            .bucket(this.bucketName)
                            .object(sourcePath)
                            .build())
                    .build());
        }
    }

    private void removeObject(String bucketName, String objectName) throws Exception {
        if (aliyunClient != null) {
            aliyunClient.deleteObject(bucketName, objectName);
        } else {
            client.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build());
        }
    }

    public void removeObjects(String bucketName, String objectName) throws Exception {
        List<String> objects = listObjects(bucketName, objectName);
        for (String object : objects) {
            removeObject(bucketName, object);
        }
    }

    public List<String> listObjects(String bucketName, String objectName) throws Exception {
        List<String> list = new ArrayList<>(12);
        if (aliyunClient != null) {
            ListObjectsRequest req = new ListObjectsRequest(bucketName, objectName, null, null, null);
            ObjectListing listing = aliyunClient.listObjects(req);
            listing.getObjectSummaries().forEach(s -> list.add(s.getKey()));
            // 递归子目录
            for (String prefix : listing.getCommonPrefixes()) {
                list.addAll(listObjects(bucketName, prefix));
            }
        } else {
            Iterable<Result<Item>> results = client.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix(objectName)
                            .build());
            for (Result<Item> result : results) {
                Item item = result.get();
                if (item.isDir()) {
                    List<String> files = listObjects(bucketName, item.objectName());
                    list.addAll(files);
                } else {
                    list.add(item.objectName());
                }
            }
        }
        return list;
    }

    @Override
    public byte[] getFile(FileRequest request) throws Exception {
        return getFileAsStream(request).readAllBytes();
    }

    @Override
    public void downloadFile(FileRequest request, String fullPath) throws Exception {
        String fileName = getPath(request);
        try (InputStream inputStream = (aliyunClient != null
                ? aliyunClient.getObject(this.bucketName, fileName).getObjectContent()
                : client.getObject(
                        GetObjectArgs.builder()
                                .bucket(this.bucketName)
                                .object(fileName)
                                .build()));
             BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(fullPath))) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
    }

    @Override
    public InputStream getFileAsStream(FileRequest request) throws Exception {
        String fileName = getPath(request);
        if (aliyunClient != null) {
            return aliyunClient.getObject(this.bucketName, fileName).getObjectContent();
        }
        return client.getObject(GetObjectArgs.builder()
                .bucket(this.bucketName)
                .object(fileName)
                .build());
    }

    @Override
    public long getFileSize(FileRequest request) throws Exception {
        String fileName = getPath(request);
        if (aliyunClient != null) {
            ObjectMetadata meta = aliyunClient.getObjectMetadata(this.bucketName, fileName);
            return meta.getContentLength();
        }
        return client.statObject(StatObjectArgs.builder()
                .bucket(this.bucketName)
                .object(fileName)
                .build()).size();
    }
}


