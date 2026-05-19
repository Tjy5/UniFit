package com.suios.admin.upload.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "upload")
public class UploadProperties {

    private String path;
    private String urlPrefix;
    private String legacyPath;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getUrlPrefix() {
        return urlPrefix;
    }

    public void setUrlPrefix(String urlPrefix) {
        this.urlPrefix = urlPrefix;
    }

    public String getLegacyPath() {
        return legacyPath;
    }

    public void setLegacyPath(String legacyPath) {
        this.legacyPath = legacyPath;
    }
}
