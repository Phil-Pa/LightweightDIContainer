package com.queomedia.di;

import com.queomedia.di.demobeans.AbstractDemo;
import com.queomedia.di.demobeans.Demo;
import com.queomedia.di.demoinjection.InjectionTargetNamed;
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

        container.addPackage("com.queomedia.di");

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

        container.addPackage("com.queomedia.di.demoinjection");

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
        } catch (IllegalArgumentException e) {

        }
    }

    @Test
    public void testThrowIfTypeToCreateIsNotBean() {
        Container container = new Container();
        Integer bean = 0;
        container.addPackage("com.queomedia.di.demoinjection");
        container.addBean("a", bean);
        container.scan();

        try {
            Container newContainer = (Container) container.getBeanByType(Container.class);
            fail(Container.class.getName() + " is not annotated with Bean and can not be created by the container");
        } catch (IllegalArgumentException e) {

        }
    }

    @Test
    public void testThrowIfBeanNameCanNotBeInstantiated() {
        Container container = new Container();
        Integer bean = 0;
        container.addPackage("com.queomedia.di.demobeans");
        container.addBean("a", bean);
        container.scan();

        try {
            Demo demo = (Demo) container.getBeanByType(Demo.class);
            fail("demo can not be instantiated and an exception must be thrown");
        } catch (IllegalArgumentException e) {

        }

        try {
            AbstractDemo demo = (AbstractDemo) container.getBeanByType(AbstractDemo.class);
            fail("demo can not be instantiated and an exception must be thrown");
        } catch (IllegalArgumentException e) {

        }
    }

}