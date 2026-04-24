package io.vanguard.testops.sdk.constants;

import org.apache.commons.lang3.StringUtils;

public enum StorageType {

    MINIO, OSS, GIT, LOCAL;

    public static boolean isGit(String storage) {
        return StringUtils.equalsIgnoreCase(GIT.name(), storage);
    }
}
