/*
 * Copyright 2008 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.core.client.impl;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;

/**
 * Private implementation class for GWT core. This API is should not be
 * considered public or stable.
 */
public final class Impl {

  /**
   * Used by {@link #entry0(Object, Object)} to handle reentrancy.
   */
  private static int entryDepth = 0;
  private static int sNextHashId = 0;

  /**
   * This method should be used whenever GWT code is entered from a JS context
   * and there is no GWT code in the same module on the call stack. Examples
   * include event handlers, exported methods, and module initialization.
   * <p>
   * The GWT compiler and Development Mode will provide a module-scoped
   * variable, <code>$entry</code>, which is an alias for this method.
   * <p>
   * This method can be called reentrantly, which will simply delegate to the
   * function.
   * <p>
   * The function passed to this method will be invoked via
   * <code>Function.apply()</code> with the current <code>this</code> value and
   * the invocation arguments passed to <code>$entry</code>.
   * 
   * @param jsFunction a JS function to invoke, which is typically a JSNI
   *          reference to a static Java method
   * @return the value returned when <code>jsFunction</code> is invoked, or
   *         <code>undefined</code> if the UncaughtExceptionHandler catches an
   *         exception raised by <code>jsFunction</code>
   */
  public static native JavaScriptObject entry(JavaScriptObject jsFunction) /*-{
    return function() {
      try {
        return @com.google.gwt.core.client.impl.Impl::entry0(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)(jsFunction, this, arguments);
      } catch (e) {
        // This catch block is here to ensure that the finally block in entry0
        // will be executed correctly on IE6/7.  We can't put a catch Throwable
        // in entry0 because this would always cause the unhandled exception to
        // be wrapped in a JavaScriptException type.
        throw e;
      }
    };
  }-*/;

  /**
   * Gets an identity-based hash code on the passed-in Object by adding an
   * expando. This method should not be used with <code>null</code> or any
   * String. The former will crash and the later will produce unstable results
   * when called repeatedly with a String primitive.
   * <p>
   * The sequence of hashcodes generated by this method are a
   * monotonically-increasing sequence.
   */
  public static native int getHashCode(Object o) /*-{
    return o.$H || (o.$H = @com.google.gwt.core.client.impl.Impl::getNextHashId()());
  }-*/;

  public static native String getHostPageBaseURL() /*-{
    var s = $doc.location.href;

    // Pull off any hash.
    var i = s.indexOf('#');
    if (i != -1)
      s = s.substring(0, i);

    // Pull off any query string.
    i = s.indexOf('?');
    if (i != -1)
      s = s.substring(0, i);

    // Rip off everything after the last slash.
    i = s.lastIndexOf('/');
    if (i != -1)
      s = s.substring(0, i);

    // Ensure a final slash if non-empty.
    return s.length > 0 ? s + "/" : "";
  }-*/;

  public static native String getModuleBaseURL() /*-{
    return $moduleBase;
  }-*/;

  public static native String getModuleName() /*-{
    return $moduleName;
  }-*/;

  /**
   * Returns the obfuscated name of members in the compiled output. This is a
   * thin wrapper around JNameOf AST nodes and is therefore meaningless to
   * implement in Development Mode.
   * 
   * @param jsniIdent a string literal specifying a type, field, or method. Raw
   *          type names may also be used to obtain the name of the type's seed
   *          function.
   * @return the name by which the named member can be accessed at runtime, or
   *         <code>null</code> if the requested member has been pruned from the
   *         output.
   * @see com.google.gwt.core.client.impl.ArtificialRescue
   */
  public static String getNameOf(String jsniIdent) {
    /*
     * In Production Mode, the compiler directly replaces calls to this method
     * with a string literal expression.
     */
    assert !GWT.isScript() : "ReplaceRebinds failed to replace this method";
    throw new UnsupportedOperationException(
        "Impl.getNameOf() is unimplemented in Development Mode");
  }

  public static native String getPermutationStrongName() /*-{
    return $strongName;
  }-*/;

  /**
   * Indicates if <code>$entry</code> has been called.
   */
  public static boolean isEntryOnStack() {
    return entryDepth > 0;
  }

  /**
   * Indicates if <code>$entry</code> is present on the stack more than once.
   */
  public static boolean isNestedEntry() {
    return entryDepth > 1;
  }

  /**
   * Implicitly called by JavaToJavaScriptCompiler.findEntryPoints().
   */
  public static native JavaScriptObject registerEntry() /*-{
    if (@com.google.gwt.core.client.GWT::isScript()()) {
      // Assignment to $entry is done by the compiler
      return @com.google.gwt.core.client.impl.Impl::entry(Lcom/google/gwt/core/client/JavaScriptObject;);
    } else {
      // But we have to do in in Development Mode
      return $entry = @com.google.gwt.core.client.impl.Impl::entry(Lcom/google/gwt/core/client/JavaScriptObject;);
    }
  }-*/;

  private static native Object apply(Object jsFunction, Object thisObj,
      Object arguments) /*-{
    if (@com.google.gwt.core.client.GWT::isScript()()) {
      return jsFunction.apply(thisObj, arguments);
    } else {
      var _ = jsFunction.apply(thisObj, arguments);
      if (_ != null) {
        // Wrap for Development Mode
        _ = Object(_);
      }
      return _;
    }
  }-*/;

  /**
   * Called by ModuleSpace in Development Mode when running onModuleLoads.
   */
  private static boolean enter() {
    assert entryDepth >= 0 : "Negative entryDepth value at entry " + entryDepth;

    // We want to disable some actions in the reentrant case
    if (entryDepth++ == 0) {
      SchedulerImpl.INSTANCE.flushEntryCommands();
      return true;
    }
    return false;
  }

  /**
   * Implements {@link #entry(JavaScriptObject)}.
   */
  @SuppressWarnings("unused")
  private static Object entry0(Object jsFunction, Object thisObj,
      Object arguments) throws Throwable {
    boolean initialEntry = enter();

    try {
      /*
       * Always invoke the UCE if we have one so that the exception never
       * percolates up to the browser's event loop, even in a reentrant
       * situation.
       */
      if (GWT.getUncaughtExceptionHandler() != null) {
        /*
         * This try block is guarded by the if statement so that we don't molest
         * the exception object traveling up the stack unless we're capable of
         * doing something useful with it.
         */
        try {
          return apply(jsFunction, thisObj, arguments);
        } catch (Throwable t) {
          GWT.getUncaughtExceptionHandler().onUncaughtException(t);
          return undefined();
        }
      } else {
        // Can't handle any exceptions, let them percolate normally
        return apply(jsFunction, thisObj, arguments);
      }

      /*
       * DO NOT ADD catch(Throwable t) here, it would always wrap the thrown
       * value. Instead, entry() has a general catch-all block.
       */
    } finally {
      exit(initialEntry);
    }
  }

  /**
   * Called by ModuleSpace in Development Mode when running onModuleLoads.
   */
  private static void exit(boolean initialEntry) {
    if (initialEntry) {
      SchedulerImpl.INSTANCE.flushFinallyCommands();
    }

    // Decrement after we call flush
    entryDepth--;
    assert entryDepth >= 0 : "Negative entryDepth value at exit " + entryDepth;
    if (initialEntry) {
      assert entryDepth == 0 : "Depth not 0" + entryDepth;
    }
  }

  /**
   * Called from JSNI. Do not change this implementation without updating:
   * <ul>
   * <li>{@link com.google.gwt.user.client.rpc.impl.SerializerBase}</li>
   * </ul>
   */
  @SuppressWarnings("unused")
  private static int getNextHashId() {
    return ++sNextHashId;
  }

  private static native Object undefined() /*-{
    // Intentionally not returning a value
    return;
  }-*/;
}
