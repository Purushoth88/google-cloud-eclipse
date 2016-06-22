package com.google.cloud.tools.eclipse.appengine.localserver.server;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jst.server.core.IWebModule;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IModuleType;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.internal.facets.FacetUtil;
import org.eclipse.wst.server.core.model.ServerDelegate;

@SuppressWarnings("restriction") // For FacetUtil
public class LocalAppEngineServerDelegate extends ServerDelegate {
	private static final String SERVLET_MODULE_FACET = "jst.web";
	private static final String ATTR_CLOUD_SDK_SERVER_MODULES = "cloudsdk-server-modules-list";

	public static LocalAppEngineServerDelegate getAppEngineServer(IServer server) {
		LocalAppEngineServerDelegate appEngineServer = server.getAdapter(LocalAppEngineServerDelegate.class);
		if (appEngineServer == null) {
			appEngineServer = (LocalAppEngineServerDelegate) server.loadAdapter(LocalAppEngineServerDelegate.class,
					new NullProgressMonitor());
		}
		return appEngineServer;
	}

	@Override
	public IStatus canModifyModules(IModule[] add, IModule[] remove) {
		if (add != null) {
			for (IModule module : add) {
				if (module.getProject() != null) {
					IStatus status = FacetUtil.verifyFacets(module.getProject(), getServer());
					if (status != null && !status.isOK()) {
						return status;
					}
				}
			}
		}
		return Status.OK_STATUS;
	}

	@Override
	public IModule[] getChildModules(IModule[] module) {
		if (module[0] != null && module[0].getModuleType() != null) {
			IModule thisModule = module[module.length - 1];
			IModuleType moduleType = thisModule.getModuleType();
			if (moduleType != null && SERVLET_MODULE_FACET.equals(moduleType.getId())) { //$NON-NLS-1$
				IWebModule webModule = (IWebModule) thisModule.loadAdapter(IWebModule.class, null);
				if (webModule != null) {
					IModule[] modules = webModule.getModules();
					return modules;
				}
			}
		}
		return new IModule[0];
	}

	@Override
	public IModule[] getRootModules(IModule module) throws CoreException {
		IStatus status = canModifyModules(new IModule[] { module }, null);
		if (status != null && !status.isOK()) {
			throw new CoreException(status);
		}
		return new IModule[] { module };
	}

	@SuppressWarnings("unchecked")
	@Override
	public void modifyModules(IModule[] add, IModule[] remove, IProgressMonitor monitor)
			throws CoreException {
		List<String> modules = this.getAttribute(ATTR_CLOUD_SDK_SERVER_MODULES, (List<String>) null);

		if (add != null && add.length > 0) {
			// TODO: ensure modules have same Project ID
			// throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, 0,
			// "This server instance cannot run more than one application", null));
			if (modules == null) {
				modules = new ArrayList<>();
			}
			for (int i = 0; i < add.length; i++) {
				if (!modules.contains(add[i].getId())) {
					modules.add(add[i].getId());
				}
			}
		}

		if (remove != null && remove.length > 0 && modules != null) {
			for (int i = 0; i < remove.length; i++) {
				modules.remove(remove[i].getId());
			}
			// schedule server stop as Cloud SDK server cannot run without modules.
			if (modules.isEmpty()) {
				getServer().stop(true);
			}
		}
		if (modules != null) {
			setAttribute(ATTR_CLOUD_SDK_SERVER_MODULES, modules);
		}
	}
}
