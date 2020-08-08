package com.queomedia.di.invalidbeans;

import com.queomedia.di.annotations.Bean;
import com.queomedia.di.annotations.Inject;
import com.queomedia.di.annotations.Named;
import com.queomedia.di.demobeans.Demo;

@Bean
@Named(name = "Demo34")
public class DemoImpl3 {

    @Inject
    @Named(name = "myDemo")
    protected Demo demo;

}
