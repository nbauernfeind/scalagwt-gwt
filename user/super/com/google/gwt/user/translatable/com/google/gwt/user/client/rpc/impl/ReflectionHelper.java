/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.user.client.rpc.impl;

import com.google.gwt.core.client.GwtScriptOnly;

/**
 * The script-mode equivalent for ReflectionHelper. This version throws
 * exceptions if used, because ReflectionHelper can only be used from bytecode.
 */
@GwtScriptOnly
public class ReflectionHelper {
  public static Class<?> loadClass(String name) throws Exception {
    throw new RuntimeException("ReflectionHelper can't be used from web mode.");
  }

  public static <T> T newInstance(Class<T> klass)
      throws Exception {
    throw new RuntimeException("ReflectionHelper can't be used from web mode.");
  }
}
