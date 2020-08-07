package com.queomedia.di;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ContainerTest {

//    private static Container setupBasicContainer() {
//
//    }

    @Test
    public void testBasicUseCase() {
        Container container = new Container();

        String currentPackage = getClass().getPackageName();
        container.addPackage(currentPackage);

        Integer beanA = 1;
        Integer beanB = 2;

        container.addBean("a", beanA);
        container.addBean("b", beanB);

        container.scan();

        InjectionTargetNamed injectionTargetNamed = (InjectionTargetNamed) container.getBeanByType(InjectionTargetNamed.class);
        assertEquals(beanA, injectionTargetNamed.valueA);
        assertEquals(beanB, injectionTargetNamed.valueB);
    }

    @Test
    public void testGetSameBeanFromContainer() {
        Container container = new Container();

        String currentPackage = getClass().getPackageName();
        container.addPackage(currentPackage);

        Integer beanA = 1;
        Integer beanB = 2;

        container.addBean("a", beanA);
        container.addBean("b", beanB);

        container.scan();

        InjectionTargetNamed injectionTargetNamedA = (InjectionTargetNamed) container.getBeanByType(InjectionTargetNamed.class);
        InjectionTargetNamed injectionTargetNamedB = (InjectionTargetNamed) container.getBeanByType(InjectionTargetNamed.class);

        assertEquals(injectionTargetNamedA, injectionTargetNamedB);
    }

    @Test
    public void testThrowExceptionWhenAddingSameBeenMultipleTimes() {
        Container container = new Container();
        Integer bean = 0;
        container.addBean("a", bean);

        try {
            container.addBean("a", bean);
            fail("");
        } catch (IllegalArgumentException e) {

        }
    }

    @Test
    public void testThrowExceptionWhenCreatingBeanFromUnknownPackage() {
        Container container = new Container();
        Integer bean = 0;
        container.addBean("a", bean);
        container.scan();

        try {
            InjectionTargetNamed injectionTargetNamed = (InjectionTargetNamed) container.getBeanByType(InjectionTargetNamed.class);
            fail("container should throw exception");
        } catch (IllegalStateException e) {

        }
    }

    @Test
    public void testCheckBeanToCreateIsInAddedPackages() {
        Container container = new Container();
        Integer bean = 0;
        container.addPackage("com.example");
        container.addBean("a", bean);
        container.scan();

        try {
            InjectionTargetNamed injectionTargetNamed = (InjectionTargetNamed) container.getBeanByType(InjectionTargetNamed.class);
            fail("class InjectionTargetNamed is not in package com.example and the container should not have added this class for scanning");
        } catch (IllegalArgumentException e) { // TODO: class not scanned exception?

        }
    }

    @Test
    public void testThrowIfTypeToCreateIsNotBean() {
        Container container = new Container();
        Integer bean = 0;
        container.addPackage("com.queomedia.di");
        container.addBean("a", bean);
        container.scan();

        try {
            Container newContainer = (Container) container.getBeanByType(Container.class);
            fail(Container.class.getName() + " is not annotated with Bean and can not be created by the container");
        } catch (IllegalArgumentException e) {

        }
    }

}