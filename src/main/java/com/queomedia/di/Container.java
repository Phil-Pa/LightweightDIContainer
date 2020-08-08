package com.queomedia.di;

import com.queomedia.di.annotations.Bean;
import com.queomedia.di.annotations.Inject;
import com.queomedia.di.annotations.Named;
import org.reflections.ReflectionUtils;
import org.reflections.Reflections;
import org.reflections.ReflectionsException;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.reflections.ReflectionUtils.withAnnotation;
import static org.reflections.ReflectionUtils.withParametersCount;

public class Container {

    private final Set<String> packageNames = new HashSet<>();
    private final Map<String, Object> injectableNameToInjectableObjectMap = new HashMap<>();
    private final Map<String, Object> beanNameToSingletonMap = new HashMap<>();

    public void addPackage(String packageName) {
        packageNames.add(packageName);
    }

    public void addBean(String beanName, Object bean) {
        if (injectableNameToInjectableObjectMap.containsKey(beanName))
            throw new IllegalArgumentException("container already contains bean with name " + beanName);

        injectableNameToInjectableObjectMap.put(beanName, bean);
    }

    public void scan() {

        final Set<Class<?>> beanTypes = new HashSet<>();

        for (String packageName : packageNames) {
            Set<Class<?>> tempBeanTypes = getAllBeanTypesByPackageName(packageName);
            beanTypes.addAll(tempBeanTypes);
        }

        for (Class<?> beanType : beanTypes) {
            if (!typeCanBeInstantiated(beanType))
                continue;

            Object newSingleton = createSingletonObjectOfType(beanType);

            if (newSingleton == null)
                continue;

            injectInjectablesIntoSingleton(newSingleton);
            String beanName = getBeanNameOfType(beanType);
            addSingletonToMap(beanName, newSingleton);
        }
    }

    private Set<Class<?>> getAllBeanTypesByPackageName(String packageName) {
        try {
            Reflections reflections = new Reflections(packageName);
            return reflections.getTypesAnnotatedWith(Bean.class);
        } catch (ReflectionsException e) {
            return new HashSet<>();
        }
    }

    public Object getBeanByType(Class<?> type) {

        checkIfTypeCanBeInstantiated(type);
        checkIfTypeIsBean(type);
        checkIfTypeIsAddedAndScanned(type);

        String beanName = getBeanNameOfType(type);
        return beanNameToSingletonMap.get(beanName);


    }

    private void checkIfTypeCanBeInstantiated(Class<?> type) {
        if (!typeCanBeInstantiated(type))
            throw new IllegalArgumentException("type " + type.getName() + " can not be instantiated");
    }

    private boolean typeCanBeInstantiated(Class<?> type) {
        if (type.isInterface() || Modifier.isAbstract(type.getModifiers()))
            return false;
        return true;
    }

    private void checkIfTypeIsBean(Class<?> type) {
        if (!typeIsBean(type))
            throw new IllegalArgumentException("type " + type.getName() + " is not annotated with Bean");
    }

    private boolean typeIsBean(Class<?> type) {
        return type.isAnnotationPresent(Bean.class);
    }

    private void checkIfTypeIsAddedAndScanned(Class<?> type) {
        if (packageNames.isEmpty())
            throw new IllegalStateException("packages must be added and scanned before getting bean");

        if (!typeIsInScannedPackages(type))
            throw new IllegalArgumentException("package of type " + type.getName() + " has not been added and scanned");
    }

    private boolean typeIsInScannedPackages(Class<?> type) {
        String packageOfType = type.getPackageName();
        for (String packageName : packageNames) {
            if (!packageOfType.startsWith(packageName))
                return false;
        }
        return true;
    }

    private void addSingletonToMap(String beanName, Object newSingleton) {
        beanNameToSingletonMap.put(beanName, newSingleton);
    }

    private void injectInjectablesIntoSingleton(Object newSingleton) {
        Set<Field> injectableFields = getInjectableFields(newSingleton);
        for (Field field : injectableFields) {
            String injectableName = getInjectableNameOfField(field);
            Object valueToInject = injectableNameToInjectableObjectMap.get(injectableName);
            try {
                field.set(newSingleton, valueToInject);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    private String getInjectableNameOfField(Field field) {
        return field.getAnnotation(Named.class).name();
    }

    private Set<Field> getInjectableFields(Object newSingleton) {
        return ReflectionUtils.getAllFields(newSingleton.getClass(), withAnnotation(Inject.class));
    }

    private Object createSingletonObjectOfType(Class<?> type) {
        Constructor<?> constructor = getDefaultConstructorOfType(type);
        try {
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            return null;
        }
    }

    private Constructor<?> getDefaultConstructorOfType(Class<?> type) {
        return ReflectionUtils.getConstructors(type, withParametersCount(0)).stream().findFirst().orElseThrow();
    }

    private boolean alreadyCreatedSingletonOfType(Class<?> type) {
        String beanName = getBeanNameOfType(type);
        return beanNameToSingletonMap.containsKey(beanName);
    }

    private String getBeanNameOfType(Class<?> type) {
        return type.getName();
    }
}
