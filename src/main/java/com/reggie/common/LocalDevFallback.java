package com.reggie.common;

import org.springframework.context.ApplicationContext;

/**
 * 凭据主密钥「本地开发兜底」判定工具。
 * <p>
 * 背景：Platform/Payment 凭据加密器的内置兜底密钥随仓库公开，环境变量主密钥缺失时
 * 静默降级使用它等同于明文存储（审查报告 P0 问题 2）。统一口径：
 * <ul>
 *   <li>active profile 含 dev / test / local（或未启动 Spring 容器的纯单元测试）
 *       → 允许回退到显式命名的 DEV_FALLBACK 密钥，并显著告警；</li>
 *   <li>其余环境（prod 等）→ 启动检查与运行期取钥均 fail-fast，抛 IllegalStateException。</li>
 * </ul>
 * </p>
 *
 * @author reggie
 * @since 2026-10-02
 */
public final class LocalDevFallback {

    /** 允许使用内置 DEV_FALLBACK 密钥的 profile（小写比较） */
    private static final String[] DEV_LIKE_PROFILES = {"dev", "test", "local"};

    private LocalDevFallback() {
        // 工具类不允许实例化
    }

    /**
     * 当前运行环境是否允许回退内置兜底密钥。
     * <p>Spring 容器尚未初始化（纯单元测试 / 独立脚本）时视为本地开发环境。</p>
     *
     * @return active profile 含 dev/test/local 或无容器时返回 true
     */
    public static boolean isDevLike() {
        ApplicationContext ctx = ApplicationContextProvider.getApplicationContext();
        if (ctx == null) {
            return true;
        }
        return isDevLikeProfiles(ctx.getEnvironment().getActiveProfiles());
    }

    /**
     * 按给定 active profiles 判断是否允许回退内置兜底密钥。
     *
     * @param activeProfiles {@code Environment#getActiveProfiles()} 返回值，允许为 null
     * @return 含 dev/test/local 时返回 true；未配置任何 profile 视为生产（保守，返回 false）
     */
    public static boolean isDevLikeProfiles(String[] activeProfiles) {
        if (activeProfiles == null || activeProfiles.length == 0) {
            return false;
        }
        for (String profile : activeProfiles) {
            for (String devLike : DEV_LIKE_PROFILES) {
                if (devLike.equalsIgnoreCase(profile)) {
                    return true;
                }
            }
        }
        return false;
    }
}
