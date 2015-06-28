package com.reversemind.nicobar

import org.codehaus.groovy.runtime.InvokerInvocationException
import org.codehaus.groovy.runtime.metaclass.MetaClassRegistryImpl

import javax.annotation.Nullable

/**
 * Customized adoptation of InvokerHelper
 */
public class ScriptInvokerHelper {

    private static final Object[] EMPTY_MAIN_ARGS = [];

    public static Object runGroovyScript(Class scriptClass, Binding binding) {
        Script script = createGroovyScript(scriptClass, binding);
        return script != null ? script.run() : null
    }

    public static Object runGroovyScript(Class scriptClass, String[] args) {
        Binding context = new Binding(args);
        Script script = createGroovyScript(scriptClass, context);
        return script != null ? script.run() : null
    }

    public static Object runScript(Class scriptClass) {
        Binding context = new Binding();
        Script script = createGroovyScript(scriptClass, context);
        return script != null ? script.run() : null
    }

    @Nullable
    public static Script createGroovyScript(Class scriptClass, Binding context) {
        Script script = null;
        try {

            GroovyObject groovyObject = null
            try {
                groovyObject = (GroovyObject) scriptClass.newInstance();
            } catch (ClassCastException ignore) {
            }

            final Object object = groovyObject == null ? scriptClass.newInstance() : groovyObject;

            if (object instanceof Script) {
                script = (Script) object;
            } else {
                // it could just be a class, so let's wrap it in a Script
                // wrapper; though the bindings will be ignored
                script = new Script() {
                    public Object run() {
                        Object args = getBinding().getVariables().get("args");
                        Object argsToPass = EMPTY_MAIN_ARGS;
                        if (args != null && args instanceof String[]) {
                            argsToPass = args;
                        }
                        object.invokeMethod("main", argsToPass);
                        return null;
                    }
                };
                Map variables = context.getVariables();
                MetaClass mc = getMetaClass(object);
                for (Object o : variables.entrySet()) {
                    Map.Entry entry = (Map.Entry) o;
                    String key = entry.getKey().toString();
                    // assume underscore variables are for the wrapper script
                    setPropertySafe(key.startsWith("_") ? script : object, mc, key, entry.getValue());
                }
            }

        } catch (Exception e) {
            throw new GroovyRuntimeException(
                    "Failed to create Script instance for class: " + scriptClass + ". Reason: " + e, e);
        }

        script.setBinding(context);
        return script;
    }

    private static void setPropertySafe(Object object, MetaClass mc, String key, Object value) {
        try {
            mc.setProperty(object, key, value);
        } catch (MissingPropertyException mpe) {
            // Ignore
        } catch (InvokerInvocationException iie) {
            // GROOVY-5802 IAE for missing properties with classes that extend List
            Throwable cause = iie.getCause();
            if (cause == null || !(cause instanceof IllegalArgumentException)) throw iie;
        }
    }

    public static MetaClass getMetaClass(Object object) {
        if (object instanceof GroovyObject)
            return ((GroovyObject) object).getMetaClass();
        else
            return ((MetaClassRegistryImpl) GroovySystem.getMetaClassRegistry()).getMetaClass(object);
    }

}
