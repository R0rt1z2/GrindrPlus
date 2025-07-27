package com.grindrplus.bridge;

interface IBridgeService {
    String getConfig();
    void setConfig(String config);
    void log(String level, String source, String message, String hookName);
    void writeRawLog(String content);
    void clearLogs();
    boolean shouldRegenAndroidId(String packageName);
    String getForcedLocation(String packageName);
    void deleteForcedLocation(String packageName);
    void logBlockEvent(String profileId, String displayName, boolean isBlock, String packageName);
    String getBlockEvents();
    boolean isRooted();
    boolean isLSPosed();
    void clearBlockEvents();
    void sendNotification(String title, String message, int notificationId, String channelId, String channelName, String channelDescription);
    void sendNotificationWithActions(String title, String message, int notificationId, String channelId,
                                    String channelName, String channelDescription,
                                    in String[] actionLabels, in String[] actionIntents, in String[] actionData);

}