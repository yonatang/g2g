package me.yonatan.g2g.core.model;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.inject.Qualifier;

@Retention(RetentionPolicy.RUNTIME)
@Qualifier
@Target({ElementType.METHOD,ElementType.FIELD})
public @interface SourceAccount {

}
