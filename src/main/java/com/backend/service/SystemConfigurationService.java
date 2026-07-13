package com.backend.service;

import com.backend.repository.SystemSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SystemConfigurationService {
    private final SystemSettingRepository settingRepository;

    public boolean registrationEnabled() { return booleanValue("registrationEnabled", true); }
    public boolean maintenanceMode() { return booleanValue("maintenanceMode", false); }

    public long sessionTimeoutMillis() {
        String value = settingRepository.findBySettingKey("sessionTimeoutMinutes")
                .map(setting -> setting.getSettingValue()).orElse("120");
        try { return Math.max(15, Math.min(1440, Long.parseLong(value))) * 60_000L; }
        catch (NumberFormatException ignored) { return 120 * 60_000L; }
    }

    private boolean booleanValue(String key, boolean fallback) {
        return settingRepository.findBySettingKey(key)
                .map(setting -> Boolean.parseBoolean(setting.getSettingValue())).orElse(fallback);
    }
}
