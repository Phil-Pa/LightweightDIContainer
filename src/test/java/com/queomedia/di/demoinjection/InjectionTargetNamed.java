package com.queomedia.di.demoinjection;

import com.queomedia.di.annotations.Bean;
import com.queomedia.di.annotations.Inject;
import com.queomedia.di.annotations.Named;

@Bean
public class InjectionTargetNamed {

    @Inject
    @Named(name = "a")
    private Integer valueA;

    @Inject
    @Named(name = "b")
    private Integer valueB;

    private Integer valueC;

    public InjectionTargetNamed() {

    }

    public void setValueC(Integer value) {
        valueC = value;
    }

    public Integer getValueA() {
        return valueA;
    }

    public Integer getValueB() {
        return valueB;
    }

    public Integer getValueC() {
        return valueC;
    }
}
