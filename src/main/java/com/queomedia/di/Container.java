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
import java.util.stream.Collectors;

import static org.reflections.ReflectionUtils.withAnnotation;
import static org.reflections.ReflectionUtils.withParametersCount;

public class Container {

    private interface SingletonProviderStrategy {
        Object provideSingleton(Class<?> type);
    }

    private static final class SingletonProviderCreateNewSingletonStrategy implements SingletonProviderStrategy {

        @Override
        public Object provideSingleton(Class<?> type) {
            return Container.createSingletonObjectOfClass(type);
        }
    }

    private static final class SingletonProviderUseManuallyInstantiatedBeanStrategy implements SingletonProviderStrategy {

        private final Object manuallyInstantiatedBean;

        public SingletonProviderUseManuallyInstantiatedBeanStrategy(Object manuallyInstantiatedBean) {
            this.manuallyInstantiatedBean = manuallyInstantiatedBean;
        }

        @Override
        public Object provideSingleton(Class<?> type) {
            return manuallyInstantiatedBean;
        }
    }

    private static final SingletonProviderStrategy CREATE_NEW_SINGLETON_STRATEGY = new SingletonProviderCreateNewSingletonStrategy();

    private final Set<String> packageNames = new HashSet<>();
    private final Map<String, Object> injectableNameToInjectableObjectMap = new HashMap<>();
    private final Map<String, Object> beanNameToSingletonMap = new HashMap<>();
    private final Set<String> classesToExcludeFromScanning = new HashSet<>();
    private final Set<Object> manuallyInstantiatedBeans = new HashSet<>();
    private final Set<Class<?>> manuallyAddedBeanClasses = new HashSet<>();

    public void addPackage(String packageName) {
        packageNames.add(packageName);
    }

    public void addInjectable(String injectableName, Object injectable) {
        if (injectableNameToInjectableObjectMap.containsKey(injectableName))
            throw new IllegalArgumentException("container already contains bean with name " + injectableName);

        injectableNameToInjectableObjectMap.put(injectableName, injectable);
    }

    public void addClass(Class<?> clazz) {
        manuallyAddedBeanClasses.add(clazz);
    }

    public void excludeClassesFromScanning(Class<?> ...classesToExclude) {
        List<Class<?>> classList = Arrays.asList(classesToExclude);
        Set<String> classNames = classList
                .stream()
                .map(Class::getSimpleName)
                .collect(Collectors.toSet());
        classesToExcludeFromScanning.addAll(classNames);
    }

    public void scan() {
        Set<Class<?>> beanClasses = findBeanClasses();
        addManuallyAddedClassesToBeanClasses(beanClasses);

        throwIfSameBeanNames(beanClasses);

        for (Class<?> clazz : beanClasses) {
            if (classCanNotBeInstantiated(clazz))
                continue;

            if (!beanClassIsManuallyAdded(clazz))
                injectFieldsIntoBean(clazz, CREATE_NEW_SINGLETON_STRATEGY);
        }

        injectManuallyAddedSingletons();
    }

    private void addManuallyAddedClassesToBeanClasses(Set<Class<?>> beanClasses) {
        beanClasses.addAll(manuallyAddedBeanClasses);
    }

    private boolean beanClassIsManuallyAdded(Class<?> clazz) {
        for (Object object : manuallyInstantiatedBeans) {
            Class<?> objectType = object.getClass();
            if (clazz.getName().equals(objectType.getName()))
                return true;
        }
        return false;
    }

    private void injectFieldsIntoBean(Class<?> beanClass, SingletonProviderStrategy singletonProviderStrategy) {
        Set<Field> injectableFields = getInjectableFieldsOfBeanClass(beanClass);

        if (beanClassHasEquallyNamedInjectableFields(injectableFields))
            throw new IllegalStateException("bean must not have 2 equally named injectable fields");

        Object singleton = singletonProviderStrategy.provideSingleton(beanClass);

        setValuesOfInjectableFields(injectableFields, singleton);
        String beanName = getBeanNameOfClass(beanClass);
        addSingletonToMap(beanName, singleton);
    }

    private void injectManuallyAddedSingletons() {
        for (Object singleton : manuallyInstantiatedBeans) {
            Class<?> beanClass = singleton.getClass();
            injectFieldsIntoBean(beanClass, new SingletonProviderUseManuallyInstantiatedBeanStrategy(singleton));
        }
    }

    private void throwIfSameBeanNames(Set<Class<?>> beanClasses) {
        for (Class<?> classI : beanClasses) {
            String beanNameI = getBeanNameOfClass(classI);
            for (Class<?> classJ : beanClasses) {

                if (classI.getName().equals(classJ.getName()))
                    continue;

                String beanNameJ = getBeanNameOfClass(classJ);
                if (beanNameI.equals(beanNameJ))
                    throw new IllegalStateException("scanned packages contain beans with equal names");
            }
        }
    }

    private Set<Class<?>> findBeanClasses() {
        Set<Class<?>> beanClasses = new HashSet<>();

        for (String packageName : packageNames) {
            Set<Class<?>> tempBeanClasses = getAllBeanClassesByPackageName(packageName);
            beanClasses.addAll(tempBeanClasses);
        }
        return beanClasses;
    }

    public Object getBeanByClass(Class<?> clazz) {

        throwIfClassCanNotBeInstantiated(clazz);
        throwIfClassIsNotBean(clazz);
        throwIfClassIsNotAddedAndScanned(clazz);

        String beanName = getBeanNameOfClass(clazz);
        return beanNameToSingletonMap.get(beanName);
    }

    private void throwIfClassIsNotAddedAndScanned(Class<?> clazz) {
        if (packageNames.isEmpty() && manuallyInstantiatedBeans.isEmpty() && manuallyAddedBeanClasses.isEmpty())
            throw new IllegalStateException("packages must be added and scanned before getting bean");

        if (!classIsInScannedPackages(clazz))
            throw new IllegalArgumentException("package of type " + clazz.getName() + " has not been added and scanned");
    }

    private boolean classIsInScannedPackages(Class<?> clazz) {
        String packageOfClass = clazz.getPackageName();
        for (String packageName : packageNames) {
            if (!packageOfClass.startsWith(packageName))
                return false;
        }
        return !classesToExcludeFromScanning.contains(clazz.getSimpleName());
    }

    private void addSingletonToMap(String beanName, Object newSingleton) {
        beanNameToSingletonMap.put(beanName, newSingleton);
    }

    private void setValuesOfInjectableFields(Set<Field> injectableFields, Object newSingleton) {
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

    private static boolean beanClassHasEquallyNamedInjectableFields(Set<Field> injectableFields) {
        List<String> fieldNames = injectableFields
                .stream()
                .map(Container::getFieldName)
                .collect(Collectors.toList());

        return CollectionUtils.containsDuplicates(fieldNames);
    }

    private Set<Class<?>> getAllBeanClassesByPackageName(String packageName) {
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
                .filterInputsBy(this::filterClassesToExclude);

        return new Reflections(
                configurationBuilder
        );
    }

    private boolean filterClassesToExclude(String classpath) {
        boolean classShouldBeScanned = !classpathContainsAnyClassToExclude(classpath, classesToExcludeFromScanning);
        System.out.println("scanning " + classpath + " = " + classShouldBeScanned);
        return classShouldBeScanned;
    }

    private static boolean classpathContainsAnyClassToExclude(String classpath, Set<String> classesToExcludeFromScanning) {
        for (String className : classesToExcludeFromScanning) {
            if (classpath.contains(className))
                return true;
        }
        return false;
    }

    private static void throwIfClassCanNotBeInstantiated(Class<?> clazz) {
        if (classCanNotBeInstantiated(clazz))
            throw new IllegalArgumentException("type " + clazz.getName() + " can not be instantiated");
    }

    private static boolean classCanNotBeInstantiated(Class<?> clazz) {
        if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers()))
            return true;
        return false;
    }

    private static void throwIfClassIsNotBean(Class<?> clazz) {
        if (!classIsBean(clazz))
            throw new IllegalArgumentException("type " + clazz.getName() + " is not annotated with Bean");
    }

    private static Set<Field> getInjectableFieldsOfBeanClass(Class<?> clazz) {
        return ReflectionUtils.getAllFields(clazz, withAnnotation(Inject.class));
    }

    private static Object createSingletonObjectOfClass(Class<?> clazz) {
        Constructor<?> constructor = getDefaultConstructorOfClass(clazz);
        try {
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            return null;
        }
    }

    private static boolean classIsBean(Class<?> clazz) {
        return clazz.isAnnotationPresent(Bean.class);
    }

    private static Constructor<?> getDefaultConstructorOfClass(Class<?> clazz) {
        return ReflectionUtils.getConstructors(clazz, withParametersCount(0)).stream().findFirst().orElseThrow();
    }

    private static String getBeanNameOfClass(Class<?> clazz) {
        if (clazz.isAnnotationPresent(Named.class))
            return clazz.getAnnotation(Named.class).name();
        else
            return clazz.getName();
    }

    private static String getFieldName(Field field) {
        Named named = field.getAnnotation(Named.class);
        if (named != null)
            return named.name();
        return field.getName();
    }

    public void addInjectable(Object newSingleton) {
        Class<?> clazz = newSingleton.getClass();
        if (!classIsBean(clazz))
            throw new IllegalArgumentException(clazz.getName() + " is not annotated with @Bean");

        manuallyInstantiatedBeans.add(newSingleton);
    }
}
