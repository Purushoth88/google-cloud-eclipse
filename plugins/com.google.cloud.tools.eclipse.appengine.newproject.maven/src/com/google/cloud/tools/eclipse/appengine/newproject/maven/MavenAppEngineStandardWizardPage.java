package com.google.cloud.tools.eclipse.appengine.newproject.maven;

import com.google.cloud.tools.eclipse.appengine.newproject.AppEngineProjectIdValidator;
import com.google.cloud.tools.eclipse.appengine.newproject.JavaPackageValidator;
import com.google.cloud.tools.eclipse.appengine.ui.AppEngineImages;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import java.text.MessageFormat;

/**
 * UI to collect all information necessary to create a new Maven-based App Engine Standard Java
 * project.
 */
public class MavenAppEngineStandardWizardPage extends WizardPage implements IWizardPage {

  private String defaultVersion = "0.1.0-SNAPSHOT";

  private Button useDefaults;
  private Text locationField;
  private Button locationBrowseButton;
  private Text groupIdField;
  private Text artifactIdField;
  private Text versionField;
  private Text javaPackageField;
  private Text projectIdField;
  
  public MavenAppEngineStandardWizardPage() {
    super("basicNewProjectPage"); //$NON-NLS-1$
    setTitle("Maven-based App Engine Standard Project");
    setDescription("Create new Maven-based App Engine Standard Project");
    setImageDescriptor(AppEngineImages.googleCloudPlatform(32));

    setPageComplete(false);
  }

  @Override
  public void createControl(Composite parent) {
    Composite container = new Composite(parent, SWT.NONE);
    container.setFont(parent.getFont());
    GridLayoutFactory.swtDefaults().numColumns(2).applyTo(container);

    createLocationArea(container);
    createMavenCoordinatesArea(container);
    createAppEngineProjectDetailsArea(container);

    setControl(container);
  }

  /** Create UI for specifying the generated location area */
  private void createLocationArea(Composite container) {
    ModifyListener pageValidator = new PageValidator();
    
    Group locationGroup = new Group(container, SWT.NONE);
    locationGroup.setFont(container.getFont());
    locationGroup.setText("Location");
    GridDataFactory.fillDefaults().span(2, 1).applyTo(locationGroup);
    GridLayoutFactory.swtDefaults().numColumns(3).applyTo(locationGroup);

    useDefaults = new Button(locationGroup, SWT.CHECK);
    useDefaults.setFont(container.getFont());
    GridDataFactory.defaultsFor(useDefaults).span(3, 1).applyTo(useDefaults);
    useDefaults.setText("Create project in workspace");
    useDefaults.setSelection(true);

    Label locationLabel = new Label(locationGroup, SWT.NONE);
    locationLabel.setText("Location:");
    locationLabel.setFont(container.getFont());
    locationLabel
        .setToolTipText("This location will contain the directory created for the project");

    locationField = new Text(locationGroup, SWT.BORDER);
    locationField.setFont(container.getFont());
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false)
        .applyTo(locationField);
    locationField.addModifyListener(pageValidator);
    locationField.setEnabled(false);

    locationBrowseButton = new Button(locationGroup, SWT.PUSH);
    locationBrowseButton.setFont(container.getFont());
    locationBrowseButton.setText("Browse");
    locationBrowseButton.setEnabled(false);
    locationBrowseButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        openLocationDialog();
      }
    });
    useDefaults.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        locationField.setEnabled(!useDefaults.getSelection());
        locationBrowseButton.setEnabled(!useDefaults.getSelection());
        checkPageComplete();
      }
    });
  }

  /** Create UI for specifying desired Maven Coordinates */
  private void createMavenCoordinatesArea(Composite container) {
    ModifyListener pageValidator = new PageValidator();

    Group mavenCoordinatesGroup = new Group(container, SWT.NONE);
    mavenCoordinatesGroup.setFont(container.getFont());
    mavenCoordinatesGroup.setText("Maven project coordinates");
    GridDataFactory.defaultsFor(mavenCoordinatesGroup).span(2, 1).applyTo(mavenCoordinatesGroup);
    GridLayoutFactory.swtDefaults().numColumns(2).applyTo(mavenCoordinatesGroup);

    Label groupIdLabel = new Label(mavenCoordinatesGroup, SWT.NONE);
    groupIdLabel.setFont(container.getFont());
    groupIdLabel.setText("Group Id:"); //$NON-NLS-1$
    groupIdField = new Text(mavenCoordinatesGroup, SWT.BORDER);
    groupIdField.setFont(container.getFont());
    GridDataFactory.defaultsFor(groupIdField).align(SWT.FILL, SWT.CENTER).applyTo(groupIdField);
    groupIdField.addModifyListener(pageValidator);

    Label artifactIdLabel = new Label(mavenCoordinatesGroup, SWT.NONE);
    artifactIdLabel.setFont(container.getFont());
    artifactIdLabel.setText("Artifact Id:"); //$NON-NLS-1$
    artifactIdField = new Text(mavenCoordinatesGroup, SWT.BORDER);
    artifactIdField.setFont(container.getFont());
    GridDataFactory.defaultsFor(artifactIdField).align(SWT.FILL, SWT.CENTER)
        .applyTo(artifactIdField);
    artifactIdField.addModifyListener(pageValidator);

    Label versionLabel = new Label(mavenCoordinatesGroup, SWT.NONE);
    versionLabel.setFont(container.getFont());
    versionLabel.setText("Version:"); //$NON-NLS-1$
    versionField = new Text(mavenCoordinatesGroup, SWT.BORDER);
    versionField.setFont(container.getFont());
    versionField.setText(defaultVersion);
    GridDataFactory.defaultsFor(versionField).align(SWT.FILL, SWT.CENTER).applyTo(versionField);
    versionField.addModifyListener(pageValidator);
  }

  /** Create UI for specifying App Engine project details */
  private void createAppEngineProjectDetailsArea(Composite container) {
    ModifyListener pageValidator = new PageValidator();

    // Java package name
    Label packageNameLabel = new Label(container, SWT.NONE);
    packageNameLabel.setFont(container.getFont());
    packageNameLabel.setText("Java package:");
    javaPackageField = new Text(container, SWT.BORDER);
    javaPackageField.setFont(container.getFont());
    GridData javaPackagePosition = new GridData(GridData.FILL_HORIZONTAL);
    javaPackagePosition.horizontalSpan = 2;
    javaPackageField.setLayoutData(javaPackagePosition);
    javaPackageField.addModifyListener(pageValidator);

    // App Engine Project ID
    Label projectIdLabel = new Label(container, SWT.NONE);
    projectIdLabel.setFont(container.getFont());
    projectIdLabel.setText("App Engine Project ID: (optional)");
    projectIdField = new Text(container, SWT.BORDER);
    projectIdField.setFont(container.getFont());
    GridData projectIdPosition = new GridData(GridData.FILL_HORIZONTAL);
    projectIdPosition.horizontalSpan = 2;
    projectIdField.setLayoutData(projectIdPosition);
    projectIdField.addModifyListener(pageValidator);
  }

  protected void openLocationDialog() {
    DirectoryDialog dialog = new DirectoryDialog(getShell());
    dialog.setText("Please select the location to contain generated project");
    String location = dialog.open();
    if (location != null) {
      locationField.setText(location);
      checkPageComplete();
    }
  }

  protected void checkPageComplete() {
    setPageComplete(validatePage());
  }

  /**
   * Validate and report on the contents of this page
   * 
   * @return true if valid, false if there is a problem
   */
  public boolean validatePage() {
    setMessage(null);
    setErrorMessage(null);
    
    // order here should match order of the UI fields

    String location = locationField.getText().trim();
    if (!useDefaults() && location.isEmpty()) {
      setMessage("Please provide a location", INFORMATION);
      return false;
    }

    if (!validateMavenSettings()) {
      return false;
    }
    if (!validateGeneratedProjectLocation()) {
      return false;
    }
    if (!validateAppEngineProjectDetails()) {
      return false;
    }

    return true;
  }

  /**
   * Check that we won't overwrite an existing location. Expects a valid Maven Artifact ID.
   */
  private boolean validateGeneratedProjectLocation() {
    String artifactId = getArtifactId();
    // assert !artifactId.isEmpty()
    IPath path = getLocationPath().append(artifactId);
    if (path.toFile().exists()) {
      setErrorMessage(MessageFormat.format("Location already exists: {0}.", path));
      return false;
    }
    return true;
  }

  private boolean validateMavenSettings() {
    String groupId = getGroupId();
    if (groupId.isEmpty()) {
      setMessage("Please provide Maven Group ID.", INFORMATION);
      return false;
    } else if (!MavenCoordinatesValidator.validateGroupId(groupId)) {
      setErrorMessage(MessageFormat.format("Illegal Maven Group ID: {0}.", groupId));
      return false;
    }
    String artifactId = getArtifactId();
    if (artifactId.isEmpty()) {
      setMessage("Please provide Maven Artifact ID.", INFORMATION);
      return false;
    } else if (!MavenCoordinatesValidator.validateArtifactId(artifactId)) {
      setErrorMessage("Illegal Maven Artifact ID: " + artifactId);
      return false;
    }
    String version = getVersion();
    if (version.isEmpty()) {
      setMessage("Please provide Maven artifact version.", INFORMATION);
      return false;
    } else if (!MavenCoordinatesValidator.validateVersion(version)) {
      setErrorMessage("Illegal Maven version: " + version);
      return false;
    }
    return true;
  }

  private boolean validateAppEngineProjectDetails() {
    String packageName = getPackageName();
    IStatus status = JavaPackageValidator.validate(packageName);
    if (!status.isOK()) {
      String details = status.getMessage() == null ? packageName : status.getMessage();
      String message = MessageFormat.format("Illegal Java package name: {0}", details);
      setErrorMessage(message);
      return false;
    }

    String projectId = getAppEngineProjectId();
    if (!AppEngineProjectIdValidator.validate(projectId)) {
      setErrorMessage(MessageFormat.format("Illegal App Engine Project ID: {0}.", projectId));
      return false;
    }
    return true;
  }
  
  /** Return the Maven group for the project */
  public String getGroupId() {
    return groupIdField.getText().trim();
  }

  /** Return the Maven artifact for the project */
  public String getArtifactId() {
    return artifactIdField.getText().trim();
  }

  /** Return the Maven version for the project */
  public String getVersion() {
    return versionField.getText().trim();
  }

  /**
   * If true, projects are generated into the workspace, otherwise placed into a specified location.
   */
  public boolean useDefaults() {
    return useDefaults.getSelection();
  }

  /** Return the App Engine Project ID (if any) */
  public String getAppEngineProjectId() {
    return this.projectIdField.getText();
  }

  /** Return the package name for any example code */
  public String getPackageName() {
    return this.javaPackageField.getText();
  }

  /** Return the location where the project should be generated into */
  public IPath getLocationPath() {
    if (useDefaults()) {
      return ResourcesPlugin.getWorkspace().getRoot().getLocation();
    }
    return new Path(locationField.getText());
  }

  private final class PageValidator implements ModifyListener {
    @Override
    public void modifyText(ModifyEvent event) {
      checkPageComplete();
    }
  }
}
