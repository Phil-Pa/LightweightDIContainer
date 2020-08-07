package com.queomedia.di;

import com.queomedia.di.annotations.Bean;
import com.queomedia.di.annotations.Inject;
import com.queomedia.di.annotations.Named;
import org.reflections.ReflectionUtils;
import org.reflections.Reflections;
import static org.reflections.ReflectionUtils.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.Predicate;

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



    }

    public Object getBeanByType(Class<?> type) {

        checkIfTypeIsBean(type);
        checkIfTypeIsAddedAndScanned(type);

        String beanName = getBeanNameOfType(type);

        if (alreadyCreatedSingletonOfType(type)) {
            return beanNameToSingletonMap.get(beanName);
        } else {
            Object newSingleton = createSingletonObjectOfType(type);
            injectInjectablesIntoSingleton(newSingleton);
            addSingletonToMap(beanName, newSingleton);
            return newSingleton;
        }
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
        return packageNames.contains(packageOfType);
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
            return constructor.newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
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
