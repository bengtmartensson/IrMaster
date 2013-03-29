<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
    <xsl:output method="text"/>

    <xsl:template match="/version">
        <xsl:value-of select="@appName"/>
        <xsl:text> version </xsl:text>
        <xsl:value-of select="@mainVersion"/>
        <xsl:text>.</xsl:text>
        <xsl:value-of select="@subVersion"/>
        <xsl:text>.</xsl:text>
        <xsl:value-of select="@subminorVersion"/>
        <xsl:value-of select="@versionSuffix"/>
    </xsl:template>

</xsl:stylesheet>
