package eu.okaeri.menu.core.annotation;

import eu.okaeri.menu.core.display.DisplayProvider;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MenuItem {

    String display() default "";

    Class<? extends DisplayProvider> displayProvider() default DEFAULT_DISPLAY_PROVIDER.class;

    String name();

    String position() default "-1";

    String description() default "";

    abstract class DEFAULT_DISPLAY_PROVIDER implements DisplayProvider {
    }
}
