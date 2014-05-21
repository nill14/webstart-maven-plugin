package org.codehaus.mojo.webstart;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

/**
 * Packages a jnlp application without launching a parallel lifecycle build.
 * Also, this mojo is not an aggregator, so it can be used multiple times
 * in a single multimodule build.
 * <p/>
 * The plugin tries to not re-sign/re-pack if the dependent jar hasn't changed.
 * As a consequence, if one modifies the pom jnlp config or a keystore, one should clean before rebuilding.
 *
 * @author <a href="jerome@coffeebreaks.org">Jerome Lacoste</a>
 * @version $Id: JnlpSingleMojo.java 18827 2013-10-27 22:49:49Z tchemit $
 */
@Mojo( name = "jnlp-single", requiresProject = true, inheritByDefault = true,
       requiresDependencyResolution = ResolutionScope.RUNTIME )
public class JnlpSingleMojo
    extends AbstractJnlpMojo
{
    // ----------------------------------------------------------------------
    // Mojo Parameters
    // ----------------------------------------------------------------------

    /**
     */
    @Component
    private MavenProject project;

    // ----------------------------------------------------------------------
    // AbstractBaseJnlpMojo implementatio
    // ----------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    public MavenProject getProject()
    {
        return project;
    }
}
