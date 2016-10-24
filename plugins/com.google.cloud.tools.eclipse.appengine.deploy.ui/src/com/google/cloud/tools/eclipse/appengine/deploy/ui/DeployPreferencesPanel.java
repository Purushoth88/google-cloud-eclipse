/*******************************************************************************
 * Copyright 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

package com.google.cloud.tools.eclipse.appengine.deploy.ui;

import com.google.cloud.tools.eclipse.appengine.facets.AppEngineFlexFacet;
import com.google.cloud.tools.eclipse.appengine.facets.AppEngineStandardFacet;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.FormColors;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.wst.common.project.facet.core.FacetedProjectFramework;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.events.IFacetedProjectEvent;
import org.eclipse.wst.common.project.facet.core.events.IFacetedProjectListener;

abstract class DeployPreferencesPanel extends Composite {
  private FormToolkit formToolkit;
  private IFacetedProjectListener facetListener;

  DeployPreferencesPanel(Composite parent, int style) {
    super(parent, style);

    initializeFormToolkit();
    initializeListeners();
  }

  public abstract DataBindingContext getDataBindingContext();

  public abstract void resetToDefaults();

  public abstract boolean savePreferences();

  @Override
  public void dispose() {
    if (formToolkit != null) {
      formToolkit.dispose();
    }
    FacetedProjectFramework.removeListener(facetListener);
    super.dispose();
  }

  protected FormToolkit getFormToolkit() {
    return formToolkit;
  }

  private void initializeFormToolkit() {
    FormColors colors = new FormColors(getDisplay());
    colors.setBackground(null);
    colors.setForeground(null);
    formToolkit = new FormToolkit(colors);
  }

  private void initializeListeners() {
    final boolean isStandardPanel = this instanceof StandardDeployPreferencesPanel;

    facetListener = new IFacetedProjectListener()
    {
        @Override
        public void handleEvent(IFacetedProjectEvent event) {
          IFacetedProject project = event.getProject();
          if (isStandardPanel) {
            if (!AppEngineStandardFacet.hasAppEngineFacet(project)) {
              disablePageContents();
            }
          } else {
            if (!AppEngineFlexFacet.hasAppEngineFacet(project)) {
              disablePageContents();
            }
          }
        }
    };

    FacetedProjectFramework.addListener(facetListener,
        IFacetedProjectEvent.Type.POST_INSTALL,
        IFacetedProjectEvent.Type.POST_UNINSTALL );
  }

  private void disablePageContents() {
    //this.setEnabled(false);
    // disable contents when receiver gets focus
    // show message
  }

}
