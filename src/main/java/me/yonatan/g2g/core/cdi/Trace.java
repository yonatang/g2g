package me.yonatan.g2g.core.cdi;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.interceptor.InterceptorBinding;

@Retention(RetentionPolicy.RUNTIME)
@InterceptorBinding
@Target({ElementType.TYPE,ElementType.METHOD})
public @interface Trace {

}
