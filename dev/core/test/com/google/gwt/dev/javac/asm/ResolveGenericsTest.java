/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.dev.javac.asm;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.typeinfo.HookableTypeOracle;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JGenericType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JPackage;
import com.google.gwt.core.ext.typeinfo.JRealClassType;
import com.google.gwt.core.ext.typeinfo.JTypeParameter;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.asm.Opcodes;
import com.google.gwt.dev.asm.Type;
import com.google.gwt.dev.asm.signature.SignatureReader;
import com.google.gwt.dev.javac.Resolver;
import com.google.gwt.dev.javac.TypeOracleMediator;
import com.google.gwt.dev.javac.TypeOracleTestingUtils;
import com.google.gwt.dev.javac.TypeParameterLookup;
import com.google.gwt.dev.javac.asm.CollectClassData.ClassType;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.TypeVariable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests for {@link ResolveClassSignature} and
 * {@link ResolveMethodSignature}.
 */
public class ResolveGenericsTest extends AsmTestCase {
  
  public static class FailErrorTreeLogger extends TreeLogger {
    @Override
    public TreeLogger branch(com.google.gwt.core.ext.TreeLogger.Type type,
        String msg, Throwable caught, HelpInfo helpInfo) {
      if (type == TreeLogger.ERROR) {
        fail(msg);
      }
      return this;
    }

    @Override
    public boolean isLoggable(com.google.gwt.core.ext.TreeLogger.Type type) {
      return true;
    }

    @Override
    public void log(com.google.gwt.core.ext.TreeLogger.Type type, String msg,
        Throwable caught, HelpInfo helpInfo) {
      if (type == TreeLogger.ERROR) {
        fail(msg);
      }
    }
  }

  /**
   * An extension of JMethod which keeps the reflected Method around.
   */
  public static class ReflectedMethod extends JMethod {
    private Method method;

    public ReflectedMethod(JClassType type, String methodName,
        Map<Class<? extends Annotation>, Annotation> annotations,
        JTypeParameter[] typeParams, Method method) {
      super(type, methodName, annotations, typeParams);
      this.method = method;
    }
    
    public Method getMethod() {
      return method;
    }
  }

  public static class ResolverMockTypeOracle extends HookableTypeOracle {
    
    private Map<String, JRealClassType> binaryMapper;

    public Map<String, JRealClassType> getBinaryMapper() {
      ensureBinaryMapper();
      return binaryMapper;
    }
    
    @Override
    protected void addNewType(JRealClassType type) {
      super.addNewType(type);
      String name = type.getQualifiedBinaryName().replace('.', '/');
      ensureBinaryMapper();
      binaryMapper.put(name, type);
    }

    private void ensureBinaryMapper() {
      if (binaryMapper == null) {
        binaryMapper = new HashMap<String, JRealClassType>();
      }
    }
  }

  private static final TreeLogger failTreeLogger = new FailErrorTreeLogger();

  private static final String OUTER_CLASS_SIG
      = "<H:Lcom/google/gwt/dev/javac/asm/TestHandler;>Ljava/lang/Object;";
  private static final String OUTER_METHOD_SIG = "(TH;)V";

  private static final String OUTER1_CLASS_SIG
      = "<V:Ljava/lang/Object;>Lcom/google/gwt/dev/javac/asm/TestOuter0<"
        + "Lcom/google/gwt/dev/javac/asm/TestHandler1<TV;>;>;";
  private static final String OUTER1_METHOD_SIG
      = "(Lcom/google/gwt/dev/javac/asm/TestHandler1<TV;>;)V";

  private static final String OUTER2_CLASS_SIG
      = "Lcom/google/gwt/dev/javac/asm/TestOuter1<Ljava/lang/String;>;";
  private static final String OUTER2_METHOD_SIG
      = "(Lcom/google/gwt/dev/javac/asm/TestHandler1<Ljava/lang/String;>;)V";

  private ResolverMockTypeOracle oracle = new ResolverMockTypeOracle();

  @SuppressWarnings("unused")
  private JRealClassType testHandler;
  @SuppressWarnings("unused")
  private JRealClassType testHandler1;

  private JRealClassType testOuter0;
  private JRealClassType testOuter1;
  private JRealClassType testOuter2;

  private ReflectedMethod testOuter0dispatch;
  private ReflectedMethod testOuter1dispatch;
  private ReflectedMethod testOuter2dispatch;

  @SuppressWarnings("unused")
  private JRealClassType testType;

  private Resolver resolver = new Resolver() {
    public Map<String, JRealClassType> getBinaryMapper() {
      return oracle.getBinaryMapper();
    }
    
    public TypeOracle getTypeOracle() {
      return oracle;
    }
    
    public boolean resolveAnnotations(TreeLogger logger,
        List<CollectAnnotationData> annotations,
        Map<Class<? extends Annotation>, Annotation> declaredAnnotations) {
      return true;
    }

    public boolean resolveAnnotation(TreeLogger logger,
        CollectAnnotationData annotVisitor,
        Map<Class<? extends Annotation>, Annotation> declaredAnnotations) {
      return true;
    }

    public boolean resolveClass(TreeLogger logger, JRealClassType type) {
      return true;
    }
  };


  public ResolveGenericsTest() {
    TypeOracleMediator mediator = new TypeOracleMediator(oracle);
    TypeOracleTestingUtils.buildStandardTypeOracleWith(mediator,
        failTreeLogger);
    createUnresolvedClass(Object.class, null);
    createUnresolvedClass(String.class, null);
    testHandler = createUnresolvedClass(TestHandler.class, null);
    testHandler1 = createUnresolvedClass(TestHandler1.class, null);
    testOuter0 = createUnresolvedClass(TestOuter0.class, null);
    testType = createUnresolvedClass(TestOuter0.Type.class, testOuter0);
    testOuter1 = createUnresolvedClass(TestOuter1.class, null);
    testOuter2 = createUnresolvedClass(TestOuter2.class, null);
    testOuter0dispatch = createUnresolvedMethod(testOuter0, TestOuter0.class,
        "dispatch", TestHandler.class);
    testOuter1dispatch = createUnresolvedMethod(testOuter1, TestOuter1.class,
        "dispatch", TestHandler.class);
    testOuter2dispatch = createUnresolvedMethod(testOuter2, TestOuter2.class,
        "dispatch", TestHandler.class);
  }

  public void testOuter0Class() {
    resolveClassSignature(testOuter0, OUTER_CLASS_SIG);
    assertNotNull(testOuter0.getSuperclass());
    // TODO(jat): additional checks?
  }

  public void testOuter0Method() {
    resolveMethodSignature(testOuter0dispatch, OUTER_METHOD_SIG);
    // TODO(jat): meaningful tests besides no errors?
  }
  
  public void testOuter1Class() {
    resolveClassSignature(testOuter1, OUTER1_CLASS_SIG);
    JClassType superClass = testOuter1.getSuperclass();
    assertNotNull(superClass);
    assertNotNull(superClass.isParameterized());
    // TODO(jat): additional checks?
  }
  
  public void testOuter1Method() {
    resolveMethodSignature(testOuter1dispatch, OUTER1_METHOD_SIG);
    // TODO(jat): meaningful tests besides no errors?
  }

  public void testOuter2Class() {
    resolveClassSignature(testOuter2, OUTER2_CLASS_SIG);
    JClassType superClass = testOuter2.getSuperclass();
    assertNotNull(superClass);
    assertNotNull(superClass.isParameterized());
    // TODO(jat): additional checks?
  }

  public void testOuter2Method() {
    resolveMethodSignature(testOuter2dispatch, OUTER2_METHOD_SIG);
    // TODO(jat): meaningful tests besides no errors?
  }
  
  private JTypeParameter[] createTypeParams(TypeVariable<?>[] typeParams) {
    int n = typeParams.length;
    JTypeParameter[] params = new JTypeParameter[n];
    for (int i = 0; i < n; ++i) {
      params[i] = new JTypeParameter(typeParams[i].getName(), i);
    }
    return params;
  }

  private JRealClassType createUnresolvedClass(Class<?> clazz,
      JRealClassType enclosingType) {
    String pkgName = clazz.getPackage().getName();
    JPackage pkg = oracle.getOrCreatePackage(pkgName);
    TypeVariable<?>[] typeParams = clazz.getTypeParameters();
    JRealClassType type;
    int n = typeParams.length;
    String enclosingTypeName = null;
    if (enclosingType != null) {
      enclosingTypeName = enclosingType.getName();
    }
    if (n == 0) {
      type = new JRealClassType(oracle, pkg, enclosingTypeName,
        false, clazz.getSimpleName(), clazz.isInterface());
    } else {
      JTypeParameter[] params = createTypeParams(typeParams);
      type = new JGenericType(oracle, pkg, enclosingTypeName, false,
          clazz.getSimpleName(), clazz.isInterface(), params);
    }
    return type;
  }

  private ReflectedMethod createUnresolvedMethod(JClassType type, Class<?> clazz,
      String methodName, Class<?>... paramTypes) {
    Method method = null;
    try {
      method = clazz.getMethod(methodName, paramTypes);
    } catch (SecurityException e) {
      fail("Exception " + e + " creating method " + methodName + " on " + clazz);
    } catch (NoSuchMethodException e) {
      fail("Exception " + e + " creating method " + methodName + " on " + clazz);
    }
    JTypeParameter[] typeParams = createTypeParams(method.getTypeParameters());
    Map<Class<? extends Annotation>, Annotation> emptyMap
        = Collections.emptyMap();
    return new ReflectedMethod(type, methodName, emptyMap, typeParams, method);
  }

  private void resolveClassSignature(JRealClassType type, String signature) {
    Map<String, JRealClassType> binaryMapper = oracle.getBinaryMapper();
    TypeParameterLookup lookup = new TypeParameterLookup();
    lookup.pushEnclosingScopes(type);
    ResolveClassSignature classResolver = new ResolveClassSignature(resolver,
        binaryMapper, failTreeLogger, type, lookup);
    new SignatureReader(signature).accept(classResolver);
    classResolver.finish();
  }

  private void resolveMethodSignature(ReflectedMethod method,
      String signature) {
    TypeParameterLookup lookup = new TypeParameterLookup();
    lookup.pushEnclosingScopes(method.getEnclosingType());
    lookup.pushScope(method.getTypeParameters());
    int access = Opcodes.ACC_PUBLIC;
    String desc = Type.getMethodDescriptor(method.getMethod());
    CollectMethodData methodData = new CollectMethodData(ClassType.TopLevel,
        access, method.getName(), desc, signature, null);
    Class<?>[] paramTypes = method.getMethod().getParameterTypes();
    int n = paramTypes.length;
    Type[] argTypes = new Type[n];
    String[] argNames = new String[n];
    for (int i = 0; i < n; ++i) {
      argNames[i] = "arg" + i;
      argTypes[i] = Type.getType(paramTypes[i]);
    }
    ResolveMethodSignature methodResolver = new ResolveMethodSignature(resolver,
        failTreeLogger, method, lookup, true, methodData, argTypes, argNames);
    new SignatureReader(signature).accept(methodResolver);
    methodResolver.finish();
  }
}