package com.grindrplus.bridge;

interface IBridgeService {
    String getTranslation(String locale);
    List<String> getAvailableTranslations();
}