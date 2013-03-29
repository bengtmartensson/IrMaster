<?xml version="1.0" encoding="UTF-8"?>
<!--
Copyright (C) 2011, 2012 Bengt Martensson.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 3 of the License, or (at
your option) any later version.

This program is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License along with
this program. If not, see http://www.gnu.org/licenses/.
-->

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
        <xsl:text>
</xsl:text>
    </xsl:template>

</xsl:stylesheet>
