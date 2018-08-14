package com.github.ibessonov.cdi;

/**
 * @author ibessonov
 */
@FunctionalInterface
public interface Extension {

    void register(Registrar registrar);
}
