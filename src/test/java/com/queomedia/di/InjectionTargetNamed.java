package com.queomedia.di;

import com.queomedia.di.annotations.Bean;
import com.queomedia.di.annotations.Inject;
import com.queomedia.di.annotations.Named;

@Bean(name = "myBean")
public class InjectionTargetNamed {

    @Inject
    @Named(name = "a")
    public Integer valueA;

    @Inject
    @Named(name = "b")
    public Integer valueB;

}
