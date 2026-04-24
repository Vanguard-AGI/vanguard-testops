package io.vanguard.testops.sdk.file;

import io.vanguard.testops.sdk.constants.StorageType;
import io.vanguard.testops.sdk.util.CommonBeanFactory;

import java.util.HashMap;
import java.util.Map;

public class FileCenter {
    private FileCenter() {
    }

    public static FileRepository getRepository(StorageType storageType) {
        return switch (storageType) {
            case MINIO -> CommonBeanFactory.getBean(OssRepository.class);
            case OSS -> CommonBeanFactory.getBean(OssRepository.class);
            case LOCAL -> CommonBeanFactory.getBean(LocalFileRepository.class);
            case GIT -> CommonBeanFactory.getBean(GitRepository.class);
            default -> getDefaultRepository();
        };
    }

    public static FileRepository getRepository(String storage) {
        Map<String, StorageType> storageTypeMap = new HashMap<>() {{
            put(StorageType.MINIO.name(), StorageType.MINIO);
            put(StorageType.OSS.name(), StorageType.OSS);
            put(StorageType.LOCAL.name(), StorageType.LOCAL);
            put(StorageType.GIT.name(), StorageType.GIT);
        }};

        return getRepository(storageTypeMap.get(storage.toUpperCase()));
    }

    public static FileRepository getDefaultRepository() {
        return CommonBeanFactory.getBean(OssRepository.class);
    }
}
