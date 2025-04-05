package com.grindrplus.bridge;

interface IBridgeService {
    String getConfig();
    void setConfig(String config);
    void log(String level, String source, String message, String hookName);
    void writeRawLog(String content);
}