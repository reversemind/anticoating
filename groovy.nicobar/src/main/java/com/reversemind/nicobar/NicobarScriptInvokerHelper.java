package com.reversemind.nicobar;

import groovy.lang.*;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.codehaus.groovy.runtime.InvokerInvocationException;

import javax.annotation.Nullable;
import java.util.Map;

/**
 *
 */
public class NicobarScriptInvokerHelper extends InvokerHelper {

    private static final Object[] EMPTY_MAIN_ARGS = new Object[]{new String[0]};

    @Nullable
    @Override
    public static Script createScript(Class scriptClass, Binding context) throws IllegalAccessException, InstantiationException {
        Script script = null;
        try{
            script = createDefaultScript(scriptClass, context);
        }catch (ClassCastException ignore){
            try{
                final Object object = scriptClass.newInstance();
                script = new Script() {
                    public Object run() {
                        // pass throw bindings
                        Object args = new Binding().getVariables().get("args");

                        Object argsToPass = EMPTY_MAIN_ARGS;
                        if (args != null && args instanceof String[]) {
                            argsToPass = args;
                        }
                        object.invokeMethod("main", argsToPass);
                        return null;
                    }
                };
            } catch (Exception e) {
                throw new GroovyRuntimeException(
                        "Failed to create Script instance for class: "
                                + scriptClass + ". Reason: " + e, e);
            }

        }

        return script;
    }

    private static Script createDefaultScript(Class scriptClass, Binding context) {{
        Script script = null;
        // for empty scripts
        if (scriptClass == null) {
            script = new Script() {
                public Object run() {
                    return null;
                }
            };
        } else {
            try {
                final GroovyObject object = (GroovyObject) scriptClass.newInstance();
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
                        "Failed to create Script instance for class: "
                                + scriptClass + ". Reason: " + e, e);
            }
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

}
