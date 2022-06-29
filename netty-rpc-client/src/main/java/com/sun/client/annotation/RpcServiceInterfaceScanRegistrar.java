package com.sun.client.annotation;

import com.sun.client.proxy.RpcFactoryBean;
import com.sun.common.annotation.RpcServiceInterface;
import com.sun.common.util.StringUtil;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
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
 * @description: 动态注册Bean
 * @author: Sun Xiaodong
 */
public class RpcServiceInterfaceScanRegistrar implements ImportBeanDefinitionRegistrar, BeanClassLoaderAware {
    private ClassLoader classLoader;

    @Override
    public void registerBeanDefinitions(AnnotationMetadata annotationMetadata, BeanDefinitionRegistry registry) {
        AnnotationAttributes attributes = AnnotationAttributes.fromMap(annotationMetadata.getAnnotationAttributes(RpcServiceInterfaceScan.class.getName()));

        if (Objects.nonNull(attributes)) {
            Set<String> packages = getScanningPackages(attributes);
            if (packages.isEmpty()) {
                packages.add(getDefaultBasePackage(annotationMetadata));
            }

            RpcServiceInterfaceScanner scanner = new RpcServiceInterfaceScanner(registry, this.classLoader);
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
     * 自定义的包扫描器，过滤所有含有自定义注解@RpcServiceInterface的类
     */
    static class RpcServiceInterfaceScanner extends ClassPathBeanDefinitionScanner {
        private final ClassLoader classLoader;

        public RpcServiceInterfaceScanner(BeanDefinitionRegistry registry, ClassLoader classLoader) {
            super(registry, false);
            this.classLoader = classLoader;
            registerFilter();
        }


        private void registerFilter() {
            addIncludeFilter(new AnnotationTypeFilter(RpcServiceInterface.class));
        }


        @Override
        protected Set<BeanDefinitionHolder> doScan(String... basePackages) {
            Set<BeanDefinitionHolder> beanDefinitionHolders = super.doScan(basePackages);

            beanDefinitionHolders.forEach(holder -> {
                GenericBeanDefinition beanDefinition = (GenericBeanDefinition) holder.getBeanDefinition();
                beanDefinition.getConstructorArgumentValues().addGenericArgumentValue(Objects.requireNonNull(beanDefinition.getBeanClassName()));
                // 指定FactoryBean
                beanDefinition.setBeanClassName(RpcFactoryBean.class.getName());
            });

            return beanDefinitionHolders;
        }

        /**
         * 重写候选判断逻辑，选出带有注解的接口
         */
        @Override
        protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
            final AnnotationMetadata metadata = beanDefinition.getMetadata();
            if (metadata.isInterface() && metadata.isInterface()) {
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
