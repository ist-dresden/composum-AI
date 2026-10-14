/**
 * Fallback for the JDK package <code>jdk.net</code>, which AEM does not export from the system bundle - and which
 * the embedded Apache HttpComponents Core (since 5.4, needed because of known vulnerabilities in 5.2/5.3) references
 * unconditionally in <code>ReflectionUtils</code>. Without it HttpClient would fail with a NoClassDefFoundError as soon
 * as the connection manager is built. Since this bundle does not import <code>jdk.net</code> (see Import-Package in
 * the pom), the bundle's own classes are used; the stub reports that no extended socket options are supported, which
 * makes HttpComponents skip TCP keep-alive tuning - exactly the behavior of the older versions.
 */
package jdk.net;
