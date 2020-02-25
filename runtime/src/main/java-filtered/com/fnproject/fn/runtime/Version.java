package com.fnproject.fn.runtime;

/**
 * This uses maven-resource filtering rather than conventional manifest versioning as it's more robust against resource changes than using standard META-INF/MANIFEST.MF
 * versioning. For native image functions this negates the need for extra configuration to include manifest resources.
 *
 * Created on 18/02/2020.
 * <p>
 *
 * (c) 2020 Oracle Corporation
 */
public class Version {
    public static final String FDK_VERSION="${project.version}";
}
