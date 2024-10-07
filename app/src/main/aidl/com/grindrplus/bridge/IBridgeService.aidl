package com.grindrplus.bridge;

interface IBridgeService {
    String getTranslation(String locale);
    List<String> getAvailableTranslations();

    boolean isHookEnabled(String name);
    void setHookState(String name, boolean enabled);
    boolean addHook(String name, String description, boolean enabled);
}