package com.reggie.test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.test.context.MergedContextConfiguration;

/**
 * 测试库自动重建的接入点：在 Spring 上下文刷新（DataSource 创建）之前执行一次
 * {@link TestDbProvisioner#provision()}。
 *
 * <p>注册方式：src/test/resources/META-INF/spring.factories →
 * {@code org.springframework.test.context.ContextCustomizerFactory}。
 * ContextCustomizer 参与上下文缓存 key，这里用无状态单例（equals/hashCode 同一实例），
 * 保证同缓存 key 的上下文不会因 customizer 不同而反复创建；
 * 每个 JVM 只真正重建一次（{@link AtomicBoolean} 守卫 + 结构指纹快速跳过）。</p>
 *
 * @author reggie
 * @since 2026-09-30
 */
public class TestDbProvisionCustomizerFactory implements ContextCustomizerFactory {

    @Override
    public ContextCustomizer createContextCustomizer(Class<?> testClass,
            List<ContextConfigurationAttributes> configAttributes) {
        return TestDbProvisionCustomizer.INSTANCE;
    }

    /** 无状态单例：equals/hashCode 恒等，避免污染 Spring 测试上下文缓存 key */
    public static final class TestDbProvisionCustomizer implements ContextCustomizer {

        static final TestDbProvisionCustomizer INSTANCE = new TestDbProvisionCustomizer();

        private static final AtomicBoolean DONE = new AtomicBoolean(false);

        private TestDbProvisionCustomizer() {
        }

        @Override
        public void customizeContext(ConfigurableApplicationContext context,
                MergedContextConfiguration mergedConfig) {
            if (DONE.compareAndSet(false, true)) {
                TestDbProvisioner.provision();
            }
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof TestDbProvisionCustomizer;
        }

        @Override
        public int hashCode() {
            return TestDbProvisionCustomizer.class.hashCode();
        }
    }
}
