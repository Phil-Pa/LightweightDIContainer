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
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.reflections.ReflectionUtils.withAnnotation;
import static org.reflections.ReflectionUtils.withParametersCount;

public class Container {

    private final Set<String> packageNames = new HashSet<>();
    private final Map<String, Object> injectableNameToInjectableObjectMap = new HashMap<>();
    private final Map<String, Object> beanNameToSingletonMap = new HashMap<>();
    private final Set<String> classesToExcludeFromScanning = new HashSet<>();

    public void addPackage(String packageName) {
        packageNames.add(packageName);
    }

    public void excludeClassesFromScanning(Class<?> ...classesToExclude) {
        List<Class<?>> classList = Arrays.asList(classesToExclude);
        Set<String> classNames = classList
                .stream()
                .map(Class::getSimpleName)
                .collect(Collectors.toSet());
        classesToExcludeFromScanning.addAll(classNames);
    }

    public void addBean(String beanName, Object bean) {
        if (injectableNameToInjectableObjectMap.containsKey(beanName))
            throw new IllegalArgumentException("container already contains bean with name " + beanName);

        injectableNameToInjectableObjectMap.put(beanName, bean);
    }

    public void scan() {

        Set<Class<?>> beanTypes = findBeanTypes();

        for (Class<?> beanType : beanTypes) {
            if (typeCanNotBeInstantiated(beanType))
                continue;

            Set<Field> injectableFields = getInjectableFields(beanType);

            if (typeHasEquallyNamedInjectableFields(injectableFields))
                throw new IllegalStateException("bean must not have 2 equally named injectable fields");

            Object newSingleton = createSingletonObjectOfType(beanType);

            injectInjectablesIntoSingleton(injectableFields, newSingleton);
            String beanName = getBeanNameOfType(beanType);
            addSingletonToMap(beanName, newSingleton);
        }
    }

    private Set<Class<?>> findBeanTypes() {
        Set<Class<?>> beanTypes = new HashSet<>();

        for (String packageName : packageNames) {
            Set<Class<?>> tempBeanTypes = getAllBeanTypesByPackageName(packageName);
            beanTypes.addAll(tempBeanTypes);
        }
        return beanTypes;
    }

    public Object getBeanByType(Class<?> type) {

        checkIfTypeCanBeInstantiated(type);
        checkIfTypeIsBean(type);
        checkIfTypeIsAddedAndScanned(type);

        String beanName = getBeanNameOfType(type);
        return beanNameToSingletonMap.get(beanName);
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

    private void injectInjectablesIntoSingleton(Set<Field> injectableFields, Object newSingleton) {
        for (Field field : injectableFields) {
            String injectableName = getFieldName(field);
            Object valueToInject = injectableNameToInjectableObjectMap.get(injectableName);
            try {
                field.setAccessible(true);
                field.set(newSingleton, valueToInject);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    private static boolean typeHasEquallyNamedInjectableFields(Set<Field> injectableFields) {
        List<String> fieldNames = injectableFields.stream().map(Container::getFieldName)
                .collect(Collectors.toList());

        return CollectionUtils.containsDuplicates(fieldNames);
    }

    private static String getFieldName(Field field) {
        Named named = field.getAnnotation(Named.class);
        if (named != null)
            return named.name();
        return field.getName();
    }

    private Set<Class<?>> getAllBeanTypesByPackageName(String packageName) {
        try {
            Reflections reflections = configureClasspathScanner(packageName);
            return reflections.getTypesAnnotatedWith(Bean.class);
        } catch (ReflectionsException e) {
            return new HashSet<>();
        }
    }

    private Reflections configureClasspathScanner(String packageName) {

        ConfigurationBuilder configurationBuilder = new ConfigurationBuilder()
                .setUrls(ClasspathHelper.forPackage(packageName))
                .setScanners(new TypeAnnotationsScanner(), new SubTypesScanner())
                .filterInputsBy(this::myFilter);

        return new Reflections(
                configurationBuilder
        );
    }

    private boolean myFilter(String classpath) {
        boolean actualValue = !classpathContainsAnyClassToExclude(classpath, classesToExcludeFromScanning);
        System.out.println("exclude " + classpath + " = " + actualValue);
        return actualValue;
    }

    private static boolean classpathContainsAnyClassToExclude(String classpath, Set<String> classesToExcludeFromScanning) {
        for (String className : classesToExcludeFromScanning) {
            if (classpath.contains(className))
                return true;
        }
        return false;
    }

    private static void checkIfTypeCanBeInstantiated(Class<?> type) {
        if (typeCanNotBeInstantiated(type))
            throw new IllegalArgumentException("type " + type.getName() + " can not be instantiated");
    }

    private static boolean typeCanNotBeInstantiated(Class<?> type) {
        if (type.isInterface() || Modifier.isAbstract(type.getModifiers()))
            return true;
        return false;
    }

    private static void checkIfTypeIsBean(Class<?> type) {
        if (!typeIsBean(type))
            throw new IllegalArgumentException("type " + type.getName() + " is not annotated with Bean");
    }

    private static Set<Field> getInjectableFields(Class<?> type) {
        return ReflectionUtils.getAllFields(type, withAnnotation(Inject.class));
    }

    private static Object createSingletonObjectOfType(Class<?> type) {
        Constructor<?> constructor = getDefaultConstructorOfType(type);
        try {
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            return null;
        }
    }

    private static boolean typeIsBean(Class<?> type) {
        return type.isAnnotationPresent(Bean.class);
    }

    private static Constructor<?> getDefaultConstructorOfType(Class<?> type) {
        return ReflectionUtils.getConstructors(type, withParametersCount(0)).stream().findFirst().orElseThrow();
    }

    private static String getBeanNameOfType(Class<?> type) {
        return type.getName();
    }
}
