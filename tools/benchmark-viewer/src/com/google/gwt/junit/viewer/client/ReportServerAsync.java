/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.junit.viewer.client;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * The asynchronous interface for ReportServer.
 *
 * @see ReportServer
 */
public interface ReportServerAsync {

  public void getReport(String reportId, AsyncCallback callback);

  public void getReportSummaries(AsyncCallback callback);
}
