/*
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
*/

package org.harctoolbox.IrMaster;

// This code has the problem that if a property is not found, null is returned
// instead of a sensible default, or a warning.
// Rewrite the get* set* function to call a helper function, possibly with default value.

import java.awt.Rectangle;
import java.io.*;
import java.util.Properties;
import org.harctoolbox.harchardware.GlobalCache;
import org.harctoolbox.harchardware.IrTrans;
import org.harctoolbox.harchardware.LircClient;

/**
 * This class handles the properties of the program, saved to a file between program invocations.
 * Normal use is to use the single static instance accessed by getInstance().
 */
public class Props {

    private Properties props;
    private String filename;
    private final static boolean useXml = true;
    private boolean needSave;

    // Possibly this should be easier for the user to manipulate?
    private final int pingTimeout = 2000;

    private String appendable(String env) {
        String str = System.getenv(env);
        return str == null ? "" : str.endsWith(File.separator) ? str : (str + File.separator);
    }

    private void update(String key, String value) {
        if (props.getProperty(key) == null) {
            props.setProperty(key, value);
            needSave = true;
        }
    }

    private void setupDefaults() {
        String irmasterHome = appendable("IRMASTERHOME");
        update("makehex_irpdir",	irmasterHome + "irps");
        update("irpmaster_configfile",	irmasterHome + "IrpProtocols.ini");
        update("exportdir",	System.getProperty("java.io.tmpdir") + File.separator + "exports");
        update("helpfileUrl", (new File(irmasterHome + "doc" + File.separator + "IrMaster.html")).toURI().toString());
        update("irpmasterUrl", (new File(irmasterHome + "doc" + File.separator + "IrpMaster.html")).toURI().toString());
        update("globalcacheIpName", GlobalCache.defaultGlobalCacheIP);
        update("globalcacheModule", "2");
        update("globalcachePort", "1");
        update("irTransIpName", IrTrans.defaultIrTransIP);
        update("irTransPort", "0");
        update("lircIpName", LircClient.defaultLircIP);
        update("lircPort", Integer.toString(LircClient.lircDefaultPort));
        update("hardwareIndex", "0");
        update("disregard_repeat_mins", "false");
        update("console_for_help", "false");
        update("protocol", "nec1");
        update("lookAndFeel", "0");
    }

    /**
     * Sets up a Props instance from a given file name.
     * @param filename File to read from and, later, save to. Need not exist.
     */
    public Props(String filename) {
        this.filename = filename;
        needSave = false;
        props = new Properties();
        FileInputStream f;
        if (filename == null || filename.isEmpty()) {
            System.err.println("Fatal error: Props filename is empty.");
            return;
        }
        try {
            f = new FileInputStream(filename);
            if (useXml)
                props.loadFromXML(f);
            else
                props.load(f);
        } catch (FileNotFoundException e) {
            System.err.println("Property File " + filename + " not found, using builtin defaults.");
            setupDefaults();
            needSave = true;
        } catch (IOException e) {
            System.err.println("Property File " + filename + " could not be read, using builtin defaults.");
            setupDefaults();
            needSave = true;
        }
        setupDefaults();
    }

    /**
     * Save instance to given file name.
     *
     * @param filename Filename to be saved to.
     * @return success of operation
     * @throws IOException
     * @throws FileNotFoundException
     */
    public boolean save(String filename) throws IOException,FileNotFoundException {
        if (!needSave && filename.equals(this.filename))
            return false;

        FileOutputStream f = new FileOutputStream(filename);

        if (useXml) {
            props.storeToXML(f, "IrMaster Properties, feel free to hand edit if desired");
        } else {
            props.store(f, "IrMaster Properties, feel free to hand edit if desired");
        }
        needSave = false;
        return true;
    }

    /**
     * Saves the properties to the default, stored, file name.
     *
     * @return success of operation
     * @throws IOException
     */
    public String save() throws IOException {
        boolean result = save(filename);
        return result ? filename : null;
    }

    private static Props instance = null;

    /**
     * Initialize a static instance, unless already initialized.
     * @param filename
     */
    public static void initialize(String filename) {
        if (filename == null) {
            String dir = System.getenv("LOCALAPPDATA"); // Win Vista and later
            if (dir == null)
                dir = System.getenv("APPDATA"); // Win < Vista
            if (dir != null) {
                dir = dir + File.separator + Version.appName;
                (new File(dir)).mkdirs();
                filename = dir + File.separator + Version.appName + ".properties.xml";
            } else
                filename = System.getProperty("user.home") + File.separator + "." + Version.appName + ".properties.xml";
        }
        if (instance == null)
            instance = new Props(filename);
    }

    /**
     * Initialize a static instance, unless already initialized.
     */
    public static void initialize() {
        initialize(null);
    }

    /**
     * Finish the static instance.
     *
     * @throws IOException
     */
    public static void finish() throws IOException {
        instance.save();
    }

    /**
     * Returns the static instance.
     * @return instance
     */
    public static Props getInstance() {
        initialize();
        return instance;
    }

    // For debugging
    private void list() {
        props.list(System.err);
    }

    /**
     * Returns preferred ping timeout in milliseconds.
     * @return pingtimeout
     */
    public int getPingTimeout() {
        return pingTimeout;
    }

    /** Returns the property */
    public boolean getDisregardRepeatMins() {
        return Boolean.parseBoolean(props.getProperty("disregard_repeat_mins"));
    }

    /** Sets the property */
    public void setDisregardRepeatMins(boolean w) {
        props.setProperty("disregard_repeat_mins", Boolean.toString(w));
        needSave = true;
    }
    
    /** Returns the property */
    public boolean getConsoleForHelp() {
        return Boolean.parseBoolean(props.getProperty("console_for_help"));
    }

    /** Sets the property */
    public void setConsoleForHelp(boolean w) {
        props.setProperty("console_for_help", Boolean.toString(w));
        needSave = true;
    }

    /** Returns the property */
    public String getIrpmasterConfigfile() {
        return props.getProperty("irpmaster_configfile");
    }

    /** Sets the property */
    public void setIrpmasterConfigfile(String s) {
        props.setProperty("irpmaster_configfile", s);
        needSave = true;
    }

    /** Returns the property */
    public String getProtocol() {
        return props.getProperty("protocol");
    }

    /** Sets the property */
    public void setProtocol(String s) {
        props.setProperty("protocol", s);
        needSave = true;
    }

    /** Returns the property */
    public int getLookAndFeel() {
        return Integer.parseInt(props.getProperty("lookAndFeel"));
    }

    /** Sets the property */
    public void setLookAndFeel(int laf) {
        props.setProperty("lookAndFeel", Integer.toString(laf));
        needSave = true;
    }

    /** Returns the property */
    public String getMakehexIrpdir() {
        return props.getProperty("makehex_irpdir");
    }

    /** Sets the property */
    public void setMakehexIrpdir(String s) {
        props.setProperty("makehex_irpdir", s);
        needSave = true;
    }

    /** Returns the property */
    public String getExportdir() {
        return props.getProperty("exportdir");
    }

    /** Sets the property */
    public void setExportdir(String dir) {
        props.setProperty("exportdir", dir);
        needSave = true;
    }

    /** Returns the property */
    public String getHelpfileUrl() {
        return props.getProperty("helpfileUrl");
    }

     /** Returns the property */
    public String getIrpmasterUrl() {
        return props.getProperty("irpmasterUrl");
    }

    public String getGlobalcacheIpName() {
        return props.getProperty("globalcacheIpName");
    }

    public void setGlobalcacheIpName(String ipName) {
        props.setProperty("globalcacheIpName", ipName);
        needSave = true;
    }

    public int getGlobalcacheModule() {
        return Integer.parseInt(props.getProperty("globalcacheModule"));
    }

    public void setGlobalcacheModule(int module) {
        props.setProperty("globalcacheModule", Integer.toString(module));
        needSave = true;
    }

    public int getGlobalcachePort() {
        return Integer.parseInt(props.getProperty("globalcachePort"));
    }

    public void setGlobalcachePort(int port) {
        props.setProperty("globalcachePort", Integer.toString(port));
        needSave = true;
    }

    public String getIrTransIpName() {
        return props.getProperty("irTransIpName");
    }

    public void setIrTransIpName(String ipName) {
        props.setProperty("irTransIpName", ipName);
        needSave = true;
    }

    public int getIrTransPort() {
        return Integer.parseInt(props.getProperty("irTransPort"));
    }

    public void setIrTransPort(int port) {
        props.setProperty("irTransPort", Integer.toString(port));
        needSave = true;
    }

    public String getLircIpName() {
        return props.getProperty("lircIpName");
    }

    public void setLircIpName(String ipName) {
        props.setProperty("lircIpName", ipName);
        needSave = true;
    }

    public String getLircPort() {
        return props.getProperty("lircPort");
    }

    public void setLircPort(String port) {
        props.setProperty("lircPort", port);
        needSave = true;
    }
    
    public String getHardwareIndex() {
        return props.getProperty("hardwareIndex");
    }

    public void setHardwareIndex(String index) {
        props.setProperty("hardwareIndex", index);
        needSave = true;
    }

    /** Returns the property */
    public Rectangle getBounds() {
        String str = props.getProperty("bounds");
        if (str == null || str.isEmpty())
            return null;
        String[] arr = str.trim().split(" +");
        return new Rectangle(Integer.parseInt(arr[0]), Integer.parseInt(arr[1]),
                Integer.parseInt(arr[2]), Integer.parseInt(arr[3]));
    }

    /** Sets the property */
    public void setBounds(Rectangle bounds) {
        props.setProperty("bounds", String.format("%d %d %d %d", bounds.x, bounds.y, bounds.width, bounds.height));
        needSave = true;
    }

    /**
     * Just for testing and debugging.
     * @param args
     */
    public static void main(String[] args) {
        String filename = args.length > 0 ? args[0] : null;
        Props p = new Props(filename);
        p.list();
        try {
            p.save();
        } catch (IOException e) {
        }
    }
}
