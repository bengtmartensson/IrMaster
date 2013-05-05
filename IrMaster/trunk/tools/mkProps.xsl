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

    <xsl:template match="/properties">/* This file was automatically generated, do not edit. Do not check in in version management. */

package <xsl:value-of select="@package"/>;

import java.awt.Rectangle;
import java.io.*;
import java.util.Properties;
<xsl:apply-templates select="import"/>
/**
 * This class handles the properties of the program, saved to a file between program invocations.
 */
public class Props {
    private final static boolean useXml = <xsl:value-of select="@useXml"/>;
    private Properties props;
    private String filename;
    private boolean needSave;
    private boolean wasReset = false;

    private String appendable(String env) {
        String str = System.getenv(env);
        return str == null ? "" : str.endsWith(File.separator) ? str : (str + File.separator);
    }

    private void update(String key, String value) {
        if (props.getProperty(key) == null) {
            if (value != null) {
                props.setProperty(key, value);
                needSave = true;
            }
        }
    }

    public boolean getWasReset() {
        return wasReset;
    }

    private void setupDefaults() {  
        String applicationHome = appendable("<xsl:value-of select='@home-environment-var'/>");
<xsl:apply-templates select="property" mode="defaults"/>
<xsl:text><![CDATA[
    }

    /**
     * Resets all properties to defaults.
     * This will probably leave the program in an inconsistent state,
     * so it should be restarted directly.
     */
    public void reset() {
        props = new Properties();
        setupDefaults();
        needSave = true;
        wasReset = true;
    }
    
    /**
     * Sets up a Props instance from system default file name.
     * @throws FileNotFoundException  
     */
    public Props() throws FileNotFoundException {
        this(null);
    }

    /**
     * Sets up a Props instance from a given file name.
     * @param filename File to read from and, later, save to. Need not exist.
     */
    public Props(String filename) {
        this.filename = filename;
        if (filename == null || filename.isEmpty()) {
            String dir = System.getenv("LOCALAPPDATA"); // Win Vista and later
            if (dir == null) {
                dir = System.getenv("APPDATA"); // Win < Vista
            }
            if (dir != null) {
                dir = dir + File.separator + Version.appName;
                if (!(new File(dir)).isDirectory()) {
                    boolean status = (new File(dir)).mkdirs();
                    if (!status) {
                        System.err.println("Cannot create directory " + dir + ", using home directory instead.");
                    }
                }
            }
            this.filename = (dir != null)
                    ? (dir + File.separator + Version.appName + ".properties.xml")
                    : System.getProperty("user.home") + File.separator + "." + Version.appName + ".properties.xml";
        }

        needSave = false;
        props = new Properties();
        FileInputStream f = null;

        try {
            f = new FileInputStream(this.filename);
            if (useXml)
                props.loadFromXML(f);
            else
                props.load(f);
        } catch (FileNotFoundException ex) {
            System.err.println("Property File " + this.filename + " not found, using builtin defaults.");
            setupDefaults();
            needSave = true;
        } catch (IOException ex) {
            System.err.println("Property File " + this.filename + " could not be read, using builtin defaults.");
            setupDefaults();
            needSave = true;
        } finally {
            if (f != null) {
                try {
                    f.close();
                } catch (IOException ex) {
                    System.err.println(ex.getMessage());
                }
            }
        }
        setupDefaults();
    }

    /**
     * Save instance to given file name.
     *
     * @param filename Filename to be saved to.
     * @return success of operation
     * @throws IOException
     */
    public boolean save(File filename) throws IOException {
        if (!needSave && filename.getAbsolutePath().equals((new File(this.filename)).getAbsolutePath()))
            return false;

        FileOutputStream f;
        boolean success = false;
        try {
            f = new FileOutputStream(filename);
        } catch (FileNotFoundException ex) {
            throw (ex);
        }

        try {
            if (useXml)
                props.storeToXML(f, ]]></xsl:text>
<xsl:value-of select="@appName"/> + " properties, feel free to hand edit if desired");
            else
                props.store(f, <xsl:value-of select="@appName"/>
<xsl:text><![CDATA[ + " properties, feel free to hand edit if desired");
            
            success = true;
            needSave = false;
        } catch (IOException ex) {
            try {
                f.close();
            } catch (IOException exx) {
                System.err.println(exx.getMessage());
            }
            throw (ex);
        }

        try {
            f.close();
        } catch (IOException ex) {
            throw (ex);
        }

        return success;
    }

    /**
     * Saves the properties to the default, stored, file name.
     *
     * @return success of operation
     * @throws IOException
     */
    public String save() throws IOException {
        boolean result = save(new File(filename));
        return result ? filename : null;
    }

    // For debugging
    private void list() {
        props.list(System.err);
    }

]]>
</xsl:text>
    <xsl:apply-templates select="property"/>
    <xsl:text><![CDATA[
    /**
     * Main routine for testing and debugging.
     * @param args filename
     */
    public static void main(String[] args) {
        String filename = args.length > 0 ? args[0] : null;
        try {
            Props p = new Props(filename);
            p.list();
            p.save();
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }
}
]]></xsl:text>
    </xsl:template>

    <xsl:template match="import">
        <xsl:text>import </xsl:text>
        <xsl:value-of select="@class"/>
        <xsl:text>;
</xsl:text>
    </xsl:template>

    <xsl:template match="property" mode="defaults">
        <xsl:text>        update("</xsl:text>
        <xsl:value-of select="@name"/>
        <xsl:text>", </xsl:text>
        <xsl:value-of select="@default"/>
        <xsl:text>);
</xsl:text>
    </xsl:template>

    <xsl:template match="@name" mode="capitalize">
        <xsl:value-of select="translate(substring(.,1,1), 'abcdefghijklmnopqrstuvwxyz', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ')"/>
        <xsl:value-of select="substring(.,2)"/>
    </xsl:template>

    <xsl:template match="@doc" mode="getter">
        <xsl:text>/** @return </xsl:text>
        <xsl:value-of select="."/>
        <xsl:text> */</xsl:text>
    </xsl:template>

    <xsl:template match="@doc" mode="int-setter">
        <xsl:text>/** @param n </xsl:text>
        <xsl:value-of select="."/>
        <xsl:text> */</xsl:text>
    </xsl:template>

    <xsl:template match="@doc" mode="boolean-setter">
        <xsl:text>/** @param val </xsl:text>
        <xsl:value-of select="."/>
        <xsl:text> */</xsl:text>
    </xsl:template>

    <xsl:template match="@doc" mode="string-setter">
        <xsl:text>/** @param str </xsl:text>
        <xsl:value-of select="."/>
        <xsl:text> */</xsl:text>
    </xsl:template>

    <xsl:template match="@doc" mode="rectangle-setter">
        <xsl:text>/** @param bounds </xsl:text>
        <xsl:value-of select="."/>
        <xsl:text> */</xsl:text>
    </xsl:template>
    
    <xsl:template match="property[@type='int']">
        <xsl:apply-templates select="@doc" mode="getter"/>
    public int get<xsl:apply-templates select="@name" mode="capitalize"/>() {
        return Integer.parseInt(props.getProperty("<xsl:value-of select="@name"/>"));
    }

    <xsl:apply-templates select="@doc" mode="int-setter"/>
    public void set<xsl:apply-templates select="@name" mode="capitalize"/>(int n) {
        props.setProperty("<xsl:value-of select="@name"/>", Integer.toString(n));
        needSave = true;
    }
    </xsl:template>
    
    <xsl:template match="property[@type='boolean']">
        <xsl:apply-templates select="@doc" mode="getter"/>
    public boolean get<xsl:apply-templates select="@name" mode="capitalize"/>() {
        return Boolean.parseBoolean(props.getProperty("<xsl:value-of select="@name"/>"));
    }

    <xsl:apply-templates select="@doc" mode="boolean-setter"/>
    public void set<xsl:apply-templates select="@name" mode="capitalize"/>(boolean val) {
        props.setProperty("<xsl:value-of select="@name"/>", Boolean.toString(val));
        needSave = true;
    }
    </xsl:template>

    <xsl:template match="property[@type='string']">
        <xsl:apply-templates select="@doc" mode="getter"/>
    public String get<xsl:apply-templates select="@name" mode="capitalize"/>() {
        return props.getProperty("<xsl:value-of select="@name"/>");
    }

    <xsl:apply-templates select="@doc" mode="string-setter"/>
    public void set<xsl:apply-templates select="@name" mode="capitalize"/>(String str) {
        props.setProperty("<xsl:value-of select="@name"/>", str);
        needSave = true;
    }
    </xsl:template>

    <xsl:template match="property[@type='rectangle']">
        <xsl:apply-templates select="@doc" mode="getter"/>
    public Rectangle get<xsl:apply-templates select="@name" mode="capitalize"/>() {
        String str = props.getProperty("<xsl:value-of select="@name"/>");
        if (str == null || str.isEmpty())
            return null;
        String[] arr = str.trim().split(" +");
        return new Rectangle(Integer.parseInt(arr[0]), Integer.parseInt(arr[1]),
                Integer.parseInt(arr[2]), Integer.parseInt(arr[3]));
    }

    <xsl:apply-templates select="@doc" mode="rectangle-setter"/>
    public void set<xsl:apply-templates select="@name" mode="capitalize"/>(Rectangle bounds) {
        if (bounds != null) {
            props.setProperty("<xsl:value-of select="@name"/>", String.format("%d %d %d %d", bounds.x, bounds.y, bounds.width, bounds.height));
            needSave = true;
        }
    }
    </xsl:template>
</xsl:stylesheet>