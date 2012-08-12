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

    // Not-really, but almose configurable settings
    private final static boolean useXml = true;

    private Properties props;
    private String filename;
    private boolean needSave;

    private static volatile Props instance = null;

    // Possibly this should be easier for the user to manipulate?
    private final static int pingTimeout = 2000;

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
        update("makehexIrpdir",	irmasterHome + "irps");
        update("irpmasterConfigfile",	irmasterHome + "IrpProtocols.ini");
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
        update("disregardRepeatMins", "false");
        update("usePopupsForHelp", "true");
        update("protocol", "nec1");
        update("lookAndFeel", "0");
        update("usePopupsForErrors", "true");
        update("showToolsMenu", "false");
        update("showShortcutMenu", "false");
        update("showEditMenu", "false");
        update("showWardialerPane", "false");
        update("showExportPane", "false");
        update("showHardwarePane", "false");
        update("showIrp", "false");
        update("showRendererSelector", "false");
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
    }

    /**
     * Sets up a Props instance from a given file name.
     * @param filename File to read from and, later, save to. Need not exist.
     */
    public Props(String filename) {
        this.filename = filename;
        needSave = false;
        props = new Properties();
        FileInputStream f = null;
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
        } catch (FileNotFoundException ex) {
            System.err.println("Property File " + filename + " not found, using builtin defaults.");
            setupDefaults();
            needSave = true;
        } catch (IOException ex) {
            System.err.println("Property File " + filename + " could not be read, using builtin defaults.");
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
            if (useXml) {
                props.storeToXML(f, "IrMaster Properties, feel free to hand edit if desired");
            } else {
                props.store(f, "IrMaster Properties, feel free to hand edit if desired");
            }
            success = true;
            needSave = false;
        } catch (IOException ex) {
            try {
                f.close();
            } catch (IOException e) {
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
                (new File(dir)).mkdirs(); // status ignored, if fail, the new Props will fail.
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

    /**
     * @return Value of the "disregard repeat mins" parameter. See IrpMaster documentation for the semantic.
     */
    public boolean getDisregardRepeatMins() {
        return Boolean.parseBoolean(props.getProperty("disregardRepeatMins"));
    }

    /**
     *
     * @param w Value of the "disregard repeat mins" parameter. See IrpMaster documentation for the semantic.
     */
    public void setDisregardRepeatMins(boolean w) {
        props.setProperty("disregardRepeatMins", Boolean.toString(w));
        needSave = true;
    }
    
    /**
     *
     * @return
     */
    public boolean getPopupsForHelp() {
        return Boolean.parseBoolean(props.getProperty("usePopupsForHelp"));
    }

    /**
     * @param w
     */
    public void setPopupsForHelp(boolean w) {
        props.setProperty("usePopupsForHelp", Boolean.toString(w));
        needSave = true;
    }

    /**
     *
     * @return
     */
    public String getIrpmasterConfigfile() {
        return props.getProperty("irpmasterConfigfile");
    }

    /**
     *
     * @param s
     */
    public void setIrpmasterConfigfile(String s) {
        props.setProperty("irpmasterConfigfile", s);
        needSave = true;
    }

    /**
     *
     * @return
     */
    public String getProtocol() {
        return props.getProperty("protocol");
    }

    /**
     *
     * @param s
     */
    public void setProtocol(String s) {
        props.setProperty("protocol", s);
        needSave = true;
    }

    /**
     *
     * @return Look and feel, as integer index in table. May be system dependent.
     */
    public int getLookAndFeel() {
        return Integer.parseInt(props.getProperty("lookAndFeel"));
    }

    /** 
     *
     * @param laf Look and feel, as integer index in table. May be system dependent.
     */
    public void setLookAndFeel(int laf) {
        props.setProperty("lookAndFeel", Integer.toString(laf));
        needSave = true;
    }
    
    /** .
     *
     * @return It true, use popups for errors etc. Otherwise the console will be used.
     */
    public boolean getUsePopupsForErrors() {
        return Boolean.parseBoolean(props.getProperty("usePopupsForErrors"));
    }

    /** 
     *
     * @param usePopups It true, use popups for errors etc. Otherwise the console will be used.
     */
    public void setUsePopupsForErrors(boolean usePopups) {
        props.setProperty("usePopupsForErrors", Boolean.toString(usePopups));
        needSave = true;
    }
    
    /** .
     *
     * @return
     */
    public boolean getShowToolsMenu() {
        return Boolean.parseBoolean(props.getProperty("showToolsMenu"));
    }

    /** 
     *
     * @param showToolsMenu
     */
    public void setShowToolsMenu(boolean showToolsMenu) {
        props.setProperty("showToolsMenu", Boolean.toString(showToolsMenu));
        needSave = true;
    }

    /** .
     *
     * @return
     */
    public boolean getShowShortcutMenu() {
        return Boolean.parseBoolean(props.getProperty("showShortcutMenu"));
    }

    /** 
     *
     * @param showShortcutMenu
     */
    public void setShowShortcutMenu(boolean showShortcutMenu) {
        props.setProperty("showShortcutMenu", Boolean.toString(showShortcutMenu));
        needSave = true;
    }
    
    /** .
     *
     * @return
     */
    public boolean getShowEditMenu() {
        return Boolean.parseBoolean(props.getProperty("showEditMenu"));
    }

    /** 
     * @param showEditMenu
     */
    public void setShowEditMenu(boolean showEditMenu) {
        props.setProperty("showEditMenu", Boolean.toString(showEditMenu));
        needSave = true;
    }
    
    /** .
     *
     * @return
     */
    public boolean getShowWardialerPane() {
        return Boolean.parseBoolean(props.getProperty("showWardialerPane"));
    }

    /** 
     *
     * @param showWardialerPane
     */
    public void setShowWardialerPane(boolean showWardialerPane) {
        props.setProperty("showWardialerPane", Boolean.toString(showWardialerPane));
        needSave = true;
    }
    
    /** .
     *
     * @return
     */
    public boolean getShowHardwarePane() {
        return Boolean.parseBoolean(props.getProperty("showHardwarePane"));
    }

    /** 
     *
     * @param showWaridialerPane
     */
    public void setShowHardwarePane(boolean showWaridialerPane) {
        props.setProperty("showHardwarePane", Boolean.toString(showWaridialerPane));
        needSave = true;
    }

    /** .
     * @return
     */
    public boolean getShowExportPane() {
        return Boolean.parseBoolean(props.getProperty("showExportPane"));
    }

    /** 
     *
     * @param showExportPane
     */
    public void setShowExportPane(boolean showExportPane) {
        props.setProperty("showExportPane", Boolean.toString(showExportPane));
        needSave = true;
    }

    /** 
     *
     * @return
     */
    public boolean getShowIrp() {
        return Boolean.parseBoolean(props.getProperty("showIrp"));
    }

    /** 
     *
     * @param showIrp
     */
    public void setShowIrp(boolean showIrp) {
        props.setProperty("showIrp", Boolean.toString(showIrp));
        needSave = true;
    }

    /** .
     * @return 
     */
    public boolean getShowRendererSelector() {
        return Boolean.parseBoolean(props.getProperty("showRendererSelector"));
    }

    /** 
     *
     * @param showRendererSelector whether the user is presented the selector for renderer, IrpMaster/Makehex.
     */
    public void setShowRendererSelector(boolean showRendererSelector) {
        props.setProperty("showRendererSelector", Boolean.toString(showRendererSelector));
        needSave = true;
    }

    /** .
     *
     * @return
     */
    public String getMakehexIrpdir() {
        return props.getProperty("makehexIrpdir");
    }

    /**
     * 
     *
     * @param s
     */
    public void setMakehexIrpdir(String s) {
        props.setProperty("makehexIrpdir", s);
        needSave = true;
    }

    /** 
     *
     * @return
     */
    public String getExportdir() {
        return props.getProperty("exportdir");
    }

    /** 
     *
     * @param dir
     */
    public void setExportdir(String dir) {
        props.setProperty("exportdir", dir);
        needSave = true;
    }

    /**
     *
     * @return
     */
    public String getHelpfileUrl() {
        return props.getProperty("helpfileUrl");
    }

     /**
     *
     * @return
     */
    public String getIrpmasterUrl() {
        return props.getProperty("irpmasterUrl");
    }

    /**
     *
     * @return IP Name or Address of GlobalCache to use.
     */
    public String getGlobalcacheIpName() {
        return props.getProperty("globalcacheIpName");
    }

    /**
     *
     * @param ipName IP Name or Address of GlobalCache to use.
     */
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

    /**
     *
     * @return Bounds of IrMaster window.
     */
    public Rectangle getBounds() {
        String str = props.getProperty("bounds");
        if (str == null || str.isEmpty())
            return null;
        String[] arr = str.trim().split(" +");
        return new Rectangle(Integer.parseInt(arr[0]), Integer.parseInt(arr[1]),
                Integer.parseInt(arr[2]), Integer.parseInt(arr[3]));
    }

    /**
     *
     * @param bounds Bounds of IrMaster window.
     */
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
            System.err.println(e.getMessage());
        }
    }
}
