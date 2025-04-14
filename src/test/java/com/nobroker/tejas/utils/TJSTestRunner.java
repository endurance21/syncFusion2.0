package com.syncfusion.utils;

import com.syncfusion.utils.annotations.TJSSuiteName;
import org.junit.runners.Suite;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerBuilder;

public class TJSTestRunner  extends Suite {
    public TJSTestRunner(Class<?> klass, RunnerBuilder builder)
            throws InitializationError {
        super(klass, builder);
    }

    @Override
    protected String getName() {
        return getTestClass()
                .getJavaClass()
                .getAnnotation(TJSSuiteName.class)
                .value();
    }
}


