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
package com.espertech.esper.event.xml;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.PropertyAccessException;
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.event.EventPropertyGetterSPI;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

/**
 * Getter for simple attributes in a DOM node.
 */
public class DOMSimpleAttributeGetter implements EventPropertyGetterSPI, DOMPropertyGetter {
    private final String propertyName;

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param node         node
     * @param propertyName property
     * @return node
     */
    public static Node getNodePropertyValue(Node node, String propertyName) {
        NamedNodeMap namedNodeMap = node.getAttributes();
        for (int i = 0; i < namedNodeMap.getLength(); i++) {
            Node attrNode = namedNodeMap.item(i);
            if (attrNode.getLocalName() != null) {
                if (propertyName.equals(attrNode.getLocalName())) {
                    return attrNode;
                }
                continue;
            }
            if (propertyName.equals(attrNode.getNodeName())) {
                return attrNode;
            }
        }
        return null;
    }

    /**
     * Ctor.
     *
     * @param propertyName property name
     */
    public DOMSimpleAttributeGetter(String propertyName) {
        this.propertyName = propertyName;
    }

    public Object getValueAsFragment(Node node) {
        return null;
    }

    public Node[] getValueAsNodeArray(Node node) {
        return null;
    }

    public Node getValueAsNode(Node node) {
        return getNodePropertyValue(node, propertyName);
    }

    public Object get(EventBean obj) throws PropertyAccessException {
        // The underlying is expected to be a map
        if (!(obj.getUnderlying() instanceof Node)) {
            throw new PropertyAccessException("Mismatched property getter to event bean type, " +
                    "the underlying data object is not of type Node");
        }

        Node node = (Node) obj.getUnderlying();
        return getValueAsNode(node);
    }

    public boolean isExistsProperty(EventBean eventBean) {
        return true;
    }

    public Object getFragment(EventBean eventBean) throws PropertyAccessException {
        return null;  // Never a fragment
    }

    public CodegenExpression codegenEventBeanGet(CodegenExpression beanExpression, CodegenContext context) {
        return codegenUnderlyingGet(castUnderlying(Node.class, beanExpression), context);
    }

    public CodegenExpression codegenEventBeanExists(CodegenExpression beanExpression, CodegenContext context) {
        return constantTrue();
    }

    public CodegenExpression codegenEventBeanFragment(CodegenExpression beanExpression, CodegenContext context) {
        return constantNull();
    }

    public CodegenExpression codegenUnderlyingGet(CodegenExpression underlyingExpression, CodegenContext context) {
        return staticMethod(this.getClass(), "getNodePropertyValue", underlyingExpression, constant(propertyName));
    }

    public CodegenExpression codegenUnderlyingExists(CodegenExpression underlyingExpression, CodegenContext context) {
        return constantTrue();
    }

    public CodegenExpression codegenUnderlyingFragment(CodegenExpression underlyingExpression, CodegenContext context) {
        return constantNull();
    }

    public CodegenExpression getValueAsNodeCodegen(CodegenExpression value, CodegenContext context) {
        return codegenUnderlyingGet(value, context);
    }

    public CodegenExpression getValueAsNodeArrayCodegen(CodegenExpression value, CodegenContext context) {
        return constantNull();
    }

    public CodegenExpression getValueAsFragmentCodegen(CodegenExpression value, CodegenContext context) {
        return constantNull();
    }
}
