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
package com.google.gwt.sample.showcase.client.content.cell;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.i18n.client.Constants;
import com.google.gwt.sample.showcase.client.ContentWidget;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseData;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseRaw;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseSource;
import com.google.gwt.sample.showcase.client.content.cell.ContactDatabase.ContactInfo;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.CellTree;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.MultiSelectionModel;
import com.google.gwt.view.client.SelectionModel;
import com.google.gwt.view.client.SelectionModel.SelectionChangeEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Example file.
 */
@ShowcaseRaw({
    "ContactDatabase.java", "ContactTreeViewModel.java", "CwCellTree.ui.xml"})
public class CwCellTree extends ContentWidget {

  /**
   * The UiBinder interface used by this example.
   */
  @ShowcaseSource
  interface Binder extends UiBinder<Widget, CwCellTree> {
  }

  /**
   * The constants used in this Content Widget.
   */
  @ShowcaseSource
  public static interface CwConstants
      extends Constants, ContentWidget.CwConstants {
    String cwCellTreeDescription();

    String cwCellTreeName();
  }

  /**
   * The CellTree.
   */
  @ShowcaseData
  @UiField(provided = true)
  CellTree cellTree;

  /**
   * The label that shows selected names.
   */
  @ShowcaseData
  @UiField
  Label selectedLabel;

  /**
   * An instance of the constants.
   */
  @ShowcaseData
  private CwConstants constants;

  /**
   * Constructor.
   *
   * @param constants the constants
   */
  public CwCellTree(CwConstants constants) {
    super(constants);
    this.constants = constants;
    registerSource("ContactDatabase.java");
    registerSource("ContactTreeViewModel.java");
    registerSource("CwCellTree.ui.xml");
  }

  @Override
  public String getDescription() {
    return constants.cwCellTreeDescription();
  }

  @Override
  public String getName() {
    return constants.cwCellTreeName();
  }

  @Override
  public boolean hasStyle() {
    return false;
  }

  /**
   * Initialize this example.
   */
  @ShowcaseSource
  @Override
  public Widget onInitialize() {
    final MultiSelectionModel<ContactInfo> selectionModel = new MultiSelectionModel<ContactInfo>();
    selectionModel.setKeyProvider(ContactDatabase.ContactInfo.KEY_PROVIDER);
    selectionModel.addSelectionChangeHandler(
        new SelectionModel.SelectionChangeHandler() {
          public void onSelectionChange(SelectionChangeEvent event) {
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            List<ContactInfo> selected = new ArrayList<ContactInfo>(
                selectionModel.getSelectedSet());
            Collections.sort(selected);
            for (ContactInfo value : selected) {
              if (first) {
                first = false;
              } else {
                sb.append(", ");
              }
              sb.append(value.getFullName());
            }
            selectedLabel.setText(sb.toString());
          }
        });

    cellTree = new CellTree(new ContactTreeViewModel(selectionModel), null);
    cellTree.setAnimationEnabled(true);

    // Create the UiBinder.
    Binder uiBinder = GWT.create(Binder.class);
    Widget widget = uiBinder.createAndBindUi(this);
    return widget;
  }

  @Override
  protected void asyncOnInitialize(final AsyncCallback<Widget> callback) {
    GWT.runAsync(CwCellTree.class, new RunAsyncCallback() {

      public void onFailure(Throwable caught) {
        callback.onFailure(caught);
      }

      public void onSuccess() {
        callback.onSuccess(onInitialize());
      }
    });
  }

  @Override
  protected void setRunAsyncPrefetches() {
    prefetchCellWidgets();
  }
}
