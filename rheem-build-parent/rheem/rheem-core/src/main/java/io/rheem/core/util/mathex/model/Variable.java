package io.rheem.core.util.mathex.model;


import io.rheem.core.util.mathex.Context;
import io.rheem.core.util.mathex.Expression;

/**
 * A variable {@link Expression}
 */
public class Variable implements Expression {

    final String name;

    public Variable(String name) {
        this.name = name;
    }

    @Override
    public double evaluate(Context context) {
        return context.getVariable(this.name);
    }

    @Override
    public String toString() {
        return this.name;
    }
}
