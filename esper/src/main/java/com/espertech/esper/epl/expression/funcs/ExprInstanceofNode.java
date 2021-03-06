/*
 ***************************************************************************************
 *  Copyright (C) 2006 EsperTech, Inc. All rights reserved.                            *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 ***************************************************************************************
 */
package com.espertech.esper.epl.expression.funcs;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.epl.core.EngineImportService;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.util.JavaClassHelper;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Represents the INSTANCEOF(a,b,...) function is an expression tree.
 */
public class ExprInstanceofNode extends ExprNodeBase implements ExprEvaluator {
    private final String[] classIdentifiers;

    private Class[] classes;
    private CopyOnWriteArrayList<Pair<Class, Boolean>> resultCache = new CopyOnWriteArrayList<Pair<Class, Boolean>>();
    private transient ExprEvaluator evaluator;
    private static final long serialVersionUID = 3358616797009364727L;

    /**
     * Ctor.
     *
     * @param classIdentifiers is a list of type names to check type for
     */
    public ExprInstanceofNode(String[] classIdentifiers) {
        this.classIdentifiers = classIdentifiers;
    }

    public ExprEvaluator getExprEvaluator() {
        return this;
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        if (this.getChildNodes().length != 1) {
            throw new ExprValidationException("Instanceof node must have 1 child expression node supplying the expression to test");
        }
        if ((classIdentifiers == null) || (classIdentifiers.length == 0)) {
            throw new ExprValidationException("Instanceof node must have 1 or more class identifiers to verify type against");
        }

        evaluator = this.getChildNodes()[0].getExprEvaluator();
        Set<Class> classList = getClassSet(classIdentifiers, validationContext.getEngineImportService());
        synchronized (this) {
            classes = classList.toArray(new Class[classList.size()]);
        }
        return null;
    }

    public boolean isConstantResult() {
        return false;
    }

    public Class getType() {
        return Boolean.class;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qExprInstanceof(this);
        }
        Object result = evaluator.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
        if (result == null) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aExprInstanceof(false);
            }
            return false;
        }

        // return cached value
        for (Pair<Class, Boolean> pair : resultCache) {
            if (pair.getFirst() == result.getClass()) {
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().aExprInstanceof(pair.getSecond());
                }
                return pair.getSecond();
            }
        }

        Boolean out = checkAddType(result.getClass());
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aExprInstanceof(out);
        }
        return out;
    }

    // Checks type and adds to cache
    private synchronized Boolean checkAddType(Class type) {
        // check again in synchronized block
        for (Pair<Class, Boolean> pair : resultCache) {
            if (pair.getFirst() == type) {
                return pair.getSecond();
            }
        }

        // get the types superclasses and interfaces, and their superclasses and interfaces
        Set<Class> classesToCheck = new HashSet<Class>();
        JavaClassHelper.getSuper(type, classesToCheck);
        classesToCheck.add(type);

        // check type against each class
        boolean fits = false;
        for (Class clazz : classes) {
            if (classesToCheck.contains(clazz)) {
                fits = true;
                break;
            }
        }

        resultCache.add(new Pair<Class, Boolean>(type, fits));
        return fits;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        writer.append("instanceof(");
        this.getChildNodes()[0].toEPL(writer, ExprPrecedenceEnum.MINIMUM);
        writer.append(",");

        String delimiter = "";
        for (int i = 0; i < classIdentifiers.length; i++) {
            writer.append(delimiter);
            writer.append(classIdentifiers[i]);
            delimiter = ",";
        }
        writer.append(')');
    }

    public ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.UNARY;
    }

    public boolean equalsNode(ExprNode node, boolean ignoreStreamPrefix) {
        if (!(node instanceof ExprInstanceofNode)) {
            return false;
        }
        ExprInstanceofNode other = (ExprInstanceofNode) node;
        if (Arrays.equals(other.classIdentifiers, classIdentifiers)) {
            return true;
        }
        return false;
    }

    /**
     * Returns the list of class names or types to check instance of.
     *
     * @return class names
     */
    public String[] getClassIdentifiers() {
        return classIdentifiers;
    }

    private Set<Class> getClassSet(String[] classIdentifiers, EngineImportService engineImportService)
            throws ExprValidationException {
        Set<Class> classList = new HashSet<Class>();
        for (String className : classIdentifiers) {
            Class clazz;

            // try the primitive names including "string"
            clazz = JavaClassHelper.getPrimitiveClassForName(className.trim());
            if (clazz != null) {
                classList.add(clazz);
                classList.add(JavaClassHelper.getBoxedType(clazz));
                continue;
            }

            // try to look up the class, not a primitive type name
            try {
                clazz = JavaClassHelper.getClassForName(className.trim(), engineImportService.getClassForNameProvider());
            } catch (ClassNotFoundException e) {
                throw new ExprValidationException("Class as listed in instanceof function by name '" + className + "' cannot be loaded", e);
            }

            // Add primitive and boxed types, or type itself if not built-in
            classList.add(JavaClassHelper.getPrimitiveType(clazz));
            classList.add(JavaClassHelper.getBoxedType(clazz));
        }
        return classList;
    }
}
