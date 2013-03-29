<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
    <xsl:output method="text"/>

    <xsl:template match="/version">/* This file was automatically generated, do not edit. Do not check in in version management. */

package <xsl:value-of select="@package"/>;

/**
 * This class contains version and license information and constants.
 */
public class Version {
    /** Verbal description of the license of the current work. */
    public final static String licenseString = "<xsl:value-of select="translate(licenseString/., '&#xA;', '')"/>";

    /** Verbal description of licenses of third-party components. */
    public final static String thirdPartyString = "<xsl:value-of select="normalize-space(thirdPartyString/.)"/>";

    public final static String appName = "<xsl:value-of select='@appName'/>";
    public final static int mainVersion = <xsl:value-of select='@mainVersion'/>;
    public final static int subVersion = <xsl:value-of select='@subVersion'/>;
    public final static int subminorVersion = <xsl:value-of select='@subminorVersion'/>;
    public final static String versionSuffix = "<xsl:value-of select='@versionSuffix'/>";
    public final static String versionString = appName + " version " + mainVersion + "." + subVersion + "." + subminorVersion + versionSuffix;

    /** Project home page. */
    public final static String homepageUrl = "<xsl:value-of select='@homepageUrl'/>";

    /** URL containing current official version. */
    public final static String currentVersionUrl = homepageUrl + "/downloads/" + appName + ".version";

    private Version() {
    }
}
    </xsl:template>

</xsl:stylesheet>
