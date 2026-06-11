package com.xxl.job.admin.core.sync;

public enum JobSyncMode {
    CREATE_ONLY,
    CREATE_UPDATE,
    DISABLED;

    public static JobSyncMode match(String value) {
        if (value == null || value.trim().isEmpty()) {
            return CREATE_ONLY;
        }
        for (JobSyncMode item : values()) {
            if (item.name().equalsIgnoreCase(value.trim())) {
                return item;
            }
        }
        return CREATE_ONLY;
    }
}
