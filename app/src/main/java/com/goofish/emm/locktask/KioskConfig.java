package com.goofish.emm.locktask;

import com.goofish.emm.tutu.TutuUtil;

/**
 * Kiosk 模式集中配置类
 * 所有管控开关在此统一管理，无需持久化，修改后重新编译即可生效
 */
public final class KioskConfig {

    private KioskConfig() {
        // 不允许实例化
    }

    // ======================== LockTask 相关 ========================

    /**
     * 是否启用 LockTask 模式
     * - true:  进入 Kiosk 时会调用 startLockTask()，设备被锁定在白名单应用中
     * - false: 不进入 LockTask，避免蓝牙配对弹窗等系统对话框被拦截
     * 默认 false
     */
    public static final boolean LOCK_TASK_ENABLED = false;

    /**
     * LockTask 模式下是否启用 HOME 键
     * 仅在 LOCK_TASK_ENABLED = true 时有效
     */
    public static final boolean LOCK_TASK_ENABLE_HOME = true;

    /**
     * LockTask 模式下是否启用通知
     * 仅在 LOCK_TASK_ENABLED = true 时有效
     */
    public static final boolean LOCK_TASK_ENABLE_NOTIFICATIONS = false;

    // ======================== 用户限制 ========================

    /**
     * 是否禁止安装应用
     * 自升级时会临时解除此限制，安装完成后自动恢复
     */
    public static final boolean DISALLOW_INSTALL = true;

    /** 是否禁止卸载应用 */
    public static final boolean DISALLOW_UNINSTALL = true;

    /** 是否禁止恢复出厂设置 */
    public static final boolean DISALLOW_FACTORY_RESET = true;

    /** 是否禁止安全模式启动 */
    public static final boolean DISALLOW_SAFE_BOOT = true;

    /** 是否禁止添加用户 */
    public static final boolean DISALLOW_ADD_USER = true;

    /** 是否禁止挂载外部存储 */
    public static final boolean DISALLOW_MOUNT_PHYSICAL_MEDIA = true;

    // ======================== 悬浮球 ========================

    /**
     * 是否启用悬浮球
     * - true:  在 Kiosk 桌面显示悬浮球（点击可跳转管理页面等）
     * - false: 不显示悬浮球
     * 默认 false
     */
    public static final boolean FLOATING_BUTTON_ENABLED = false;

    // ======================== 应用冻结（Suspend） ========================

    /**
     * 是否启用应用冻结（suspend 黑名单应用）
     */
    public static final boolean SUSPEND_BLACKLIST_ENABLED = true;

    /**
     * 需要被冻结的黑名单应用包名列表
     * 这些应用会被 setPackagesSuspended() 冻结，用户无法打开
     */
    public static final String[] BLACKLIST_APPS = {
            // 示例：
            // "com.example.unwanted.app",
            "com.lenovo.leos.appstore.pad",
            "com.lenovo.browser.hd",
            "com.microsoft.office.officehub",
            "com.microsoft.office.onenote",
            "com.android.fmradio",
            "cn.wps.moffice_eng",
            "com.android.email",
            "com.android.calculator2",
            "com.factory.mmigroup",
            "com.tblenovo.soundrecorder",
            "com.android.calendar",
            "com.android.deskclock",
            "com.lenovo.styluspen",
            "com.android.contacts",
            "com.android.gallery3d",
            "com.google.android.apps.nbu.files",
            "com.mediatek.camera",
            "com.android.music",
            "com.dolby.daxappui",
    };

    // ======================== 桌面应用 ========================

    /**
     * 在 Kiosk 桌面上显示的应用（白名单）
     */
    public static final String[] DESKTOP_APPS = {
            TutuUtil.TUTU_PKG,
            "com.tencent.wemeet.app",
    };

    /**
     * 无需在桌面显示但需要加入 LockTask 白名单的系统应用
     * 仅在 LOCK_TASK_ENABLED = true 时使用
     */
    public static final String[] LOCK_TASK_SYSTEM_PACKAGES = {
            "com.android.packageinstaller",
            "com.android.settings",
            "com.android.systemui",
            "com.android.bluetooth",
    };
}

