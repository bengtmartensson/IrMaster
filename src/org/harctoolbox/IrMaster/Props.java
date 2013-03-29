/* This file was automatically generated, do not edit. Do not check in in version management. */

package org.harctoolbox.IrMaster;

import java.awt.Rectangle;
import java.io.*;
import java.util.Properties;
import org.harctoolbox.harchardware.GlobalCache;
import org.harctoolbox.harchardware.IrTrans;
import org.harctoolbox.harchardware.LircClient;

/**
 * This class handles the properties of the program, saved to a file between program invocations.
 */
public class Props {
    private final static boolean useXml = true;
    private Properties props;
    private String filename;
    private boolean needSave;

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

    private void setupDefaults() {  
        String applicationHome = appendable("IRMASTERHOME");
        update("bounds", null);
        update("disregardRepeatMins", "false");
        update("exportdir", System.getProperty("java.io.tmpdir") + File.separator + "exports");
        update("globalcacheIpName", GlobalCache.defaultGlobalCacheIP);
        update("globalcacheModule", "2");
        update("globalcachePort", "1");
        update("hardwareIndex", "0");
        update("helpfileUrl", (new File(applicationHome + "doc" + File.separator + "IrMaster.html")).toURI().toString());
        update("irTransIpName", IrTrans.defaultIrTransIP);
        update("irTransPort", "0");
        update("irpmasterConfigfile", applicationHome + "IrpProtocols.ini");
        update("irpmasterUrl", (new File(applicationHome + "doc" + File.separator + "IrpMaster.html")).toURI().toString());
        update("lircIpName", LircClient.defaultLircIP);
        update("lircPort", Integer.toString(LircClient.lircDefaultPort));
        update("lookAndFeel", "0");
        update("makehexIrpdir", applicationHome + "irps");
        update("outputFormat", "0");
        update("pingTimeout", "2000");
        update("protocol", "nec1");
        update("showEditMenu", "false");
        update("showExportPane", "false");
        update("showHardwarePane", "false");
        update("showIrp", "false");
        update("showRendererSelector", "false");
        update("showShortcutMenu", "false");
        update("showToolsMenu", "false");
        update("showWardialerPane", "false");
        update("usePopupsForErrors", "true");
        update("usePopupsForHelp", "true");

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
                props.storeToXML(f, "IrMaster properties, feel free to hand edit if desired");
            else
                props.store(f, "IrMaster properties, feel free to hand edit if desired");
            
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


/** @return Bounds of IrMaster window. */
    public Rectangle getBounds() {
        String str = props.getProperty("bounds");
        if (str == null || str.isEmpty())
            return null;
        String[] arr = str.trim().split(" +");
        return new Rectangle(Integer.parseInt(arr[0]), Integer.parseInt(arr[1]),
                Integer.parseInt(arr[2]), Integer.parseInt(arr[3]));
    }

    /** @param bounds Bounds of IrMaster window. */
    public void setBounds(Rectangle bounds) {
        props.setProperty("bounds", bounds == null ? null : String.format("%d %d %d %d", bounds.x, bounds.y, bounds.width, bounds.height));
        needSave = true;
    }
    /** @return Value of the "disregard repeat mins" parameter. See IrpMaster documentation for the semantic. */
    public boolean getDisregardRepeatMins() {
        return Boolean.parseBoolean(props.getProperty("disregardRepeatMins"));
    }

    /** @param val Value of the "disregard repeat mins" parameter. See IrpMaster documentation for the semantic. */
    public void setDisregardRepeatMins(boolean val) {
        props.setProperty("disregardRepeatMins", Boolean.toString(val));
        needSave = true;
    }
    /** @return Directory to which to write exports. */
    public String getExportdir() {
        return props.getProperty("exportdir");
    }

    /** @param str Directory to which to write exports. */
    public void setExportdir(String str) {
        props.setProperty("exportdir", str);
        needSave = true;
    }
    /** @return IP Name or Address of GlobalCache to use. */
    public String getGlobalcacheIpName() {
        return props.getProperty("globalcacheIpName");
    }

    /** @param str IP Name or Address of GlobalCache to use. */
    public void setGlobalcacheIpName(String str) {
        props.setProperty("globalcacheIpName", str);
        needSave = true;
    }
    /** @return Module number of Global Cache to use, see its documentation. */
    public int getGlobalcacheModule() {
        return Integer.parseInt(props.getProperty("globalcacheModule"));
    }

    /** @param n Module number of Global Cache to use, see its documentation. */
    public void setGlobalcacheModule(int n) {
        props.setProperty("globalcacheModule", Integer.toString(n));
        needSave = true;
    }
    /** @return IR Port number of selected Global Cache module, see Global Cache documenation. */
    public int getGlobalcachePort() {
        return Integer.parseInt(props.getProperty("globalcachePort"));
    }

    /** @param n IR Port number of selected Global Cache module, see Global Cache documenation. */
    public void setGlobalcachePort(int n) {
        props.setProperty("globalcachePort", Integer.toString(n));
        needSave = true;
    }
    /** @return System dependent semantics. */
    public int getHardwareIndex() {
        return Integer.parseInt(props.getProperty("hardwareIndex"));
    }

    /** @param n System dependent semantics. */
    public void setHardwareIndex(int n) {
        props.setProperty("hardwareIndex", Integer.toString(n));
        needSave = true;
    }
    /** @return URL for help file. */
    public String getHelpfileUrl() {
        return props.getProperty("helpfileUrl");
    }

    /** @param str URL for help file. */
    public void setHelpfileUrl(String str) {
        props.setProperty("helpfileUrl", str);
        needSave = true;
    }
    /** @return IP name or address of IRTrans unit. */
    public String getIrTransIpName() {
        return props.getProperty("irTransIpName");
    }

    /** @param str IP name or address of IRTrans unit. */
    public void setIrTransIpName(String str) {
        props.setProperty("irTransIpName", str);
        needSave = true;
    }
    /** @return IRTrans port to use. */
    public int getIrTransPort() {
        return Integer.parseInt(props.getProperty("irTransPort"));
    }

    /** @param n IRTrans port to use. */
    public void setIrTransPort(int n) {
        props.setProperty("irTransPort", Integer.toString(n));
        needSave = true;
    }
    /** @return Filename of IrpProtocols.ini */
    public String getIrpmasterConfigfile() {
        return props.getProperty("irpmasterConfigfile");
    }

    /** @param str Filename of IrpProtocols.ini */
    public void setIrpmasterConfigfile(String str) {
        props.setProperty("irpmasterConfigfile", str);
        needSave = true;
    }
    /** @return URL of IrpMaster documentation. */
    public String getIrpmasterUrl() {
        return props.getProperty("irpmasterUrl");
    }

    /** @param str URL of IrpMaster documentation. */
    public void setIrpmasterUrl(String str) {
        props.setProperty("irpmasterUrl", str);
        needSave = true;
    }
    /** @return IP name or address of LIRC server. */
    public String getLircIpName() {
        return props.getProperty("lircIpName");
    }

    /** @param str IP name or address of LIRC server. */
    public void setLircIpName(String str) {
        props.setProperty("lircIpName", str);
        needSave = true;
    }
    /** @return TCP port number of LIRC sserver, typically 8765. */
    public int getLircPort() {
        return Integer.parseInt(props.getProperty("lircPort"));
    }

    /** @param n TCP port number of LIRC sserver, typically 8765. */
    public void setLircPort(int n) {
        props.setProperty("lircPort", Integer.toString(n));
        needSave = true;
    }
    /** @return Look and feel, as integer index in table. Semantics is system dependent. */
    public int getLookAndFeel() {
        return Integer.parseInt(props.getProperty("lookAndFeel"));
    }

    /** @param n Look and feel, as integer index in table. Semantics is system dependent. */
    public void setLookAndFeel(int n) {
        props.setProperty("lookAndFeel", Integer.toString(n));
        needSave = true;
    }
    /** @return Directory of Makehex IRP files. */
    public String getMakehexIrpdir() {
        return props.getProperty("makehexIrpdir");
    }

    /** @param str Directory of Makehex IRP files. */
    public void setMakehexIrpdir(String str) {
        props.setProperty("makehexIrpdir", str);
        needSave = true;
    }
    /** @return System dependant syntax. */
    public int getOutputFormat() {
        return Integer.parseInt(props.getProperty("outputFormat"));
    }

    /** @param n System dependant syntax. */
    public void setOutputFormat(int n) {
        props.setProperty("outputFormat", Integer.toString(n));
        needSave = true;
    }
    /** @return Ping timeout in milli seconds. */
    public int getPingTimeout() {
        return Integer.parseInt(props.getProperty("pingTimeout"));
    }

    /** @param n Ping timeout in milli seconds. */
    public void setPingTimeout(int n) {
        props.setProperty("pingTimeout", Integer.toString(n));
        needSave = true;
    }
    /** @return Protocol name, as in IrpProtocols.ini */
    public String getProtocol() {
        return props.getProperty("protocol");
    }

    /** @param str Protocol name, as in IrpProtocols.ini */
    public void setProtocol(String str) {
        props.setProperty("protocol", str);
        needSave = true;
    }
    
    public boolean getShowEditMenu() {
        return Boolean.parseBoolean(props.getProperty("showEditMenu"));
    }

    
    public void setShowEditMenu(boolean val) {
        props.setProperty("showEditMenu", Boolean.toString(val));
        needSave = true;
    }
    
    public boolean getShowExportPane() {
        return Boolean.parseBoolean(props.getProperty("showExportPane"));
    }

    
    public void setShowExportPane(boolean val) {
        props.setProperty("showExportPane", Boolean.toString(val));
        needSave = true;
    }
    
    public boolean getShowHardwarePane() {
        return Boolean.parseBoolean(props.getProperty("showHardwarePane"));
    }

    
    public void setShowHardwarePane(boolean val) {
        props.setProperty("showHardwarePane", Boolean.toString(val));
        needSave = true;
    }
    
    public boolean getShowIrp() {
        return Boolean.parseBoolean(props.getProperty("showIrp"));
    }

    
    public void setShowIrp(boolean val) {
        props.setProperty("showIrp", Boolean.toString(val));
        needSave = true;
    }
    
    public boolean getShowRendererSelector() {
        return Boolean.parseBoolean(props.getProperty("showRendererSelector"));
    }

    
    public void setShowRendererSelector(boolean val) {
        props.setProperty("showRendererSelector", Boolean.toString(val));
        needSave = true;
    }
    
    public boolean getShowShortcutMenu() {
        return Boolean.parseBoolean(props.getProperty("showShortcutMenu"));
    }

    
    public void setShowShortcutMenu(boolean val) {
        props.setProperty("showShortcutMenu", Boolean.toString(val));
        needSave = true;
    }
    
    public boolean getShowToolsMenu() {
        return Boolean.parseBoolean(props.getProperty("showToolsMenu"));
    }

    
    public void setShowToolsMenu(boolean val) {
        props.setProperty("showToolsMenu", Boolean.toString(val));
        needSave = true;
    }
    
    public boolean getShowWardialerPane() {
        return Boolean.parseBoolean(props.getProperty("showWardialerPane"));
    }

    
    public void setShowWardialerPane(boolean val) {
        props.setProperty("showWardialerPane", Boolean.toString(val));
        needSave = true;
    }
    /** @return If true, use popups for help. Otherwise the console will be used. */
    public boolean getUsePopupsForErrors() {
        return Boolean.parseBoolean(props.getProperty("usePopupsForErrors"));
    }

    /** @param val If true, use popups for help. Otherwise the console will be used. */
    public void setUsePopupsForErrors(boolean val) {
        props.setProperty("usePopupsForErrors", Boolean.toString(val));
        needSave = true;
    }
    /** @return If true, use popups for help. Otherwise the console will be used. */
    public boolean getUsePopupsForHelp() {
        return Boolean.parseBoolean(props.getProperty("usePopupsForHelp"));
    }

    /** @param val If true, use popups for help. Otherwise the console will be used. */
    public void setUsePopupsForHelp(boolean val) {
        props.setProperty("usePopupsForHelp", Boolean.toString(val));
        needSave = true;
    }
    
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
