package com.google.cloud.tools.eclipse.appengine.newproject;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.templates.DocumentTemplateContext;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateBuffer;
import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.jface.text.templates.TemplateException;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.PatternSyntaxException;

public class CodeTemplates {

  /**
   * Load the named template into the supplied Eclipse project.
   *  
   * @param project the Eclipse project to be filled with templated code
   * @param config replacement values
   * @param monitor progress monitor
   * @param name directory from which to load template
   */
  // todo: config details are going to vary based on type of template; need a more generic
  // solution such as key-value map or a different design
  // todo what if project isn't empty?
  public static void materialize(IProject project, AppEngineStandardProjectConfig config,
      IProgressMonitor monitor, String name) throws CoreException {
    
    String packageName = config.getPackageName();
    createCode(monitor, project, packageName);
  }

  // todo replace with something that simply copies from a file system while replacing tokens
  private static void createCode(IProgressMonitor monitor, IProject project, String packageName) 
      throws CoreException {
    
    SubMonitor subMonitor = SubMonitor.convert(monitor, 100);
    subMonitor.setTaskName("Generating code");
    boolean force = true;
    boolean local = true;
    IFolder src = project.getFolder("src");
    if (!src.exists()) {
      src.create(force, local, subMonitor);
    }
    IFolder main = createChildFolder("main", src, subMonitor);
    IFolder java = createChildFolder("java", main, subMonitor);
    IFolder test = createChildFolder("test", src, subMonitor);
    IFolder testJava = createChildFolder("java", test, subMonitor);

    Map<String, String> values = new HashMap<>();
    if (packageName != null && !packageName.isEmpty()) {
      String[] packages = packageName.split("\\.");
      for (int i = 0; i < packages.length; i++) {
        java = createChildFolder(packages[i], java, subMonitor);
      }
      values.put("Package", "package " + packageName + ";");
    } else {
      values.put("Package", "");
    }
    createChildFile("HelloAppEngine.java", java, subMonitor, values);
    
    // now set up the test directory
    if (packageName != null && !packageName.isEmpty()) {
      String[] packages = packageName.split("\\.");
      IFolder parent = testJava;
      for (int i = 0; i < packages.length; i++) {
        parent = createChildFolder(packages[i], parent, subMonitor);
      }
    }
    
    IFolder webapp = createChildFolder("webapp", main, subMonitor);
    createChildFile("appengine-web.xml", webapp, subMonitor);
    createChildFile("web.xml", webapp, subMonitor);
    IFolder webinf = createChildFolder("WEB-INF", webapp, subMonitor);
    createChildFile("index.xhtml", webinf, subMonitor);
  }

  // visible for testing
  static IFolder createChildFolder(String name, IFolder parent, SubMonitor monitor) 
      throws CoreException {
    monitor.subTask("Creating folder " + name);
    monitor.newChild(10);

    boolean force = true;
    boolean local = true;
    IFolder child = parent.getFolder(name);
    if (!child.exists()) {
      child.create(force, local, monitor);
    }
    return child;
  }
  
  // visible for testing
  static IFile createChildFile(String name, IFolder parent, SubMonitor monitor) 
      throws CoreException {
     
    monitor.subTask("Creating file " + name);
    monitor.newChild(20);
    
    boolean force = true;
    IFile child = parent.getFile(name);
    InputStream in = CodeTemplates.class.getResourceAsStream("templates/" + name + ".ftl");
    
    if (in == null) {
      IStatus status = new Status(Status.ERROR, "todo plugin ID", 2, 
          "Could not load template for " + name, null);
      throw new CoreException(status);
    }
    
    if (!child.exists()) {
      child.create(in, force, monitor);
    }
    return child;
  }
  
  // visible for testing
  static IFile createChildFile(String name, IFolder parent, SubMonitor monitor,
      Map<String, String> values) throws CoreException {
    
    monitor.subTask("Creating file " + name);
    monitor.newChild(20);
    
    boolean force = true;
    IFile child = parent.getFile(name);
    InputStream in = CodeTemplates.class.getResourceAsStream("templates/" + name + ".ftl");
    if (in == null) {
      IStatus status = new Status(Status.ERROR, "todo plugin ID", 2, 
          "Could not load template for " + name, null);
      throw new CoreException(status);
    }
    
    if (!child.exists()) {
      // todo total hack; lots of problems with edge conditions and performance;
      // replace this with FreeMarker or better
      try {
        Reader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
        StringBuilder builder = new StringBuilder();
        for (int c = reader.read(); c != -1; c = reader.read()) {
          builder.append((char) c);
        }
        String template = builder.toString();
        
        // uncertain of importance of this contextTypeId
        String contextTypeId = "com.google.cloud.templating.context";
        TemplateContextType contextType = new TemplateContextType(contextTypeId);
        Template jtemplate = new Template("", "", contextTypeId, template, true);
        IDocument document = new Document();
        TemplateContext context = new DocumentTemplateContext(contextType, document, 0, 0);
        for (Entry<String, String> mapping : values.entrySet()) {
          context.setVariable(mapping.getKey(), mapping.getValue());
        }
        TemplateBuffer buffer = context.evaluate(jtemplate);
        template = buffer.getString();
        
        byte[] data = template.getBytes("UTF-8");
        child.create(new ByteArrayInputStream(data), force, monitor);
      } catch (IOException | PatternSyntaxException | BadLocationException | TemplateException ex) {
        IStatus status = new Status(Status.ERROR, "todo plugin ID", 3, 
            "Could not process template for " + name, null);
        throw new CoreException(status);
      }
    }
    return child;
  }

}
