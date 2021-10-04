package eu.okaeri.menu.core.annotation;

import eu.okaeri.menu.core.display.DisplayProvider;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Menu {

    String name();

    Class<? extends DisplayProvider> displayProvider() default DEFAULT_DISPLAY_PROVIDER.class;

    String rows() default "-1";

    abstract class DEFAULT_DISPLAY_PROVIDER implements DisplayProvider {
    }
}
