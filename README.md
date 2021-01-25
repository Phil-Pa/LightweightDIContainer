# Lightweight Dependency Injection Container

In an employment interview I was given a task to solve, developing a dependency injection solution. It should work like this:
```java
Integer beanA = 0;
Integer beanB = 1;
Container container = new Container();
container.addPackage("<my package name>");

container.addBean("a", beanA);
container.addBean("a", beanB);
container.start();

InjectionTargetNamed injectionTargetNamed = container.getBeanByType(InjectionTargetNamed.class);
assertEquals(beanA, injectionTargetNamed.valueA);
assertEquals(beanB, injectionTargetNamed.valueB);
```

```java
public class InjectionTargetNamed {

    @Inject
    @Named("a")
    public Integer valueA;
    
    @Inject
    @Named("b")
    public Integer valueB;
}
```

It took be about 2 sessions, each about 5 hours, totalling 10 hours, to solve this question and I ended up with about 600 lines of code, 300 in production and 300 in tests. The actual code provides some more features than this short example, dealing with some edge cases. As you can see in the example, you can scan whole packages for dependency injection. Additionally, you can exclude certain classes to be scanned, or not scan packages at all and just scan the set of classes you actually want.
