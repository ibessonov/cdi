package com.github.ibessonov.cdi.runtime.st;

/**
 * @author ibessonov
 */
public interface StNode {
    void accept(StVisitor visitor);
}
