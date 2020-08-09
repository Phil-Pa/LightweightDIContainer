package com.queomedia.di;

import com.queomedia.di.demobeans.AbstractDemo;
import com.queomedia.di.demobeans.Demo;
import com.queomedia.di.demobeans.DemoImpl2;
import com.queomedia.di.demoinjection.InjectionTargetNamed;
import com.queomedia.di.invalidbeans.DemoImpl1;
import com.queomedia.di.invalidbeans.DemoImpl3;
import com.queomedia.di.invalidbeans.DemoImpl4;
import org.junit.Test;

import static org.junit.Assert.*;

public class ContainerTest {

    @Test
    public void testBasicUseCase() {
        Container container = new Container();

        container.addClass(InjectionTargetNamed.class);

        Integer injectableA = 1;
        Integer injectableB = 2;

        container.addInjectable("a", injectableA);
        container.addInjectable("b", injectableB);

        container.scan();

        InjectionTargetNamed injectionTargetNamed = (InjectionTargetNamed) container.getBeanOfClass(InjectionTargetNamed.class);
        assertEquals(injectableA, injectionTargetNamed.getValueA());
        assertEquals(injectableB, injectionTargetNamed.getValueB());
    }

    @Test
    public void testGetSameBeanFromContainer() {
        Container container = new Container();

        container.addPackage("com.queomedia.di.demoinjection");
        container.excludeClassesFromScanning(DemoImpl1.class, DemoImpl3.class);

        Integer injectableA = 1;
        Integer injectableB = 2;

        container.addInjectable("a", injectableA);
        container.addInjectable("b", injectableB);

        container.scan();

        InjectionTargetNamed injectionTargetNamedA = (InjectionTargetNamed) container.getBeanOfClass(InjectionTargetNamed.class);
        InjectionTargetNamed injectionTargetNamedB = (InjectionTargetNamed) container.getBeanOfClass(InjectionTargetNamed.class);

        assertEquals(injectionTargetNamedA, injectionTargetNamedB);
    }

    @Test
    public void testThrowExceptionWhenAddingInjectableWithSameNameMultipleTimes() {
        Container container = new Container();
        Integer injectable = 0;
        container.addInjectable("a", injectable);

        try {
            container.addInjectable("a", injectable);
            fail("should not be able to add injectable with name a twice");
        } catch (IllegalArgumentException e) {

        }
    }

    @Test
    public void testThrowExceptionWhenCreatingNotRegisteredBean() {
        Container container = new Container();
        Integer injectable = 0;
        container.addInjectable("a", injectable);
        container.scan();

        try {
            InjectionTargetNamed injectionTargetNamed = (InjectionTargetNamed) container.getBeanOfClass(InjectionTargetNamed.class);
            fail("InjectionTargetNamed is not registered and container should throw exception");
        } catch (IllegalStateException e) {

        }
    }

    @Test
    public void testCheckBeanToCreateIsInAddedPackages() {
        Container container = new Container();
        Integer injectable = 0;
        container.addPackage("com.example");
        container.addInjectable("a", injectable);
        container.scan();

        try {
            InjectionTargetNamed injectionTargetNamed = (InjectionTargetNamed) container.getBeanOfClass(InjectionTargetNamed.class);
            fail("class InjectionTargetNamed is not in package com.example and the container should not have added this class for scanning");
        } catch (IllegalArgumentException e) {

        }
    }

    @Test
    public void testThrowIfClassToCreateIsNotBean() {
        Container container = new Container();
        container.addPackage("com.queomedia.di.demoinjection");
        container.excludeClassesFromScanning(DemoImpl1.class, DemoImpl3.class);
        container.scan();

        try {
            Container newContainer = (Container) container.getBeanOfClass(Container.class);
            fail(Container.class.getName() + " is not annotated with Bean and can not be created by the container");
        } catch (IllegalArgumentException e) {

        }
    }

    @Test
    public void testThrowIfBeanClassCanNotBeInstantiated() {
        Container container = new Container();
        container.addClass(Demo.class);
        container.addClass(AbstractDemo.class);
        container.scan();

        try {
            Demo demo = (Demo) container.getBeanOfClass(Demo.class);
            fail("demo can not be instantiated and an exception must be thrown");
        } catch (IllegalArgumentException e) {

        }

        try {
            AbstractDemo demo = (AbstractDemo) container.getBeanOfClass(AbstractDemo.class);
            fail("demo can not be instantiated and an exception must be thrown");
        } catch (IllegalArgumentException e) {

        }
    }

    @Test
    public void testThrowIfInjectableFieldsHaveSameName() {
        Container container = new Container();
        container.addClass(Demo.class);
        container.addClass(DemoImpl1.class);
        container.addInjectable("demo2", null);

        try {
            container.scan();
            // DemoImpl1 demoImpl = (DemoImpl1) container.getBeanOfClass(DemoImpl1.class);
            fail("DemoImpl1 has 2 equally named fields to inject");
        } catch (IllegalStateException e) {

        }
    }

    @Test
    public void testThrowWhenGettingExcludedBean() {
        Container container = new Container();
        container.addPackage("com.queomedia.di.invalidbeans");
        container.excludeClassesFromScanning(DemoImpl1.class, DemoImpl2.class);

        container.scan();

        try {
            DemoImpl2 demo = (DemoImpl2) container.getBeanOfClass(DemoImpl2.class);
            fail("DemoImpl2 has been excluded from scanning and can not be available");
        } catch (IllegalArgumentException e) {

        }

    }

    @Test
    public void testThrowIfBeansHaveSameName() {
        Container container = new Container();
        container.addPackage("com.queomedia.di");
        container.excludeClassesFromScanning(DemoImpl1.class, DemoImpl2.class);

        try {
            container.scan();
//            DemoImpl3 demo = (DemoImpl3) container.getBeanOfClass(DemoImpl3.class);
            fail("DemoImpl3 and DemoImpl4 have the same been name");
        } catch (IllegalStateException e) {

        }

    }

    @Test
    public void testAddInstantiatedBean() {
        Container container = new Container();

        Integer injectableA = 3;
        Integer injectableB = 5;
        InjectionTargetNamed injectionTargetNamed = new InjectionTargetNamed();

        container.addInjectable(injectionTargetNamed);

        container.addInjectable("a", injectableA);
        container.addInjectable("b", injectableB);

        container.scan();

        InjectionTargetNamed injectionTargetNamedInjected = (InjectionTargetNamed) container.getBeanOfClass(InjectionTargetNamed.class);

        assertEquals(injectionTargetNamed, injectionTargetNamedInjected);
        assertEquals(injectableA, injectionTargetNamed.getValueA());
        assertEquals(injectableB, injectionTargetNamed.getValueB());
    }

    @Test
    public void testManuallyInstantiatedBeanIsNotRecreatedWhenScanning() {
        Container container = new Container();

        Integer injectableA = 3;
        Integer injectableB = 5;
        Integer valueC = 9;
        InjectionTargetNamed injectionTargetNamed = new InjectionTargetNamed();
        injectionTargetNamed.setValueC(valueC);

        container.addPackage("com.queomedia.di");
        container.excludeClassesFromScanning(DemoImpl1.class, DemoImpl2.class, DemoImpl3.class, DemoImpl4.class);
        container.addInjectable(injectionTargetNamed);

        container.addInjectable("a", injectableA);
        container.addInjectable("b", injectableB);

        container.scan();

        InjectionTargetNamed injectionTargetNamedInjected = (InjectionTargetNamed) container.getBeanOfClass(InjectionTargetNamed.class);

        assertEquals(injectionTargetNamed, injectionTargetNamedInjected);
        assertEquals(valueC, injectionTargetNamedInjected.getValueC());
    }

    @Test
    public void testAddClass() {
        Container container = new Container();
        Integer beanA = 3;
        Integer beanB = 5;

        container.addClass(InjectionTargetNamed.class);
        container.addInjectable("a", beanA);
        container.addInjectable("b", beanB);

        container.scan();

        InjectionTargetNamed injectionTargetNamed = (InjectionTargetNamed) container.getBeanOfClass(InjectionTargetNamed.class);
        assertEquals(beanA, injectionTargetNamed.getValueA());
        assertEquals(beanB, injectionTargetNamed.getValueB());
    }

}