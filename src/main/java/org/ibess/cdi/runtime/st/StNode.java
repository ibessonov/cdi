package org.ibess.cdi.runtime.st;

/**
 * @author ibessonov
 */
public interface StNode {
    void accept(StVisitor visitor);
}
