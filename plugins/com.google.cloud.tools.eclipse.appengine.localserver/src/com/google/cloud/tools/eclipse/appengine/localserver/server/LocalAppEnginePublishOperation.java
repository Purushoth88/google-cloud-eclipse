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

package com.google.cloud.tools.eclipse.appengine.localserver.server;

import com.google.cloud.tools.eclipse.util.status.StatusUtil;
import com.google.common.collect.Lists;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jst.server.core.IJ2EEModule;
import org.eclipse.jst.server.core.IWebModule;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.model.IModuleResource;
import org.eclipse.wst.server.core.model.IModuleResourceDelta;
import org.eclipse.wst.server.core.model.PublishOperation;
import org.eclipse.wst.server.core.model.ServerBehaviourDelegate;
import org.eclipse.wst.server.core.util.PublishHelper;

/**
 * Handles the publishing operations for the App Engine development server.
 */
public class LocalAppEnginePublishOperation extends PublishOperation {
  private static final String PLUGIN_ID = LocalAppEnginePublishOperation.class.getName();

  /**
   * @throws {@link CoreException} if status list is not empty
   */
  private static void failOnError(List<IStatus> statusList) throws CoreException {
    if (statusList == null || statusList.isEmpty()) {
      return;
    }
    IStatus[] children = statusList.toArray(new IStatus[statusList.size()]);
    throw new CoreException(new MultiStatus(PLUGIN_ID, 0, children, "Error during publish operation", null));
  }

  private LocalAppEngineServerBehaviour server;
  /** The module path */
  private IModule[] module;
  private int kind;
  private int deltaKind;
  private PublishHelper helper;

  @Override
  public int getKind() {
    return REQUIRED;
  }

  @Override
  public int getOrder() {
    return 0;
  }

  /**
   * Construct the operation object to publish the specified module(s) to the
   * specified server.
   */
  public LocalAppEnginePublishOperation(LocalAppEngineServerBehaviour server, int kind, IModule[] modules,
      int deltaKind) {
    super("Publish to server", "Publish module to App Engine Development Server");
    this.server = server;
    this.kind = kind;
    this.deltaKind = deltaKind;
    IPath base = server.getRuntimeBaseDirectory();
    helper = new PublishHelper(base.toFile());

    if (modules != null) {
      this.module = Arrays.copyOf(modules, modules.length);
    } else {
      this.module = new IModule[0];
    }
  }

  @Override
  public void execute(IProgressMonitor monitor, IAdaptable info) throws CoreException {
    // Error out on currently-unhandled cases
    if (module.length > 2) {
      throw new CoreException(
          StatusUtil.error(getClass(), "Grandchild modules not currently handled"));
    }
    SubMonitor progress = SubMonitor.convert(monitor);
    List<IStatus> statusList = Lists.newArrayList();
    IPath deployPath = server.getModuleDeployDirectory(module[0]);
    if (module.length == 1) {
      publishDirectory(deployPath, statusList, progress);
    } else {
      // it's a child module
      // todo: we are not tracking file movements here, as might happen if the
      // the module is renamed
      IWebModule parentModule =
          (IWebModule) module[0].loadAdapter(IWebModule.class, progress.newChild(5));
      if (parentModule == null) {
        throw new CoreException(
            StatusUtil.error(getClass(), "Unhandled module type: " + module[0].getModuleType()));
      }
      IJ2EEModule childModule =
          (IJ2EEModule) module[1].loadAdapter(IJ2EEModule.class, progress.newChild(5));
      // child modules published as jars if not already zipped
      boolean isBinary = childModule != null && childModule.isBinary();
      String childLocation = parentModule.getURI(module[1]);
      if (childModule == null || isBinary || childLocation == null) {
        throw new CoreException(StatusUtil.error(getClass(),
            "Unhandled child module type: " + module[1].getModuleType()));
      }

      deployPath = deployPath.append(childLocation);
      if (!isBinary) {
        publishJar(deployPath, statusList, progress.newChild(10));
        // } else {
        // publishDirectory(deployPath, statusList, progress.newChild(10));
      }
    }
    failOnError(statusList);
    server.setModulePublishState2(module, IServer.PUBLISH_STATE_NONE);
  }

  /**
   * @param deployPath
   * @param statusList
   * @param newChild
   */
  private void publishJar(IPath path, List<IStatus> statusList, SubMonitor monitor) {
    // delete if needed
    if (kind == IServer.PUBLISH_CLEAN || deltaKind == ServerBehaviourDelegate.REMOVED) {
      File file = path.toFile();
      if (file.exists()) {
        IStatus[] status = PublishHelper.deleteDirectory(file, monitor.newChild(10));
        statusList.addAll(Arrays.asList(status));
      }
      // request for remove
      if (deltaKind == ServerBehaviourDelegate.REMOVED) {
        return;
      }
    }
    // republish or publish fully
    if (kind != IServer.PUBLISH_CLEAN && kind != IServer.PUBLISH_FULL) {
      IModuleResourceDelta[] deltas = server.getPublishedResourceDelta(module);
      if (deltas == null || deltas.length == 0) {
        // nothing to be done
        return;
      }
    }
    IModuleResource[] resources = server.getResources(module);
    IStatus[] publishStatus = helper.publishZip(resources, path, monitor.newChild(10));
    statusList.addAll(Arrays.asList(publishStatus));
  }

  /**
   * Publish module as directory.
   */
  private void publishDirectory(IPath path, List<IStatus> statusList, SubMonitor monitor) {
    // delete if needed
    if (kind == IServer.PUBLISH_CLEAN || deltaKind == ServerBehaviourDelegate.REMOVED) {
      File file = path.toFile();
      if (file.exists()) {
        IStatus[] status = PublishHelper.deleteDirectory(file, monitor.newChild(10));
        statusList.addAll(Arrays.asList(status));
      }
      // request for remove
      if (deltaKind == ServerBehaviourDelegate.REMOVED) {
        return;
      }
    }
    // republish or publish fully
    if (kind == IServer.PUBLISH_CLEAN || kind == IServer.PUBLISH_FULL) {
      IModuleResource[] resources = server.getResources(module);
      IStatus[] publishStatus = helper.publishFull(resources, path, monitor.newChild(10));
      statusList.addAll(Arrays.asList(publishStatus));
      return;
    }
    // publish changes only
    IModuleResourceDelta[] deltas = server.getPublishedResourceDelta(module);
    for (IModuleResourceDelta delta : deltas) {
      IStatus[] publishStatus = helper.publishDelta(delta, path, monitor.newChild(10));
      statusList.addAll(Arrays.asList(publishStatus));
    }
  }


}
