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
package com.google.gwt.dev.jjs;

import com.google.gwt.dev.jjs.test.AutoboxTest;
import com.google.gwt.dev.jjs.test.BlankInterfaceTest;
import com.google.gwt.dev.jjs.test.ClassCastTest;
import com.google.gwt.dev.jjs.test.ClassObjectTest;
import com.google.gwt.dev.jjs.test.CompilerTest;
import com.google.gwt.dev.jjs.test.CoverageTest;
import com.google.gwt.dev.jjs.test.EnhancedForLoopTest;
import com.google.gwt.dev.jjs.test.EnumsTest;
import com.google.gwt.dev.jjs.test.HostedTest;
import com.google.gwt.dev.jjs.test.InnerClassTest;
import com.google.gwt.dev.jjs.test.InnerOuterSuperTest;
import com.google.gwt.dev.jjs.test.JsniConstructorTest;
import com.google.gwt.dev.jjs.test.MemberShadowingTest;
import com.google.gwt.dev.jjs.test.MethodBindTest;
import com.google.gwt.dev.jjs.test.MethodCallTest;
import com.google.gwt.dev.jjs.test.MethodInterfaceTest;
import com.google.gwt.dev.jjs.test.MiscellaneousTest;
import com.google.gwt.dev.jjs.test.NativeLongTest;
import com.google.gwt.dev.jjs.test.VarargsTest;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * The complete compiler suite.
 */
public class CompilerSuite {

  public static Test suite() {
    TestSuite suite = new TestSuite("Test for com.google.gwt.dev.jjs");

    // $JUnit-BEGIN$
    suite.addTestSuite(AutoboxTest.class);
    suite.addTestSuite(ClassCastTest.class);
    suite.addTestSuite(ClassObjectTest.class);
    suite.addTestSuite(CompilerTest.class);
    suite.addTestSuite(CoverageTest.class);
    suite.addTestSuite(EnhancedForLoopTest.class);
    suite.addTestSuite(EnumsTest.class);
    suite.addTestSuite(HostedTest.class);
    suite.addTestSuite(InnerClassTest.class);
    suite.addTestSuite(InnerOuterSuperTest.class);
    suite.addTestSuite(JsniConstructorTest.class);
    suite.addTestSuite(MemberShadowingTest.class);
    suite.addTestSuite(MethodBindTest.class);
    suite.addTestSuite(MethodCallTest.class);
    suite.addTestSuite(MethodInterfaceTest.class);
    suite.addTestSuite(MiscellaneousTest.class);
    suite.addTestSuite(NativeLongTest.class);
    suite.addTestSuite(BlankInterfaceTest.class);
    suite.addTestSuite(VarargsTest.class);
    // $JUnit-END$

    return suite;
  }

}
