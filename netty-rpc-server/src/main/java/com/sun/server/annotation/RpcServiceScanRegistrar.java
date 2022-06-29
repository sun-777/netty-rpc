package com.sun.server.annotation;

import com.sun.common.annotation.RpcService;
import com.sun.common.util.StringUtil;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @description:
 * @author: Sun Xiaodong
 */
public class RpcServiceScanRegistrar implements ImportBeanDefinitionRegistrar, BeanClassLoaderAware {
    private ClassLoader classLoader;

    @Override
    public void registerBeanDefinitions(AnnotationMetadata annotationMetadata, BeanDefinitionRegistry registry) {
        AnnotationAttributes attributes = AnnotationAttributes.fromMap(annotationMetadata.getAnnotationAttributes(RpcServiceScan.class.getName()));

        if (Objects.nonNull(attributes)) {
            Set<String> packages = getScanningPackages(attributes);
            if (packages.isEmpty()) {
                packages.add(getDefaultBasePackage(annotationMetadata));
            }
            RpcServiceScanner scanner = new RpcServiceScanner(registry, classLoader);
            scanner.setBeanNameGenerator(AnnotationBeanNameGenerator.INSTANCE);
            scanner.scan(packages.toArray(/*String[]::new*/new String[0]));
        }
    }


    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }


    private Set<String> getScanningPackages(AnnotationAttributes attributes) {
        Set<String> packages = new HashSet<>();
        if (null != attributes) {
            addPackages(packages, Arrays.stream(attributes.getStringArray("value")).filter(StringUtil::hasText).collect(Collectors.toSet()));
            addPackages(packages, Arrays.stream(attributes.getStringArray("basePackages")).filter(StringUtil::hasText).collect(Collectors.toSet()));
            addClasses(packages, Arrays.stream(attributes.getClassArray("basePackageClasses")).map(ClassUtils::getPackageName).collect(Collectors.toSet()));
        }
        return packages;
    }

    private static void addPackages(Set<String> packages, Set<String> values) {
        if (Objects.nonNull(packages) && Objects.nonNull(values) && !values.isEmpty()) {
            Collections.addAll(packages, values.toArray(new String[0]));
        }
    }

    private static void addClasses(Set<String> packages, Set<String> values) {
        if (Objects.nonNull(packages) && Objects.nonNull(values) && !values.isEmpty()) {
            values.forEach(val -> packages.add(ClassUtils.getPackageName(val)));
        }
    }

    private static String getDefaultBasePackage(AnnotationMetadata annotationMetadata) {
        return ClassUtils.getPackageName(annotationMetadata.getClassName());
    }


    /**
     * 自定义的包扫描器，过滤所有含有自定义注解@RpcService的类
     */
    static class RpcServiceScanner extends ClassPathBeanDefinitionScanner {
        private final ClassLoader classLoader;

        public RpcServiceScanner(BeanDefinitionRegistry registry, ClassLoader classLoader) {
            super(registry, false);
            this.classLoader = classLoader;
            registerFilter();
        }


        private void registerFilter() {
            //添加自定义的过滤条件：有@RpcService注解的类才会被扫描
            addIncludeFilter(new AnnotationTypeFilter(RpcService.class));
        }

        @Override
        protected Set<BeanDefinitionHolder> doScan(String... basePackages) {
            return super.doScan(basePackages);
        }

        /**
         * 重写候选判断逻辑，选出带有注解的接口
         */
        @Override
        protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
            final AnnotationMetadata metadata = beanDefinition.getMetadata();
            // AnnotationMetadata::isConcrete：是否允许创建（不是接口且不是抽象类）
            if (metadata.isConcrete() && metadata.isIndependent()) {
                try {
                    Class<?> target = ClassUtils.forName(metadata.getClassName(), classLoader);
                    return !target.isAnnotation();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
            return false;
        }
    }
}
