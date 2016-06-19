package org.ibess.cdi;

/**
 * @author ibessonov
 */
@FunctionalInterface
public interface Extension {

    void register(Registrar registrar);
}
