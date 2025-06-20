package com.goofish.emm.locktask;

import android.graphics.drawable.Drawable;

/**
 * 应用信息类，用于存储应用的基本信息
 * 支持两种类型：外部应用和内部组件
 */
public class AppInfo {
    public enum ItemType {
        EXTERNAL_APP,    // 外部应用
        INTERNAL_COMPONENT  // 内部组件
    }

    private String packageName;
    private String appName;
    private Drawable appIcon;
    private ItemType itemType;
    private String componentName;  // 用于内部组件的完整组件名
    private String action;         // 用于内部组件的 action

    // 外部应用构造函数
    public AppInfo(String packageName, String appName, Drawable appIcon) {
        this.packageName = packageName;
        this.appName = appName;
        this.appIcon = appIcon;
        this.itemType = ItemType.EXTERNAL_APP;
    }

    // 内部组件构造函数
    public AppInfo(String componentName, String appName, Drawable appIcon, String action) {
        this.componentName = componentName;
        this.appName = appName;
        this.appIcon = appIcon;
        this.action = action;
        this.itemType = ItemType.INTERNAL_COMPONENT;
        // 从组件名中提取包名
        if (componentName != null && componentName.contains("/")) {
            this.packageName = componentName.split("/")[0];
        }
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public Drawable getAppIcon() {
        return appIcon;
    }

    public void setAppIcon(Drawable appIcon) {
        this.appIcon = appIcon;
    }

    public ItemType getItemType() {
        return itemType;
    }

    public void setItemType(ItemType itemType) {
        this.itemType = itemType;
    }

    public String getComponentName() {
        return componentName;
    }

    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    /**
     * 判断是否为外部应用
     */
    public boolean isExternalApp() {
        return itemType == ItemType.EXTERNAL_APP;
    }

    /**
     * 判断是否为内部组件
     */
    public boolean isInternalComponent() {
        return itemType == ItemType.INTERNAL_COMPONENT;
    }
}
