/*
Copyright (C) 2011, 2012 Bengt Martensson.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published byto
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

import com.hifiremote.exchangeir.Analyzer;
import com.hifiremote.makehex.Makehex;
import java.awt.Desktop;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.*;
import java.io.*;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.antlr.runtime.RecognitionException;
import org.harctoolbox.IrpMaster.*;
import org.harctoolbox.amx_beacon;
import org.harctoolbox.globalcache;
import org.harctoolbox.harcutils; // use only with care
import org.harctoolbox.irtrans;
import org.harctoolbox.lirc;
import org.harctoolbox.toggletype;

/**
 * This class implements a GUI for several IR programs.
 *
 * Being a user interface, it does not have much of an API itself.
 */

public class GuiMain extends javax.swing.JFrame {
    private static IrpMaster irpMaster = null;
    private static HashMap<String, Protocol> protocols = null;
    private final static short invalidParameter = (short)-1;
    private int debug = 0;
    private boolean verbose = false;
    private String[] lafNames;
    private UIManager.LookAndFeelInfo[] lafInfo;
    private static final String IrpFileExtension = "irp";
    private GlobalcacheThread globalcacheProtocolThread = null;
    private IrtransThread irtransThread = null;
    private File lastExportFile = null;

    private javax.swing.DefaultComboBoxModel noSendsSignalsComboBoxModel =
            new javax.swing.DefaultComboBoxModel(new String[] { "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "20", "50", "100" });
    private javax.swing.DefaultComboBoxModel noSendsLircPredefinedsComboBoxModel =
            new javax.swing.DefaultComboBoxModel(new String[] { "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "20", "50", "100" });
    private javax.swing.DefaultComboBoxModel noSendsIrtransFlashedComboBoxModel =
            new javax.swing.DefaultComboBoxModel(new String[] { "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "20", "50", "100" });
    private globalcache gc = null;
    private irtrans irt = null;
    private lirc lircClient = null;
    private static final int lircTransmitterDefaultIndex = 1;
    private int hardwareIndex = 0;
    private final static int hardwareIndexGlobalCache = 0;
    private final static int hardwareIndexIrtrans = 1;
    private final static int hardwareIndexLirc = 2;
    private final static int hardwareIndexAudio = 3;
    private AudioFormat audioFormat = null;
    private SourceDataLine audioLine = null;
    private int plotNumber = 0;
    String codeNotationString = null;

    private HashMap<String, String> filechooserdirs = new HashMap<String, String>();

    // Interfaces to Desktop
    private static void browse(String uri, boolean verbose) {
        browse(URI.create(uri), verbose);
    }

    private static void browse(URI uri, boolean verbose) {
        if (! Desktop.isDesktopSupported()) {
            System.err.println("Desktop not supported");
            return;
        }
        if (uri == null || uri.toString().isEmpty()) {
            System.err.println("No URI.");
            return;
        }
        try {
            Desktop.getDesktop().browse(uri);
            if (verbose)
                System.err.println("Browsing URI `" + uri.toString() + "'");
        } catch (IOException ex) {
            System.err.println("Could not start browser using uri `" + uri.toString() + "'" + ex.getMessage());
        }
    }

    private static void open(String filename, boolean verbose) {
        open(new File(filename), verbose);
    }

    private static void open(File file, boolean verbose) {
        if (! Desktop.isDesktopSupported()) {
            System.err.println("Desktop not supported");
            return;
        }

        try {
            Desktop.getDesktop().open(file);
            if (verbose)
                System.err.println("open file `" + file.toString() + "'");
        } catch (IOException ex) {
            System.err.println("Could not open file `" + file.toString() + "'");
        }
    }

    private static void edit(File file, boolean verbose) {
        if (!Desktop.isDesktopSupported()) {
            System.err.println("Desktop not supported");
            return;
        }

        if (!Desktop.getDesktop().isSupported(Desktop.Action.EDIT))
            browse(file.toURI(), verbose);
        else {

            try {
                Desktop.getDesktop().edit(file);
                if (verbose)
                    System.err.println("edit file `" + file.toString() + "'");
            } catch (IOException ex) {
                System.err.println("Could not edit file `" + file.toString() + "'");
            }
        }
    }

    
    private File selectFile(String title, String fileTypeDesc, boolean save, String defaultdir, String extension) {
        return selectFile(title, fileTypeDesc, save, defaultdir, extension, null, null);
    }
    
    private File selectFile(String title, String fileTypeDesc, boolean save, String defaultdir, String extension,
            String altFileTypeDesc, String altExtension) {
        String startdir = filechooserdirs.containsKey(title) ? filechooserdirs.get(title) : defaultdir;
        JFileChooser chooser = new JFileChooser(startdir);
        chooser.setDialogTitle(title);
        if (extension == null || extension.equals("")) {
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        } else {
            chooser.setFileFilter(new FileNameExtensionFilter(fileTypeDesc, extension));
            if (altExtension != null && !altExtension.equals(""))
                chooser.addChoosableFileFilter(new FileNameExtensionFilter(altFileTypeDesc, altExtension));
        }
        int result = save ? chooser.showSaveDialog(this) : chooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            filechooserdirs.put(title, chooser.getSelectedFile().getAbsoluteFile().getParent());
            return chooser.getSelectedFile();
        } else
            return null;
    }

    private class CopyClipboardText implements ClipboardOwner {

        @Override
        public void lostOwnership(Clipboard c, Transferable t) {
        }

        public void toClipboard(String str) {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(str), this);
        }

        public String fromClipboard() {
            try {
                return (String) Toolkit.getDefaultToolkit().getSystemClipboard().getContents(this).getTransferData(DataFlavor.stringFlavor);
            } catch (UnsupportedFlavorException ex) {
                System.err.println(ex.getMessage());
            } catch (IOException ex) {
                System.err.println(ex.getMessage());
            }
            return null;
        }
    }

    private static Protocol getProtocol(String name) throws UnassignedException, RecognitionException {
        if (!protocols.containsKey(name)) {
            Protocol protocol = irpMaster.newProtocol(name);
            protocols.put(name, protocol);
        }
        return protocols.get(name);
    }

    /**
     * Main class for the GUI.
     *
     * @param verbose Verbose execution of some commands, dependent on invoked programs.
     * @param debug Debug value handed over to invoked programs/functions.
     */
    public GuiMain(boolean verbose, int debug) {
        this.verbose = verbose;
        this.debug = debug;
        lafInfo = UIManager.getInstalledLookAndFeels();
        lafNames = new String[lafInfo.length];
        for (int i = 0; i < lafInfo.length; i++)
            lafNames[i] = lafInfo[i].getName();

        try {
            UIManager.setLookAndFeel(lafInfo[Props.getInstance().getLookAndFeel()].getClassName());
        } catch (ClassNotFoundException ex) {
            error(ex.getMessage());
        } catch (InstantiationException ex) {
            error(ex.getMessage());
        } catch (IllegalAccessException ex) {
            error(ex.getMessage());
        } catch (UnsupportedLookAndFeelException ex) {
            error(ex.getMessage());
        }

        try {
            irpMaster = new IrpMaster(Props.getInstance().getIrpmasterConfigfile());
        } catch (FileNotFoundException ex) {
            error(ex.getMessage());
        } catch (IncompatibleArgumentException ex) {
            error(ex.getMessage());
        }
        protocols = new HashMap<String, Protocol>();

        initComponents();
        lafComboBox.setSelectedIndex(Props.getInstance().getLookAndFeel());
        Rectangle bounds = Props.getInstance().getBounds();
        if (bounds != null)
            setBounds(bounds);

        gc_module_ComboBox.setSelectedItem(Integer.toString(Props.getInstance().getGlobalcacheModule()));
        gc_connector_ComboBox.setSelectedItem(Integer.toString(Props.getInstance().getGlobalcachePort()));

        irtrans_led_ComboBox.setSelectedIndex(Props.getInstance().getIrTransPort());

        disregard_repeat_mins_CheckBoxMenuItem.setSelected(Props.getInstance().getDisregardRepeatMins());
        disregard_repeat_mins_CheckBox.setSelected(Props.getInstance().getDisregardRepeatMins());

        System.setErr(consolePrintStream);
        System.setOut(consolePrintStream);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    Props.getInstance().save();
                } catch (Exception e) {
                    System.out.println("Problems saving properties; " + e.getMessage());
                }
                System.out.println("*** Normal GUI shutdown ***");
            }
        });

        protocol_ComboBox.setSelectedItem(Props.getInstance().getProtocol());
        updateProtocolParameters();
        verbose_CheckBoxMenuItem.setSelected(verbose);
        verbose_CheckBox.setSelected(verbose);

        gc = new globalcache("globalcache", globalcache.gc_model.gc_unknown, verbose);
        irt = new irtrans("irtrans", verbose);

        exportdir_TextField.setText(Props.getInstance().getExportdir());
        updateFromFrequency();
        hardwareIndex = Integer.parseInt(Props.getInstance().getHardwareIndex());
        protocol_outputhw_ComboBox.setSelectedIndex(hardwareIndex);
        war_dialer_outputhw_ComboBox.setSelectedIndex(hardwareIndex);
        outputHWTabbedPane.setSelectedIndex(hardwareIndex);
        enableExportFormatRelated();
    }

    // From Real Gagnon
    class FilteredStream extends FilterOutputStream {

        public FilteredStream(OutputStream aStream) {
            super(aStream);
        }

        @Override
        public void write(byte b[]) throws IOException {
            String aString = new String(b);
            consoleTextArea.append(aString);
        }

        @Override
        public void write(byte b[], int off, int len) throws IOException {
            String aString = new String(b, off, len);
            consoleTextArea.append(aString);
            consoleTextArea.setCaretPosition(consoleTextArea.getDocument().getLength());
        }
    }

    PrintStream consolePrintStream = new PrintStream(
            new FilteredStream(
            new ByteArrayOutputStream()));

    //TODO: boolean logFile;
    private void warning(String message) {
        System.err.println("Warning: " + message);
    }

    private void error(String message) {
        System.err.println("Error: " + message);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        consolePopupMenu = new javax.swing.JPopupMenu();
        consoleClearMenuItem = new javax.swing.JMenuItem();
        jSeparator5 = new javax.swing.JPopupMenu.Separator();
        consoleCopySelectionMenuItem = new javax.swing.JMenuItem();
        consoleCopyMenuItem = new javax.swing.JMenuItem();
        jSeparator8 = new javax.swing.JPopupMenu.Separator();
        consoleSaveMenuItem = new javax.swing.JMenuItem();
        CCFCodePopupMenu = new javax.swing.JPopupMenu();
        rawCodeClearMenuItem = new javax.swing.JMenuItem();
        jSeparator3 = new javax.swing.JPopupMenu.Separator();
        rawCodeCopyMenuItem = new javax.swing.JMenuItem();
        rawCodeCopyAllMenuItem = new javax.swing.JMenuItem();
        rawCodePasteMenuItem = new javax.swing.JMenuItem();
        rawCodeSelectAllMenuItem = new javax.swing.JMenuItem();
        jSeparator7 = new javax.swing.JPopupMenu.Separator();
        rawCodeSaveMenuItem = new javax.swing.JMenuItem();
        rawCodeImportMenuItem = new javax.swing.JMenuItem();
        jSeparator9 = new javax.swing.JPopupMenu.Separator();
        listIrpDefMenuItem = new javax.swing.JMenuItem();
        docuProtocolMenuItem = new javax.swing.JMenuItem();
        listIrpMenuItem = new javax.swing.JMenuItem();
        copyPopupMenu = new javax.swing.JPopupMenu();
        copyMenuItem = new javax.swing.JMenuItem();
        copyPastePopupMenu = new javax.swing.JPopupMenu();
        cpCopyMenuItem = new javax.swing.JMenuItem();
        pasteMenuItem = new javax.swing.JMenuItem();
        mainSplitPane = new javax.swing.JSplitPane();
        mainTabbedPane = new javax.swing.JTabbedPane();
        protocolsPanel = new javax.swing.JPanel();
        protocol_ComboBox = new javax.swing.JComboBox();
        deviceno_TextField = new javax.swing.JTextField();
        subdevice_TextField = new javax.swing.JTextField();
        commandno_TextField = new javax.swing.JTextField();
        toggle_ComboBox = new javax.swing.JComboBox();
        protocol_params_TextField = new javax.swing.JTextField();
        protocolsSubPane = new javax.swing.JTabbedPane();
        analyzePanel = new javax.swing.JPanel();
        IRP_TextField = new javax.swing.JTextField();
        jScrollPane3 = new javax.swing.JScrollPane();
        protocol_raw_TextArea = new javax.swing.JTextArea();
        jPanel6 = new javax.swing.JPanel();
        protocol_generate_Button = new javax.swing.JButton();
        protocolAnalyzeButton = new javax.swing.JButton();
        protocolPlotButton = new javax.swing.JButton();
        protocol_decode_Button = new javax.swing.JButton();
        icf_import_Button = new javax.swing.JButton();
        protocol_clear_Button = new javax.swing.JButton();
        jPanel5 = new javax.swing.JPanel();
        protocol_send_Button = new javax.swing.JButton();
        protocol_outputhw_ComboBox = new javax.swing.JComboBox();
        protocol_stop_Button = new javax.swing.JButton();
        no_sends_protocol_ComboBox = new javax.swing.JComboBox();
        exportPanel = new javax.swing.JPanel();
        protocolExportButton = new javax.swing.JButton();
        automaticFileNamesCheckBox = new javax.swing.JCheckBox();
        exportGenerateTogglesCheckBox = new javax.swing.JCheckBox();
        lastFTextField = new javax.swing.JTextField();
        jLabel17 = new javax.swing.JLabel();
        exportFormatComboBox = new javax.swing.JComboBox();
        jLabel20 = new javax.swing.JLabel();
        exportdir_TextField = new javax.swing.JTextField();
        exportdir_browse_Button = new javax.swing.JButton();
        jLabel21 = new javax.swing.JLabel();
        exportRawCheckBox = new javax.swing.JCheckBox();
        exportProntoCheckBox = new javax.swing.JCheckBox();
        viewExportButton = new javax.swing.JButton();
        openExportDirButton = new javax.swing.JButton();
        exportRepetitionsComboBox = new javax.swing.JComboBox();
        jLabel54 = new javax.swing.JLabel();
        warDialerPanel = new javax.swing.JPanel();
        jLabel32 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        jPanel2 = new javax.swing.JPanel();
        notesClearButton = new javax.swing.JButton();
        notesSaveButton = new javax.swing.JButton();
        notesEditButton = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        pauseButton = new javax.swing.JButton();
        currentFTextField = new javax.swing.JTextField();
        startButton = new javax.swing.JButton();
        stopButton = new javax.swing.JButton();
        jPanel4 = new javax.swing.JPanel();
        jLabel60 = new javax.swing.JLabel();
        endFTextField = new javax.swing.JTextField();
        jLabel27 = new javax.swing.JLabel();
        delayTextField = new javax.swing.JTextField();
        jLabel28 = new javax.swing.JLabel();
        war_dialer_outputhw_ComboBox = new javax.swing.JComboBox();
        jLabel29 = new javax.swing.JLabel();
        warDialerNoSendsComboBox = new javax.swing.JComboBox();
        rendererComboBox = new javax.swing.JComboBox();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
        outputHWTabbedPane = new javax.swing.JTabbedPane();
        globalcache_Panel = new javax.swing.JPanel();
        jLabel34 = new javax.swing.JLabel();
        gcDiscoveredTypeLabel = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        gc_address_TextField = new javax.swing.JTextField();
        jLabel19 = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();
        discoverButton = new javax.swing.JButton();
        jLabel35 = new javax.swing.JLabel();
        gc_browse_Button = new javax.swing.JButton();
        jLabel36 = new javax.swing.JLabel();
        gc_connector_ComboBox = new javax.swing.JComboBox();
        globalCachePingButton = new javax.swing.JButton();
        gc_module_ComboBox = new javax.swing.JComboBox();
        irtrans_Panel = new javax.swing.JPanel();
        irtransVersionLabel = new javax.swing.JLabel();
        jLabel18 = new javax.swing.JLabel();
        irtransIPPanel = new javax.swing.JPanel();
        irtransPingButton = new javax.swing.JButton();
        irtrans_address_TextField = new javax.swing.JTextField();
        irtrans_browse_Button = new javax.swing.JButton();
        jLabel38 = new javax.swing.JLabel();
        irtrans_led_ComboBox = new javax.swing.JComboBox();
        read_Button = new javax.swing.JButton();
        jLabel37 = new javax.swing.JLabel();
        irtransPredefinedPanel = new javax.swing.JPanel();
        jLabel33 = new javax.swing.JLabel();
        jLabel52 = new javax.swing.JLabel();
        irtransCommandsComboBox = new javax.swing.JComboBox();
        no_sends_irtrans_flashed_ComboBox = new javax.swing.JComboBox();
        jLabel53 = new javax.swing.JLabel();
        irtransRemotesComboBox = new javax.swing.JComboBox();
        jLabel51 = new javax.swing.JLabel();
        irtransSendFlashedButton = new javax.swing.JButton();
        lircPanel = new javax.swing.JPanel();
        lircServerVersionText = new javax.swing.JLabel();
        lircServerVersionLabel = new javax.swing.JLabel();
        lircIPPanel = new javax.swing.JPanel();
        jLabel45 = new javax.swing.JLabel();
        LircIPAddressTextField = new javax.swing.JTextField();
        read_lirc_Button = new javax.swing.JButton();
        lircPingButton = new javax.swing.JButton();
        lircPortTextField = new javax.swing.JTextField();
        jLabel44 = new javax.swing.JLabel();
        lircTransmitterComboBox = new javax.swing.JComboBox();
        jLabel46 = new javax.swing.JLabel();
        lircStopIrButton = new javax.swing.JButton();
        lircPredefinedPanel = new javax.swing.JPanel();
        jLabel47 = new javax.swing.JLabel();
        lircCommandsComboBox = new javax.swing.JComboBox();
        jLabel49 = new javax.swing.JLabel();
        lircRemotesComboBox = new javax.swing.JComboBox();
        jLabel50 = new javax.swing.JLabel();
        lircSendPredefinedButton = new javax.swing.JButton();
        jLabel48 = new javax.swing.JLabel();
        noLircPredefinedsComboBox = new javax.swing.JComboBox();
        audioPanel = new javax.swing.JPanel();
        audioGetLineButton = new javax.swing.JButton();
        audioReleaseLineButton = new javax.swing.JButton();
        jLabel59 = new javax.swing.JLabel();
        audioFormatPanel = new javax.swing.JPanel();
        jLabel56 = new javax.swing.JLabel();
        audioSampleSizeComboBox = new javax.swing.JComboBox();
        jLabel55 = new javax.swing.JLabel();
        jLabel57 = new javax.swing.JLabel();
        audioSampleFrequencyComboBox = new javax.swing.JComboBox();
        jLabel58 = new javax.swing.JLabel();
        audioChannelsComboBox = new javax.swing.JComboBox();
        audioBigEndianCheckBox = new javax.swing.JCheckBox();
        audioOptionsPanel = new javax.swing.JPanel();
        audioWaveformComboBox = new javax.swing.JComboBox();
        audioOmitCheckBox = new javax.swing.JCheckBox();
        audioDivideCheckBox = new javax.swing.JCheckBox();
        jLabel30 = new javax.swing.JLabel();
        hexcalcPanel = new javax.swing.JPanel();
        numbersPanel = new javax.swing.JPanel();
        hex_TextField = new javax.swing.JTextField();
        reverse_decimal_TextField = new javax.swing.JTextField();
        jLabel8 = new javax.swing.JLabel();
        jLabel43 = new javax.swing.JLabel();
        complement_decimal_TextField = new javax.swing.JTextField();
        reverse_complement_hex_TextField = new javax.swing.JTextField();
        jLabel14 = new javax.swing.JLabel();
        jLabel41 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        from_efc_decimal_TextField = new javax.swing.JTextField();
        from_efc_hex_TextField = new javax.swing.JTextField();
        reverse_hex_TextField = new javax.swing.JTextField();
        jLabel40 = new javax.swing.JLabel();
        reverse_complement_decimal_TextField = new javax.swing.JTextField();
        jLabel15 = new javax.swing.JLabel();
        efc5_decimal_TextField = new javax.swing.JTextField();
        from_efc5_decimal_TextField = new javax.swing.JTextField();
        efc_decimal_TextField = new javax.swing.JTextField();
        jLabel9 = new javax.swing.JLabel();
        from_efc5_hex_TextField = new javax.swing.JTextField();
        efc_hex_TextField = new javax.swing.JTextField();
        decimal_TextField = new javax.swing.JTextField();
        jLabel42 = new javax.swing.JLabel();
        complement_hex_TextField = new javax.swing.JTextField();
        efc5_hex_TextField = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        timeFrequencyPanel = new javax.swing.JPanel();
        jLabel24 = new javax.swing.JLabel();
        frequency_TextField = new javax.swing.JTextField();
        prontocode_TextField = new javax.swing.JTextField();
        jLabel23 = new javax.swing.JLabel();
        jLabel39 = new javax.swing.JLabel();
        time_TextField = new javax.swing.JTextField();
        jLabel25 = new javax.swing.JLabel();
        no_periods_hex_TextField = new javax.swing.JTextField();
        jLabel22 = new javax.swing.JLabel();
        no_periods_TextField = new javax.swing.JTextField();
        optionsPanel = new javax.swing.JPanel();
        jLabel16 = new javax.swing.JLabel();
        IrpProtocolsTextField = new javax.swing.JTextField();
        irpProtocolsSelectButton = new javax.swing.JButton();
        IrpProtocolsBrowseButton = new javax.swing.JButton();
        makehexIrpDirTextField = new javax.swing.JTextField();
        macro_select_Button = new javax.swing.JButton();
        makehexIrpDirBrowseButton = new javax.swing.JButton();
        verbose_CheckBox = new javax.swing.JCheckBox();
        debug_TextField = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        disregard_repeat_mins_CheckBox = new javax.swing.JCheckBox();
        lafComboBox = new javax.swing.JComboBox();
        jLabel26 = new javax.swing.JLabel();
        consoleScrollPane = new javax.swing.JScrollPane();
        consoleTextArea = new javax.swing.JTextArea();
        menuBar = new javax.swing.JMenuBar();
        fileMenu = new javax.swing.JMenu();
        saveMenuItem = new javax.swing.JMenuItem();
        saveAsMenuItem = new javax.swing.JMenuItem();
        jSeparator4 = new javax.swing.JSeparator();
        consoletext_save_MenuItem = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JSeparator();
        exitMenuItem = new javax.swing.JMenuItem();
        editMenu = new javax.swing.JMenu();
        copy_console_to_clipboard_MenuItem = new javax.swing.JMenuItem();
        clear_console_MenuItem = new javax.swing.JMenuItem();
        jMenu1 = new javax.swing.JMenu();
        verbose_CheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        disregard_repeat_mins_CheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        helpMenu = new javax.swing.JMenu();
        aboutMenuItem = new javax.swing.JMenuItem();
        browseHomePageMenuItem = new javax.swing.JMenuItem();
        contentMenuItem = new javax.swing.JMenuItem();
        browseIRPMasterMenuItem = new javax.swing.JMenuItem();
        jSeparator15 = new javax.swing.JPopupMenu.Separator();
        checkUpdatesMenuItem = new javax.swing.JMenuItem();
        jSeparator13 = new javax.swing.JPopupMenu.Separator();
        browseIRPSpecMenuItem = new javax.swing.JMenuItem();
        browseDecodeIRMenuItem = new javax.swing.JMenuItem();
        browseJP1Wiki = new javax.swing.JMenuItem();

        consoleClearMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DELETE, 0));
        consoleClearMenuItem.setText("Clear");
        consoleClearMenuItem.setToolTipText("Discard the content of the console window.");
        consoleClearMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                consoleClearMenuItemActionPerformed(evt);
            }
        });
        consolePopupMenu.add(consoleClearMenuItem);
        consolePopupMenu.add(jSeparator5);

        consoleCopySelectionMenuItem.setText("Copy selection");
        consoleCopySelectionMenuItem.setToolTipText("Copy currently selected text to the clipboard.");
        consoleCopySelectionMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                consoleCopySelectionMenuItemActionPerformed(evt);
            }
        });
        consolePopupMenu.add(consoleCopySelectionMenuItem);

        consoleCopyMenuItem.setText("Copy all");
        consoleCopyMenuItem.setToolTipText("Copy the content of the console to the clipboard.");
        consoleCopyMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                consoleCopyMenuItemActionPerformed(evt);
            }
        });
        consolePopupMenu.add(consoleCopyMenuItem);
        consolePopupMenu.add(jSeparator8);

        consoleSaveMenuItem.setText("Save...");
        consoleSaveMenuItem.setToolTipText("Save the content of the console to a text file.");
        consoleSaveMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                consoleSaveMenuItemActionPerformed(evt);
            }
        });
        consolePopupMenu.add(consoleSaveMenuItem);

        rawCodeClearMenuItem.setText("Clear");
        rawCodeClearMenuItem.setToolTipText("Clean this area");
        rawCodeClearMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rawCodeClearMenuItemActionPerformed(evt);
            }
        });
        CCFCodePopupMenu.add(rawCodeClearMenuItem);
        CCFCodePopupMenu.add(jSeparator3);

        rawCodeCopyMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C, java.awt.event.InputEvent.CTRL_MASK));
        rawCodeCopyMenuItem.setText("Copy selection");
        rawCodeCopyMenuItem.setToolTipText("Copy current selection to the clipboard");
        rawCodeCopyMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rawCodeCopyMenuItemActionPerformed(evt);
            }
        });
        CCFCodePopupMenu.add(rawCodeCopyMenuItem);

        rawCodeCopyAllMenuItem.setText("Copy all");
        rawCodeCopyAllMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rawCodeCopyAllMenuItemActionPerformed(evt);
            }
        });
        CCFCodePopupMenu.add(rawCodeCopyAllMenuItem);

        rawCodePasteMenuItem.setText("Paste");
        rawCodePasteMenuItem.setToolTipText("Paste from clipboard");
        rawCodePasteMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rawCodePasteMenuItemActionPerformed(evt);
            }
        });
        CCFCodePopupMenu.add(rawCodePasteMenuItem);

        rawCodeSelectAllMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_A, java.awt.event.InputEvent.CTRL_MASK));
        rawCodeSelectAllMenuItem.setText("Select all");
        rawCodeSelectAllMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rawCodeSelectAllMenuItemActionPerformed(evt);
            }
        });
        CCFCodePopupMenu.add(rawCodeSelectAllMenuItem);
        CCFCodePopupMenu.add(jSeparator7);

        rawCodeSaveMenuItem.setText("Save...");
        rawCodeSaveMenuItem.setToolTipText("Save current content to text file");
        rawCodeSaveMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rawCodeSaveMenuItemActionPerformed(evt);
            }
        });
        CCFCodePopupMenu.add(rawCodeSaveMenuItem);

        rawCodeImportMenuItem.setText("Import...");
        rawCodeImportMenuItem.setToolTipText("Import from external file");
        rawCodeImportMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rawCodeImportMenuItemActionPerformed(evt);
            }
        });
        CCFCodePopupMenu.add(rawCodeImportMenuItem);
        CCFCodePopupMenu.add(jSeparator9);

        listIrpDefMenuItem.setText("List current IRP definition");
        listIrpDefMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                listIrpDefMenuItemActionPerformed(evt);
            }
        });
        CCFCodePopupMenu.add(listIrpDefMenuItem);

        docuProtocolMenuItem.setText("List current protocol docs.");
        docuProtocolMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                docuProtocolMenuItemActionPerformed(evt);
            }
        });
        CCFCodePopupMenu.add(docuProtocolMenuItem);

        listIrpMenuItem.setText("List current IRP-File");
        listIrpMenuItem.setToolTipText("List the content of current IRP file (only for Makehex).");
        listIrpMenuItem.setEnabled(false);
        listIrpMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                listIrpMenuItemActionPerformed(evt);
            }
        });
        CCFCodePopupMenu.add(listIrpMenuItem);

        copyMenuItem.setMnemonic('C');
        copyMenuItem.setText("Copy");
        copyMenuItem.setToolTipText("Copy content of window to clipboard.");
        copyMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                copyMenuItemActionPerformed(evt);
            }
        });
        copyPopupMenu.add(copyMenuItem);

        cpCopyMenuItem.setMnemonic('C');
        cpCopyMenuItem.setText("Copy");
        cpCopyMenuItem.setToolTipText("Copy content of window to clipboard.");
        cpCopyMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cpCopyMenuItemActionPerformed(evt);
            }
        });
        copyPastePopupMenu.add(cpCopyMenuItem);

        pasteMenuItem.setMnemonic('P');
        pasteMenuItem.setText("Paste");
        pasteMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pasteMenuItemActionPerformed(evt);
            }
        });
        copyPastePopupMenu.add(pasteMenuItem);

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("IrMaster -- GUI for several IR programs");
        setPreferredSize(new java.awt.Dimension(660, 607));
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        mainSplitPane.setBorder(null);
        mainSplitPane.setDividerLocation(450);
        mainSplitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        mainSplitPane.setToolTipText("splitter tooltext");

        mainTabbedPane.setToolTipText("This tab allows the change of options for the program.");
        mainTabbedPane.setPreferredSize(new java.awt.Dimension(600, 472));

        protocolsPanel.setToolTipText("This pane deals with generating, sending, exporting, and analyzing of IR protocols.");
        protocolsPanel.setPreferredSize(new java.awt.Dimension(600, 377));

        protocol_ComboBox.setMaximumRowCount(20);
        protocol_ComboBox.setModel(new DefaultComboBoxModel(irpMaster == null ? new String[]{"--"} : harcutils.sort_unique(irpMaster.getNames().toArray(new String[0]))));
        protocol_ComboBox.setToolTipText("Protocol name");
        protocol_ComboBox.setMaximumSize(new java.awt.Dimension(100, 25));
        protocol_ComboBox.setMinimumSize(new java.awt.Dimension(100, 25));
        protocol_ComboBox.setPreferredSize(new java.awt.Dimension(100, 25));
        protocol_ComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                protocol_ComboBoxActionPerformed(evt);
            }
        });

        deviceno_TextField.setToolTipText("D, Device number");
        deviceno_TextField.setMinimumSize(new java.awt.Dimension(35, 27));
        deviceno_TextField.setPreferredSize(new java.awt.Dimension(35, 27));
        deviceno_TextField.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                generic_copy_paste_menu(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                generic_copy_paste_menu(evt);
            }
        });
        deviceno_TextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deviceno_TextFieldActionPerformed(evt);
            }
        });
        deviceno_TextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                deviceno_TextFieldFocusLost(evt);
            }
        });

        subdevice_TextField.setToolTipText("S, Subdevice number");
        subdevice_TextField.setMinimumSize(new java.awt.Dimension(35, 27));
        subdevice_TextField.setPreferredSize(new java.awt.Dimension(35, 27));
        subdevice_TextField.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                generic_copy_paste_menu(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                generic_copy_paste_menu(evt);
            }
        });

        commandno_TextField.setToolTipText("F, Function number (also called Command number or OBC).");
        commandno_TextField.setMinimumSize(new java.awt.Dimension(35, 27));
        commandno_TextField.setPreferredSize(new java.awt.Dimension(35, 27));
        commandno_TextField.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                generic_copy_paste_menu(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                generic_copy_paste_menu(evt);
            }
        });
        commandno_TextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                commandno_TextFieldActionPerformed(evt);
            }
        });
        commandno_TextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                commandno_TextFieldFocusLost(evt);
            }
        });

        toggle_ComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "0", "1", "-" }));
        toggle_ComboBox.setSelectedIndex(2);
        toggle_ComboBox.setToolTipText("Toggles to generate");
        toggle_ComboBox.setMaximumSize(new java.awt.Dimension(50, 32767));

        protocol_params_TextField.setToolTipText("Additional parameters needed for some protocols.");
        protocol_params_TextField.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                generic_copy_paste_menu(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                generic_copy_paste_menu(evt);
            }
        });

        IRP_TextField.setEditable(false);
        IRP_TextField.setToolTipText("IRP description of current protocol");
        IRP_TextField.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                generic_copy_menu(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                generic_copy_menu(evt);
            }
        });

        protocol_raw_TextArea.setColumns(20);
        protocol_raw_TextArea.setFont(new java.awt.Font("Lucida Sans Typewriter", 0, 14)); // NOI18N
        protocol_raw_TextArea.setLineWrap(true);
        protocol_raw_TextArea.setRows(5);
        protocol_raw_TextArea.setToolTipText("Pronto code; may be edited. Press right mouse button for menu.");
        protocol_raw_TextArea.setWrapStyleWord(true);
        protocol_raw_TextArea.setMinimumSize(new java.awt.Dimension(240, 17));
        protocol_raw_TextArea.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseExited(java.awt.event.MouseEvent evt) {
                protocol_raw_TextAreaMouseExited(evt);
            }
            public void mousePressed(java.awt.event.MouseEvent evt) {
                protocol_raw_TextAreaMousePressed(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                protocol_raw_TextAreaMouseReleased(evt);
            }
        });
        jScrollPane3.setViewportView(protocol_raw_TextArea);

        jPanel6.setBorder(null);

        protocol_generate_Button.setMnemonic('R');
        protocol_generate_Button.setText("Render");
        protocol_generate_Button.setToolTipText("Compute Pronto code from upper row protocol description");
        protocol_generate_Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                protocol_generate_ButtonActionPerformed(evt);
            }
        });

        protocolAnalyzeButton.setMnemonic('A');
        protocolAnalyzeButton.setText("Analyze");
        protocolAnalyzeButton.setToolTipText("Sends content of code windows to Analyze.");
        protocolAnalyzeButton.setEnabled(false);
        protocolAnalyzeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                protocolAnalyzeButtonActionPerformed(evt);
            }
        });

        protocolPlotButton.setMnemonic('P');
        protocolPlotButton.setText("Plot");
        protocolPlotButton.setToolTipText("Graphical display of signal. Not yet implemented.");
        protocolPlotButton.setEnabled(false);
        protocolPlotButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                protocolPlotButtonActionPerformed(evt);
            }
        });

        protocol_decode_Button.setMnemonic('D');
        protocol_decode_Button.setText("Decode");
        protocol_decode_Button.setToolTipText("Send content of Code window(s) to DecodeIR");
        protocol_decode_Button.setEnabled(false);
        protocol_decode_Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                protocol_decode_ButtonActionPerformed(evt);
            }
        });

        icf_import_Button.setText("Import...");
        icf_import_Button.setToolTipText("Import wave file or file from IR WIdget/IRScope");
        icf_import_Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                icf_import_ButtonActionPerformed(evt);
            }
        });

        protocol_clear_Button.setMnemonic('C');
        protocol_clear_Button.setText("Clear");
        protocol_clear_Button.setToolTipText("Clears code text areas");
        protocol_clear_Button.setEnabled(false);
        protocol_clear_Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                protocol_clear_ButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(protocol_generate_Button)
            .addComponent(icf_import_Button, javax.swing.GroupLayout.PREFERRED_SIZE, 83, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addComponent(protocol_decode_Button, javax.swing.GroupLayout.PREFERRED_SIZE, 83, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addComponent(protocolAnalyzeButton, javax.swing.GroupLayout.PREFERRED_SIZE, 83, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addComponent(protocol_clear_Button, javax.swing.GroupLayout.PREFERRED_SIZE, 83, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addComponent(protocolPlotButton, javax.swing.GroupLayout.PREFERRED_SIZE, 83, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        jPanel6Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {icf_import_Button, protocolAnalyzeButton, protocolPlotButton, protocol_clear_Button, protocol_decode_Button, protocol_generate_Button});

        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addComponent(protocol_generate_Button)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(icf_import_Button)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(protocol_decode_Button)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(protocolAnalyzeButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(protocol_clear_Button)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(protocolPlotButton, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel6Layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {icf_import_Button, protocolAnalyzeButton, protocolPlotButton, protocol_clear_Button, protocol_decode_Button, protocol_generate_Button});

        jPanel5.setBorder(javax.swing.BorderFactory.createTitledBorder("Send"));

        protocol_send_Button.setMnemonic('S');
        protocol_send_Button.setText("Send");
        protocol_send_Button.setToolTipText("Send code in Code window, or if empty, render new signal and send it to selected output device.");
        protocol_send_Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                protocol_send_ButtonActionPerformed(evt);
            }
        });

        protocol_outputhw_ComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "GlobalCache", "IRTrans (udp)", "LIRC", "Audio" }));
        protocol_outputhw_ComboBox.setToolTipText("Device used for when sending");
        protocol_outputhw_ComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                protocol_outputhw_ComboBoxActionPerformed(evt);
            }
        });

        protocol_stop_Button.setMnemonic('T');
        protocol_stop_Button.setText("Stop");
        protocol_stop_Button.setToolTipText("Stop ongoing IR transmission");
        protocol_stop_Button.setEnabled(false);
        protocol_stop_Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                protocol_stop_ButtonActionPerformed(evt);
            }
        });

        no_sends_protocol_ComboBox.setMaximumRowCount(20);
        no_sends_protocol_ComboBox.setModel(noSendsSignalsComboBoxModel);
        no_sends_protocol_ComboBox.setToolTipText("Number of times to send IR signal");

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(protocol_stop_Button, javax.swing.GroupLayout.PREFERRED_SIZE, 82, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addComponent(protocol_send_Button, javax.swing.GroupLayout.PREFERRED_SIZE, 82, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addComponent(no_sends_protocol_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 82, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addComponent(protocol_outputhw_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 82, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addComponent(protocol_outputhw_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(no_sends_protocol_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(protocol_send_Button)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(protocol_stop_Button)
                .addContainerGap())
        );

        javax.swing.GroupLayout analyzePanelLayout = new javax.swing.GroupLayout(analyzePanel);
        analyzePanel.setLayout(analyzePanelLayout);
        analyzePanelLayout.setHorizontalGroup(
            analyzePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(analyzePanelLayout.createSequentialGroup()
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 356, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addComponent(IRP_TextField)
        );
        analyzePanelLayout.setVerticalGroup(
            analyzePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(analyzePanelLayout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addComponent(IRP_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(analyzePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(analyzePanelLayout.createSequentialGroup()
                        .addGroup(analyzePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, 220, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 65, Short.MAX_VALUE))
                    .addComponent(jScrollPane3)))
        );

        protocolsSubPane.addTab("Analyze", analyzePanel);

        protocolExportButton.setMnemonic('X');
        protocolExportButton.setText("Export");
        protocolExportButton.setToolTipText("Perform actual export.");
        protocolExportButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                protocolExportButtonActionPerformed(evt);
            }
        });

        automaticFileNamesCheckBox.setMnemonic('A');
        automaticFileNamesCheckBox.setSelected(true);
        automaticFileNamesCheckBox.setText("Automatic File Names");
        automaticFileNamesCheckBox.setToolTipText("Perform the export to a file with automatically generated name. Otherwise, a file browser will be started.");

        exportGenerateTogglesCheckBox.setMnemonic('T');
        exportGenerateTogglesCheckBox.setText("Generate toggle pairs");
        exportGenerateTogglesCheckBox.setToolTipText("For protocol with toggles, generate both versions in the export file.");

        lastFTextField.setToolTipText("Last F to export (inclusive)");
        lastFTextField.setMinimumSize(new java.awt.Dimension(35, 27));
        lastFTextField.setPreferredSize(new java.awt.Dimension(35, 27));
        lastFTextField.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                generic_copy_paste_menu(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                generic_copy_paste_menu(evt);
            }
        });
        lastFTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                lastFTextFieldActionPerformed(evt);
            }
        });
        lastFTextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                lastFTextFieldFocusLost(evt);
            }
        });

        jLabel17.setText("Last F");

        exportFormatComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Text", "XML", "LIRC", "Wave", "Lintronic" }));
        exportFormatComboBox.setToolTipText("Type of export file");
        exportFormatComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportFormatComboBoxActionPerformed(evt);
            }
        });

        jLabel20.setText("Export Format");

        exportdir_TextField.setMaximumSize(new java.awt.Dimension(300, 27));
        exportdir_TextField.setMinimumSize(new java.awt.Dimension(300, 27));
        exportdir_TextField.setPreferredSize(new java.awt.Dimension(300, 27));
        exportdir_TextField.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                generic_copy_paste_menu(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                generic_copy_paste_menu(evt);
            }
        });
        exportdir_TextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportdir_TextFieldActionPerformed(evt);
            }
        });
        exportdir_TextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                exportdir_TextFieldFocusLost(evt);
            }
        });

        exportdir_browse_Button.setText("...");
        exportdir_browse_Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportdir_browse_ButtonActionPerformed(evt);
            }
        });

        jLabel21.setText("Export dir.");

        exportRawCheckBox.setMnemonic('R');
        exportRawCheckBox.setText("Raw");
        exportRawCheckBox.setToolTipText("Generate raw codes (timing in microseconds) in export");

        exportProntoCheckBox.setMnemonic('P');
        exportProntoCheckBox.setSelected(true);
        exportProntoCheckBox.setText("Pronto");
        exportProntoCheckBox.setToolTipText("Generate Pronto (CCF) codes in export");

        viewExportButton.setMnemonic('O');
        viewExportButton.setText("Open Last File");
        viewExportButton.setToolTipText("Open last export file (if one exists).");
        viewExportButton.setEnabled(false);
        viewExportButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                viewExportButtonActionPerformed(evt);
            }
        });

        openExportDirButton.setMnemonic('O');
        openExportDirButton.setText("Open");
        openExportDirButton.setToolTipText("Shows export directory");
        openExportDirButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openExportDirButtonActionPerformed(evt);
            }
        });

        exportRepetitionsComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "20", "50", "100" }));
        exportRepetitionsComboBox.setToolTipText("The number of times the repetition should be included in export. For wave only.");

        jLabel54.setText("# Repetitions");

        javax.swing.GroupLayout exportPanelLayout = new javax.swing.GroupLayout(exportPanel);
        exportPanel.setLayout(exportPanelLayout);
        exportPanelLayout.setHorizontalGroup(
            exportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(exportPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(exportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(exportPanelLayout.createSequentialGroup()
                        .addComponent(protocolExportButton)
                        .addGap(18, 18, 18)
                        .addComponent(viewExportButton))
                    .addGroup(exportPanelLayout.createSequentialGroup()
                        .addGroup(exportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel20)
                            .addComponent(jLabel17)
                            .addComponent(jLabel21))
                        .addGap(18, 18, 18)
                        .addGroup(exportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(exportPanelLayout.createSequentialGroup()
                                .addComponent(exportdir_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, 367, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(exportdir_browse_Button)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(openExportDirButton))
                            .addGroup(exportPanelLayout.createSequentialGroup()
                                .addGroup(exportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(lastFTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 41, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(exportFormatComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(12, 12, 12)
                                .addGroup(exportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(exportPanelLayout.createSequentialGroup()
                                        .addComponent(exportRawCheckBox)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(exportProntoCheckBox)
                                        .addGap(18, 18, 18)
                                        .addComponent(jLabel54, javax.swing.GroupLayout.PREFERRED_SIZE, 96, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(exportRepetitionsComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addComponent(exportGenerateTogglesCheckBox)))))
                    .addComponent(automaticFileNamesCheckBox))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        exportPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {protocolExportButton, viewExportButton});

        exportPanelLayout.setVerticalGroup(
            exportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(exportPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(exportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel17)
                    .addComponent(lastFTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(exportGenerateTogglesCheckBox))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(exportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel20)
                    .addComponent(exportFormatComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(exportRawCheckBox)
                    .addComponent(exportProntoCheckBox)
                    .addComponent(jLabel54)
                    .addComponent(exportRepetitionsComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(exportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel21)
                    .addComponent(exportdir_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(exportdir_browse_Button)
                    .addComponent(openExportDirButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(automaticFileNamesCheckBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(exportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(protocolExportButton)
                    .addComponent(viewExportButton))
                .addContainerGap(125, Short.MAX_VALUE))
        );

        exportPanelLayout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {protocolExportButton, viewExportButton});

        protocolsSubPane.addTab("Export", exportPanel);

        jLabel32.setText(" ");

        jTextArea1.setColumns(20);
        jTextArea1.setEditable(false);
        jTextArea1.setLineWrap(true);
        jTextArea1.setRows(2);
        jTextArea1.setText("Warning: Sending undocumented IR commands to your equipment may damage or even destroy it. By using this program, you agree to take the responsibility for possible damages yourself, and not to hold the author responsible.");
        jTextArea1.setToolTipText("You have been warned!");
        jTextArea1.setWrapStyleWord(true);
        jTextArea1.setFocusable(false);
        jScrollPane2.setViewportView(jTextArea1);

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Notes"));

        notesClearButton.setText("Clear");
        notesClearButton.setToolTipText("Not yet implemented.");
        notesClearButton.setEnabled(false);

        notesSaveButton.setText("Save");
        notesSaveButton.setToolTipText("Not yet implemented.");
        notesSaveButton.setEnabled(false);

        notesEditButton.setText("Edit");
        notesEditButton.setToolTipText("Not yet implemented.");
        notesEditButton.setEnabled(false);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(notesEditButton)
                    .addComponent(notesClearButton)
                    .addComponent(notesSaveButton))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel2Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {notesClearButton, notesEditButton, notesSaveButton});

        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(notesEditButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(notesClearButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(notesSaveButton)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel2Layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {notesClearButton, notesEditButton, notesSaveButton});

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("Run"));

        pauseButton.setMnemonic('P');
        pauseButton.setText("Pause");
        pauseButton.setToolTipText("Pause transmission, with possibility to resume. Not yet implemented.");
        pauseButton.setEnabled(false);
        pauseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pauseButtonActionPerformed(evt);
            }
        });

        currentFTextField.setEditable(false);
        currentFTextField.setToolTipText("Value of F in the signal recently sent.");
        currentFTextField.setMinimumSize(new java.awt.Dimension(35, 27));
        currentFTextField.setPreferredSize(new java.awt.Dimension(35, 27));
        currentFTextField.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                generic_copy_menu(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                generic_copy_menu(evt);
            }
        });
        currentFTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                currentFTextFieldActionPerformed(evt);
            }
        });
        currentFTextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                currentFTextFieldFocusLost(evt);
            }
        });

        startButton.setMnemonic('S');
        startButton.setText("Start");
        startButton.setToolTipText("Start sending sequence");
        startButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                startButtonActionPerformed(evt);
            }
        });

        stopButton.setMnemonic('T');
        stopButton.setText("Stop");
        stopButton.setEnabled(false);
        stopButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stopButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(startButton, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(stopButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(pauseButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(currentFTextField, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(0, 13, Short.MAX_VALUE))
        );

        jPanel3Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {pauseButton, startButton, stopButton});

        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addComponent(startButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(stopButton)
                .addGap(12, 12, 12)
                .addComponent(pauseButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(currentFTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel3Layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {pauseButton, startButton, stopButton});

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder("Parameters"));

        jLabel60.setText("# Sends");

        endFTextField.setToolTipText("Ending F to send");
        endFTextField.setMinimumSize(new java.awt.Dimension(35, 27));
        endFTextField.setPreferredSize(new java.awt.Dimension(35, 27));
        endFTextField.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                generic_copy_paste_menu(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                generic_copy_paste_menu(evt);
            }
        });
        endFTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                endFTextFieldActionPerformed(evt);
            }
        });
        endFTextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                endFTextFieldFocusLost(evt);
            }
        });

        jLabel27.setText("IR Device");

        delayTextField.setText("2");
        delayTextField.setToolTipText("Delay in seconds between different signals. Decimal number allowed.");
        delayTextField.setMinimumSize(new java.awt.Dimension(35, 27));
        delayTextField.setPreferredSize(new java.awt.Dimension(35, 27));
        delayTextField.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                generic_copy_paste_menu(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                generic_copy_paste_menu(evt);
            }
        });
        delayTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                delayTextFieldActionPerformed(evt);
            }
        });
        delayTextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                delayTextFieldFocusLost(evt);
            }
        });

        jLabel28.setText("Ending F");

        war_dialer_outputhw_ComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "GlobalCache", "IRTrans (udp)", "LIRC", "Audio" }));
        war_dialer_outputhw_ComboBox.setToolTipText("Device to use for sending");
        war_dialer_outputhw_ComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                war_dialer_outputhw_ComboBoxActionPerformed(evt);
            }
        });

        jLabel29.setText("Delay (s)");

        warDialerNoSendsComboBox.setMaximumRowCount(20);
        warDialerNoSendsComboBox.setModel(noSendsSignalsComboBoxModel);
        warDialerNoSendsComboBox.setToolTipText("Number of times to send IR signal");

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel60)
                    .addComponent(jLabel29)
                    .addComponent(jLabel28)
                    .addComponent(jLabel27, javax.swing.GroupLayout.PREFERRED_SIZE, 93, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(war_dialer_outputhw_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(endFTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 41, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(delayTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 41, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(warDialerNoSendsComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 87, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(0, 20, Short.MAX_VALUE))
        );

        jPanel4Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {delayTextField, endFTextField, warDialerNoSendsComboBox, war_dialer_outputhw_ComboBox});

        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel27)
                    .addComponent(war_dialer_outputhw_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(endFTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel28))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel29)
                    .addComponent(delayTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel60)
                    .addComponent(warDialerNoSendsComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel4Layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {delayTextField, endFTextField, warDialerNoSendsComboBox, war_dialer_outputhw_ComboBox});

        javax.swing.GroupLayout warDialerPanelLayout = new javax.swing.GroupLayout(warDialerPanel);
        warDialerPanel.setLayout(warDialerPanelLayout);
        warDialerPanelLayout.setHorizontalGroup(
            warDialerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(warDialerPanelLayout.createSequentialGroup()
                .addGroup(warDialerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(warDialerPanelLayout.createSequentialGroup()
                        .addGap(655, 655, 655)
                        .addComponent(jLabel32))
                    .addGroup(warDialerPanelLayout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 473, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        warDialerPanelLayout.setVerticalGroup(
            warDialerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(warDialerPanelLayout.createSequentialGroup()
                .addGroup(warDialerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(warDialerPanelLayout.createSequentialGroup()
                        .addGap(194, 194, 194)
                        .addComponent(jLabel32))
                    .addGroup(warDialerPanelLayout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(warDialerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(jPanel3, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jPanel4, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 25, Short.MAX_VALUE)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 88, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        protocolsSubPane.addTab("War Dialer", warDialerPanel);

        rendererComboBox.setMaximumRowCount(2);
        rendererComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "IrpMaster", "MakeHex" }));
        rendererComboBox.setToolTipText("Select IrpMaster (recommended) or MakeHex");
        rendererComboBox.setMaximumSize(new java.awt.Dimension(100, 25));
        rendererComboBox.setMinimumSize(new java.awt.Dimension(100, 25));
        rendererComboBox.setPreferredSize(new java.awt.Dimension(100, 25));
        rendererComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rendererComboBoxActionPerformed(evt);
            }
        });

        jLabel2.setText("Renderer");

        jLabel3.setText("Protocol");

        jLabel4.setText("D");

        jLabel5.setText("S");

        jLabel10.setText("F");

        jLabel12.setText("T");

        jLabel13.setText("Additional Parameters");

        javax.swing.GroupLayout protocolsPanelLayout = new javax.swing.GroupLayout(protocolsPanel);
        protocolsPanel.setLayout(protocolsPanelLayout);
        protocolsPanelLayout.setHorizontalGroup(
            protocolsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(protocolsPanelLayout.createSequentialGroup()
                .addGroup(protocolsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(protocolsPanelLayout.createSequentialGroup()
                        .addGroup(protocolsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(rendererComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 85, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel2))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(protocolsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(protocol_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 94, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel3))
                        .addGroup(protocolsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(protocolsPanelLayout.createSequentialGroup()
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(deviceno_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, 41, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(protocolsPanelLayout.createSequentialGroup()
                                .addGap(26, 26, 26)
                                .addComponent(jLabel4)))
                        .addGroup(protocolsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(protocolsPanelLayout.createSequentialGroup()
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(subdevice_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, 41, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(protocolsPanelLayout.createSequentialGroup()
                                .addGap(25, 25, 25)
                                .addComponent(jLabel5)))
                        .addGroup(protocolsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(protocolsPanelLayout.createSequentialGroup()
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(commandno_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, 41, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(protocolsPanelLayout.createSequentialGroup()
                                .addGap(25, 25, 25)
                                .addComponent(jLabel10)))
                        .addGroup(protocolsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(protocolsPanelLayout.createSequentialGroup()
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(toggle_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(protocolsPanelLayout.createSequentialGroup()
                                .addGap(23, 23, 23)
                                .addComponent(jLabel12, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(protocolsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel13)
                            .addComponent(protocol_params_TextField)))
                    .addComponent(protocolsSubPane, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addGap(640, 640, 640))
        );
        protocolsPanelLayout.setVerticalGroup(
            protocolsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(protocolsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(protocolsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(jLabel3)
                    .addComponent(jLabel13)
                    .addComponent(jLabel4)
                    .addComponent(jLabel5)
                    .addComponent(jLabel10)
                    .addComponent(jLabel12))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(protocolsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(rendererComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(protocol_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(deviceno_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(subdevice_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(commandno_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(toggle_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(protocol_params_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(protocolsSubPane)
                .addContainerGap())
        );

        protocolsPanelLayout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {commandno_TextField, deviceno_TextField, protocol_ComboBox, protocol_params_TextField, rendererComboBox, subdevice_TextField, toggle_ComboBox});

        mainTabbedPane.addTab("IR Protocols", null, protocolsPanel, "This pane deals with generating, sending, exporting, and analyzing of IR protocols.");

        outputHWTabbedPane.setToolTipText("This pane sets the properties of the output hardware.");

        jLabel34.setText("Discovered GC Type:");

        gcDiscoveredTypeLabel.setText("<unknown>");

        jPanel1.setBorder(javax.swing.BorderFactory.createEtchedBorder(javax.swing.border.EtchedBorder.RAISED));

        gc_address_TextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        gc_address_TextField.setText(Props.getInstance().getGlobalcacheIpName());
        gc_address_TextField.setToolTipText("IP-Address/Name of GlobalCache to use");
        gc_address_TextField.setMinimumSize(new java.awt.Dimension(120, 27));
        gc_address_TextField.setPreferredSize(new java.awt.Dimension(120, 27));
        gc_address_TextField.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                generic_copy_paste_menu(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                generic_copy_paste_menu(evt);
            }
        });
        gc_address_TextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                gc_address_TextFieldActionPerformed(evt);
            }
        });

        jLabel19.setText("IP Name/Address");

        jButton1.setMnemonic('T');
        jButton1.setText("Stop IR");
        jButton1.setToolTipText("Send the selected GlobalCache the stopir command.");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                gc_stop_ir_ActionPerformed(evt);
            }
        });

        discoverButton.setMnemonic('D');
        discoverButton.setText("Discover");
        discoverButton.setToolTipText("Try to discover a GlobalCache on LAN. Takes up to 60 seconds!");
        discoverButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                discoverButtonActionPerformed(evt);
            }
        });

        jLabel35.setText("Module");

        gc_browse_Button.setMnemonic('B');
        gc_browse_Button.setText("Browse");
        gc_browse_Button.setToolTipText("Open selected GlobalCache in the browser.");
        gc_browse_Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                gc_browse_ButtonActionPerformed(evt);
            }
        });

        jLabel36.setText("Port");

        gc_connector_ComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "1", "2", "3" }));
        gc_connector_ComboBox.setToolTipText("GlobalCache IR Connector to use");
        gc_connector_ComboBox.setMaximumSize(new java.awt.Dimension(32767, 27));
        gc_connector_ComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                gc_connector_ComboBoxActionPerformed(evt);
            }
        });

        globalCachePingButton.setMnemonic('P');
        globalCachePingButton.setText("Ping");
        globalCachePingButton.setToolTipText("Try to ping the device");
        globalCachePingButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                globalCachePingButtonActionPerformed(evt);
            }
        });

        gc_module_ComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "1", "2", "4", "5" }));
        gc_module_ComboBox.setToolTipText("GlobalCache IR Module to use");
        gc_module_ComboBox.setMaximumSize(new java.awt.Dimension(48, 28));
        gc_module_ComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                gc_module_ComboBoxActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(gc_address_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, 139, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(gc_module_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel19)
                        .addGap(27, 27, 27)
                        .addComponent(jLabel35)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel36)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(gc_connector_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(globalCachePingButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(discoverButton))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jButton1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(gc_browse_Button)))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel1Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {gc_connector_ComboBox, gc_module_ComboBox});

        jPanel1Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {discoverButton, gc_browse_Button, globalCachePingButton, jButton1});

        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel19)
                    .addComponent(jLabel35)
                    .addComponent(jLabel36))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(gc_address_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(gc_module_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(gc_connector_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton1)
                    .addComponent(gc_browse_Button))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(globalCachePingButton)
                    .addComponent(discoverButton))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel1Layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {gc_address_TextField, gc_connector_ComboBox, gc_module_ComboBox});

        jPanel1Layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {discoverButton, gc_browse_Button, globalCachePingButton, jButton1});

        javax.swing.GroupLayout globalcache_PanelLayout = new javax.swing.GroupLayout(globalcache_Panel);
        globalcache_Panel.setLayout(globalcache_PanelLayout);
        globalcache_PanelLayout.setHorizontalGroup(
            globalcache_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(globalcache_PanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(globalcache_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(globalcache_PanelLayout.createSequentialGroup()
                        .addComponent(jLabel34)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(gcDiscoveredTypeLabel)))
                .addContainerGap(74, Short.MAX_VALUE))
        );
        globalcache_PanelLayout.setVerticalGroup(
            globalcache_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(globalcache_PanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(globalcache_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel34)
                    .addComponent(gcDiscoveredTypeLabel))
                .addContainerGap(240, Short.MAX_VALUE))
        );

        outputHWTabbedPane.addTab("GlobalCache", globalcache_Panel);

        irtransVersionLabel.setText("<unknown>");

        jLabel18.setText("IrTrans Version:");

        irtransIPPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder(javax.swing.border.EtchedBorder.RAISED));

        irtransPingButton.setMnemonic('P');
        irtransPingButton.setText("Ping");
        irtransPingButton.setToolTipText("Try to ping the device");
        irtransPingButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                irtransPingButtonActionPerformed(evt);
            }
        });

        irtrans_address_TextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        irtrans_address_TextField.setText(Props.getInstance().getIrTransIpName());
        irtrans_address_TextField.setToolTipText("IP-Address/Name of IRTrans");
        irtrans_address_TextField.setMinimumSize(new java.awt.Dimension(120, 27));
        irtrans_address_TextField.setPreferredSize(new java.awt.Dimension(120, 27));
        irtrans_address_TextField.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                generic_copy_paste_menu(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                generic_copy_paste_menu(evt);
            }
        });
        irtrans_address_TextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                irtrans_address_TextFieldActionPerformed(evt);
            }
        });

        irtrans_browse_Button.setMnemonic('B');
        irtrans_browse_Button.setText("Browse");
        irtrans_browse_Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                irtrans_browse_ButtonActionPerformed(evt);
            }
        });

        jLabel38.setText("IR Port");

        irtrans_led_ComboBox.setMaximumRowCount(12);
        irtrans_led_ComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "intern", "extern", "both", "0", "1", "2", "3", "4", "5", "6", "7", "8" }));
        irtrans_led_ComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                irtrans_led_ComboBoxActionPerformed(evt);
            }
        });

        read_Button.setMnemonic('R');
        read_Button.setText("Read");
        read_Button.setToolTipText("Read version and predefined commands into memory");
        read_Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                read_ButtonActionPerformed(evt);
            }
        });

        jLabel37.setText("IP Name/Address");

        javax.swing.GroupLayout irtransIPPanelLayout = new javax.swing.GroupLayout(irtransIPPanel);
        irtransIPPanel.setLayout(irtransIPPanelLayout);
        irtransIPPanelLayout.setHorizontalGroup(
            irtransIPPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(irtransIPPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(irtransIPPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel37)
                    .addComponent(irtrans_address_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(irtransIPPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(irtransIPPanelLayout.createSequentialGroup()
                        .addComponent(irtrans_led_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(12, 12, 12)
                        .addComponent(read_Button)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(irtrans_browse_Button)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(irtransPingButton))
                    .addComponent(jLabel38))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        irtransIPPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {irtransPingButton, irtrans_browse_Button, read_Button});

        irtransIPPanelLayout.setVerticalGroup(
            irtransIPPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(irtransIPPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(irtransIPPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel37)
                    .addComponent(jLabel38))
                .addGap(2, 2, 2)
                .addGroup(irtransIPPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(irtrans_address_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(irtrans_led_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(irtrans_browse_Button)
                    .addComponent(irtransPingButton)
                    .addComponent(read_Button))
                .addContainerGap(92, Short.MAX_VALUE))
        );

        irtransIPPanelLayout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {irtransPingButton, irtrans_address_TextField, irtrans_browse_Button, irtrans_led_ComboBox, read_Button});

        irtransPredefinedPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder(javax.swing.border.EtchedBorder.RAISED));

        jLabel33.setText("Flashed Commands (IRDB)");

        jLabel52.setText("Remote");

        irtransCommandsComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "--" }));
        irtransCommandsComboBox.setToolTipText("Predefined Command");
        irtransCommandsComboBox.setEnabled(false);
        irtransCommandsComboBox.setMinimumSize(new java.awt.Dimension(140, 28));
        irtransCommandsComboBox.setPreferredSize(new java.awt.Dimension(140, 28));

        no_sends_irtrans_flashed_ComboBox.setMaximumRowCount(20);
        no_sends_irtrans_flashed_ComboBox.setModel(noSendsIrtransFlashedComboBoxModel);
        no_sends_irtrans_flashed_ComboBox.setToolTipText("Number of times to send IR signal");

        jLabel53.setText("Command");

        irtransRemotesComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "--" }));
        irtransRemotesComboBox.setToolTipText("Predefined Remote");
        irtransRemotesComboBox.setEnabled(false);
        irtransRemotesComboBox.setMinimumSize(new java.awt.Dimension(140, 28));
        irtransRemotesComboBox.setPreferredSize(new java.awt.Dimension(140, 28));
        irtransRemotesComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                irtransRemotesComboBoxActionPerformed(evt);
            }
        });

        jLabel51.setText("# Sends");

        irtransSendFlashedButton.setMnemonic('S');
        irtransSendFlashedButton.setText("Send");
        irtransSendFlashedButton.setToolTipText("Send selected command/remote from the IRTrans");
        irtransSendFlashedButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                irtransSendFlashedButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout irtransPredefinedPanelLayout = new javax.swing.GroupLayout(irtransPredefinedPanel);
        irtransPredefinedPanel.setLayout(irtransPredefinedPanelLayout);
        irtransPredefinedPanelLayout.setHorizontalGroup(
            irtransPredefinedPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(irtransPredefinedPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(irtransPredefinedPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel33)
                    .addGroup(irtransPredefinedPanelLayout.createSequentialGroup()
                        .addGroup(irtransPredefinedPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(irtransPredefinedPanelLayout.createSequentialGroup()
                                .addComponent(no_sends_irtrans_flashed_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(irtransRemotesComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(irtransPredefinedPanelLayout.createSequentialGroup()
                                .addComponent(jLabel51)
                                .addGap(36, 36, 36)
                                .addComponent(jLabel52)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(irtransPredefinedPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel53)
                            .addGroup(irtransPredefinedPanelLayout.createSequentialGroup()
                                .addComponent(irtransCommandsComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(irtransSendFlashedButton)))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        irtransPredefinedPanelLayout.setVerticalGroup(
            irtransPredefinedPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(irtransPredefinedPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel33)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(irtransPredefinedPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel51)
                    .addComponent(jLabel52)
                    .addComponent(jLabel53))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(irtransPredefinedPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(no_sends_irtrans_flashed_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(irtransRemotesComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(irtransCommandsComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(irtransSendFlashedButton))
                .addContainerGap(86, Short.MAX_VALUE))
        );

        irtransPredefinedPanelLayout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {irtransCommandsComboBox, irtransRemotesComboBox, irtransSendFlashedButton, no_sends_irtrans_flashed_ComboBox});

        javax.swing.GroupLayout irtrans_PanelLayout = new javax.swing.GroupLayout(irtrans_Panel);
        irtrans_Panel.setLayout(irtrans_PanelLayout);
        irtrans_PanelLayout.setHorizontalGroup(
            irtrans_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(irtrans_PanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(irtrans_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(irtrans_PanelLayout.createSequentialGroup()
                        .addComponent(jLabel18)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(irtransVersionLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(irtrans_PanelLayout.createSequentialGroup()
                        .addGroup(irtrans_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(irtransPredefinedPanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(irtransIPPanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(0, 48, Short.MAX_VALUE)))
                .addContainerGap())
        );
        irtrans_PanelLayout.setVerticalGroup(
            irtrans_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(irtrans_PanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(irtransIPPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(irtransPredefinedPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(irtrans_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel18)
                    .addComponent(irtransVersionLabel))
                .addContainerGap())
        );

        outputHWTabbedPane.addTab("IRTrans", irtrans_Panel);

        lircServerVersionText.setText("<unknown>");
        lircServerVersionText.setEnabled(false);

        lircServerVersionLabel.setText("Lirc Server Version:");
        lircServerVersionLabel.setEnabled(false);

        lircIPPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder(javax.swing.border.EtchedBorder.RAISED));
        lircIPPanel.setToolTipText("IP properties");

        jLabel45.setText("TCP Port");

        LircIPAddressTextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        LircIPAddressTextField.setText(Props.getInstance().getLircIpName());
        LircIPAddressTextField.setToolTipText("IP-Address/Name of Lirc Server");
        LircIPAddressTextField.setMinimumSize(new java.awt.Dimension(120, 27));
        LircIPAddressTextField.setPreferredSize(new java.awt.Dimension(120, 27));
        LircIPAddressTextField.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                generic_copy_paste_menu(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                generic_copy_paste_menu(evt);
            }
        });
        LircIPAddressTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                LircIPAddressTextFieldActionPerformed(evt);
            }
        });

        read_lirc_Button.setMnemonic('R');
        read_lirc_Button.setText("Read");
        read_lirc_Button.setToolTipText("Read version and preprogrammed commands into memory");
        read_lirc_Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                read_lirc_ButtonActionPerformed(evt);
            }
        });

        lircPingButton.setMnemonic('P');
        lircPingButton.setText("Ping");
        lircPingButton.setToolTipText("Try to ping device");
        lircPingButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                lircPingButtonActionPerformed(evt);
            }
        });

        lircPortTextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        lircPortTextField.setText(Props.getInstance().getLircPort());
        lircPortTextField.setToolTipText("Port number of LIRC server to use. Default is 8765.");
        lircPortTextField.setMinimumSize(new java.awt.Dimension(120, 27));
        lircPortTextField.setPreferredSize(new java.awt.Dimension(120, 27));
        lircPortTextField.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                generic_copy_paste_menu(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                generic_copy_paste_menu(evt);
            }
        });
        lircPortTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                lircPortTextFieldActionPerformed(evt);
            }
        });

        jLabel44.setText("IP Name/Address");

        lircTransmitterComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "0", "1", "2", "3", "4", "5", "6", "7", "8" }));
        lircTransmitterComboBox.setSelectedIndex(lircTransmitterDefaultIndex);
        lircTransmitterComboBox.setToolTipText("IR Transmitter to use. Not implemented or sensible on many Lirc servers.");
        lircTransmitterComboBox.setEnabled(false);
        lircTransmitterComboBox.setMaximumSize(new java.awt.Dimension(32767, 27));
        lircTransmitterComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                lircTransmitterComboBoxActionPerformed(evt);
            }
        });

        jLabel46.setText("Transm.");

        lircStopIrButton.setMnemonic('T');
        lircStopIrButton.setText("Stop IR");
        lircStopIrButton.setToolTipText("Send the selected LIRC-server a stop command.");
        lircStopIrButton.setEnabled(false);
        lircStopIrButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                lircStopIrButtongc_stop_ir_ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout lircIPPanelLayout = new javax.swing.GroupLayout(lircIPPanel);
        lircIPPanel.setLayout(lircIPPanelLayout);
        lircIPPanelLayout.setHorizontalGroup(
            lircIPPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(lircIPPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(lircIPPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(LircIPAddressTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 138, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel44))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(lircIPPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel45)
                    .addComponent(lircPortTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 52, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(lircIPPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(lircIPPanelLayout.createSequentialGroup()
                        .addComponent(lircTransmitterComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(lircStopIrButton, javax.swing.GroupLayout.PREFERRED_SIZE, 84, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(read_lirc_Button)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lircPingButton))
                    .addComponent(jLabel46))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        lircIPPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {lircPingButton, lircStopIrButton, read_lirc_Button});

        lircIPPanelLayout.setVerticalGroup(
            lircIPPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(lircIPPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(lircIPPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel45)
                    .addComponent(jLabel46)
                    .addComponent(jLabel44))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(lircIPPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lircPortTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lircTransmitterComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lircStopIrButton)
                    .addComponent(read_lirc_Button)
                    .addComponent(lircPingButton)
                    .addComponent(LircIPAddressTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        lircIPPanelLayout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {LircIPAddressTextField, lircPingButton, lircPortTextField, lircStopIrButton, lircTransmitterComboBox, read_lirc_Button});

        lircPredefinedPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder(javax.swing.border.EtchedBorder.RAISED));

        jLabel47.setText("Predefined commands");

        lircCommandsComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "--" }));
        lircCommandsComboBox.setToolTipText("Predefined Command");
        lircCommandsComboBox.setEnabled(false);
        lircCommandsComboBox.setMinimumSize(new java.awt.Dimension(140, 28));
        lircCommandsComboBox.setPreferredSize(new java.awt.Dimension(140, 28));

        jLabel49.setText("Remote");

        lircRemotesComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "--" }));
        lircRemotesComboBox.setToolTipText("Predefined Remote");
        lircRemotesComboBox.setEnabled(false);
        lircRemotesComboBox.setMinimumSize(new java.awt.Dimension(140, 28));
        lircRemotesComboBox.setPreferredSize(new java.awt.Dimension(140, 28));
        lircRemotesComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                lircRemotesComboBoxActionPerformed(evt);
            }
        });

        jLabel50.setText("Command");

        lircSendPredefinedButton.setMnemonic('S');
        lircSendPredefinedButton.setText("Send");
        lircSendPredefinedButton.setToolTipText("Send selected command/remote from the LIRC server");
        lircSendPredefinedButton.setEnabled(false);
        lircSendPredefinedButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                lircSendPredefinedButtonActionPerformed(evt);
            }
        });

        jLabel48.setText("# Sends");

        noLircPredefinedsComboBox.setMaximumRowCount(20);
        noLircPredefinedsComboBox.setModel(noSendsLircPredefinedsComboBoxModel);
        noLircPredefinedsComboBox.setToolTipText("Number of times to send IR signal");
        noLircPredefinedsComboBox.setEnabled(false);

        javax.swing.GroupLayout lircPredefinedPanelLayout = new javax.swing.GroupLayout(lircPredefinedPanel);
        lircPredefinedPanel.setLayout(lircPredefinedPanelLayout);
        lircPredefinedPanelLayout.setHorizontalGroup(
            lircPredefinedPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(lircPredefinedPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(lircPredefinedPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(lircPredefinedPanelLayout.createSequentialGroup()
                        .addGroup(lircPredefinedPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel48)
                            .addComponent(noLircPredefinedsComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(lircPredefinedPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lircRemotesComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel49))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(lircPredefinedPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(lircPredefinedPanelLayout.createSequentialGroup()
                                .addComponent(lircCommandsComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(lircSendPredefinedButton, javax.swing.GroupLayout.PREFERRED_SIZE, 68, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(lircPredefinedPanelLayout.createSequentialGroup()
                                .addComponent(jLabel50)
                                .addGap(0, 0, Short.MAX_VALUE))))
                    .addGroup(lircPredefinedPanelLayout.createSequentialGroup()
                        .addComponent(jLabel47)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        lircPredefinedPanelLayout.setVerticalGroup(
            lircPredefinedPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(lircPredefinedPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel47)
                .addGap(18, 18, 18)
                .addGroup(lircPredefinedPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel48)
                    .addComponent(jLabel49)
                    .addComponent(jLabel50))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(lircPredefinedPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(noLircPredefinedsComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lircRemotesComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lircCommandsComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lircSendPredefinedButton))
                .addContainerGap())
        );

        javax.swing.GroupLayout lircPanelLayout = new javax.swing.GroupLayout(lircPanel);
        lircPanel.setLayout(lircPanelLayout);
        lircPanelLayout.setHorizontalGroup(
            lircPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(lircPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(lircPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(lircPanelLayout.createSequentialGroup()
                        .addComponent(lircServerVersionLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lircServerVersionText, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(lircIPPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lircPredefinedPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );
        lircPanelLayout.setVerticalGroup(
            lircPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(lircPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lircIPPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lircPredefinedPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addGroup(lircPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lircServerVersionLabel)
                    .addComponent(lircServerVersionText))
                .addGap(35, 35, 35))
        );

        outputHWTabbedPane.addTab("LIRC", lircPanel);

        audioPanel.setToolTipText("Parameters for Audio/wave creation");

        audioGetLineButton.setMnemonic('G');
        audioGetLineButton.setText("Get Line");
        audioGetLineButton.setToolTipText("Try to allocate an appropriate audio line to the system's audio mixer.");
        audioGetLineButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                audioGetLineButtonActionPerformed(evt);
            }
        });

        audioReleaseLineButton.setMnemonic('R');
        audioReleaseLineButton.setText("Release Line");
        audioReleaseLineButton.setToolTipText("Release audio line");
        audioReleaseLineButton.setEnabled(false);
        audioReleaseLineButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                audioReleaseLineButtonActionPerformed(evt);
            }
        });

        jLabel59.setText("The settings herein also take effect when generating wave-exports!");

        audioFormatPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder(javax.swing.border.EtchedBorder.RAISED));
        audioFormatPanel.setToolTipText("Parameters of the audio format are set here.");

        jLabel56.setText("Sample size");

        audioSampleSizeComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "8", "16" }));
        audioSampleSizeComboBox.setToolTipText("Number of bits in one sample. Normally 8.");

        jLabel55.setText("Sample freq.");

        jLabel57.setText("Channels");

        audioSampleFrequencyComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "44100", "48000", "96000", "192000" }));
        audioSampleFrequencyComboBox.setSelectedIndex(1);
        audioSampleFrequencyComboBox.setToolTipText("The number of samples per second in generated data.");

        jLabel58.setText("Byte order");

        audioChannelsComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "1", "2" }));
        audioChannelsComboBox.setToolTipText("Number of channels in generated data. Normal setting is 1. If 2 (\"stereo\") the second channel is in opposite phase to the first.");

        audioBigEndianCheckBox.setText("Big endian");
        audioBigEndianCheckBox.setToolTipText("Order of bytes in 16 bit samples. Normally unchecked.");

        javax.swing.GroupLayout audioFormatPanelLayout = new javax.swing.GroupLayout(audioFormatPanel);
        audioFormatPanel.setLayout(audioFormatPanelLayout);
        audioFormatPanelLayout.setHorizontalGroup(
            audioFormatPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(audioFormatPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(audioFormatPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(audioFormatPanelLayout.createSequentialGroup()
                        .addGroup(audioFormatPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(audioFormatPanelLayout.createSequentialGroup()
                                .addGroup(audioFormatPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel56)
                                    .addComponent(jLabel57))
                                .addGap(18, 18, 18)
                                .addGroup(audioFormatPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(audioSampleSizeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(audioChannelsComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addGroup(audioFormatPanelLayout.createSequentialGroup()
                                .addComponent(jLabel55)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(audioSampleFrequencyComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(17, 17, Short.MAX_VALUE))
                    .addGroup(audioFormatPanelLayout.createSequentialGroup()
                        .addComponent(jLabel58)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(audioBigEndianCheckBox)
                        .addContainerGap())))
        );

        audioFormatPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {audioChannelsComboBox, audioSampleFrequencyComboBox, audioSampleSizeComboBox});

        audioFormatPanelLayout.setVerticalGroup(
            audioFormatPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(audioFormatPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(audioFormatPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel55)
                    .addComponent(audioSampleFrequencyComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(audioFormatPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel56)
                    .addComponent(audioSampleSizeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(audioFormatPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel57)
                    .addComponent(audioChannelsComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(audioFormatPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel58)
                    .addComponent(audioBigEndianCheckBox))
                .addContainerGap(26, Short.MAX_VALUE))
        );

        audioFormatPanelLayout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {audioChannelsComboBox, audioSampleFrequencyComboBox, audioSampleSizeComboBox});

        audioOptionsPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder(javax.swing.border.EtchedBorder.RAISED));
        audioOptionsPanel.setToolTipText("Other parameters for generation of wave files");

        audioWaveformComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "square", "sine" }));
        audioWaveformComboBox.setToolTipText("Use sines or square waves in the generated data.");

        audioOmitCheckBox.setMnemonic('T');
        audioOmitCheckBox.setText("Omit trailing gap");
        audioOmitCheckBox.setToolTipText("If checked, do not generate data for the last period of silence.");

        audioDivideCheckBox.setSelected(true);
        audioDivideCheckBox.setText("Divide carrier");
        audioDivideCheckBox.setToolTipText("Divide carrier frequency by two, for the use of 20kHz sound equipment and a pair of IR LEDs in antiparallel. Normally selected.");

        jLabel30.setText("Wave form");

        javax.swing.GroupLayout audioOptionsPanelLayout = new javax.swing.GroupLayout(audioOptionsPanel);
        audioOptionsPanel.setLayout(audioOptionsPanelLayout);
        audioOptionsPanelLayout.setHorizontalGroup(
            audioOptionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(audioOptionsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(audioOptionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(audioDivideCheckBox)
                    .addComponent(audioOmitCheckBox)
                    .addGroup(audioOptionsPanelLayout.createSequentialGroup()
                        .addComponent(audioWaveformComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel30)))
                .addContainerGap())
        );
        audioOptionsPanelLayout.setVerticalGroup(
            audioOptionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(audioOptionsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(audioDivideCheckBox)
                .addGap(22, 22, 22)
                .addComponent(audioOmitCheckBox)
                .addGap(18, 18, 18)
                .addGroup(audioOptionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(audioWaveformComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel30))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout audioPanelLayout = new javax.swing.GroupLayout(audioPanel);
        audioPanel.setLayout(audioPanelLayout);
        audioPanelLayout.setHorizontalGroup(
            audioPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(audioPanelLayout.createSequentialGroup()
                .addComponent(audioFormatPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(audioOptionsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(audioPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(audioPanelLayout.createSequentialGroup()
                        .addGap(68, 68, 68)
                        .addComponent(audioGetLineButton))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, audioPanelLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(audioReleaseLineButton)))
                .addContainerGap())
            .addGroup(audioPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel59)
                .addGap(0, 0, Short.MAX_VALUE))
        );

        audioPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {audioGetLineButton, audioReleaseLineButton});

        audioPanelLayout.setVerticalGroup(
            audioPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(audioPanelLayout.createSequentialGroup()
                .addGap(19, 19, 19)
                .addGroup(audioPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(audioPanelLayout.createSequentialGroup()
                        .addGap(4, 4, 4)
                        .addComponent(audioGetLineButton)
                        .addGap(18, 18, 18)
                        .addComponent(audioReleaseLineButton))
                    .addComponent(audioFormatPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(audioOptionsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 172, Short.MAX_VALUE)
                .addComponent(jLabel59)
                .addContainerGap())
        );

        outputHWTabbedPane.addTab("Audio", audioPanel);

        mainTabbedPane.addTab("Output HW", null, outputHWTabbedPane, "This pane sets the properties of the output hardware.");

        hexcalcPanel.setToolTipText("This pane consists of an interactive calculator for common computations on IR signals.");

        numbersPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder(javax.swing.border.EtchedBorder.RAISED));

        hex_TextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        hex_TextField.setText("00");
        hex_TextField.setToolTipText("Enter hexadecimal number here, then press return.");
        hex_TextField.setMinimumSize(new java.awt.Dimension(100, 27));
        hex_TextField.setPreferredSize(new java.awt.Dimension(100, 27));
        hex_TextField.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                generic_copy_paste_menu(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                generic_copy_paste_menu(evt);
            }
        });
        hex_TextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                hex_TextFieldActionPerformed(evt);
            }
        });
        hex_TextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                hex_TextFieldFocusLost(evt);
            }
        });

        reverse_decimal_TextField.setEditable(false);
        reverse_decimal_TextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        reverse_decimal_TextField.setText("0");
        reverse_decimal_TextField.setPreferredSize(new java.awt.Dimension(100, 27));
        reverse_decimal_TextField.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                generic_copy_menu(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                generic_copy_menu(evt);
            }
        });

        jLabel8.setText("Complement");

        jLabel43.setText("EFC5^-1");

        complement_decimal_TextField.setEditable(false);
        complement_decimal_TextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        complement_decimal_TextField.setText("255");
        complement_decimal_TextField.setPreferredSize(new java.awt.Dimension(100, 27));
        complement_decimal_TextField.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                generic_copy_menu(evt);
            }
        });

        reverse_complement_hex_TextField.setEditable(false);
        reverse_complement_hex_TextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        reverse_complement_hex_TextField.setText("FF");
        reverse_complement_hex_TextField.setPreferredSize(new java.awt.Dimension(100, 27));
        reverse_complement_hex_TextField.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                generic_copy_menu(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                generic_copy_menu(evt);
            }
        });

        jLabel14.setText("Reverse");

        jLabel41.setText("EFC^-1");

        jLabel6.setText("Decimal");

        from_efc_decimal_TextField.setEditable(false);
        from_efc_decimal_TextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        from_efc_decimal_TextField.setText("70");
        from_efc_decimal_TextField.setPreferredSize(new java.awt.Dimension(100, 27));
        from_efc_decimal_TextField.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                generic_copy_menu(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                generic_copy_menu(evt);
            }
        });

        from_efc_hex_TextField.setEditable(false);
        from_efc_hex_TextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        from_efc_hex_TextField.setText("46");
        from_efc_hex_TextField.setPreferredSize(new java.awt.Dimension(100, 27));
        from_efc_hex_TextField.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                generic_copy_menu(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                generic_copy_menu(evt);
            }
        });

        reverse_hex_TextField.setEditable(false);
        reverse_hex_TextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        reverse_hex_TextField.setText("00");
        reverse_hex_TextField.setMinimumSize(new java.awt.Dimension(100, 27));
        reverse_hex_TextField.setPreferredSize(new java.awt.Dimension(100, 27));
        reverse_hex_TextField.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                generic_copy_menu(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                generic_copy_menu(evt);
            }
        });

        jLabel40.setText("EFC");

        reverse_complement_decimal_TextField.setEditable(false);
        reverse_complement_decimal_TextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        reverse_complement_decimal_TextField.setText("255");
        reverse_complement_decimal_TextField.setPreferredSize(new java.awt.Dimension(100, 27));
        reverse_complement_decimal_TextField.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                generic_copy_menu(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                generic_copy_menu(evt);
            }
        });

        jLabel15.setText("Rev.-Compl.");

        efc5_decimal_TextField.setEditable(false);
        efc5_decimal_TextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        efc5_decimal_TextField.setText("18");
        efc5_decimal_TextField.setPreferredSize(new java.awt.Dimension(100, 27));
        efc5_decimal_TextField.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                generic_copy_menu(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                generic_copy_menu(evt);
            }
        });

        from_efc5_decimal_TextField.setEditable(false);
        from_efc5_decimal_TextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        from_efc5_decimal_TextField.setText("70");
        from_efc5_decimal_TextField.setPreferredSize(new java.awt.Dimension(100, 27));
        from_efc5_decimal_TextField.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                generic_copy_menu(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                generic_copy_menu(evt);
            }
        });

        efc_decimal_TextField.setEditable(false);
        efc_decimal_TextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        efc_decimal_TextField.setText("18");
        efc_decimal_TextField.setPreferredSize(new java.awt.Dimension(100, 27));
        efc_decimal_TextField.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                generic_copy_menu(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                generic_copy_menu(evt);
            }
        });

        jLabel9.setText("Input");

        from_efc5_hex_TextField.setEditable(false);
        from_efc5_hex_TextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        from_efc5_hex_TextField.setText("46");
        from_efc5_hex_TextField.setPreferredSize(new java.awt.Dimension(100, 27));
        from_efc5_hex_TextField.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                generic_copy_menu(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                generic_copy_menu(evt);
            }
        });

        efc_hex_TextField.setEditable(false);
        efc_hex_TextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        efc_hex_TextField.setText("12");
        efc_hex_TextField.setPreferredSize(new java.awt.Dimension(100, 27));
        efc_hex_TextField.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                generic_copy_menu(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                generic_copy_menu(evt);
            }
        });

        decimal_TextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        decimal_TextField.setText("0");
        decimal_TextField.setToolTipText("Enter decimal number here, then press return.");
        decimal_TextField.setMinimumSize(new java.awt.Dimension(100, 27));
        decimal_TextField.setPreferredSize(new java.awt.Dimension(100, 27));
        decimal_TextField.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                generic_copy_paste_menu(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                generic_copy_paste_menu(evt);
            }
        });
        decimal_TextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                decimal_TextFieldActionPerformed(evt);
            }
        });
        decimal_TextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                decimal_TextFieldFocusLost(evt);
            }
        });

        jLabel42.setText("EFC5");

        complement_hex_TextField.setEditable(false);
        complement_hex_TextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        complement_hex_TextField.setText("FF");
        complement_hex_TextField.setMinimumSize(new java.awt.Dimension(100, 27));
        complement_hex_TextField.setPreferredSize(new java.awt.Dimension(100, 27));
        complement_hex_TextField.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                generic_copy_menu(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                generic_copy_menu(evt);
            }
        });

        efc5_hex_TextField.setEditable(false);
        efc5_hex_TextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        efc5_hex_TextField.setText("12");
        efc5_hex_TextField.setPreferredSize(new java.awt.Dimension(100, 27));
        efc5_hex_TextField.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                generic_copy_menu(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                generic_copy_menu(evt);
            }
        });

        jLabel7.setText("Hex");

        javax.swing.GroupLayout numbersPanelLayout = new javax.swing.GroupLayout(numbersPanel);
        numbersPanel.setLayout(numbersPanelLayout);
        numbersPanelLayout.setHorizontalGroup(
            numbersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(numbersPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(numbersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel14)
                    .addComponent(jLabel15)
                    .addComponent(jLabel40)
                    .addComponent(jLabel41)
                    .addComponent(jLabel42)
                    .addComponent(jLabel43)
                    .addComponent(jLabel8)
                    .addComponent(jLabel9))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(numbersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel6)
                    .addComponent(decimal_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(complement_decimal_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(reverse_decimal_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(reverse_complement_decimal_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(efc_decimal_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(from_efc_decimal_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(efc5_decimal_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(from_efc5_decimal_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(numbersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(complement_hex_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(reverse_hex_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(reverse_complement_hex_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(efc_hex_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(from_efc_hex_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(efc5_hex_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(from_efc5_hex_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(hex_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel7))
                .addContainerGap())
        );
        numbersPanelLayout.setVerticalGroup(
            numbersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(numbersPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(numbersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(jLabel7))
                .addGroup(numbersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel9)
                    .addComponent(decimal_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(hex_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(numbersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(complement_decimal_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(complement_hex_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel8))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(numbersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(reverse_decimal_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(reverse_hex_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel14))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(numbersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(reverse_complement_decimal_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(reverse_complement_hex_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel15))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(numbersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(efc_decimal_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(efc_hex_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel40))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(numbersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(from_efc_decimal_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(from_efc_hex_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel41))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(numbersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(efc5_decimal_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(efc5_hex_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel42))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(numbersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(from_efc5_decimal_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(from_efc5_hex_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel43))
                .addContainerGap(162, Short.MAX_VALUE))
        );

        timeFrequencyPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder(javax.swing.border.EtchedBorder.RAISED));

        jLabel24.setText("Time (us)");

        frequency_TextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        frequency_TextField.setText("40000");
        frequency_TextField.setToolTipText("Enter modulation frequency here, then press return.");
        frequency_TextField.setMinimumSize(new java.awt.Dimension(100, 27));
        frequency_TextField.setPreferredSize(new java.awt.Dimension(100, 27));
        frequency_TextField.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                generic_copy_paste_menu(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                generic_copy_paste_menu(evt);
            }
        });
        frequency_TextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                frequency_TextFieldActionPerformed(evt);
            }
        });
        frequency_TextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                frequency_TextFieldFocusLost(evt);
            }
        });

        prontocode_TextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        prontocode_TextField.setText("0");
        prontocode_TextField.setToolTipText("Enter Pronto frequency code here, then press return.");
        prontocode_TextField.setMinimumSize(new java.awt.Dimension(100, 27));
        prontocode_TextField.setPreferredSize(new java.awt.Dimension(100, 27));
        prontocode_TextField.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                generic_copy_paste_menu(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                generic_copy_paste_menu(evt);
            }
        });
        prontocode_TextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                prontocode_TextFieldActionPerformed(evt);
            }
        });
        prontocode_TextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                prontocode_TextFieldFocusLost(evt);
            }
        });

        jLabel23.setText("# Periods (dec)");

        jLabel39.setText("# Periods (hex)");

        time_TextField.setEditable(false);
        time_TextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        time_TextField.setText("0");
        time_TextField.setToolTipText("Duration in microseconds");
        time_TextField.setMinimumSize(new java.awt.Dimension(100, 27));
        time_TextField.setPreferredSize(new java.awt.Dimension(100, 27));
        time_TextField.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                time_TextFieldMouseEntered(evt);
            }
            public void mousePressed(java.awt.event.MouseEvent evt) {
                generic_copy_paste_menu(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                generic_copy_paste_menu(evt);
            }
        });
        time_TextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                time_TextFieldActionPerformed(evt);
            }
        });
        time_TextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                time_TextFieldFocusLost(evt);
            }
        });

        jLabel25.setText("Prontocode");

        no_periods_hex_TextField.setEditable(false);
        no_periods_hex_TextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        no_periods_hex_TextField.setText("1");
        no_periods_hex_TextField.setToolTipText("Click to enable");
        no_periods_hex_TextField.setMinimumSize(new java.awt.Dimension(100, 27));
        no_periods_hex_TextField.setPreferredSize(new java.awt.Dimension(100, 27));
        no_periods_hex_TextField.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                no_periods_hex_TextFieldMouseEntered(evt);
            }
            public void mousePressed(java.awt.event.MouseEvent evt) {
                generic_copy_paste_menu(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                generic_copy_paste_menu(evt);
            }
        });
        no_periods_hex_TextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                no_periods_hex_TextFieldActionPerformed(evt);
            }
        });
        no_periods_hex_TextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                no_periods_hex_TextFieldFocusLost(evt);
            }
        });

        jLabel22.setText("Frequency (Hz)");

        no_periods_TextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        no_periods_TextField.setText("1");
        no_periods_TextField.setToolTipText("Number of periods in selected frequency");
        no_periods_TextField.setMinimumSize(new java.awt.Dimension(100, 27));
        no_periods_TextField.setPreferredSize(new java.awt.Dimension(100, 27));
        no_periods_TextField.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                no_periods_TextFieldMouseEntered(evt);
            }
            public void mousePressed(java.awt.event.MouseEvent evt) {
                generic_copy_paste_menu(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                generic_copy_paste_menu(evt);
            }
        });
        no_periods_TextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                no_periods_TextFieldActionPerformed(evt);
            }
        });
        no_periods_TextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                no_periods_TextFieldFocusLost(evt);
            }
        });

        javax.swing.GroupLayout timeFrequencyPanelLayout = new javax.swing.GroupLayout(timeFrequencyPanel);
        timeFrequencyPanel.setLayout(timeFrequencyPanelLayout);
        timeFrequencyPanelLayout.setHorizontalGroup(
            timeFrequencyPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(timeFrequencyPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(timeFrequencyPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(time_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(timeFrequencyPanelLayout.createSequentialGroup()
                        .addGroup(timeFrequencyPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(frequency_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel22)
                            .addComponent(jLabel24)
                            .addComponent(no_periods_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel23, javax.swing.GroupLayout.Alignment.TRAILING))
                        .addGap(18, 18, 18)
                        .addGroup(timeFrequencyPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(no_periods_hex_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel25)
                            .addComponent(prontocode_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel39))))
                .addContainerGap())
        );
        timeFrequencyPanelLayout.setVerticalGroup(
            timeFrequencyPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(timeFrequencyPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(timeFrequencyPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel22)
                    .addComponent(jLabel25))
                .addGroup(timeFrequencyPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(frequency_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(prontocode_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(14, 14, 14)
                .addGroup(timeFrequencyPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel23)
                    .addComponent(jLabel39))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(timeFrequencyPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(no_periods_hex_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(no_periods_TextField, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(jLabel24)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(time_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout hexcalcPanelLayout = new javax.swing.GroupLayout(hexcalcPanel);
        hexcalcPanel.setLayout(hexcalcPanelLayout);
        hexcalcPanelLayout.setHorizontalGroup(
            hexcalcPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(hexcalcPanelLayout.createSequentialGroup()
                .addComponent(numbersPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 334, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(timeFrequencyPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 250, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        hexcalcPanelLayout.setVerticalGroup(
            hexcalcPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(timeFrequencyPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(numbersPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        mainTabbedPane.addTab("IRcalc", null, hexcalcPanel, "This pane consists of an interactive calculator for common computations on IR signals.");
        hexcalcPanel.getAccessibleContext().setAccessibleName("IRCalc");

        optionsPanel.setToolTipText("This pane sets some program options.");

        jLabel16.setText("IRP Protocols");

        IrpProtocolsTextField.setText(Props.getInstance().getIrpmasterConfigfile());
        IrpProtocolsTextField.setToolTipText("Path to IrpMaster's configuration file.");
        IrpProtocolsTextField.setMaximumSize(new java.awt.Dimension(300, 27));
        IrpProtocolsTextField.setMinimumSize(new java.awt.Dimension(300, 27));
        IrpProtocolsTextField.setPreferredSize(new java.awt.Dimension(300, 27));
        IrpProtocolsTextField.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                generic_copy_paste_menu(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                generic_copy_paste_menu(evt);
            }
        });
        IrpProtocolsTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                IrpProtocolsTextFieldActionPerformed(evt);
            }
        });
        IrpProtocolsTextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                IrpProtocolsTextFieldFocusLost(evt);
            }
        });

        irpProtocolsSelectButton.setText("...");
        irpProtocolsSelectButton.setToolTipText("Browse for file path.");
        irpProtocolsSelectButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                irpProtocolsSelect(evt);
            }
        });

        IrpProtocolsBrowseButton.setText("Open");
        IrpProtocolsBrowseButton.setToolTipText("Open file in editor.");
        IrpProtocolsBrowseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                irpProtocolsBrowse(evt);
            }
        });

        makehexIrpDirTextField.setText(Props.getInstance().getMakehexIrpdir());
        makehexIrpDirTextField.setToolTipText("Directory containing Makehex' IRP-Files. Not used by IrpMaster.");
        makehexIrpDirTextField.setMaximumSize(new java.awt.Dimension(300, 27));
        makehexIrpDirTextField.setMinimumSize(new java.awt.Dimension(300, 27));
        makehexIrpDirTextField.setPreferredSize(new java.awt.Dimension(300, 27));
        makehexIrpDirTextField.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                generic_copy_paste_menu(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                generic_copy_paste_menu(evt);
            }
        });
        makehexIrpDirTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                makehexIrpDirTextFieldActionPerformed(evt);
            }
        });
        makehexIrpDirTextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                makehexIrpDirTextFieldFocusLost(evt);
            }
        });

        macro_select_Button.setText("...");
        macro_select_Button.setToolTipText("Browse for directory.");
        macro_select_Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                makehexIrpDirSelect(evt);
            }
        });

        makehexIrpDirBrowseButton.setText("Open");
        makehexIrpDirBrowseButton.setToolTipText("Open directory in browser.");
        makehexIrpDirBrowseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                makehexIrpDirBrowseButtonActionPerformed(evt);
            }
        });

        verbose_CheckBox.setMnemonic('V');
        verbose_CheckBox.setText("Verbose");
        verbose_CheckBox.setToolTipText("Select for verbose execution of some commands.");
        verbose_CheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                verbose_CheckBoxActionPerformed(evt);
            }
        });

        debug_TextField.setText(Integer.toString(debug));
        debug_TextField.setToolTipText("Debug code to be handled over to invoked programs. Hexadecimal, octal, binary, decimal entry are all accepted.");
        debug_TextField.setMaximumSize(new java.awt.Dimension(50, 27));
        debug_TextField.setMinimumSize(new java.awt.Dimension(50, 27));
        debug_TextField.setPreferredSize(new java.awt.Dimension(50, 27));
        debug_TextField.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                generic_copy_paste_menu(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                generic_copy_paste_menu(evt);
            }
        });
        debug_TextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                debug_TextFieldActionPerformed(evt);
            }
        });
        debug_TextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                debug_TextFieldFocusLost(evt);
            }
        });

        jLabel1.setText("Makehex IRP dir.");

        jLabel11.setText("Debug code");

        disregard_repeat_mins_CheckBox.setMnemonic('D');
        disregard_repeat_mins_CheckBox.setText("disregard repeat mins");
        disregard_repeat_mins_CheckBox.setToolTipText("Affects the generation of IR signals, see the documentation of IrpMaster");
        disregard_repeat_mins_CheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                disregard_repeat_mins_CheckBoxActionPerformed(evt);
            }
        });

        lafComboBox.setModel(new DefaultComboBoxModel(lafNames));
        lafComboBox.setToolTipText("Select look and feel");
        lafComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                lafComboBoxActionPerformed(evt);
            }
        });

        jLabel26.setText("Look and Feel");

        javax.swing.GroupLayout optionsPanelLayout = new javax.swing.GroupLayout(optionsPanel);
        optionsPanel.setLayout(optionsPanelLayout);
        optionsPanelLayout.setHorizontalGroup(
            optionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(optionsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(optionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(verbose_CheckBox)
                    .addGroup(optionsPanelLayout.createSequentialGroup()
                        .addGroup(optionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel16)
                            .addComponent(jLabel1)
                            .addComponent(jLabel11)
                            .addComponent(jLabel26))
                        .addGap(336, 336, 336)
                        .addGroup(optionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(IrpProtocolsTextField, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(makehexIrpDirTextField, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(optionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                .addComponent(debug_TextField, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(lafComboBox, javax.swing.GroupLayout.Alignment.LEADING, 0, 108, Short.MAX_VALUE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(optionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(irpProtocolsSelectButton)
                            .addComponent(macro_select_Button))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(optionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(IrpProtocolsBrowseButton)
                            .addComponent(makehexIrpDirBrowseButton)))
                    .addComponent(disregard_repeat_mins_CheckBox))
                .addGap(337, 337, 337))
        );

        optionsPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {IrpProtocolsBrowseButton, makehexIrpDirBrowseButton});

        optionsPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {irpProtocolsSelectButton, macro_select_Button});

        optionsPanelLayout.setVerticalGroup(
            optionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, optionsPanelLayout.createSequentialGroup()
                .addGap(26, 26, 26)
                .addGroup(optionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(IrpProtocolsTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel16)
                    .addComponent(irpProtocolsSelectButton)
                    .addComponent(IrpProtocolsBrowseButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(optionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(makehexIrpDirTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1)
                    .addComponent(macro_select_Button)
                    .addComponent(makehexIrpDirBrowseButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(optionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(debug_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel11))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(optionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lafComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel26))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(verbose_CheckBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(disregard_repeat_mins_CheckBox)
                .addContainerGap(203, Short.MAX_VALUE))
        );

        optionsPanelLayout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {irpProtocolsSelectButton, macro_select_Button});

        optionsPanelLayout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {IrpProtocolsBrowseButton, makehexIrpDirBrowseButton});

        optionsPanelLayout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {IrpProtocolsTextField, debug_TextField, lafComboBox, makehexIrpDirTextField});

        mainTabbedPane.addTab("Options", null, optionsPanel, "This tab allows the change of options for the program.");

        mainSplitPane.setTopComponent(mainTabbedPane);

        consoleTextArea.setColumns(20);
        consoleTextArea.setEditable(false);
        consoleTextArea.setLineWrap(true);
        consoleTextArea.setRows(5);
        consoleTextArea.setToolTipText("This is the console, where errors and messages go. Press right mouse button for menu.");
        consoleTextArea.setWrapStyleWord(true);
        consoleTextArea.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                consoleTextAreaMousePressed(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                consoleTextAreaMouseReleased(evt);
            }
        });
        consoleScrollPane.setViewportView(consoleTextArea);

        mainSplitPane.setBottomComponent(consoleScrollPane);

        fileMenu.setMnemonic('F');
        fileMenu.setText("File");

        saveMenuItem.setMnemonic('S');
        saveMenuItem.setText("Save properties");
        saveMenuItem.setToolTipText("Write the current values of the program's properties to disk");
        saveMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(saveMenuItem);

        saveAsMenuItem.setMnemonic('A');
        saveAsMenuItem.setText("Save properties as ...");
        saveAsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveAsMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(saveAsMenuItem);
        fileMenu.add(jSeparator4);

        consoletext_save_MenuItem.setMnemonic('c');
        consoletext_save_MenuItem.setText("Save console text as...");
        consoletext_save_MenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                consoletext_save_MenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(consoletext_save_MenuItem);
        fileMenu.add(jSeparator1);

        exitMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Q, java.awt.event.InputEvent.CTRL_MASK));
        exitMenuItem.setMnemonic('x');
        exitMenuItem.setText("Exit");
        exitMenuItem.setToolTipText("Exists the program, saving the preferences.");
        exitMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exitMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(exitMenuItem);

        menuBar.add(fileMenu);

        editMenu.setMnemonic('E');
        editMenu.setText("Edit");

        copy_console_to_clipboard_MenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C, java.awt.event.InputEvent.CTRL_MASK));
        copy_console_to_clipboard_MenuItem.setText("Copy Console to clipboard");
        copy_console_to_clipboard_MenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                copy_console_to_clipboard_MenuItemActionPerformed(evt);
            }
        });
        editMenu.add(copy_console_to_clipboard_MenuItem);

        clear_console_MenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DELETE, java.awt.event.InputEvent.CTRL_MASK));
        clear_console_MenuItem.setMnemonic('c');
        clear_console_MenuItem.setText("Clear console");
        clear_console_MenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clear_console_MenuItemActionPerformed(evt);
            }
        });
        editMenu.add(clear_console_MenuItem);

        menuBar.add(editMenu);

        jMenu1.setMnemonic('O');
        jMenu1.setText("Options");

        verbose_CheckBoxMenuItem.setMnemonic('v');
        verbose_CheckBoxMenuItem.setText("Verbose");
        verbose_CheckBoxMenuItem.setToolTipText("Report actual command sent to devices");
        verbose_CheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                verbose_CheckBoxMenuItemActionPerformed(evt);
            }
        });
        jMenu1.add(verbose_CheckBoxMenuItem);

        disregard_repeat_mins_CheckBoxMenuItem.setText("disregard repeat mins");
        disregard_repeat_mins_CheckBoxMenuItem.setToolTipText("Affects the generation of IR signals, see the documentation of IrpMaster");
        disregard_repeat_mins_CheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                disregard_repeat_mins_CheckBoxMenuItemActionPerformed(evt);
            }
        });
        jMenu1.add(disregard_repeat_mins_CheckBoxMenuItem);

        menuBar.add(jMenu1);

        helpMenu.setMnemonic('H');
        helpMenu.setText("Help");

        aboutMenuItem.setMnemonic('A');
        aboutMenuItem.setText("About...");
        aboutMenuItem.setToolTipText("The mandatory About popup (version, copyright, etc)");
        aboutMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aboutMenuItemActionPerformed(evt);
            }
        });
        helpMenu.add(aboutMenuItem);

        browseHomePageMenuItem.setMnemonic('h');
        browseHomePageMenuItem.setText("Project Homepage...");
        browseHomePageMenuItem.setToolTipText("Browse the project's home page");
        browseHomePageMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browseHomePageMenuItemActionPerformed(evt);
            }
        });
        helpMenu.add(browseHomePageMenuItem);

        contentMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F1, 0));
        contentMenuItem.setMnemonic('M');
        contentMenuItem.setText("Main documentation");
        contentMenuItem.setToolTipText("Brings up documentation.");
        contentMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                contentMenuItemActionPerformed(evt);
            }
        });
        helpMenu.add(contentMenuItem);

        browseIRPMasterMenuItem.setMnemonic('D');
        browseIRPMasterMenuItem.setText("IRPMaster doc");
        browseIRPMasterMenuItem.setToolTipText("Brings up documentation for IRPMaster, the main rendering engine");
        browseIRPMasterMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browseIRPMasterMenuItemActionPerformed(evt);
            }
        });
        helpMenu.add(browseIRPMasterMenuItem);
        helpMenu.add(jSeparator15);

        checkUpdatesMenuItem.setMnemonic('u');
        checkUpdatesMenuItem.setText("Check for updates");
        checkUpdatesMenuItem.setToolTipText("Checks if a newer version is available");
        checkUpdatesMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkUpdatesMenuItemActionPerformed(evt);
            }
        });
        helpMenu.add(checkUpdatesMenuItem);
        helpMenu.add(jSeparator13);

        browseIRPSpecMenuItem.setMnemonic('I');
        browseIRPSpecMenuItem.setText("IRP Notation Specs");
        browseIRPSpecMenuItem.setToolTipText("Displays the specification of the IRP notation");
        browseIRPSpecMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browseIRPSpecMenuItemActionPerformed(evt);
            }
        });
        helpMenu.add(browseIRPSpecMenuItem);

        browseDecodeIRMenuItem.setMnemonic('P');
        browseDecodeIRMenuItem.setText("Protocol specs");
        browseDecodeIRMenuItem.setToolTipText("Displays \"Decodeir.html\", containing a description of the protocols");
        browseDecodeIRMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browseDecodeIRMenuItemActionPerformed(evt);
            }
        });
        helpMenu.add(browseDecodeIRMenuItem);

        browseJP1Wiki.setMnemonic('J');
        browseJP1Wiki.setText("JP1 Wiki");
        browseJP1Wiki.setToolTipText("JP1 Wiki: Some important and interesting background info");
        browseJP1Wiki.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browseJP1WikiActionPerformed(evt);
            }
        });
        helpMenu.add(browseJP1Wiki);

        menuBar.add(helpMenu);

        setJMenuBar(menuBar);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(mainSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 573, Short.MAX_VALUE)
                .addGap(87, 87, 87))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addComponent(mainSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 580, Short.MAX_VALUE)
                .addGap(0, 0, 0))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private class GlobalcacheThread extends Thread {
        private IrSignal code;
        private int module;
        private int connector;
        private int count;
        private JButton startButton;
        private JButton stopButton;

        public GlobalcacheThread(IrSignal code, int module, int connector, int count,
                JButton startButton, JButton stopButton) {
            super("globalcache_thread");
            this.code = code;
            this.module = module;
            this.connector = connector;
            this.count = count;
            this.startButton = startButton;
            this.stopButton = stopButton;
        }

        @Override
        public void run() {
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
            boolean success = false;
            try {
                success = gc.send_ir(code, module, connector, count);
            } catch (UnknownHostException ex) {
                System.err.println("Globalcache hostname is not found.");
            } catch (IOException e) {
                System.err.println(e);
            } catch (InterruptedException e) {
                System.err.println("*** Interrupted *** ");
                success = true;
            }

            if (!success)
                System.err.println("** Failed **");

            startButton.setEnabled(true);
            stopButton.setEnabled(false);
            globalcacheProtocolThread = null;
        }
    }

    private class IrtransThread extends Thread {
        private IrSignal code;
        private irtrans.led_t led;
        private int count;
        private JButton startButton;
        private JButton stopButton;

        public IrtransThread(IrSignal code, irtrans.led_t led, int count,
                JButton startButton, JButton stopButton) {
            super("irtrans_thread");
            this.code = code;
            this.led = led;
            this.count = count;
            this.startButton = startButton;
            this.stopButton = stopButton;
        }

        @Override
        public void run() {
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
            boolean success = false;
            try {
                success = irt.send_ir(code, led, count);
            } catch (IncompatibleArgumentException ex) {
                System.err.println(ex.getMessage());
            } catch (UnknownHostException ex) {
                System.err.println("IRTrans hostname not found.");
            } catch (IOException e) {
                System.err.println(e);
            }

            if (!success)
                System.err.println("** Failed **");

            irtransThread = null;
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
        }
    }

    private void doExit() {
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));
        System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.err)));
        releaseAudioLine();
        Props.getInstance().setBounds(getBounds());
        Props.getInstance().setHardwareIndex(Integer.toString(hardwareIndex));
        System.out.println("Exiting...");
        System.exit(0);
    }

    private void exitMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitMenuItemActionPerformed
        doExit();
    }//GEN-LAST:event_exitMenuItemActionPerformed

    private void saveMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveMenuItemActionPerformed
        try {
            String result = Props.getInstance().save();
            System.err.println(result == null ? "No need to save properties." : ("Property file written to " + result + "."));
        } catch (Exception e) {
            error("Problems saving properties: " + e.getMessage());
        }
    }//GEN-LAST:event_saveMenuItemActionPerformed

    private void aboutMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aboutMenuItemActionPerformed
        if (aboutBox == null) {
            aboutBox = new AboutPopup(this, false);
            aboutBox.setLocationRelativeTo(this);
        }
        aboutBox.setVisible(true);
    }//GEN-LAST:event_aboutMenuItemActionPerformed

    private void contentMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_contentMenuItemActionPerformed
        browse(Props.getInstance().getHelpfileUrl(), verbose);
}//GEN-LAST:event_contentMenuItemActionPerformed

    private void saveAsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveAsMenuItemActionPerformed
        try {
            String props = selectFile("Select properties save", "XML Files", true, null, "xml").getAbsolutePath();
            Props.getInstance().save(props);
            System.err.println("Property file written to " + props + ".");
        } catch (IOException e) {
            System.err.println(e);
        } catch (NullPointerException e) {
        }
    }//GEN-LAST:event_saveAsMenuItemActionPerformed

    private void updateVerbosity() {
        UserPrefs.getInstance().setVerbose(verbose);
        gc.set_verbosity(verbose);
        irt.set_verbosity(verbose);
        verbose_CheckBoxMenuItem.setSelected(verbose);
        verbose_CheckBox.setSelected(verbose);
    }

    private void verbose_CheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_verbose_CheckBoxMenuItemActionPerformed
        verbose = verbose_CheckBoxMenuItem.isSelected();
        updateVerbosity();
    }//GEN-LAST:event_verbose_CheckBoxMenuItemActionPerformed


    private void copy_console_to_clipboard_MenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_copy_console_to_clipboard_MenuItemActionPerformed
        (new CopyClipboardText()).toClipboard(consoleTextArea.getText());
    }//GEN-LAST:event_copy_console_to_clipboard_MenuItemActionPerformed

    private void clear_console_MenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clear_console_MenuItemActionPerformed
        consoleTextArea.setText(null);
    }//GEN-LAST:event_clear_console_MenuItemActionPerformed

    private File getMakehexIrpFile() {
        String protocolName = (String) protocol_ComboBox.getModel().getSelectedItem();
        return new File(Props.getInstance().getMakehexIrpdir(), protocolName + "." + IrpFileExtension);
    }

    private String renderMakehexCode(int FOverride) {
        Makehex makehex = new Makehex(getMakehexIrpFile());
        toggletype toggle = toggletype.decode_toggle((String) toggle_ComboBox.getModel().getSelectedItem());
        int tog = toggletype.toInt(toggle);
        int devno = deviceno_TextField.getText().trim().isEmpty() ? invalidParameter : (int) IrpUtils.parseLong(deviceno_TextField.getText());
        int subDevno = subdevice_TextField.getText().trim().isEmpty() ? invalidParameter : (int) IrpUtils.parseLong(subdevice_TextField.getText());
        int cmdNo = FOverride >= 0 ? FOverride : (int) IrpUtils.parseLong(commandno_TextField.getText());

        return makehex.prontoString(devno, subDevno, cmdNo, tog);
    }

    private void renderProtocolDocu() {
        if (irpmasterRenderer()) {
            String protocolName = (String) protocol_ComboBox.getSelectedItem();
            protocol_raw_TextArea.setText(irpMaster.getDocumentation(protocolName));
        } else
            System.err.println("Internal error.");
    }

    private IrSignal extractCode() throws NumberFormatException, IrpMasterException, RecognitionException {
        return extractCode(invalidParameter);
    }

    private IrSignal extractCode(int FOverride) throws NumberFormatException, IrpMasterException, RecognitionException {
        if (makehexRenderer()) {
            return Pronto.ccfSignal(renderMakehexCode(FOverride));
        } else {
            String protocolName = (String) protocol_ComboBox.getModel().getSelectedItem();
            long devno = deviceno_TextField.getText().trim().isEmpty() ? invalidParameter : IrpUtils.parseLong(deviceno_TextField.getText());
            long subDevno = invalidParameter;
            Protocol protocol = getProtocol(protocolName);
            if (protocol.hasParameter("S") && !(protocol.hasParameterDefault("S") && subdevice_TextField.getText().trim().equals("")))
                subDevno = IrpUtils.parseLong(subdevice_TextField.getText());
            long cmdNo = FOverride >= 0 ? (long) FOverride : IrpUtils.parseLong(commandno_TextField.getText());
            String tog = (String) toggle_ComboBox.getModel().getSelectedItem();
            toggletype toggle = toggletype.decode_toggle((String) toggle_ComboBox.getModel().getSelectedItem());
            String addParams = protocol_params_TextField.getText();

            if (protocol == null)
                return null;

            HashMap<String, Long> params = new HashMap<String, Long>();
            if (devno != invalidParameter)
                params.put("D", devno);
            if (subDevno != invalidParameter)
                params.put("S", subDevno);
            if (cmdNo != invalidParameter)
                params.put("F", cmdNo);
            if (toggle != toggletype.dont_care)
                params.put("T", (long) toggletype.toInt(toggle));
            if (addParams != null && !addParams.trim().isEmpty()) {
                String[] str = addParams.trim().split("[ \t]+");
                for (String s : str) {
                    String[] q = s.split("=");
                    if (q.length == 2)
                        params.put(q[0], IrpUtils.parseLong(q[1]));
                }
            }
            IrSignal irSignal = protocol.renderIrSignal(params, !Props.getInstance().getDisregardRepeatMins());
            codeNotationString = protocolName + ": " + protocol.notationString("=", " "); // Not really too nice :-(
            return irSignal;
        }
    }

    private void exportIrSignal(PrintStream printStream, Protocol protocol, HashMap<String, Long> params,
            boolean doXML, boolean doRaw, boolean doPronto, LircExport lircExport)
            throws IrpMasterException {
        IrSignal irSignal = protocol.renderIrSignal(params, !Props.getInstance().getDisregardRepeatMins());
        if (lircExport != null) {
            lircExport.addSignal(params, irSignal);
        } else {
            if (doXML)
                protocol.addSignal(params);
            if (doRaw && irSignal != null) {
                if (doXML) {
                    protocol.addRawSignalRepresentation(irSignal);
                } else {
                    printStream.println(IrpUtils.variableHeader(params));
                    printStream.println(irSignal.toPrintString());
                }
            }
            if (doPronto && irSignal != null) {
                if (doXML) {
                    protocol.addProntoSignalRepresentation(irSignal);
                } else {
                    if (!doRaw)
                        printStream.println(IrpUtils.variableHeader(params));
                    printStream.println(irSignal.ccfString());
                }
            }
        }
    }
    
    // FIXME: this code sucks.
    private void export() throws NumberFormatException, IrpMasterException, RecognitionException, FileNotFoundException {
        String format = (String) exportFormatComboBox.getSelectedItem();
        boolean doXML = format.equalsIgnoreCase("XML");
        boolean doText = format.equalsIgnoreCase("text");
        boolean doLirc = format.equalsIgnoreCase("lirc");
        boolean doWave = format.equalsIgnoreCase("wave");
        boolean doLintronic = format.equalsIgnoreCase("lintronic");
        boolean doRaw = exportRawCheckBox.isSelected();
        boolean doPronto = exportProntoCheckBox.isSelected();
        String protocolName = (String) protocol_ComboBox.getModel().getSelectedItem();
        long devno = deviceno_TextField.getText().trim().isEmpty() ? invalidParameter : IrpUtils.parseLong(deviceno_TextField.getText());
        long sub_devno = invalidParameter;
        if (!subdevice_TextField.getText().trim().equals(""))
            sub_devno = IrpUtils.parseLong(subdevice_TextField.getText());
        long cmd_no_lower = deviceno_TextField.getText().trim().isEmpty() ? invalidParameter : IrpUtils.parseLong(commandno_TextField.getText());
        long cmd_no_upper = (doWave || lastFTextField.getText().isEmpty()) ? cmd_no_lower : IrpUtils.parseLong(lastFTextField.getText());
        toggletype toggle = toggletype.decode_toggle((String) toggle_ComboBox.getModel().getSelectedItem());
        String add_params = protocol_params_TextField.getText();
        String extension = doXML ? "xml"
                : doLirc  ? "lirc"
                : doWave  ? "wav"
                : "txt";
        String formatDescription = "Export files"; // FIXME

        if (automaticFileNamesCheckBox.isSelected()) {
            File exp = new File(Props.getInstance().getExportdir());
            if (!exp.exists()) {
                System.err.print("Export directory " + exp + " does not exist, trying to create... ");
                boolean success = exp.mkdirs();
                System.err.println(success ? "succeeded." : "failed.");
            }
            if (!exp.isDirectory() || !exp.canWrite()) {
                System.err.println("Export directory `" + exp + "' is not a writable directory, please correct.");
                return;
            }
        }

        boolean useCcf = devno == invalidParameter
                && sub_devno == invalidParameter
                && cmd_no_lower == invalidParameter;
        File file = automaticFileNamesCheckBox.isSelected()
                ? harcutils.create_export_file(Props.getInstance().getExportdir(),
                    useCcf
                    ? "rawccf"
                    : (protocolName + "_" + devno + (sub_devno != invalidParameter ? ("_" + sub_devno) : "")
                       + (doWave ? ("_" + cmd_no_lower) : "")),
                  extension)
                : selectFile("Select export file", formatDescription, true, Props.getInstance().getExportdir(), extension);

        if (file == null) // user pressed cancel
            return;

        if (useCcf) {
            IrSignal irSignal = Pronto.ccfSignal(protocol_raw_TextArea.getText()); // may throw exceptions, caught by the caller
            int repetitions = Integer.parseInt((String) exportRepetitionsComboBox.getSelectedItem());
            ModulatedIrSequence irSequence = irSignal.toModulatedIrSequence(repetitions);
            System.err.println("Exporting raw CCF signal to " + file + ".");
            if (doWave) {
                updateAudioFormat();
                Wave wave = new Wave(irSequence, audioFormat,
                        audioOmitCheckBox.isSelected(),
                        audioWaveformComboBox.getSelectedIndex() == 0, audioDivideCheckBox.isSelected());
                wave.export(file);
            } else if (doLintronic) {
                PrintStream printStream = new PrintStream(file);
                printStream.print(Lintronic.toExport(irSequence));
                printStream.close();
            } else {
                System.err.println("Error: Parameters (D, S, F,...) are missing, and not using wave/Lintronic export.");
                return;
            }
        } else if (irpmasterRenderer()) {
            Protocol protocol = irpMaster.newProtocol(protocolName);
            HashMap<String, Long> params = Protocol.parseParams((int) devno, (int) sub_devno,
                    (int) cmd_no_lower, toggletype.toInt(toggle), add_params);
            if (doWave || doLintronic) {
                int repetitions = Integer.parseInt((String) exportRepetitionsComboBox.getSelectedItem());
                toggletype tt = toggletype.decode_toggle((String) toggle_ComboBox.getSelectedItem());
                if (tt != toggletype.dont_care)
                    params.put("T", (long) toggletype.toInt(tt));
                IrSignal irSignal = protocol.renderIrSignal(params, !Props.getInstance().getDisregardRepeatMins());
                ModulatedIrSequence irSequence = irSignal.toModulatedIrSequence(repetitions);
                if (doWave) {
                    updateAudioFormat();
                    Wave wave = new Wave(irSequence, audioFormat, audioOmitCheckBox.isSelected(),
                        audioWaveformComboBox.getSelectedIndex() == 0, audioDivideCheckBox.isSelected());
                    wave.export(file);
                } else {
                    // doLintronic
                    PrintStream printStream = new PrintStream(file);
                    printStream.print(Lintronic.toExport(irSequence));
                    printStream.close();
                }
                System.err.println("Exporting to " + file + ".");
            } else {
                LircExport lircExport = null;
                if (doXML)
                    protocol.setupDOM();
                if (doLirc)
                    lircExport = new LircExport(protocolName, "Generated by IrMaster", protocol.getFrequency());
                
                PrintStream printStream = new PrintStream(file);
                System.err.println("Exporting to " + file);
            
                    for (long cmd_no = cmd_no_lower; cmd_no <= cmd_no_upper; cmd_no++) {
                    params.put("F", cmd_no);
                    if (exportGenerateTogglesCheckBox.isSelected()) {
                        for (long t = 0; t <= 1L; t++) {
                            params.put("T", t);
                            exportIrSignal(printStream, protocol, params, doXML, doRaw, doPronto, lircExport);
                        }

                    } else {
                        toggletype tt = toggletype.decode_toggle((String) toggle_ComboBox.getSelectedItem());
                        if (tt != toggletype.dont_care)
                            params.put("T", (long) toggletype.toInt(tt));
                        exportIrSignal(printStream, protocol, params, doXML, doRaw, doPronto, lircExport);
                    }
                }
                if (doXML)
                    protocol.printDOM(printStream);
                if (doLirc)
                    lircExport.write(printStream);
            }
        } else {
            // Makehex
            if (!doText || doRaw || doLirc) {
                System.err.println("Using Makehex only export in text files using Pronto format is supported");
            } else {
                PrintStream printStream = new PrintStream(file);
                System.err.println("Exporting to " + file);
                String protocol_name = (String) protocol_ComboBox.getModel().getSelectedItem();
                Makehex makehex = new Makehex(new File(Props.getInstance().getMakehexIrpdir(), protocol_name + "." + IrpFileExtension));
                for (int cmd_no = (int) cmd_no_lower; cmd_no <= cmd_no_upper; cmd_no++) {
                    String ccf = makehex.prontoString((int)devno, (int)sub_devno, (int)cmd_no, toggletype.toInt(toggle));
                    printStream.println("Device Code: " + devno + (sub_devno != invalidParameter ? ("." + sub_devno) : "") + ", Function: " + cmd_no);
                    printStream.println(ccf);
                }
                printStream.close();
            }
        }
        
        lastExportFile = file.getAbsoluteFile();
        viewExportButton.setEnabled(true);
    }

    private void updateProtocolParameters() {
        String currentProtocol = (String) protocol_ComboBox.getSelectedItem();
        boolean initialize = ! Props.getInstance().getProtocol().equalsIgnoreCase(currentProtocol);
        Props.getInstance().setProtocol(currentProtocol.toLowerCase());
        if (irpmasterRenderer()) {
            if (irpMaster == null)
                return;
            try {
                Protocol protocol = getProtocol((String) protocol_ComboBox.getModel().getSelectedItem());
                deviceno_TextField.setEnabled(protocol.hasParameter("D"));
                subdevice_TextField.setEnabled(protocol.hasParameter("S"));
                commandno_TextField.setEnabled(protocol.hasParameter("F"));
                toggle_ComboBox.setEnabled(protocol.hasParameter("T"));
                if (initialize) {
                    deviceno_TextField.setText(null);
                    commandno_TextField.setText(null);
                    subdevice_TextField.setText(null);
                    toggle_ComboBox.setSelectedItem(toggletype.dont_care);

                    if (protocol.hasParameter("D"))
                        deviceno_TextField.setText(Long.toString(protocol.getParameterMin("D")));
                    
                    if (protocol.hasParameter("S") && !protocol.hasParameterDefault("S"))
                        subdevice_TextField.setText(Long.toString(protocol.getParameterMin("S")));
                    else
                        subdevice_TextField.setText(null);
                    if (protocol.hasParameter("F")) {
                        commandno_TextField.setText(Long.toString(protocol.getParameterMin("F")));
                        endFTextField.setText(Long.toString(protocol.getParameterMax("F")));
                        lastFTextField.setText(Long.toString(protocol.getParameterMax("F")));
                    }
                    protocol_raw_TextArea.setText(null);
                }
                exportGenerateTogglesCheckBox.setEnabled(protocol.hasParameter("T"));
                IRP_TextField.setText(protocol.getIrp());
                protocol_params_TextField.setEnabled(true);
            } catch (UnassignedException ex) {
                subdevice_TextField.setEnabled(false);
                toggle_ComboBox.setEnabled(false);
            } catch (RecognitionException ex) {
                subdevice_TextField.setEnabled(false);
                toggle_ComboBox.setEnabled(false);
            }
        } else {
            // Makehex
            deviceno_TextField.setEnabled(true);
            subdevice_TextField.setEnabled(true);
            commandno_TextField.setEnabled(true);
            toggle_ComboBox.setEnabled(true);
            if (initialize) {
                deviceno_TextField.setText("0");
                subdevice_TextField.setText("0");
                commandno_TextField.setText("0");
                toggle_ComboBox.setSelectedIndex(2);
            }
            IRP_TextField.setText(null);
            protocol_params_TextField.setEnabled(false);
            exportGenerateTogglesCheckBox.setEnabled(false);
            exportGenerateTogglesCheckBox.setSelected(false);
        }
    }

    private void consoletext_save_MenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_consoletext_save_MenuItemActionPerformed
        try {
            String filename = selectFile("Save console text as...", "Text file", true, null, "txt").getAbsolutePath();
            PrintStream ps = new PrintStream(new FileOutputStream(filename));
            ps.println(consoleTextArea.getText());
        } catch (FileNotFoundException ex) {
            System.err.println(ex);
        } catch (NullPointerException e) {
        }
    }//GEN-LAST:event_consoletext_save_MenuItemActionPerformed

    private void verbose_CheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_verbose_CheckBoxActionPerformed
        verbose = verbose_CheckBox.isSelected();
	updateVerbosity();
    }//GEN-LAST:event_verbose_CheckBoxActionPerformed

    private void exportdir_browse_ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportdir_browse_ButtonActionPerformed

        try {
	    String dir = selectFile("Select export directory", "Directories", false, (new File(Props.getInstance().getExportdir())).getAbsoluteFile().getParent(), null).getAbsolutePath();
	    Props.getInstance().setExportdir(dir);
	    exportdir_TextField.setText(dir);
	} catch (NullPointerException e) {
	}
    }//GEN-LAST:event_exportdir_browse_ButtonActionPerformed

    private void exportdir_TextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_exportdir_TextFieldFocusLost
        Props.getInstance().setExportdir(exportdir_TextField.getText());
     }//GEN-LAST:event_exportdir_TextFieldFocusLost

    private void exportdir_TextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportdir_TextFieldActionPerformed
        Props.getInstance().setExportdir(exportdir_TextField.getText());
     }//GEN-LAST:event_exportdir_TextFieldActionPerformed

    private void makehexIrpDirTextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_makehexIrpDirTextFieldFocusLost
        Props.getInstance().setMakehexIrpdir(makehexIrpDirTextField.getText());
    }//GEN-LAST:event_makehexIrpDirTextFieldFocusLost

    private void IrpProtocolsTextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_IrpProtocolsTextFieldFocusLost
        Props.getInstance().setIrpmasterConfigfile(IrpProtocolsTextField.getText());
     }//GEN-LAST:event_IrpProtocolsTextFieldFocusLost

    private void IrpProtocolsTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_IrpProtocolsTextFieldActionPerformed
        Props.getInstance().setIrpmasterConfigfile(IrpProtocolsTextField.getText());
     }//GEN-LAST:event_IrpProtocolsTextFieldActionPerformed

    private void selectPeriodTime(boolean selectPeriod, boolean useHex) {
        no_periods_TextField.setEditable(selectPeriod && ! useHex);
        no_periods_hex_TextField.setEditable(selectPeriod && useHex);
        time_TextField.setEditable(!selectPeriod);
    }

    private void prontocode_TextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_prontocode_TextFieldFocusLost
        updateFromFrequencycode();
     }//GEN-LAST:event_prontocode_TextFieldFocusLost

    private void prontocode_TextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_prontocode_TextFieldActionPerformed
        updateFromFrequencycode();
     }//GEN-LAST:event_prontocode_TextFieldActionPerformed

    private void time_TextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_time_TextFieldFocusLost
        time_TextFieldActionPerformed(null);
     }//GEN-LAST:event_time_TextFieldFocusLost

    private void time_TextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_time_TextFieldActionPerformed
        int time = Integer.parseInt(time_TextField.getText());
	int freq = Integer.parseInt(frequency_TextField.getText());
        double periods = (((double) time) * ((double) freq))/1000000;
	no_periods_TextField.setText(String.format("%.1f", periods));
        no_periods_hex_TextField.setText(String.format("%04X", Math.round(periods)));
     }//GEN-LAST:event_time_TextFieldActionPerformed

    private void frequency_TextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_frequency_TextFieldFocusLost
        updateFromFrequency();
     }//GEN-LAST:event_frequency_TextFieldFocusLost

    private void frequency_TextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_frequency_TextFieldActionPerformed
        updateFromFrequency();
     }//GEN-LAST:event_frequency_TextFieldActionPerformed

    private void no_periods_TextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_no_periods_TextFieldFocusLost
        no_periods_TextFieldActionPerformed(null);
     }//GEN-LAST:event_no_periods_TextFieldFocusLost

    private void no_periods_TextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_no_periods_TextFieldActionPerformed
        double noPeriods = Double.parseDouble(no_periods_TextField.getText());
        no_periods_hex_TextField.setText(String.format("%04X", Math.round(noPeriods)));
        int freq = Integer.parseInt(frequency_TextField.getText());
        time_TextField.setText(Integer.toString((int) (1000000 * ((double) noPeriods) / (double) freq)));
     }//GEN-LAST:event_no_periods_TextFieldActionPerformed

    private void hex_TextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_hex_TextFieldFocusLost
        hex_TextFieldActionPerformed(null);
     }//GEN-LAST:event_hex_TextFieldFocusLost

    private void hex_TextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_hex_TextFieldActionPerformed
        try {
             int in = Integer.parseInt(hex_TextField.getText(), 16);
             int no_bytes = (in >= 256 || hex_TextField.getText().length() > 2) ? 2 : 1;
             hex_TextField.setText(String.format(no_bytes == 2 ? "%04X" : "%02X", in));
             decimal_TextField.setText(Integer.toString(in));
             updateHexcalc(in, no_bytes);
         } catch (NumberFormatException e) {
             decimal_TextField.setText("*");
             hexcalcSillyNumber(e);
         }
     }//GEN-LAST:event_hex_TextFieldActionPerformed

    private void decimal_TextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_decimal_TextFieldFocusLost
        decimal_TextFieldActionPerformed(null);
     }//GEN-LAST:event_decimal_TextFieldFocusLost

    private void decimal_TextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_decimal_TextFieldActionPerformed
        try {
             int in = Integer.parseInt(decimal_TextField.getText());
             int no_bytes = (in >= 256 || decimal_TextField.getText().length() > 3) ? 2 : 1;
             hex_TextField.setText(String.format(no_bytes == 2 ? "%04X" : "%02X", in));
             updateHexcalc(in, no_bytes);
         } catch (NumberFormatException e) {
             hex_TextField.setText("*");
             hexcalcSillyNumber(e);
         }
     }//GEN-LAST:event_decimal_TextFieldActionPerformed

    private void lircPortTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_lircPortTextFieldActionPerformed
        Props.getInstance().setLircPort(lircPortTextField.getText());
        LircIPAddressTextFieldActionPerformed(evt);
    }//GEN-LAST:event_lircPortTextFieldActionPerformed

    private void lircStopIrButtongc_stop_ir_ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_lircStopIrButtongc_stop_ir_ActionPerformed
        try {
            lircClient.stop_ir();
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
        }
	}//GEN-LAST:event_lircStopIrButtongc_stop_ir_ActionPerformed

        private void LircIPAddressTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_LircIPAddressTextFieldActionPerformed
            String lircIp = LircIPAddressTextField.getText();
            Props.getInstance().setLircIpName(lircIp);
            lircClient = new lirc(lircIp, Integer.parseInt(lircPortTextField.getText()), verbose);
            try {
                lircServerVersionText.setText(lircClient.get_version());
                String[] remotes = lircClient.get_remotes();
                Arrays.sort(remotes, String.CASE_INSENSITIVE_ORDER);
                lircRemotesComboBox.setModel(new DefaultComboBoxModel(remotes));
                lircRemotesComboBox.setEnabled(true);
                lircRemotesComboBoxActionPerformed(null);
                lircTransmitterComboBox.setEnabled(true);
                lircSendPredefinedButton.setEnabled(true);
                lircStopIrButton.setEnabled(true);
                noLircPredefinedsComboBox.setEnabled(true);
                lircServerVersionLabel.setEnabled(true);
                lircServerVersionText.setEnabled(true);
            } catch (IOException ex) {
                error(ex.getMessage());
            }
	}//GEN-LAST:event_LircIPAddressTextFieldActionPerformed

    private void irtrans_browse_ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_irtrans_browse_ButtonActionPerformed
        browse(URI.create("http://" + irtrans_address_TextField.getText()), verbose);
     }//GEN-LAST:event_irtrans_browse_ButtonActionPerformed

    private void irtrans_address_TextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_irtrans_address_TextFieldActionPerformed
        Props.getInstance().setIrTransIpName(irtrans_address_TextField.getText());
        irt = new irtrans(irtrans_address_TextField.getText(), verbose);
        try {
            irtransVersionLabel.setText(irt.get_version());
            String[] remotes = irt.get_remotes();
            Arrays.sort(remotes, String.CASE_INSENSITIVE_ORDER);
            irtransRemotesComboBox.setModel(new DefaultComboBoxModel(remotes));
            irtransRemotesComboBox.setEnabled(true);
            irtransRemotesComboBoxActionPerformed(null);
        } catch (UnknownHostException ex) {
            error("Unknown host: " + irtrans_address_TextField.getText());
        } catch (IOException ex) {
            error(ex.getMessage());
        } catch (InterruptedException ex) {
            error(ex.getMessage());
        }
     }//GEN-LAST:event_irtrans_address_TextFieldActionPerformed

    private class GlobalcacheDiscoverThread extends Thread {
        public GlobalcacheDiscoverThread() {
        }

        @Override
        public void run() {
            discoverButton.setEnabled(false);
            amx_beacon.result beacon = globalcache.listen_beacon();
            if (beacon != null) {
                System.err.println("A GlobalCache was found!");
                String gcHostname = beacon.addr.getCanonicalHostName();
                gc_address_TextField.setText(gcHostname);
                gcDiscoveredTypeLabel.setText(beacon.table.get("-Model"));
                gc_address_TextFieldActionPerformed(null);
            } else
                System.err.println("No GlobalCache was found.");
            discoverButton.setEnabled(true);
            amx_beacon.reset(); // making it sensible to press button again.
        }
    }

    private void discoverButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_discoverButtonActionPerformed
        System.err.println("Now trying to discover a GlobalCache on LAN. This may take up to 60 seconds.");
	GlobalcacheDiscoverThread thread = new GlobalcacheDiscoverThread();
        thread.start();
    }//GEN-LAST:event_discoverButtonActionPerformed

    private void gc_stop_ir_ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_gc_stop_ir_ActionPerformed
        try {
	    gc.stop_ir(getGcModule(), getGcConnector());
	} catch (UnknownHostException ex) {
	    System.err.println(ex.getMessage());
	} catch (IOException ex) {
	    System.err.println(ex.getMessage());
	} catch (InterruptedException ex) {
	    System.err.println(ex.getMessage());
	}
    }//GEN-LAST:event_gc_stop_ir_ActionPerformed

    private void gc_browse_ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_gc_browse_ButtonActionPerformed
        browse(URI.create("http://" + gc_address_TextField.getText()), verbose);
    }//GEN-LAST:event_gc_browse_ButtonActionPerformed

    private void gc_address_TextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_gc_address_TextFieldActionPerformed
        Props.getInstance().setGlobalcacheIpName(gc_address_TextField.getText());
        gc = new globalcache(gc_address_TextField.getText(), verbose_CheckBoxMenuItem.getState());
	try {
	    gc_module_ComboBox.setEnabled(false);
	    gc_connector_ComboBox.setEnabled(false);
	    String devs = gc.getdevices();
	    String[] dvs = devs.split("\n");
	    String[] s = new String[dvs.length];
	    for (int i = 0; i < s.length; i++) {
		s[i] = dvs[i].endsWith("IR") ? dvs[i].substring(7, 8) : null;
	    }
	    String[] modules = harcutils.nonnulls(s);
	    gc_module_ComboBox.setModel(new DefaultComboBoxModel(modules != null ? modules : new String[]{"-"}));
	    gc_module_ComboBox.setEnabled(modules != null);
	    gc_connector_ComboBox.setEnabled(modules != null);
	} catch (UnknownHostException e) {
	    gc = null;
	    error("Unknown host: " + gc_address_TextField.getText());
	} catch (IOException e) {
	    gc = null;
	    error(e.getMessage());
	} catch (InterruptedException e) {
	    gc = null;
	    error(e.getMessage());
	}
	protocol_send_Button.setEnabled(gc != null);
    }//GEN-LAST:event_gc_address_TextFieldActionPerformed

    private void protocolExportButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_protocolExportButtonActionPerformed
        try {
	    export();
	} catch (NumberFormatException ex) {
	    System.err.println(ex.getMessage());
	} catch (IrpMasterException ex) {
	    System.err.println(ex.getMessage());
	} catch (RecognitionException ex) {
	    System.err.println(ex.getMessage());
	} catch (FileNotFoundException ex) {
	    System.err.println(ex.getMessage());
	}
    }//GEN-LAST:event_protocolExportButtonActionPerformed

    private void icf_import_ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_icf_import_ButtonActionPerformed
        File file = selectFile("Select import file", "ict Files", false, null, "ict", "Wave Files", "wav");
	if (file != null) {
	    try {
		if (verbose)
		    System.err.println("Imported " + file.getName());

                IrSignal ip;
                if (file.getName().endsWith(".wav")) {
                    Wave wave = new Wave(file);
                    ip = wave.analyze(this.audioDivideCheckBox.isSelected());
                } else
                    ip = ICT.parse(file);

		protocol_raw_TextArea.setText(ip.ccfString());
                enableProtocolButtons(true);
	    } catch (UnsupportedAudioFileException ex) {
		System.err.println(ex.getMessage());
            } catch (IncompatibleArgumentException ex) {
		System.err.println(ex.getMessage());
	    } catch (FileNotFoundException ex) {
		System.err.println(ex);
	    } catch (IOException ex) {
		System.err.println(ex);
	    }
	}
    }//GEN-LAST:event_icf_import_ButtonActionPerformed

    private void protocol_clear_ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_protocol_clear_ButtonActionPerformed
        protocol_raw_TextArea.setText(null);
        enableProtocolButtons(false);
    }//GEN-LAST:event_protocol_clear_ButtonActionPerformed

    private void protocol_stop_ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_protocol_stop_ButtonActionPerformed

        try {
	    if (globalcacheProtocolThread != null)
		globalcacheProtocolThread.interrupt();
	    gc.stop_ir(getGcModule(), getGcConnector());
	} catch (UnknownHostException e) {
            System.err.println(e);
	} catch (IOException e) {
	    System.err.println(e);
	} catch (InterruptedException e) {
	    System.err.println(e);
	}
    }//GEN-LAST:event_protocol_stop_ButtonActionPerformed

    private void protocol_decode_ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_protocol_decode_ButtonActionPerformed
        String code = protocol_raw_TextArea.getText();
        try {
             DecodeIR.DecodedSignal[] result = DecodeIR.decode(code);
             if (result == null || result.length == 0) {
                System.err.println("DecodeIR failed (but was found).");
                return;
            }
            for (int i = 0; i < result.length; i++) {
                System.err.println(result[i]);
            }
        } catch (IrpMasterException ex) {
            System.err.println(ex.getMessage());
        } catch (UnsatisfiedLinkError e) {
	    System.err.println("Error: DecodeIR not found.");
	} catch (NumberFormatException e) {
	    System.err.println("Parse error in string; " + e.getMessage());
	}
    }//GEN-LAST:event_protocol_decode_ButtonActionPerformed

    private void protocol_generate_ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_protocol_generate_ButtonActionPerformed
        try {
	    IrSignal code = extractCode();
	    if (code == null)
		return;
	    protocol_raw_TextArea.setText(code.ccfString());
            enableProtocolButtons(true);
	} catch (RecognitionException ex) {
	    System.err.println(ex.getMessage());
	} catch (IrpMasterException ex) {
	    System.err.println(ex.getMessage());
	} catch (NumberFormatException e) {
	    System.err.println("Parse error " + e.getMessage());
	}
    }//GEN-LAST:event_protocol_generate_ButtonActionPerformed

    private void protocol_send_ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_protocol_send_ButtonActionPerformed
        int count = Integer.parseInt((String) no_sends_protocol_ComboBox.getModel().getSelectedItem());
        boolean useGlobalcache = protocol_outputhw_ComboBox.getSelectedIndex() == hardwareIndexGlobalCache;
        boolean useIrtrans = protocol_outputhw_ComboBox.getSelectedIndex() == hardwareIndexIrtrans;
        boolean useLirc = protocol_outputhw_ComboBox.getSelectedIndex() == hardwareIndexLirc;
        boolean useAudio = protocol_outputhw_ComboBox.getSelectedIndex() == hardwareIndexAudio;

        String ccf = protocol_raw_TextArea.getText();
        /* If raw code null, take code from the upper row, ignoring text areas*/
        IrSignal code = null;
        try {
            code = (ccf == null || ccf.trim().equals("")) ? extractCode() : Pronto.ccfSignal(ccf);
        } catch (NumberFormatException ex) {
            System.err.println(ex.getMessage());
        } catch (IrpMasterException ex) {
            System.err.println(ex.getMessage());
        } catch (RecognitionException ex) {
            System.err.println(ex.getMessage());
        }
        if (code == null)
            return;
        if (useGlobalcache) {
            if (globalcacheProtocolThread != null) {
                System.err.println("Warning: the_globalcache_protocol_thread != null, waiting...");
                try {
                    globalcacheProtocolThread.join();
                } catch (InterruptedException ex) {
                    System.err.println("***** Interrupted *********");
                }
            }
            globalcacheProtocolThread = new GlobalcacheThread(code, getGcModule(), getGcConnector(), count, protocol_send_Button, protocol_stop_Button);
            globalcacheProtocolThread.start();
        } else if (useIrtrans) {
            //irt.send_ir(code, get_irtrans_led(), count);
            if (irtransThread != null) {
                System.err.println("Warning: the_irtrans_thread != null, waiting...");
                try {
                    irtransThread.join();
                } catch (InterruptedException ex) {
                    System.err.println("***** Interrupted *********");
                }
            }
            irtransThread = new IrtransThread(code, getIrtransLed(), count, protocol_send_Button, protocol_stop_Button);
            irtransThread.start();

        } else if (useLirc) {
            if (lircClient == null) {
                System.err.println("No LIRC server initialized, blindly trying...");
                LircIPAddressTextFieldActionPerformed(null);
            }
            if (lircClient == null) {
                System.err.println("No LIRC server defined.");
                return;
            }
            try {
                boolean success = lircClient.send_ccf(ccf, count);
                if (!success)
                    System.err.println("** Failed **");
            } catch (IOException ex) {
                System.err.println(ex.getMessage());
            }
        } else if (useAudio) {
            getAudioLine();
            if (audioLine == null)
                return;
            try {
                Wave wave = new Wave(code.toModulatedIrSequence(count-1), audioFormat,
                        audioOmitCheckBox.isSelected(), audioWaveformComboBox.getSelectedIndex() == 0,
                        audioDivideCheckBox.isSelected());
                wave.play(audioLine);
            } catch (LineUnavailableException ex) {
                Logger.getLogger(GuiMain.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(GuiMain.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IncompatibleArgumentException ex) {
                System.err.println(ex.getMessage());
                //return;
            }
        } else
            System.err.println("This cannot happen, internal error.");
    }//GEN-LAST:event_protocol_send_ButtonActionPerformed

    private void commandno_TextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_commandno_TextFieldFocusLost
        possiblyEnableEncodeSend();
    }//GEN-LAST:event_commandno_TextFieldFocusLost

    private void commandno_TextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_commandno_TextFieldActionPerformed
        possiblyEnableEncodeSend();
    }//GEN-LAST:event_commandno_TextFieldActionPerformed

    private void deviceno_TextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_deviceno_TextFieldFocusLost
	possiblyEnableEncodeSend();
    }//GEN-LAST:event_deviceno_TextFieldFocusLost

    private void deviceno_TextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deviceno_TextFieldActionPerformed
        possiblyEnableEncodeSend();
    }//GEN-LAST:event_deviceno_TextFieldActionPerformed

    private void protocol_ComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_protocol_ComboBoxActionPerformed
        updateProtocolParameters();
    }//GEN-LAST:event_protocol_ComboBoxActionPerformed

    private void irpProtocolsBrowse(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_irpProtocolsBrowse
        open(Props.getInstance().getIrpmasterConfigfile(), verbose);
        System.err.println("If editing the file, changes will not take effect before you save the file AND restart IrMaster!");
    }//GEN-LAST:event_irpProtocolsBrowse

    private void irpProtocolsSelect(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_irpProtocolsSelect
        File f = selectFile("Select protocol file for IrpMaster", "Configuration files", false, null, "ini");
        if (f != null) {
            Props.getInstance().setIrpmasterConfigfile(f.getAbsolutePath());
            IrpProtocolsTextField.setText(f.getAbsolutePath());
        }
    }//GEN-LAST:event_irpProtocolsSelect

    private void makehexIrpDirSelect(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_makehexIrpDirSelect
        File f = selectFile("Select direcory containing IRP files for Makehex", "Directories", false, null, null);
        if (f != null) {
            Props.getInstance().setMakehexIrpdir(f.getAbsolutePath());
            makehexIrpDirTextField.setText(f.getAbsolutePath());
        }
    }//GEN-LAST:event_makehexIrpDirSelect

    private boolean irpmasterRenderer() {
        return rendererComboBox.getSelectedIndex() == 0;
    }

    private boolean makehexRenderer() {
        return rendererComboBox.getSelectedIndex() == 1;
    }

    private void rendererComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rendererComboBoxActionPerformed
        if (irpmasterRenderer()) {
            // IrpMaster
            protocol_ComboBox.setModel(new DefaultComboBoxModel(irpMaster == null ? new String[]{"--"} : harcutils.sort_unique(irpMaster.getNames().toArray(new String[0]))));
            protocol_ComboBox.setSelectedItem(Props.getInstance().getProtocol());
            exportFormatComboBox.setEnabled(true);
            exportRawCheckBox.setEnabled(true);
            exportProntoCheckBox.setEnabled(true);
        } else {
            // Makehex
            String[] filenames = harcutils.get_basenames(Props.getInstance().getMakehexIrpdir(), IrpFileExtension, false);
            java.util.Arrays.sort(filenames, String.CASE_INSENSITIVE_ORDER);
            protocol_ComboBox.setModel(new DefaultComboBoxModel(filenames));
            String old_protocol = Props.getInstance().getProtocol();
            for (int i = 0; i < filenames.length; i++)
                if (filenames[i].equalsIgnoreCase(old_protocol)) {
                    protocol_ComboBox.setSelectedIndex(i);
                    break;
                }

            exportFormatComboBox.setSelectedIndex(0);
            exportFormatComboBox.setEnabled(false);
            exportRawCheckBox.setSelected(false);
            exportRawCheckBox.setEnabled(false);
            exportProntoCheckBox.setSelected(true);
            exportProntoCheckBox.setEnabled(false);
        }
        
        IRP_TextField.setEnabled(irpmasterRenderer());
        updateProtocolParameters();
        protocol_params_TextField.setText(null);
        protocol_raw_TextArea.setText(null);
        enableProtocolButtons(false);
        listIrpMenuItem.setEnabled(makehexRenderer());
        docuProtocolMenuItem.setEnabled(irpmasterRenderer());
    }//GEN-LAST:event_rendererComboBoxActionPerformed

    private void lastFTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_lastFTextFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_lastFTextFieldActionPerformed

    private void lastFTextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_lastFTextFieldFocusLost
        // TODO add your handling code here:
    }//GEN-LAST:event_lastFTextFieldFocusLost

    private void endFTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_endFTextFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_endFTextFieldActionPerformed

    private void endFTextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_endFTextFieldFocusLost
        // TODO add your handling code here:
    }//GEN-LAST:event_endFTextFieldFocusLost

    private void delayTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_delayTextFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_delayTextFieldActionPerformed

    private void delayTextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_delayTextFieldFocusLost
        // TODO add your handling code here:
    }//GEN-LAST:event_delayTextFieldFocusLost

    private void currentFTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_currentFTextFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_currentFTextFieldActionPerformed

    private void currentFTextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_currentFTextFieldFocusLost
        // TODO add your handling code here:
    }//GEN-LAST:event_currentFTextFieldFocusLost

    private void protocolAnalyzeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_protocolAnalyzeButtonActionPerformed
        String code = protocol_raw_TextArea.getText().trim();
	try {
	     IrSignal irSignal = Pronto.ccfSignal(code);
             Analyzer analyzer = new Analyzer(irSignal, debug > 0);
             System.out.println("Analyzer result: " + analyzer.getIrpWithAltLeadout());
	} catch (IrpMasterException e) {
	    System.err.println(e.getMessage());
	} catch (NumberFormatException e) {
	    System.err.println("Parse error in string; " + e.getMessage());
	}
    }//GEN-LAST:event_protocolAnalyzeButtonActionPerformed

    private void protocolPlotButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_protocolPlotButtonActionPerformed
        String ccf = protocol_raw_TextArea.getText();
        String legend = null;
        IrSignal irSignal = null;
        try {
            if (!ccf.isEmpty() && ccf.startsWith("0000")) {
                irSignal = Pronto.ccfSignal(ccf);
                legend = ccf.substring(0, 40);
            } else {
                irSignal = extractCode();
                legend = codeNotationString;
            }
        } catch (IrpMasterException ex) {
            System.err.println(ex.getMessage());
        } catch (RecognitionException ex) {
            System.err.println(ex.getMessage());
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm");
        Plotter junk = new Plotter(irSignal, false, "IrMaster plot #" + ++plotNumber
                + " (" + dateFormat.format(new Date()) + ")", legend);
        
        // The autors of PLPlot thinks that "Java look is pretty lame", so they tinker
        // with the UIManager, grrr. Fix up after them.
        updateLAF();
    }//GEN-LAST:event_protocolPlotButtonActionPerformed

    private class WarDialerThread extends Thread {
        public WarDialerThread() {
        }

        @Override
        public void run() {
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
            //pauseButton.setEnabled(true);
            int beg = -1;
            int end = -1;
            int delay = -9999;
            try {
                beg = Integer.parseInt(commandno_TextField.getText());
                end = Integer.parseInt(endFTextField.getText());
                delay = Math.round((int) (Double.parseDouble(delayTextField.getText()) * 1000));
            } catch (NumberFormatException ex) {
                System.err.println(ex.getMessage());
                startButton.setEnabled(true);
                stopButton.setEnabled(false);
                pauseButton.setEnabled(false);
                warDialerThread = null;
                return;
            }
            int hw_index = war_dialer_outputhw_ComboBox.getSelectedIndex();
            int noSends = Integer.parseInt((String)warDialerNoSendsComboBox.getSelectedItem());
            for (int cmd = beg; cmd <= end; cmd++) {
                currentFTextField.setText(Integer.toString(cmd));
                try {
                    IrSignal code = extractCode(cmd);
                    boolean success;
                    switch (hw_index) {
                        case hardwareIndexGlobalCache:
                            success = gc.send_ir(code, getGcModule(), getGcConnector(), noSends);
                            break;
                        case hardwareIndexIrtrans:
                            success = irt.send_ir(code, getIrtransLed(), noSends);
                            break;
                        case hardwareIndexLirc:
                            success = lircClient.send_ccf(code.ccfString(), noSends);
                            break;
                        case hardwareIndexAudio:
                            Wave wave = new Wave(code.toModulatedIrSequence(noSends - 1), audioFormat,
                                    audioOmitCheckBox.isSelected(),
                                    audioWaveformComboBox.getSelectedIndex() == 0,
                                    audioDivideCheckBox.isSelected());
                            wave.play(audioLine);
                            success = true;
                            break;
                        default:
                            System.err.println("Internal error, sorry.");
                            success = false;
                            break;
                    }
                    if (!success)
                        break;
                    Thread.sleep(delay);
                } catch (LineUnavailableException ex) {
                    System.err.println(ex.getMessage());
                } catch (UnknownHostException ex) {
                    System.err.println("Hostname not found");
                } catch (IOException ex) {
                    System.err.println(ex.getMessage());
                } catch (NumberFormatException ex) {
                    System.err.println(ex.getMessage());
                } catch (IrpMasterException ex) {
                    System.err.println(ex.getMessage());
                } catch (RecognitionException ex) {
                    System.err.println(ex.getMessage());
                } catch (InterruptedException ex) {
                    System.err.println("*** Interrupted ***");
                    break;
                }
            }
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
            pauseButton.setEnabled(false);
            warDialerThread = null;
        }
    }

    private static WarDialerThread warDialerThread = null;

    private void enableProtocolButtons(boolean state) {
        protocol_clear_Button.setEnabled(state);
        protocolAnalyzeButton.setEnabled(state);
        protocol_decode_Button.setEnabled(state);
        possiblyEnableEncodeSend();
        //protocolPlotButton.setEnabled(state);
    }

    private void enableProtocolButtons() {
        enableProtocolButtons(!protocol_raw_TextArea.getText().isEmpty());
    }

    private void startButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_startButtonActionPerformed
        if (warDialerThread != null)
            System.err.println("Warning: warDialerThread != null");

        int hw_index = war_dialer_outputhw_ComboBox.getSelectedIndex();
        if (hw_index == hardwareIndexAudio) {
            updateAudioFormat();
            getAudioLine();
            if (audioLine == null) {
                System.err.println("Could not get an audio line.");
                return;
            }
        }
        warDialerThread = new WarDialerThread();
        warDialerThread.start();
    }//GEN-LAST:event_startButtonActionPerformed

    private void stopButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_stopButtonActionPerformed
        warDialerThread.interrupt();
    }//GEN-LAST:event_stopButtonActionPerformed

    private void pauseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pauseButtonActionPerformed

    }//GEN-LAST:event_pauseButtonActionPerformed

    private void consoleClearMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_consoleClearMenuItemActionPerformed
        consoleTextArea.setText(null);
    }//GEN-LAST:event_consoleClearMenuItemActionPerformed

    private void consoleTextAreaMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_consoleTextAreaMousePressed
        if (evt.isPopupTrigger())
           consolePopupMenu.show(evt.getComponent(), evt.getX(), evt.getY());
    }//GEN-LAST:event_consoleTextAreaMousePressed

    private void consoleTextAreaMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_consoleTextAreaMouseReleased
        consoleTextAreaMousePressed(evt);
    }//GEN-LAST:event_consoleTextAreaMouseReleased

    private void consoleCopyMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_consoleCopyMenuItemActionPerformed
        (new CopyClipboardText()).toClipboard(consoleTextArea.getText());
    }//GEN-LAST:event_consoleCopyMenuItemActionPerformed

    private void consoleSaveMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_consoleSaveMenuItemActionPerformed
        consoletext_save_MenuItemActionPerformed(evt);
    }//GEN-LAST:event_consoleSaveMenuItemActionPerformed

    private void consoleCopySelectionMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_consoleCopySelectionMenuItemActionPerformed
        (new CopyClipboardText()).toClipboard(consoleTextArea.getSelectedText());
    }//GEN-LAST:event_consoleCopySelectionMenuItemActionPerformed

    private void protocol_raw_TextAreaMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_protocol_raw_TextAreaMousePressed
        if (evt.isPopupTrigger())
           CCFCodePopupMenu.show(evt.getComponent(), evt.getX(), evt.getY());
    }//GEN-LAST:event_protocol_raw_TextAreaMousePressed

    private void protocol_raw_TextAreaMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_protocol_raw_TextAreaMouseReleased
        protocol_raw_TextAreaMousePressed(evt);
    }//GEN-LAST:event_protocol_raw_TextAreaMouseReleased

    private void makehexIrpDirBrowseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_makehexIrpDirBrowseButtonActionPerformed
        open(makehexIrpDirTextField.getText(), verbose);
    }//GEN-LAST:event_makehexIrpDirBrowseButtonActionPerformed

    private void makehexIrpDirTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_makehexIrpDirTextFieldActionPerformed
        Props.getInstance().setMakehexIrpdir(makehexIrpDirTextField.getText());
    }//GEN-LAST:event_makehexIrpDirTextFieldActionPerformed

    private void debug_TextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_debug_TextFieldActionPerformed
        try {
            debug = (int) IrpUtils.parseLong(debug_TextField.getText());
        } catch (NumberFormatException e) {
            System.err.println("Debug code entry did not parse as number. Assuming 0.");
            debug = 0;
        }
        Makehex.setDebug(debug);
        Debug.setDebug(debug);

    }//GEN-LAST:event_debug_TextFieldActionPerformed

    private void listIrpMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_listIrpMenuItemActionPerformed
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(getMakehexIrpFile())));
            protocol_raw_TextArea.setText("");
            String line;
            while ((line = reader.readLine()) != null) {
                if (!protocol_raw_TextArea.getText().isEmpty())
                    protocol_raw_TextArea.append("\n");
                protocol_raw_TextArea.append(line);
            }
            enableProtocolButtons();
        } catch (FileNotFoundException ex) {
            System.err.println("IRP file " + getMakehexIrpFile() + " not found.");
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
        }
    }//GEN-LAST:event_listIrpMenuItemActionPerformed

    private void rawCodeClearMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rawCodeClearMenuItemActionPerformed
        protocol_raw_TextArea.setText("");
    }//GEN-LAST:event_rawCodeClearMenuItemActionPerformed

    private void rawCodeCopyMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rawCodeCopyMenuItemActionPerformed
        (new CopyClipboardText()).toClipboard(protocol_raw_TextArea.getSelectedText());
    }//GEN-LAST:event_rawCodeCopyMenuItemActionPerformed

    private void rawCodePasteMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rawCodePasteMenuItemActionPerformed
        protocol_raw_TextArea.setText((new CopyClipboardText()).fromClipboard());
        enableProtocolButtons();
    }//GEN-LAST:event_rawCodePasteMenuItemActionPerformed

    private void rawCodeSaveMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rawCodeSaveMenuItemActionPerformed
        if (protocol_raw_TextArea.getText().isEmpty()) {
            System.err.println("Nothing to save.");
            return;
        }
        File export = selectFile("Select file to save", null, true, Props.getInstance().getExportdir(), null);
        if (export != null) {
            try {
                PrintStream printStream = new PrintStream(export);
                printStream.println(protocol_raw_TextArea.getText());
                printStream.close();
            } catch (FileNotFoundException ex) {
                System.err.println(ex.getMessage());
            }
        }
    }//GEN-LAST:event_rawCodeSaveMenuItemActionPerformed

    private void rawCodeImportMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rawCodeImportMenuItemActionPerformed
        icf_import_ButtonActionPerformed(evt);
    }//GEN-LAST:event_rawCodeImportMenuItemActionPerformed

    private void viewExportButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_viewExportButtonActionPerformed
        edit(lastExportFile, verbose);
    }//GEN-LAST:event_viewExportButtonActionPerformed

    private void debug_TextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_debug_TextFieldFocusLost
        debug_TextFieldActionPerformed(null);
    }//GEN-LAST:event_debug_TextFieldFocusLost

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        doExit();
    }//GEN-LAST:event_formWindowClosing

    private void checkUpdatesMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkUpdatesMenuItemActionPerformed
        BufferedReader in = null;
        try {
            URL url = new URL(Version.currentVersionUrl);
            in = new BufferedReader(new InputStreamReader(url.openStream()));
            String current = in.readLine().trim();
            System.out.println(current.equals(Version.versionString)
                    ? "You are using the latest version of IrMaster, " + Version.versionString
                    : "Current version is " + current + ", your version is " + Version.versionString);
        } catch (IOException ex) {
            System.err.println("Problem getting current version");
            if (verbose)
                System.err.println(ex.getMessage());
        } finally {
            try {
                if (in != null)
                    in.close();
            } catch (IOException ex) {
                System.err.println("Problem closing version check Url");
            }
        }
    }//GEN-LAST:event_checkUpdatesMenuItemActionPerformed

    private void docuProtocolMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_docuProtocolMenuItemActionPerformed
        renderProtocolDocu();
    }//GEN-LAST:event_docuProtocolMenuItemActionPerformed

    private void copyMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_copyMenuItemActionPerformed
        JMenuItem jmi = (JMenuItem)evt.getSource();
        JPopupMenu jpm = (JPopupMenu)jmi.getParent();
        JTextField jtf = (JTextField) jpm.getInvoker();
        (new CopyClipboardText()).toClipboard(jtf.getText());
    }//GEN-LAST:event_copyMenuItemActionPerformed

    private void cpCopyMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cpCopyMenuItemActionPerformed
        copyMenuItemActionPerformed(evt);
    }//GEN-LAST:event_cpCopyMenuItemActionPerformed

    private void pasteMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pasteMenuItemActionPerformed
        JMenuItem jmi = (JMenuItem)evt.getSource();
        JPopupMenu jpm = (JPopupMenu)jmi.getParent();
        JTextField jtf = (JTextField) jpm.getInvoker();
        if (jtf.isEditable()) {
            jtf.setText((new CopyClipboardText()).fromClipboard());
            jtf.postActionEvent();
        }
    }//GEN-LAST:event_pasteMenuItemActionPerformed

    private void generic_copy_paste_menu(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_generic_copy_paste_menu
        if (evt.isPopupTrigger())
           copyPastePopupMenu.show(evt.getComponent(), evt.getX(), evt.getY());
    }//GEN-LAST:event_generic_copy_paste_menu

    private void generic_copy_menu(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_generic_copy_menu
        if (evt.isPopupTrigger())
           copyPopupMenu.show(evt.getComponent(), evt.getX(), evt.getY());
    }//GEN-LAST:event_generic_copy_menu

    private void openExportDirButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openExportDirButtonActionPerformed
        open(Props.getInstance().getExportdir(), verbose);
    }//GEN-LAST:event_openExportDirButtonActionPerformed

    private void exportFormatComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportFormatComboBoxActionPerformed
        enableExportFormatRelated();
    }//GEN-LAST:event_exportFormatComboBoxActionPerformed

    private void enableExportFormatRelated() {
        String format = (String) exportFormatComboBox.getSelectedItem();
        boolean isWave = format.equalsIgnoreCase("wave");
        boolean isLirc = format.equalsIgnoreCase("lirc");
        boolean isLintronic = format.equalsIgnoreCase("lintronic");
        exportRawCheckBox.setEnabled(!(isWave || isLirc || isLintronic));
        exportProntoCheckBox.setEnabled(!(isWave || isLirc || isLintronic));
        lastFTextField.setEnabled(!(isWave || isLintronic));
        exportGenerateTogglesCheckBox.setEnabled(!(isWave || isLintronic));
        exportRepetitionsComboBox.setEnabled(isWave || isLintronic);
    }

    private void irtransRemotesComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_irtransRemotesComboBoxActionPerformed
        try {
            String[] commands = irt.get_commands((String)irtransRemotesComboBox.getModel().getSelectedItem());
            java.util.Arrays.sort(commands, String.CASE_INSENSITIVE_ORDER);
            irtransCommandsComboBox.setModel(new DefaultComboBoxModel(commands));
            irtransCommandsComboBox.setEnabled(true);
        } catch (InterruptedException ex) {
            System.err.println(ex.getMessage());
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
        }
    }//GEN-LAST:event_irtransRemotesComboBoxActionPerformed

    private void irtransSendFlashedButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_irtransSendFlashedButtonActionPerformed
        try {
            irt.send_flashed_command((String)irtransRemotesComboBox.getModel().getSelectedItem(),
                    (String) irtransCommandsComboBox.getModel().getSelectedItem(),
                    getIrtransLed(),
                    Integer.parseInt((String) no_sends_irtrans_flashed_ComboBox.getModel().getSelectedItem()));
        } catch (UnknownHostException ex) {
            System.err.println(ex.getMessage());
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
        } catch (InterruptedException ex) {
            System.err.println(ex.getMessage());
        }
    }//GEN-LAST:event_irtransSendFlashedButtonActionPerformed

    private void lircRemotesComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_lircRemotesComboBoxActionPerformed
        String remote = (String) lircRemotesComboBox.getModel().getSelectedItem();
        try {
            String[] commands = lircClient.get_commands(remote);
            if (commands == null) {
                System.err.println("Getting commands failed. Try again.");
                lircCommandsComboBox.setEnabled(false);
            } else {
                Arrays.sort(commands, String.CASE_INSENSITIVE_ORDER);
                lircCommandsComboBox.setModel(new DefaultComboBoxModel(commands));
                lircCommandsComboBox.setEnabled(true);
            }
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
        }
    }//GEN-LAST:event_lircRemotesComboBoxActionPerformed

    private void lircSendPredefinedButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_lircSendPredefinedButtonActionPerformed
        try {
            lircClient.send_ir((String) lircRemotesComboBox.getModel().getSelectedItem(),
                    (String) lircCommandsComboBox.getModel().getSelectedItem(),
                    Integer.parseInt((String) noLircPredefinedsComboBox.getModel().getSelectedItem()));
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
        }
    }//GEN-LAST:event_lircSendPredefinedButtonActionPerformed

    private void lircTransmitterComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_lircTransmitterComboBoxActionPerformed
        // Selecting more than one transmitter is not supported.
        if (lircClient == null) {
            System.err.println("Error: No Lirc Server selected.");
            return;
        }
        int transmitter = Integer.parseInt((String) lircTransmitterComboBox.getModel().getSelectedItem());
        int[] arr = new int[1];
        arr[0] = transmitter;
        try {
            lircClient.set_transmitters(arr);
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
        }
    }//GEN-LAST:event_lircTransmitterComboBoxActionPerformed

    private void browseHomePageMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browseHomePageMenuItemActionPerformed
        browse(Version.homepageUrl, verbose);
    }//GEN-LAST:event_browseHomePageMenuItemActionPerformed

    private void protocol_outputhw_ComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_protocol_outputhw_ComboBoxActionPerformed
        hardwareIndex = protocol_outputhw_ComboBox.getSelectedIndex();
        outputHWTabbedPane.setSelectedIndex(hardwareIndex);
    }//GEN-LAST:event_protocol_outputhw_ComboBoxActionPerformed

    private void war_dialer_outputhw_ComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_war_dialer_outputhw_ComboBoxActionPerformed
        hardwareIndex = war_dialer_outputhw_ComboBox.getSelectedIndex();
        outputHWTabbedPane.setSelectedIndex(hardwareIndex);
    }//GEN-LAST:event_war_dialer_outputhw_ComboBoxActionPerformed

    private void disregard_repeat_mins_CheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_disregard_repeat_mins_CheckBoxMenuItemActionPerformed
        Props.getInstance().setDisregardRepeatMins(disregard_repeat_mins_CheckBoxMenuItem.isSelected());
        disregard_repeat_mins_CheckBox.setSelected(disregard_repeat_mins_CheckBoxMenuItem.isSelected());
    }//GEN-LAST:event_disregard_repeat_mins_CheckBoxMenuItemActionPerformed

    private void disregard_repeat_mins_CheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_disregard_repeat_mins_CheckBoxActionPerformed
        Props.getInstance().setDisregardRepeatMins(disregard_repeat_mins_CheckBox.isSelected());
        disregard_repeat_mins_CheckBoxMenuItem.setSelected(disregard_repeat_mins_CheckBox.isSelected());
    }//GEN-LAST:event_disregard_repeat_mins_CheckBoxActionPerformed

    private void read_ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_read_ButtonActionPerformed
        irtrans_address_TextFieldActionPerformed(null);
    }//GEN-LAST:event_read_ButtonActionPerformed

    private void read_lirc_ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_read_lirc_ButtonActionPerformed
        LircIPAddressTextFieldActionPerformed(null);
    }//GEN-LAST:event_read_lirc_ButtonActionPerformed

    private void lafComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_lafComboBoxActionPerformed
        updateLAF();
    }//GEN-LAST:event_lafComboBoxActionPerformed

    private void gc_module_ComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_gc_module_ComboBoxActionPerformed
        Props.getInstance().setGlobalcacheModule(Integer.parseInt((String)gc_module_ComboBox.getSelectedItem()));
    }//GEN-LAST:event_gc_module_ComboBoxActionPerformed

    private void gc_connector_ComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_gc_connector_ComboBoxActionPerformed
        Props.getInstance().setGlobalcachePort(Integer.parseInt((String)gc_connector_ComboBox.getSelectedItem()));
    }//GEN-LAST:event_gc_connector_ComboBoxActionPerformed

    private void irtrans_led_ComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_irtrans_led_ComboBoxActionPerformed
        Props.getInstance().setIrTransPort(irtrans_led_ComboBox.getSelectedIndex());
    }//GEN-LAST:event_irtrans_led_ComboBoxActionPerformed

    private void no_periods_hex_TextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_no_periods_hex_TextFieldActionPerformed
        int noPeriods = Integer.parseInt(no_periods_hex_TextField.getText(), 16);
        no_periods_TextField.setText(String.format("%d", noPeriods));
        int freq = Integer.parseInt(frequency_TextField.getText());
        time_TextField.setText(Integer.toString((int) (1000000 * ((double) noPeriods) / (double) freq)));
    }//GEN-LAST:event_no_periods_hex_TextFieldActionPerformed

    private void no_periods_hex_TextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_no_periods_hex_TextFieldFocusLost
        no_periods_hex_TextFieldActionPerformed(null);
    }//GEN-LAST:event_no_periods_hex_TextFieldFocusLost

    private void time_TextFieldMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_time_TextFieldMouseEntered
        selectPeriodTime(false, false);
    }//GEN-LAST:event_time_TextFieldMouseEntered

    private void no_periods_TextFieldMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_no_periods_TextFieldMouseEntered
        selectPeriodTime(true, false);
    }//GEN-LAST:event_no_periods_TextFieldMouseEntered

    private void no_periods_hex_TextFieldMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_no_periods_hex_TextFieldMouseEntered
        selectPeriodTime(true, true);
    }//GEN-LAST:event_no_periods_hex_TextFieldMouseEntered

    private void protocol_raw_TextAreaMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_protocol_raw_TextAreaMouseExited
        enableProtocolButtons();
    }//GEN-LAST:event_protocol_raw_TextAreaMouseExited

    private void browseIRPMasterMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browseIRPMasterMenuItemActionPerformed
        browse(Props.getInstance().getIrpmasterUrl(), verbose);
    }//GEN-LAST:event_browseIRPMasterMenuItemActionPerformed

    private void browseJP1WikiActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browseJP1WikiActionPerformed
        browse("http://www.hifi-remote.com/wiki/index.php?title=Main_Page", verbose);
    }//GEN-LAST:event_browseJP1WikiActionPerformed

    private void browseIRPSpecMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browseIRPSpecMenuItemActionPerformed
        browse("http://www.hifi-remote.com/wiki/index.php?title=IRP_Notation", verbose);
    }//GEN-LAST:event_browseIRPSpecMenuItemActionPerformed

    private void browseDecodeIRMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browseDecodeIRMenuItemActionPerformed
        browse("http://www.hifi-remote.com/wiki/index.php?title=DecodeIR", verbose);
    }//GEN-LAST:event_browseDecodeIRMenuItemActionPerformed

    private void rawCodeSelectAllMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rawCodeSelectAllMenuItemActionPerformed
        protocol_raw_TextArea.selectAll();
    }//GEN-LAST:event_rawCodeSelectAllMenuItemActionPerformed

    private void rawCodeCopyAllMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rawCodeCopyAllMenuItemActionPerformed
        (new CopyClipboardText()).toClipboard(protocol_raw_TextArea.getText());
    }//GEN-LAST:event_rawCodeCopyAllMenuItemActionPerformed

    private void listIrpDefMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_listIrpDefMenuItemActionPerformed
        protocol_raw_TextArea.setText(irpMaster.getIrp((String) protocol_ComboBox.getSelectedItem()));
    }//GEN-LAST:event_listIrpDefMenuItemActionPerformed

    private void audioGetLineButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_audioGetLineButtonActionPerformed
        getAudioLine();
    }//GEN-LAST:event_audioGetLineButtonActionPerformed

    private void audioReleaseLineButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_audioReleaseLineButtonActionPerformed
        releaseAudioLine();
    }//GEN-LAST:event_audioReleaseLineButtonActionPerformed

    private void lircPingButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_lircPingButtonActionPerformed
        ping(LircIPAddressTextField);
    }//GEN-LAST:event_lircPingButtonActionPerformed

    private void irtransPingButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_irtransPingButtonActionPerformed
        ping(irtrans_address_TextField);
    }//GEN-LAST:event_irtransPingButtonActionPerformed

    private void globalCachePingButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_globalCachePingButtonActionPerformed
        ping(gc_address_TextField);
    }//GEN-LAST:event_globalCachePingButtonActionPerformed

    private boolean ping(JTextField jTextField) {
        String host = jTextField.getText();
        boolean success = false;
        try {
            success = InetAddress.getByName(host).isReachable(Props.getInstance().getPingTimeout());
            System.err.println(host + " is reachable");
        } catch (IOException ex) {
            System.err.println(host + " is not reachable (using Java's isReachable): " + ex.getMessage());
        }
        return success;
    }

    private void getAudioLine() {
        if (audioLine != null)
            return;
        updateAudioFormat();
        try {
            audioLine = Wave.getLine(audioFormat);
            System.err.println("Got an audio line for " + audioFormat.toString());
            audioGetLineButton.setEnabled(false);
            audioReleaseLineButton.setEnabled(true);
        } catch (LineUnavailableException ex) {
            System.err.println(ex.getMessage());
            audioLine = null;
            //audioGetLineButton.setSelected(false);
            audioGetLineButton.setEnabled(true);//;.setSelected(true);
            audioReleaseLineButton.setEnabled(false);
        }
    }

    private void releaseAudioLine() {
        if (audioLine != null)
            audioLine.close();
        audioLine = null;
        audioGetLineButton.setEnabled(true);
        audioReleaseLineButton.setEnabled(false);
    }

    private void updateAudioFormat() {
        int sampleFrequency = Integer.parseInt((String) audioSampleFrequencyComboBox.getSelectedItem());
        int sampleSize = Integer.parseInt((String) audioSampleSizeComboBox.getSelectedItem());
        int channels = audioChannelsComboBox.getSelectedIndex() + 1;
        boolean bigEndian = audioBigEndianCheckBox.isSelected();
        //boolean square = ((String) this.audioWaveformComboBox.getSelectedItem()).equalsIgnoreCase("square");
        audioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, (float) sampleFrequency,
                sampleSize, channels, sampleSize/8*channels, (float) sampleFrequency, bigEndian);
    }

    private void updateLAF() {
        int index = lafComboBox.getSelectedIndex();
        try {
            UIManager.setLookAndFeel(lafInfo[index].getClassName());
            Props.getInstance().setLookAndFeel(index);
        } catch (ClassNotFoundException ex) {
            error(ex.getMessage());
        } catch (InstantiationException ex) {
            error(ex.getMessage());
        } catch (IllegalAccessException ex) {
            error(ex.getMessage());
        } catch (UnsupportedLookAndFeelException ex) {
            error(ex.getMessage());
        }
        SwingUtilities.updateComponentTreeUI(this);
        pack();
    }


    private void updateHexcalc(int in, int noBytes) {
        int comp = noBytes == 2 ? 65535 : 255;
        int rev = noBytes == 2 ? ((Integer.reverse(in) >> 16) & 65535) : ((Integer.reverse(in) >> 24) & 255);
        String hex_format = noBytes == 2 ? "%04X" : "%02X";

        complement_decimal_TextField.setText(Integer.toString(comp - in));
        complement_hex_TextField.setText(String.format(hex_format, comp - in));
        reverse_decimal_TextField.setText(Integer.toString(rev));
        reverse_hex_TextField.setText(String.format(hex_format, rev));
        reverse_complement_hex_TextField.setText(String.format(hex_format, comp-rev));
        reverse_complement_decimal_TextField.setText(Integer.toString(comp-rev));
        efc_decimal_TextField.setText(Integer.toString(EFC.hex2efc(in)));
        efc_hex_TextField.setText(String.format("%02X", EFC.hex2efc(in)));
        efc5_decimal_TextField.setText(Integer.toString(EFC.hex2efc5(in, noBytes)));
        efc5_hex_TextField.setText(String.format(hex_format, EFC.hex2efc5(in, noBytes)));
        from_efc_decimal_TextField.setText(Integer.toString(EFC.efc2hex(in)));
        from_efc_hex_TextField.setText(String.format("%02X", EFC.efc2hex(in)));
        from_efc5_decimal_TextField.setText(Integer.toString(EFC.efc52hex(in, noBytes)));
        from_efc5_hex_TextField.setText(String.format(hex_format, EFC.efc52hex(in, noBytes)));
    }

    private void hexcalcSillyNumber(NumberFormatException e) {
        System.err.println("Parse error " + e.getMessage());
        complement_decimal_TextField.setText("****");
        complement_hex_TextField.setText("****");
        reverse_decimal_TextField.setText("****");
        reverse_hex_TextField.setText("****");
    }

    private void updateFromFrequency() {
        int freq = Integer.parseInt(frequency_TextField.getText());
        prontocode_TextField.setText(Pronto.formatInteger(Pronto.getProntoCode(freq)));//ir_code.ccf_integer(ir_code.get_frequency_code(freq)));
        updateFromFrequency(freq);
    }

    private void updateFromFrequencycode() {
        int freq = (int) Pronto.getFrequency(Integer.parseInt(prontocode_TextField.getText(),16));
        frequency_TextField.setText(Integer.toString(freq));
        updateFromFrequency(freq);
    }

    private void updateFromFrequency(int freq) {
        //if (period_selection_enable_CheckBox.isSelected()) {
        //    double no_periods = Double.parseDouble(no_periods_TextField.getText());
        //    time_TextField.setText(Integer.toString((int)(1000000.0*no_periods/freq)));
        //} else {
            int time = Integer.parseInt(time_TextField.getText());
            no_periods_TextField.setText(String.format("%.1f", (time*freq)/1000000.0));
        //}
    }

    private void possiblyEnableEncodeSend() {
        boolean looks_ok = !commandno_TextField.getText().isEmpty();
        protocol_send_Button.setEnabled(looks_ok || !protocol_raw_TextArea.getText().isEmpty());
        protocolPlotButton.setEnabled(looks_ok || !protocol_raw_TextArea.getText().isEmpty());
        protocol_generate_Button.setEnabled(looks_ok);
    }

    private int getGcModule() {
        return Integer.parseInt((String)gc_module_ComboBox.getSelectedItem());
    }

    private int getGcConnector() {
        return Integer.parseInt((String) gc_connector_ComboBox.getSelectedItem());
    }

    private irtrans.led_t getIrtransLed() {
        return irtrans.led_t.parse((String)irtrans_led_ComboBox.getSelectedItem());
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPopupMenu CCFCodePopupMenu;
    private javax.swing.JTextField IRP_TextField;
    private javax.swing.JButton IrpProtocolsBrowseButton;
    private javax.swing.JTextField IrpProtocolsTextField;
    private javax.swing.JTextField LircIPAddressTextField;
    private javax.swing.JMenuItem aboutMenuItem;
    private javax.swing.JPanel analyzePanel;
    private javax.swing.JCheckBox audioBigEndianCheckBox;
    private javax.swing.JComboBox audioChannelsComboBox;
    private javax.swing.JCheckBox audioDivideCheckBox;
    private javax.swing.JPanel audioFormatPanel;
    private javax.swing.JButton audioGetLineButton;
    private javax.swing.JCheckBox audioOmitCheckBox;
    private javax.swing.JPanel audioOptionsPanel;
    private javax.swing.JPanel audioPanel;
    private javax.swing.JButton audioReleaseLineButton;
    private javax.swing.JComboBox audioSampleFrequencyComboBox;
    private javax.swing.JComboBox audioSampleSizeComboBox;
    private javax.swing.JComboBox audioWaveformComboBox;
    private javax.swing.JCheckBox automaticFileNamesCheckBox;
    private javax.swing.JMenuItem browseDecodeIRMenuItem;
    private javax.swing.JMenuItem browseHomePageMenuItem;
    private javax.swing.JMenuItem browseIRPMasterMenuItem;
    private javax.swing.JMenuItem browseIRPSpecMenuItem;
    private javax.swing.JMenuItem browseJP1Wiki;
    private javax.swing.JMenuItem checkUpdatesMenuItem;
    private javax.swing.JMenuItem clear_console_MenuItem;
    private javax.swing.JTextField commandno_TextField;
    private javax.swing.JTextField complement_decimal_TextField;
    private javax.swing.JTextField complement_hex_TextField;
    private javax.swing.JMenuItem consoleClearMenuItem;
    private javax.swing.JMenuItem consoleCopyMenuItem;
    private javax.swing.JMenuItem consoleCopySelectionMenuItem;
    private javax.swing.JPopupMenu consolePopupMenu;
    private javax.swing.JMenuItem consoleSaveMenuItem;
    private javax.swing.JScrollPane consoleScrollPane;
    private javax.swing.JTextArea consoleTextArea;
    private javax.swing.JMenuItem consoletext_save_MenuItem;
    private javax.swing.JMenuItem contentMenuItem;
    private javax.swing.JMenuItem copyMenuItem;
    private javax.swing.JPopupMenu copyPastePopupMenu;
    private javax.swing.JPopupMenu copyPopupMenu;
    private javax.swing.JMenuItem copy_console_to_clipboard_MenuItem;
    private javax.swing.JMenuItem cpCopyMenuItem;
    private javax.swing.JTextField currentFTextField;
    private javax.swing.JTextField debug_TextField;
    private javax.swing.JTextField decimal_TextField;
    private javax.swing.JTextField delayTextField;
    private javax.swing.JTextField deviceno_TextField;
    private javax.swing.JButton discoverButton;
    private javax.swing.JCheckBox disregard_repeat_mins_CheckBox;
    private javax.swing.JCheckBoxMenuItem disregard_repeat_mins_CheckBoxMenuItem;
    private javax.swing.JMenuItem docuProtocolMenuItem;
    private javax.swing.JMenu editMenu;
    private javax.swing.JTextField efc5_decimal_TextField;
    private javax.swing.JTextField efc5_hex_TextField;
    private javax.swing.JTextField efc_decimal_TextField;
    private javax.swing.JTextField efc_hex_TextField;
    private javax.swing.JTextField endFTextField;
    private javax.swing.JMenuItem exitMenuItem;
    private javax.swing.JComboBox exportFormatComboBox;
    private javax.swing.JCheckBox exportGenerateTogglesCheckBox;
    private javax.swing.JPanel exportPanel;
    private javax.swing.JCheckBox exportProntoCheckBox;
    private javax.swing.JCheckBox exportRawCheckBox;
    private javax.swing.JComboBox exportRepetitionsComboBox;
    private javax.swing.JTextField exportdir_TextField;
    private javax.swing.JButton exportdir_browse_Button;
    private javax.swing.JMenu fileMenu;
    private javax.swing.JTextField frequency_TextField;
    private javax.swing.JTextField from_efc5_decimal_TextField;
    private javax.swing.JTextField from_efc5_hex_TextField;
    private javax.swing.JTextField from_efc_decimal_TextField;
    private javax.swing.JTextField from_efc_hex_TextField;
    private javax.swing.JLabel gcDiscoveredTypeLabel;
    private javax.swing.JTextField gc_address_TextField;
    private javax.swing.JButton gc_browse_Button;
    private javax.swing.JComboBox gc_connector_ComboBox;
    private javax.swing.JComboBox gc_module_ComboBox;
    private javax.swing.JButton globalCachePingButton;
    private javax.swing.JPanel globalcache_Panel;
    private javax.swing.JMenu helpMenu;
    private javax.swing.JTextField hex_TextField;
    private javax.swing.JPanel hexcalcPanel;
    private javax.swing.JButton icf_import_Button;
    private javax.swing.JButton irpProtocolsSelectButton;
    private javax.swing.JComboBox irtransCommandsComboBox;
    private javax.swing.JPanel irtransIPPanel;
    private javax.swing.JButton irtransPingButton;
    private javax.swing.JPanel irtransPredefinedPanel;
    private javax.swing.JComboBox irtransRemotesComboBox;
    private javax.swing.JButton irtransSendFlashedButton;
    private javax.swing.JLabel irtransVersionLabel;
    private javax.swing.JPanel irtrans_Panel;
    private javax.swing.JTextField irtrans_address_TextField;
    private javax.swing.JButton irtrans_browse_Button;
    private javax.swing.JComboBox irtrans_led_ComboBox;
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel24;
    private javax.swing.JLabel jLabel25;
    private javax.swing.JLabel jLabel26;
    private javax.swing.JLabel jLabel27;
    private javax.swing.JLabel jLabel28;
    private javax.swing.JLabel jLabel29;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel30;
    private javax.swing.JLabel jLabel32;
    private javax.swing.JLabel jLabel33;
    private javax.swing.JLabel jLabel34;
    private javax.swing.JLabel jLabel35;
    private javax.swing.JLabel jLabel36;
    private javax.swing.JLabel jLabel37;
    private javax.swing.JLabel jLabel38;
    private javax.swing.JLabel jLabel39;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel40;
    private javax.swing.JLabel jLabel41;
    private javax.swing.JLabel jLabel42;
    private javax.swing.JLabel jLabel43;
    private javax.swing.JLabel jLabel44;
    private javax.swing.JLabel jLabel45;
    private javax.swing.JLabel jLabel46;
    private javax.swing.JLabel jLabel47;
    private javax.swing.JLabel jLabel48;
    private javax.swing.JLabel jLabel49;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel50;
    private javax.swing.JLabel jLabel51;
    private javax.swing.JLabel jLabel52;
    private javax.swing.JLabel jLabel53;
    private javax.swing.JLabel jLabel54;
    private javax.swing.JLabel jLabel55;
    private javax.swing.JLabel jLabel56;
    private javax.swing.JLabel jLabel57;
    private javax.swing.JLabel jLabel58;
    private javax.swing.JLabel jLabel59;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel60;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JPopupMenu.Separator jSeparator13;
    private javax.swing.JPopupMenu.Separator jSeparator15;
    private javax.swing.JPopupMenu.Separator jSeparator3;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JPopupMenu.Separator jSeparator5;
    private javax.swing.JPopupMenu.Separator jSeparator7;
    private javax.swing.JPopupMenu.Separator jSeparator8;
    private javax.swing.JPopupMenu.Separator jSeparator9;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JComboBox lafComboBox;
    private javax.swing.JTextField lastFTextField;
    private javax.swing.JComboBox lircCommandsComboBox;
    private javax.swing.JPanel lircIPPanel;
    private javax.swing.JPanel lircPanel;
    private javax.swing.JButton lircPingButton;
    private javax.swing.JTextField lircPortTextField;
    private javax.swing.JPanel lircPredefinedPanel;
    private javax.swing.JComboBox lircRemotesComboBox;
    private javax.swing.JButton lircSendPredefinedButton;
    private javax.swing.JLabel lircServerVersionLabel;
    private javax.swing.JLabel lircServerVersionText;
    private javax.swing.JButton lircStopIrButton;
    private javax.swing.JComboBox lircTransmitterComboBox;
    private javax.swing.JMenuItem listIrpDefMenuItem;
    private javax.swing.JMenuItem listIrpMenuItem;
    private javax.swing.JButton macro_select_Button;
    private javax.swing.JSplitPane mainSplitPane;
    private javax.swing.JTabbedPane mainTabbedPane;
    private javax.swing.JButton makehexIrpDirBrowseButton;
    private javax.swing.JTextField makehexIrpDirTextField;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JComboBox noLircPredefinedsComboBox;
    private javax.swing.JTextField no_periods_TextField;
    private javax.swing.JTextField no_periods_hex_TextField;
    private javax.swing.JComboBox no_sends_irtrans_flashed_ComboBox;
    private javax.swing.JComboBox no_sends_protocol_ComboBox;
    private javax.swing.JButton notesClearButton;
    private javax.swing.JButton notesEditButton;
    private javax.swing.JButton notesSaveButton;
    private javax.swing.JPanel numbersPanel;
    private javax.swing.JButton openExportDirButton;
    private javax.swing.JPanel optionsPanel;
    private javax.swing.JTabbedPane outputHWTabbedPane;
    private javax.swing.JMenuItem pasteMenuItem;
    private javax.swing.JButton pauseButton;
    private javax.swing.JTextField prontocode_TextField;
    private javax.swing.JButton protocolAnalyzeButton;
    private javax.swing.JButton protocolExportButton;
    private javax.swing.JButton protocolPlotButton;
    private javax.swing.JComboBox protocol_ComboBox;
    private javax.swing.JButton protocol_clear_Button;
    private javax.swing.JButton protocol_decode_Button;
    private javax.swing.JButton protocol_generate_Button;
    private javax.swing.JComboBox protocol_outputhw_ComboBox;
    private javax.swing.JTextField protocol_params_TextField;
    private javax.swing.JTextArea protocol_raw_TextArea;
    private javax.swing.JButton protocol_send_Button;
    private javax.swing.JButton protocol_stop_Button;
    private javax.swing.JPanel protocolsPanel;
    private javax.swing.JTabbedPane protocolsSubPane;
    private javax.swing.JMenuItem rawCodeClearMenuItem;
    private javax.swing.JMenuItem rawCodeCopyAllMenuItem;
    private javax.swing.JMenuItem rawCodeCopyMenuItem;
    private javax.swing.JMenuItem rawCodeImportMenuItem;
    private javax.swing.JMenuItem rawCodePasteMenuItem;
    private javax.swing.JMenuItem rawCodeSaveMenuItem;
    private javax.swing.JMenuItem rawCodeSelectAllMenuItem;
    private javax.swing.JButton read_Button;
    private javax.swing.JButton read_lirc_Button;
    private javax.swing.JComboBox rendererComboBox;
    private javax.swing.JTextField reverse_complement_decimal_TextField;
    private javax.swing.JTextField reverse_complement_hex_TextField;
    private javax.swing.JTextField reverse_decimal_TextField;
    private javax.swing.JTextField reverse_hex_TextField;
    private javax.swing.JMenuItem saveAsMenuItem;
    private javax.swing.JMenuItem saveMenuItem;
    private javax.swing.JButton startButton;
    private javax.swing.JButton stopButton;
    private javax.swing.JTextField subdevice_TextField;
    private javax.swing.JPanel timeFrequencyPanel;
    private javax.swing.JTextField time_TextField;
    private javax.swing.JComboBox toggle_ComboBox;
    private javax.swing.JCheckBox verbose_CheckBox;
    private javax.swing.JCheckBoxMenuItem verbose_CheckBoxMenuItem;
    private javax.swing.JButton viewExportButton;
    private javax.swing.JComboBox warDialerNoSendsComboBox;
    private javax.swing.JPanel warDialerPanel;
    private javax.swing.JComboBox war_dialer_outputhw_ComboBox;
    // End of variables declaration//GEN-END:variables
    private AboutPopup aboutBox;
}
