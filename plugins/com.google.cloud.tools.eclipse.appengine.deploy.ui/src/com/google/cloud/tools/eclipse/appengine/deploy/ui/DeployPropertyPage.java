/*
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
 */

package com.google.cloud.tools.eclipse.appengine.deploy.ui;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.databinding.preference.PreferencePageSupport;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;

import com.google.cloud.tools.eclipse.appengine.facets.AppEngineFlexFacet;
import com.google.cloud.tools.eclipse.appengine.facets.AppEngineStandardFacet;
import com.google.cloud.tools.eclipse.appengine.login.IGoogleLoginService;
import com.google.cloud.tools.eclipse.util.AdapterUtil;

/**
 * Displays the App Engine deployment page for the selected project in the property page dialog.
 * The contents of the App Engine deployment page vary depending on if the selected project
 * has the App Engine Standard facet, the App Engine flex facet, or no App Engine facet.
 */
public class DeployPropertyPage extends PropertyPage {

  private DeployPreferencesPanel content;
  private boolean doesProjectHaveAppEngineFacet;
  private boolean isStandardPanel;
  private IFacetedProject facetedProject = null;
  private static final Logger logger = Logger.getLogger(DeployPropertyPage.class.getName());

  @Override
  protected Control createContents(Composite parent) {
    Composite container = new Composite(parent, SWT.NONE);
    IProject project = AdapterUtil.adapt(getElement(), IProject.class);
    //IFacetedProject facetedProject = null;

    try {
      facetedProject = ProjectFacetsManager.create(project);
    } catch (CoreException ex) {
      logger.log(Level.WARNING, ex.getMessage());
      return container;
    }

    content = getPreferencesPanel(project, facetedProject, container);
    if (content == null) {
      return container;
    }
    isStandardPanel = content instanceof StandardDeployPreferencesPanel;
    initializeListeners(facetedProject, container, content instanceof StandardDeployPreferencesPanel);

    GridDataFactory.fillDefaults().grab(true, false).applyTo(content);
    GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
    GridLayoutFactory.fillDefaults().generateLayout(container);

    PreferencePageSupport.create(this, content.getDataBindingContext());
    return content;
  }

  private Runnable getLayoutChangedHandler() {
    return new Runnable() {

      @Override
      public void run() {
        // resize the page to work around https://bugs.eclipse.org/bugs/show_bug.cgi?id=265237
        Composite parent = content.getParent();
        while (parent != null) {
          if (parent instanceof ScrolledComposite) {
            ScrolledComposite scrolledComposite = (ScrolledComposite) parent;
            scrolledComposite.setMinSize(content.getParent().computeSize(SWT.DEFAULT, SWT.DEFAULT));
            content.layout();
            return;
          }
          parent = parent.getParent();
        }
      }
    };
  }

  @Override
  public boolean performOk() {
    // Content can be null if Properties Page is opened and App Engine facets are uninstalled
    if (isValid()) {
      if (content == null) {
        return true;
      } else {
        return content.savePreferences();
      }
    }
    return false;
  }

  @Override
  protected void performDefaults() {
    content.resetToDefaults();
    super.performDefaults();
  }

  @Override
  public void dispose() {
    if (content != null) {
      // Content can be null if Properties Page is opened and App Engine facets are uninstalled
      content.dispose();
    }
    super.dispose();
  }

  @Override
  public void setErrorMessage(String newMessage) {
    if (newMessage != null) {
      System.out.println(newMessage);
    } else {
      System.out.println("newMessage == null");
    }
    if (!doesProjectHaveAppEngineFacet) {
      return;
    }

    super.setErrorMessage(newMessage);
    if (getContainer() != null) {
      getContainer().updateMessage();
    }
  }

  @Override
  public void setMessage(String newMessage, int newType) {
    if (!doesProjectHaveAppEngineFacet) {
      return;
    }

    super.setMessage(newMessage, newType);
    if (getContainer() != null) {
      getContainer().updateMessage();
    }
  }

  private DeployPreferencesPanel getPreferencesPanel(IProject project, IFacetedProject facetedProject, Composite container) {
    IGoogleLoginService loginService =
        PlatformUI.getWorkbench().getService(IGoogleLoginService.class);

    if (AppEngineStandardFacet.hasAppEngineFacet(facetedProject)) {
      setTitle(Messages.getString("standard.page.title"));
      return new StandardDeployPreferencesPanel(
          container, project, loginService, getLayoutChangedHandler(), false /* requireValues */);
    } else if (AppEngineFlexFacet.hasAppEngineFacet(facetedProject)) {
      setTitle(Messages.getString("flex.page.title"));
      return new FlexDeployPreferencesPanel(container, project);
    } else {
      logger.log(Level.WARNING, "App Engine Deployment property page is only visible if project contains an App Engine facet");
      return null;
    }
  }

  private void initializeListeners(final IFacetedProject facetedProject, Composite container, final boolean isStandardPanel) {
    
    
    
    
    container.addListener(SWT.Paint, new Listener() {

      @Override
      public void handleEvent(Event event) {
        if (isStandardPanel && !AppEngineStandardFacet.hasAppEngineFacet(facetedProject)) {
          IProjectFacet projectFacet = ProjectFacetsManager.getProjectFacet(AppEngineStandardFacet.ID);
          //setErrorMessage(Messages.getString("invalid.deploy.page.state", projectFacet.getLabel()));
          //content.setAllowSave(false);
          doesProjectHaveAppEngineFacet = false;
        } else if (!isStandardPanel && !AppEngineFlexFacet.hasAppEngineFacet(facetedProject)) {
          IProjectFacet projectFacet = ProjectFacetsManager.getProjectFacet(AppEngineFlexFacet.ID);
          //setErrorMessage(Messages.getString("invalid.deploy.page.state", projectFacet.getLabel()));
          //content.setAllowSave(false);
          doesProjectHaveAppEngineFacet = false;
        } else {
          //setErrorMessage(null);
          //content.setAllowSave(true);
          //doesProjectHaveAppEngineFacet = true;
          ///System.out.println("error message set to null");
        }
      }

    });
  }

  @Override
  public void setVisible(boolean visible) {
    super.setVisible(visible);
    doSomething();
  }

  /**
   * Check to see if the project associated with this Property dialog,
   * still has an App Engine facet. If it does, do nothing. If it doesn't
   * display appropriate error message and don't allow the preferences to be saved.
   */
  private void doSomething() {
    // allow saving
    // allow error message from dialog validation checks
    // print out error messages
    
    if (isStandardPanel && !AppEngineStandardFacet.hasAppEngineFacet(facetedProject)) {
      IProjectFacet projectFacet = ProjectFacetsManager.getProjectFacet(AppEngineStandardFacet.ID);
      setErrorMessage(Messages.getString("invalid.deploy.page.state", projectFacet.getLabel()));
      content.setAllowSave(false);
      doesProjectHaveAppEngineFacet = false;
    } else if (!isStandardPanel && !AppEngineFlexFacet.hasAppEngineFacet(facetedProject)) {
      IProjectFacet projectFacet = ProjectFacetsManager.getProjectFacet(AppEngineFlexFacet.ID);
      setErrorMessage(Messages.getString("invalid.deploy.page.state", projectFacet.getLabel()));
      content.setAllowSave(false);
      doesProjectHaveAppEngineFacet = false;
    } else {
      setErrorMessage(null);
      content.setAllowSave(true);
      doesProjectHaveAppEngineFacet = true;
      System.out.println("error message set to null");
    }
    
  }
 
}
