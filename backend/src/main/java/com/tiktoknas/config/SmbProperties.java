package com.tiktoknas.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.smb")
@Data
public class SmbProperties {
    private boolean enabled = false;
    private String host;
    private String share = "videos";
    private String domain = "";
    private String username;
    private String password;
    private String path = "";
}
