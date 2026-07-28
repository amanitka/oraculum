package com.oraculum.ui.views.components.renderer;

public class RendererUtil {
    public static String formatKeyTitle(String key) {
        if (key == null || key.isEmpty()) return key;
        String withSpaces = key.replaceAll("([a-z])([A-Z]+)", "$1 $2").replace("_", " ").toLowerCase();
        return withSpaces.substring(0, 1).toUpperCase() + withSpaces.substring(1);
    }
}
