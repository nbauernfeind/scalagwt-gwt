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
package com.google.gwt.user.client.ui;

import java.util.ArrayList;

/**
 * A helper class for implementers of the
 * {@link com.google.gwt.user.client.ui.SourcesTableEvents} interface. This
 * subclass of {@link ArrayList} assumes that all objects added to it will be of
 * type {@link com.google.gwt.user.client.ui.TableListener}.
 */
@Deprecated
public class TableListenerCollection extends ArrayList<TableListener> {

  /**
   * Fires a cellClicked event to all listeners.
   * 
   * @param sender the widget sending the event
   * @param row the row of the cell being clicked
   * @param cell the index of the cell being clicked
   */
  @Deprecated
  public void fireCellClicked(SourcesTableEvents sender, int row, int cell) {
    for (TableListener listener : this) {
      listener.onCellClicked(sender, row, cell);
    }
  }
}
