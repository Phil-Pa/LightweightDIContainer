package com.queomedia.di.invalidbeans;

import com.queomedia.di.annotations.Bean;
import com.queomedia.di.annotations.Inject;
import com.queomedia.di.annotations.Named;
import com.queomedia.di.demobeans.Demo;

@Bean
public class DemoImpl1 {

    @Inject
    @Named(name = "demo2")
    private Demo demo1;

    @Inject
    private Demo demo2;

}
