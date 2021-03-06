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

package com.google.cloud.tools.eclipse.appengine.libraries.repository;

import com.google.cloud.tools.eclipse.appengine.libraries.model.MavenCoordinates;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.maven.artifact.Artifact;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.JavaCore;

/**
 * Provides methods to convert a {@link MavenCoordinates} object to a {@link IClasspathAttribute} and vice versa.
 */
public class MavenCoordinatesClasspathAttributesTransformer {

  private static final String CLASSPATH_ATTRIBUTE_REPOSITORY =
      "com.google.cloud.tools.eclipse.appengine.libraries.repository";
  private static final String CLASSPATH_ATTRIBUTE_GROUP_ID =
      "com.google.cloud.tools.eclipse.appengine.libraries.groupid";
  private static final String CLASSPATH_ATTRIBUTE_ARTIFACT_ID =
      "com.google.cloud.tools.eclipse.appengine.libraries.artifactId";
  private static final String CLASSPATH_ATTRIBUTE_TYPE =
      "com.google.cloud.tools.eclipse.appengine.libraries.type";
  private static final String CLASSPATH_ATTRIBUTE_VERSION =
      "com.google.cloud.tools.eclipse.appengine.libraries.version";
  private static final String CLASSPATH_ATTRIBUTE_CLASSIFIER =
      "com.google.cloud.tools.eclipse.appengine.libraries.classifier";

  public List<IClasspathAttribute> createClasspathAttributes(Artifact artifact,
                                                                            MavenCoordinates mavenCoordinates) {
    List<IClasspathAttribute> attributes = Lists.newArrayList(
        JavaCore.newClasspathAttribute(CLASSPATH_ATTRIBUTE_REPOSITORY, mavenCoordinates.getRepository()),
        JavaCore.newClasspathAttribute(CLASSPATH_ATTRIBUTE_GROUP_ID, artifact.getGroupId()),
        JavaCore.newClasspathAttribute(CLASSPATH_ATTRIBUTE_ARTIFACT_ID, artifact.getArtifactId()),
        JavaCore.newClasspathAttribute(CLASSPATH_ATTRIBUTE_TYPE, artifact.getType()),
        JavaCore.newClasspathAttribute(CLASSPATH_ATTRIBUTE_VERSION, artifact.getVersion())
        );
    if (artifact.getClassifier() != null) {
      attributes.add(JavaCore.newClasspathAttribute(CLASSPATH_ATTRIBUTE_CLASSIFIER, artifact.getClassifier()));
    }
    return attributes;
  }

  public MavenCoordinates createMavenCoordinates(IClasspathAttribute[] attributes) throws LibraryRepositoryServiceException {
    Map<String, String> attributeMap = new HashMap<>(attributes.length);
    for (IClasspathAttribute attribute : attributes) {
      attributeMap.put(attribute.getName(), attribute.getValue());
    }
    String groupId = attributeMap.get(CLASSPATH_ATTRIBUTE_GROUP_ID);
    String artifactId = attributeMap.get(CLASSPATH_ATTRIBUTE_ARTIFACT_ID);
    if (Strings.isNullOrEmpty(groupId)) {
      throw new LibraryRepositoryServiceException("Attribute value for Maven group ID not found");
    }
    if (Strings.isNullOrEmpty(artifactId)) {
      throw new LibraryRepositoryServiceException("Attribute value for Maven artifact ID not found");
    }
    MavenCoordinates mavenCoordinates = new MavenCoordinates(groupId, artifactId);
    if (attributeMap.containsKey(CLASSPATH_ATTRIBUTE_REPOSITORY)) {
      mavenCoordinates.setRepository(attributeMap.get(CLASSPATH_ATTRIBUTE_REPOSITORY));
    }
    if (attributeMap.containsKey(CLASSPATH_ATTRIBUTE_TYPE)) {
      mavenCoordinates.setType(attributeMap.get(CLASSPATH_ATTRIBUTE_TYPE));
    }
    if (attributeMap.containsKey(CLASSPATH_ATTRIBUTE_CLASSIFIER)) {
      mavenCoordinates.setClassifier(attributeMap.get(CLASSPATH_ATTRIBUTE_CLASSIFIER));
    }
    if (attributeMap.containsKey(CLASSPATH_ATTRIBUTE_VERSION)) {
      mavenCoordinates.setVersion(attributeMap.get(CLASSPATH_ATTRIBUTE_VERSION));
    }
    return mavenCoordinates;
  }

}
