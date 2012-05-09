/*
Copyright (C) 2011, 2012 Bengt Martensson.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published byto
the Free Software Foundation; either version 3 of the License, or (at
your option) any later version.

This program is distributed in the hope thlat it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License along with
this program. If not, see http://www.gnu.org/licenses/.
*/

package org.harctoolbox.IrMaster;

import com.hifiremote.exchangeir.Analyzer;
import com.hifiremote.makehex.Makehex;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.*;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.antlr.runtime.RecognitionException;
import org.harctoolbox.IrpMaster.*;
import org.harctoolbox.*;

/**
 * This class implements a GUI for several IR programs.
 */

public class GuiMain extends javax.swing.JFrame {
    private static IrpMaster irpMaster = null;
    private static HashMap<String, Protocol> protocols = null;
    private final static short invalid_parameter = (short)-1;
    private int debug = 0;
    private boolean verbose = false;
    private DefaultComboBoxModel rendererDcbm;
    private String[] lafNames;
    private UIManager.LookAndFeelInfo[] lafInfo;
    private static final String IrpFileExtension = "irp";
    private globalcache_thread the_globalcache_protocol_thread = null;
    private irtrans_thread the_irtrans_thread = null;
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

    private HashMap<String, String> filechooserdirs = new HashMap<String, String>();

    private File select_file(String title, String extension, String file_type_desc, boolean save, String defaultdir) {
        String startdir = this.filechooserdirs.containsKey(title) ? this.filechooserdirs.get(title) : defaultdir;
        JFileChooser chooser = new JFileChooser(startdir);
        chooser.setDialogTitle(title);
        if (extension == null) {
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        } else if (!extension.isEmpty())
            chooser.setFileFilter(new FileNameExtensionFilter(file_type_desc, extension));

        int result = save ? chooser.showSaveDialog(this) : chooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            filechooserdirs.put(title, chooser.getSelectedFile().getAbsoluteFile().getParent());
            return chooser.getSelectedFile();
        } else
            return null;
    }

    private class copy_clipboard_text implements ClipboardOwner {

        @Override
        public void lostOwnership(Clipboard c, Transferable t) {
        }

        public void to_clipboard(String str) {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(str), this);
        }

        public String from_clipboard() {
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

    private static Protocol get_protocol(String name) throws UnassignedException, RecognitionException {
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
            UIManager.setLookAndFeel(lafInfo[Props.get_instance().get_lookAndFeel()].getClassName());
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
            irpMaster = new IrpMaster(Props.get_instance().get_irpmaster_configfile());
        } catch (FileNotFoundException ex) {
            error(ex.getMessage());
        } catch (IncompatibleArgumentException ex) {
            error(ex.getMessage());
        }
        protocols = new HashMap<String, Protocol>();

        initComponents();
        lafComboBox.setSelectedIndex(Props.get_instance().get_lookAndFeel());
        Rectangle bounds = Props.get_instance().get_bounds();
        if (bounds != null)
            setBounds(bounds);

        gc_module_ComboBox.setSelectedItem(Integer.toString(Props.get_instance().get_globalcacheModule()));
        gc_connector_ComboBox.setSelectedItem(Integer.toString(Props.get_instance().get_globalcachePort()));

        irtrans_led_ComboBox.setSelectedIndex(Props.get_instance().get_irTransPort());

        disregard_repeat_mins_CheckBoxMenuItem.setSelected(Props.get_instance().get_disregard_repeat_mins());
        disregard_repeat_mins_CheckBox.setSelected(Props.get_instance().get_disregard_repeat_mins());

        System.setErr(console_PrintStream);
        System.setOut(console_PrintStream);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    Props.get_instance().save();
                    //socket_storage.dispose_sockets(true);
                } catch (Exception e) {
                    System.out.println("Problems saving properties; " + e.getMessage());
                }
                System.out.println("*** Normal GUI shutdown ***");
            }
        });

        protocol_ComboBox.setSelectedItem(Props.get_instance().get_protocol());
        update_protocol_parameters();
        verbose_CheckBoxMenuItem.setSelected(verbose);
        verbose_CheckBox.setSelected(verbose);

        gc = new globalcache("globalcache", globalcache.gc_model.gc_unknown, verbose);
        irt = new irtrans("irtrans", verbose);

        exportdir_TextField.setText(Props.get_instance().get_exportdir());
        update_from_frequency();
        hardwareIndex = Integer.parseInt(Props.get_instance().get_hardwareIndex());
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
            console_TextArea.append(aString);
            //console_TextArea.repaint();
        }

        @Override
        public void write(byte b[], int off, int len) throws IOException {
            String aString = new String(b, off, len);
            console_TextArea.append(aString);
            console_TextArea.setCaretPosition(console_TextArea.getDocument().getLength());
            //console_TextArea.repaint();
        }
    }

    PrintStream console_PrintStream = new PrintStream(
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
        rawCodePasteMenuItem = new javax.swing.JMenuItem();
        jSeparator7 = new javax.swing.JPopupMenu.Separator();
        rawCodeSaveMenuItem = new javax.swing.JMenuItem();
        rawCodeImportMenuItem = new javax.swing.JMenuItem();
        jSeparator9 = new javax.swing.JPopupMenu.Separator();
        docuProtocolMenuItem = new javax.swing.JMenuItem();
        listIrpMenuItem = new javax.swing.JMenuItem();
        copyPopupMenu = new javax.swing.JPopupMenu();
        copyMenuItem = new javax.swing.JMenuItem();
        copyPastePopupMenu = new javax.swing.JPopupMenu();
        cpCopyMenuItem = new javax.swing.JMenuItem();
        pasteMenuItem = new javax.swing.JMenuItem();
        mainTabbedPane = new javax.swing.JTabbedPane();
        protocolsPanel = new javax.swing.JPanel();
        protocol_ComboBox = new javax.swing.JComboBox();
        deviceno_TextField = new javax.swing.JTextField();
        subdevice_TextField = new javax.swing.JTextField();
        commandno_TextField = new javax.swing.JTextField();
        toggle_ComboBox = new javax.swing.JComboBox();
        protocol_params_TextField = new javax.swing.JTextField();
        DecodeIRVersion = new javax.swing.JLabel();
        protocolsSubPane = new javax.swing.JTabbedPane();
        analyzePanel = new javax.swing.JPanel();
        IRP_Label = new javax.swing.JLabel();
        IRP_TextField = new javax.swing.JTextField();
        jScrollPane3 = new javax.swing.JScrollPane();
        protocol_raw_TextArea = new javax.swing.JTextArea();
        protocol_generate_Button = new javax.swing.JButton();
        protocol_decode_Button = new javax.swing.JButton();
        protocol_clear_Button = new javax.swing.JButton();
        icf_import_Button = new javax.swing.JButton();
        protocol_outputhw_ComboBox = new javax.swing.JComboBox();
        no_sends_protocol_ComboBox = new javax.swing.JComboBox();
        protocol_send_Button = new javax.swing.JButton();
        protocol_stop_Button = new javax.swing.JButton();
        protocolAnalyzeButton = new javax.swing.JButton();
        protocolPlotButton = new javax.swing.JButton();
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
        war_dialer_outputhw_ComboBox = new javax.swing.JComboBox();
        endFTextField = new javax.swing.JTextField();
        jLabel27 = new javax.swing.JLabel();
        jLabel28 = new javax.swing.JLabel();
        jLabel29 = new javax.swing.JLabel();
        delayTextField = new javax.swing.JTextField();
        startButton = new javax.swing.JButton();
        stopButton = new javax.swing.JButton();
        pauseButton = new javax.swing.JButton();
        jLabel30 = new javax.swing.JLabel();
        currentFTextField = new javax.swing.JTextField();
        notesEditButton = new javax.swing.JButton();
        jSeparator2 = new javax.swing.JSeparator();
        notesClearButton = new javax.swing.JButton();
        notesSaveButton = new javax.swing.JButton();
        jLabel31 = new javax.swing.JLabel();
        jLabel32 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
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
        gc_address_TextField = new javax.swing.JTextField();
        gc_module_ComboBox = new javax.swing.JComboBox();
        gc_connector_ComboBox = new javax.swing.JComboBox();
        gc_browse_Button = new javax.swing.JButton();
        jButton1 = new javax.swing.JButton();
        discoverButton = new javax.swing.JButton();
        gcDiscoveredTypejTextField = new javax.swing.JTextField();
        jLabel34 = new javax.swing.JLabel();
        jLabel19 = new javax.swing.JLabel();
        jLabel35 = new javax.swing.JLabel();
        jLabel36 = new javax.swing.JLabel();
        irtrans_Panel = new javax.swing.JPanel();
        irtrans_address_TextField = new javax.swing.JTextField();
        irtrans_led_ComboBox = new javax.swing.JComboBox();
        irtrans_browse_Button = new javax.swing.JButton();
        jLabel37 = new javax.swing.JLabel();
        jLabel38 = new javax.swing.JLabel();
        irtransRemotesComboBox = new javax.swing.JComboBox();
        irtransCommandsComboBox = new javax.swing.JComboBox();
        irtransVersionLabel = new javax.swing.JLabel();
        no_sends_irtrans_flashed_ComboBox = new javax.swing.JComboBox();
        jSeparator10 = new javax.swing.JSeparator();
        jSeparator11 = new javax.swing.JSeparator();
        jLabel18 = new javax.swing.JLabel();
        irtransSendFlashedButton = new javax.swing.JButton();
        jLabel33 = new javax.swing.JLabel();
        jLabel51 = new javax.swing.JLabel();
        jLabel52 = new javax.swing.JLabel();
        jLabel53 = new javax.swing.JLabel();
        read_Button = new javax.swing.JButton();
        lircPanel = new javax.swing.JPanel();
        LircIPAddressTextField = new javax.swing.JTextField();
        lircStopIrButton = new javax.swing.JButton();
        lircPortTextField = new javax.swing.JTextField();
        lircServerVersionText = new javax.swing.JLabel();
        noLircPredefinedsComboBox = new javax.swing.JComboBox();
        lircRemotesComboBox = new javax.swing.JComboBox();
        lircCommandsComboBox = new javax.swing.JComboBox();
        lircSendPredefinedButton = new javax.swing.JButton();
        lircServerVersionLabel = new javax.swing.JLabel();
        lircTransmitterComboBox = new javax.swing.JComboBox();
        jLabel44 = new javax.swing.JLabel();
        jLabel45 = new javax.swing.JLabel();
        jLabel46 = new javax.swing.JLabel();
        jSeparator12 = new javax.swing.JSeparator();
        jSeparator14 = new javax.swing.JSeparator();
        jLabel47 = new javax.swing.JLabel();
        jLabel48 = new javax.swing.JLabel();
        jLabel49 = new javax.swing.JLabel();
        jLabel50 = new javax.swing.JLabel();
        read_lirc_Button = new javax.swing.JButton();
        hexcalcPanel = new javax.swing.JPanel();
        decimal_TextField = new javax.swing.JTextField();
        hex_TextField = new javax.swing.JTextField();
        complement_decimal_TextField = new javax.swing.JTextField();
        complement_hex_TextField = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jLabel14 = new javax.swing.JLabel();
        reverse_hex_TextField = new javax.swing.JTextField();
        reverse_decimal_TextField = new javax.swing.JTextField();
        no_periods_TextField = new javax.swing.JTextField();
        frequency_TextField = new javax.swing.JTextField();
        jLabel22 = new javax.swing.JLabel();
        jLabel23 = new javax.swing.JLabel();
        time_TextField = new javax.swing.JTextField();
        jLabel24 = new javax.swing.JLabel();
        prontocode_TextField = new javax.swing.JTextField();
        jLabel25 = new javax.swing.JLabel();
        period_selection_enable_CheckBox = new javax.swing.JCheckBox();
        jSeparator6 = new javax.swing.JSeparator();
        reverse_complement_decimal_TextField = new javax.swing.JTextField();
        reverse_complement_hex_TextField = new javax.swing.JTextField();
        efc_hex_TextField = new javax.swing.JTextField();
        efc_decimal_TextField = new javax.swing.JTextField();
        efc5_decimal_TextField = new javax.swing.JTextField();
        efc5_hex_TextField = new javax.swing.JTextField();
        from_efc_decimal_TextField = new javax.swing.JTextField();
        from_efc_hex_TextField = new javax.swing.JTextField();
        from_efc5_decimal_TextField = new javax.swing.JTextField();
        from_efc5_hex_TextField = new javax.swing.JTextField();
        jLabel9 = new javax.swing.JLabel();
        jLabel15 = new javax.swing.JLabel();
        jLabel40 = new javax.swing.JLabel();
        jLabel41 = new javax.swing.JLabel();
        jLabel42 = new javax.swing.JLabel();
        jLabel43 = new javax.swing.JLabel();
        no_periods_hex_TextField = new javax.swing.JTextField();
        jLabel39 = new javax.swing.JLabel();
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
        jScrollPane1 = new javax.swing.JScrollPane();
        console_TextArea = new javax.swing.JTextArea();
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

        rawCodeCopyMenuItem.setText("Copy");
        rawCodeCopyMenuItem.setToolTipText("Copy current contents to the clipboard");
        rawCodeCopyMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rawCodeCopyMenuItemActionPerformed(evt);
            }
        });
        CCFCodePopupMenu.add(rawCodeCopyMenuItem);

        rawCodePasteMenuItem.setText("Paste");
        rawCodePasteMenuItem.setToolTipText("Paste from clipboard");
        rawCodePasteMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rawCodePasteMenuItemActionPerformed(evt);
            }
        });
        CCFCodePopupMenu.add(rawCodePasteMenuItem);
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
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

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

        IRP_Label.setText("IRP");

        IRP_TextField.setEditable(false);
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

        protocol_generate_Button.setMnemonic('R');
        protocol_generate_Button.setText("Render");
        protocol_generate_Button.setToolTipText("Compute Pronto code from upper row protocol description");
        protocol_generate_Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                protocol_generate_ButtonActionPerformed(evt);
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

        protocol_clear_Button.setMnemonic('C');
        protocol_clear_Button.setText("Clear");
        protocol_clear_Button.setToolTipText("Clears code text areas");
        protocol_clear_Button.setEnabled(false);
        protocol_clear_Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                protocol_clear_ButtonActionPerformed(evt);
            }
        });

        icf_import_Button.setText("Import...");
        icf_import_Button.setToolTipText("Import file from IR WIdget/IRScope");
        icf_import_Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                icf_import_ButtonActionPerformed(evt);
            }
        });

        protocol_outputhw_ComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "GlobalCache", "IRTrans (udp)", "LIRC" }));
        protocol_outputhw_ComboBox.setToolTipText("Device used for when sending");
        protocol_outputhw_ComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                protocol_outputhw_ComboBoxActionPerformed(evt);
            }
        });

        no_sends_protocol_ComboBox.setMaximumRowCount(20);
        no_sends_protocol_ComboBox.setModel(noSendsSignalsComboBoxModel);
        no_sends_protocol_ComboBox.setToolTipText("Number of times to send IR signal");

        protocol_send_Button.setMnemonic('S');
        protocol_send_Button.setText("Send");
        protocol_send_Button.setToolTipText("Send code in Code window, or if empty, render new signal and send it to selected output device.");
        protocol_send_Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                protocol_send_ButtonActionPerformed(evt);
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

        javax.swing.GroupLayout analyzePanelLayout = new javax.swing.GroupLayout(analyzePanel);
        analyzePanel.setLayout(analyzePanelLayout);
        analyzePanelLayout.setHorizontalGroup(
            analyzePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(analyzePanelLayout.createSequentialGroup()
                .addGroup(analyzePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(protocol_outputhw_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(no_sends_protocol_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(protocol_send_Button, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(protocol_stop_Button, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 406, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(analyzePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(protocol_generate_Button, javax.swing.GroupLayout.PREFERRED_SIZE, 83, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(icf_import_Button, javax.swing.GroupLayout.PREFERRED_SIZE, 83, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(protocol_decode_Button, javax.swing.GroupLayout.PREFERRED_SIZE, 83, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(protocolAnalyzeButton, javax.swing.GroupLayout.PREFERRED_SIZE, 83, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(protocol_clear_Button, javax.swing.GroupLayout.PREFERRED_SIZE, 83, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(protocolPlotButton, javax.swing.GroupLayout.PREFERRED_SIZE, 83, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
            .addGroup(analyzePanelLayout.createSequentialGroup()
                .addComponent(IRP_Label)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(IRP_TextField, javax.swing.GroupLayout.DEFAULT_SIZE, 599, Short.MAX_VALUE))
        );

        analyzePanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {icf_import_Button, protocolAnalyzeButton, protocolPlotButton, protocol_clear_Button, protocol_decode_Button, protocol_generate_Button});

        analyzePanelLayout.setVerticalGroup(
            analyzePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(analyzePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(analyzePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(IRP_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(IRP_Label))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(analyzePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(analyzePanelLayout.createSequentialGroup()
                        .addComponent(protocol_outputhw_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(16, 16, 16)
                        .addComponent(no_sends_protocol_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(protocol_send_Button)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(protocol_stop_Button))
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 231, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(analyzePanelLayout.createSequentialGroup()
                        .addComponent(protocol_generate_Button, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(icf_import_Button)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(protocol_decode_Button, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(protocolAnalyzeButton, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(protocol_clear_Button, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(protocolPlotButton, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );

        analyzePanelLayout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {icf_import_Button, protocolAnalyzeButton, protocolPlotButton, protocol_clear_Button, protocol_decode_Button, protocol_generate_Button});

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
        automaticFileNamesCheckBox.setText("Automatic File Names");
        automaticFileNamesCheckBox.setToolTipText("Perform export to a file with automatically generated name, Otherwise a file browser will be started.");

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

        jLabel17.setText("Ending F");

        exportFormatComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Text", "XML", "LIRC", "Wave" }));
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

        jLabel21.setText("Export directory");

        exportRawCheckBox.setMnemonic('R');
        exportRawCheckBox.setText("Raw");
        exportRawCheckBox.setToolTipText("Generate raw codes (timing in microseconds) in export");

        exportProntoCheckBox.setMnemonic('P');
        exportProntoCheckBox.setSelected(true);
        exportProntoCheckBox.setText("Pronto");
        exportProntoCheckBox.setToolTipText("Generate Pronto (CCF) codes in export");

        viewExportButton.setMnemonic('V');
        viewExportButton.setText("View Export");
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

        exportRepetitionsComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "0", "1", "2", "3", "4", "5", "7", "10", "12", "15", "20", "25", "30", "40", "50", "70", "100" }));

        jLabel54.setText("# Repetitions");

        javax.swing.GroupLayout exportPanelLayout = new javax.swing.GroupLayout(exportPanel);
        exportPanel.setLayout(exportPanelLayout);
        exportPanelLayout.setHorizontalGroup(
            exportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(exportPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(exportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(exportGenerateTogglesCheckBox)
                    .addGroup(exportPanelLayout.createSequentialGroup()
                        .addGroup(exportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel20)
                            .addComponent(jLabel17)
                            .addComponent(jLabel21))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(exportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lastFTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 41, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(exportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                .addGroup(javax.swing.GroupLayout.Alignment.LEADING, exportPanelLayout.createSequentialGroup()
                                    .addComponent(exportFormatComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGap(18, 18, 18)
                                    .addComponent(exportRawCheckBox)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(exportProntoCheckBox)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(jLabel54, javax.swing.GroupLayout.PREFERRED_SIZE, 96, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGap(3, 3, 3)
                                    .addComponent(exportRepetitionsComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGroup(javax.swing.GroupLayout.Alignment.LEADING, exportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(protocolExportButton)
                                    .addGroup(exportPanelLayout.createSequentialGroup()
                                        .addComponent(exportdir_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, 367, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(exportdir_browse_Button)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(openExportDirButton))
                                    .addComponent(viewExportButton)))))
                    .addComponent(automaticFileNamesCheckBox))
                .addContainerGap(37, Short.MAX_VALUE))
        );
        exportPanelLayout.setVerticalGroup(
            exportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(exportPanelLayout.createSequentialGroup()
                .addGap(26, 26, 26)
                .addGroup(exportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel17)
                    .addComponent(lastFTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(exportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel20)
                    .addComponent(exportFormatComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(exportProntoCheckBox)
                    .addComponent(exportRawCheckBox)
                    .addComponent(exportRepetitionsComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel54))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(exportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel21)
                    .addComponent(exportdir_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(exportdir_browse_Button)
                    .addComponent(openExportDirButton))
                .addGap(10, 10, 10)
                .addGroup(exportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(exportGenerateTogglesCheckBox)
                    .addComponent(protocolExportButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(exportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(automaticFileNamesCheckBox)
                    .addComponent(viewExportButton))
                .addGap(65, 65, 65))
        );

        protocolsSubPane.addTab("Export", exportPanel);

        war_dialer_outputhw_ComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "GlobalCache", "IRTrans (udp)", "LIRC" }));
        war_dialer_outputhw_ComboBox.setToolTipText("Device to use for sending");
        war_dialer_outputhw_ComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                war_dialer_outputhw_ComboBoxActionPerformed(evt);
            }
        });

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

        jLabel28.setText("Ending F");

        jLabel29.setText("Delay (s)");

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

        pauseButton.setMnemonic('P');
        pauseButton.setText("Pause");
        pauseButton.setToolTipText("Pause transmission, with possibility to resume. Not yet implemented.");
        pauseButton.setEnabled(false);
        pauseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pauseButtonActionPerformed(evt);
            }
        });

        jLabel30.setText("Current F");

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

        notesEditButton.setText("Edit");
        notesEditButton.setToolTipText("Not yet implemented.");
        notesEditButton.setEnabled(false);

        notesClearButton.setText("Clear");
        notesClearButton.setToolTipText("Not yet implemented.");
        notesClearButton.setEnabled(false);

        notesSaveButton.setText("Save");
        notesSaveButton.setToolTipText("Not yet implemented.");
        notesSaveButton.setEnabled(false);

        jLabel31.setText("Notes:");
        jLabel31.setToolTipText("Make a note on the recently sent IR signal (not yet implemented).");
        jLabel31.setEnabled(false);

        jLabel32.setText(" ");

        jTextArea1.setColumns(20);
        jTextArea1.setEditable(false);
        jTextArea1.setLineWrap(true);
        jTextArea1.setRows(2);
        jTextArea1.setText("Warning: Sending undocumented IR commands to your equipment may damage or even destroy it. By using this program, you agree to take the responsibility for possible damages yourself, and not to hold the author responsible.");
        jTextArea1.setToolTipText("You have been warned!");
        jTextArea1.setWrapStyleWord(true);
        jScrollPane2.setViewportView(jTextArea1);

        javax.swing.GroupLayout warDialerPanelLayout = new javax.swing.GroupLayout(warDialerPanel);
        warDialerPanel.setLayout(warDialerPanelLayout);
        warDialerPanelLayout.setHorizontalGroup(
            warDialerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(warDialerPanelLayout.createSequentialGroup()
                .addGroup(warDialerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(warDialerPanelLayout.createSequentialGroup()
                        .addComponent(jLabel31)
                        .addGap(18, 18, 18)
                        .addComponent(notesEditButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(notesClearButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(notesSaveButton))
                    .addGroup(warDialerPanelLayout.createSequentialGroup()
                        .addGroup(warDialerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel28)
                            .addComponent(jLabel27, javax.swing.GroupLayout.PREFERRED_SIZE, 93, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel29))
                        .addGap(41, 41, 41)
                        .addGroup(warDialerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(war_dialer_outputhw_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(warDialerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(warDialerPanelLayout.createSequentialGroup()
                                    .addComponent(delayTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 41, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGap(146, 146, 146))
                                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, warDialerPanelLayout.createSequentialGroup()
                                    .addComponent(endFTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 41, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGap(97, 97, 97)
                                    .addGroup(warDialerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(warDialerPanelLayout.createSequentialGroup()
                                            .addComponent(jLabel30)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                            .addComponent(currentFTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 41, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addGroup(warDialerPanelLayout.createSequentialGroup()
                                            .addComponent(startButton)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                            .addComponent(stopButton)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                            .addComponent(pauseButton)))))))
                    .addGroup(warDialerPanelLayout.createSequentialGroup()
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 631, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(24, 24, 24)
                        .addComponent(jLabel32))
                    .addComponent(jSeparator2))
                .addContainerGap())
        );
        warDialerPanelLayout.setVerticalGroup(
            warDialerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(warDialerPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(warDialerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel27)
                    .addComponent(war_dialer_outputhw_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(warDialerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel28)
                    .addComponent(endFTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel30)
                    .addComponent(currentFTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(warDialerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel29)
                    .addComponent(delayTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(startButton)
                    .addComponent(stopButton)
                    .addComponent(pauseButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(warDialerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel31)
                    .addComponent(notesEditButton)
                    .addComponent(notesClearButton)
                    .addComponent(notesSaveButton))
                .addGroup(warDialerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(warDialerPanelLayout.createSequentialGroup()
                        .addGap(28, 28, 28)
                        .addComponent(jLabel32)
                        .addContainerGap(50, Short.MAX_VALUE))
                    .addGroup(warDialerPanelLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 95, Short.MAX_VALUE))))
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
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, protocolsPanelLayout.createSequentialGroup()
                .addComponent(protocolsSubPane, javax.swing.GroupLayout.PREFERRED_SIZE, 645, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(DecodeIRVersion, javax.swing.GroupLayout.PREFERRED_SIZE, 112, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(516, 516, 516))
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
                    .addComponent(protocol_params_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, 237, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(648, 648, 648))
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
                .addGroup(protocolsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(protocolsPanelLayout.createSequentialGroup()
                        .addGap(21, 21, 21)
                        .addComponent(DecodeIRVersion, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(292, 292, 292))
                    .addGroup(protocolsPanelLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(protocolsSubPane, javax.swing.GroupLayout.PREFERRED_SIZE, 312, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap())))
        );

        mainTabbedPane.addTab("IR Protocols", null, protocolsPanel, "This pane deals with generating, sending, exporting, and analyzing of IR protocols.");

        outputHWTabbedPane.setToolTipText("This pane sets the properties of the output hardware.");

        gc_address_TextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        gc_address_TextField.setText(Props.get_instance().get_globalcacheIpName());
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

        gc_module_ComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "1", "2", "4", "5" }));
        gc_module_ComboBox.setToolTipText("GlobalCache IR Module to use");
        gc_module_ComboBox.setMaximumSize(new java.awt.Dimension(48, 28));
        gc_module_ComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                gc_module_ComboBoxActionPerformed(evt);
            }
        });

        gc_connector_ComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "1", "2", "3" }));
        gc_connector_ComboBox.setToolTipText("GlobalCache IR Connector to use");
        gc_connector_ComboBox.setMaximumSize(new java.awt.Dimension(32767, 27));
        gc_connector_ComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                gc_connector_ComboBoxActionPerformed(evt);
            }
        });

        gc_browse_Button.setMnemonic('B');
        gc_browse_Button.setText("Browse");
        gc_browse_Button.setToolTipText("Open selected GlobalCache in the browser.");
        gc_browse_Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                gc_browse_ButtonActionPerformed(evt);
            }
        });

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

        gcDiscoveredTypejTextField.setEditable(false);
        gcDiscoveredTypejTextField.setText("<unknown>");
        gcDiscoveredTypejTextField.setToolTipText("Type of discovered GlobalCache");
        gcDiscoveredTypejTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                gcDiscoveredTypejTextFieldActionPerformed(evt);
            }
        });

        jLabel34.setText("GC Type:");

        jLabel19.setText("IP Name/Address");

        jLabel35.setText("Module");

        jLabel36.setText("Port");

        javax.swing.GroupLayout globalcache_PanelLayout = new javax.swing.GroupLayout(globalcache_Panel);
        globalcache_Panel.setLayout(globalcache_PanelLayout);
        globalcache_PanelLayout.setHorizontalGroup(
            globalcache_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(globalcache_PanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(globalcache_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(globalcache_PanelLayout.createSequentialGroup()
                        .addComponent(jLabel34)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(gcDiscoveredTypejTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 528, Short.MAX_VALUE))
                    .addGroup(globalcache_PanelLayout.createSequentialGroup()
                        .addComponent(gc_address_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(gc_module_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(gc_connector_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(33, 33, 33)
                        .addComponent(gc_browse_Button)
                        .addGap(12, 12, 12)
                        .addComponent(jButton1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 122, Short.MAX_VALUE)
                        .addComponent(discoverButton))
                    .addGroup(globalcache_PanelLayout.createSequentialGroup()
                        .addComponent(jLabel19)
                        .addGap(12, 12, 12)
                        .addComponent(jLabel35)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel36)))
                .addContainerGap())
        );
        globalcache_PanelLayout.setVerticalGroup(
            globalcache_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(globalcache_PanelLayout.createSequentialGroup()
                .addGap(23, 23, 23)
                .addGroup(globalcache_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel19)
                    .addComponent(jLabel35)
                    .addComponent(jLabel36))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(globalcache_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(gc_address_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(gc_module_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(gc_connector_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(gc_browse_Button)
                    .addComponent(jButton1)
                    .addComponent(discoverButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 134, Short.MAX_VALUE)
                .addGroup(globalcache_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel34)
                    .addComponent(gcDiscoveredTypejTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(63, 63, 63))
        );

        outputHWTabbedPane.addTab("GlobalCache", globalcache_Panel);

        irtrans_address_TextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        irtrans_address_TextField.setText(Props.get_instance().get_irTransIpName());
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

        irtrans_led_ComboBox.setMaximumRowCount(12);
        irtrans_led_ComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "intern", "extern", "both", "0", "1", "2", "3", "4", "5", "6", "7", "8" }));
        irtrans_led_ComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                irtrans_led_ComboBoxActionPerformed(evt);
            }
        });

        irtrans_browse_Button.setMnemonic('B');
        irtrans_browse_Button.setText("Browse");
        irtrans_browse_Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                irtrans_browse_ButtonActionPerformed(evt);
            }
        });

        jLabel37.setText("IP Name/Address");

        jLabel38.setText("Port");

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

        irtransCommandsComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "--" }));
        irtransCommandsComboBox.setToolTipText("Predefined Command");
        irtransCommandsComboBox.setEnabled(false);
        irtransCommandsComboBox.setMinimumSize(new java.awt.Dimension(140, 28));
        irtransCommandsComboBox.setPreferredSize(new java.awt.Dimension(140, 28));

        irtransVersionLabel.setText("<unknown>");

        no_sends_irtrans_flashed_ComboBox.setMaximumRowCount(20);
        no_sends_irtrans_flashed_ComboBox.setModel(noSendsIrtransFlashedComboBoxModel);
        no_sends_irtrans_flashed_ComboBox.setToolTipText("Number of times to send IR signal");

        jLabel18.setText("IrTrans Version:");

        irtransSendFlashedButton.setText("Send");
        irtransSendFlashedButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                irtransSendFlashedButtonActionPerformed(evt);
            }
        });

        jLabel33.setText("Flashed Commands (IRDB)");

        jLabel51.setText("#");

        jLabel52.setText("Remote");

        jLabel53.setText("Command");

        read_Button.setText("Read");
        read_Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                read_ButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout irtrans_PanelLayout = new javax.swing.GroupLayout(irtrans_Panel);
        irtrans_Panel.setLayout(irtrans_PanelLayout);
        irtrans_PanelLayout.setHorizontalGroup(
            irtrans_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSeparator10, javax.swing.GroupLayout.DEFAULT_SIZE, 631, Short.MAX_VALUE)
            .addGroup(irtrans_PanelLayout.createSequentialGroup()
                .addComponent(jSeparator11, javax.swing.GroupLayout.DEFAULT_SIZE, 619, Short.MAX_VALUE)
                .addContainerGap())
            .addGroup(irtrans_PanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(irtrans_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, irtrans_PanelLayout.createSequentialGroup()
                        .addGroup(irtrans_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(irtrans_address_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel37))
                        .addGap(18, 18, 18)
                        .addGroup(irtrans_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(irtrans_PanelLayout.createSequentialGroup()
                                .addComponent(irtrans_led_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(irtrans_browse_Button))
                            .addComponent(jLabel38))
                        .addGap(309, 309, 309))
                    .addGroup(irtrans_PanelLayout.createSequentialGroup()
                        .addComponent(jLabel18)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(irtransVersionLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 481, Short.MAX_VALUE)
                        .addContainerGap())
                    .addGroup(irtrans_PanelLayout.createSequentialGroup()
                        .addGroup(irtrans_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(irtrans_PanelLayout.createSequentialGroup()
                                .addGroup(irtrans_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(no_sends_irtrans_flashed_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel51))
                                .addGap(18, 18, 18)
                                .addGroup(irtrans_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(irtransRemotesComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel52))
                                .addGap(18, 18, 18)
                                .addGroup(irtrans_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(irtrans_PanelLayout.createSequentialGroup()
                                        .addComponent(irtransCommandsComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(18, 18, 18)
                                        .addComponent(irtransSendFlashedButton)
                                        .addGap(18, 18, 18)
                                        .addComponent(read_Button))
                                    .addComponent(jLabel53)))
                            .addComponent(jLabel33))
                        .addContainerGap(95, Short.MAX_VALUE))))
        );
        irtrans_PanelLayout.setVerticalGroup(
            irtrans_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(irtrans_PanelLayout.createSequentialGroup()
                .addGap(13, 13, 13)
                .addGroup(irtrans_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel37)
                    .addComponent(jLabel38))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(irtrans_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(irtrans_address_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(irtrans_led_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(irtrans_browse_Button))
                .addGap(14, 14, 14)
                .addComponent(jSeparator10, javax.swing.GroupLayout.PREFERRED_SIZE, 6, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel33)
                .addGap(34, 34, 34)
                .addGroup(irtrans_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel51)
                    .addComponent(jLabel52)
                    .addComponent(jLabel53))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(irtrans_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(no_sends_irtrans_flashed_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(irtransRemotesComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(irtransCommandsComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(irtransSendFlashedButton)
                    .addComponent(read_Button))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jSeparator11, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(irtrans_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel18)
                    .addComponent(irtransVersionLabel))
                .addContainerGap(42, Short.MAX_VALUE))
        );

        outputHWTabbedPane.addTab("IRTrans", irtrans_Panel);

        LircIPAddressTextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        LircIPAddressTextField.setText(Props.get_instance().get_lircIpName());
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

        lircStopIrButton.setMnemonic('T');
        lircStopIrButton.setText("Stop IR");
        lircStopIrButton.setToolTipText("Send the selected LIRC-server a stop command.");
        lircStopIrButton.setEnabled(false);
        lircStopIrButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                lircStopIrButtongc_stop_ir_ActionPerformed(evt);
            }
        });

        lircPortTextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        lircPortTextField.setText(Props.get_instance().get_lircPort());
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

        lircServerVersionText.setText("<unknown>");
        lircServerVersionText.setEnabled(false);

        noLircPredefinedsComboBox.setMaximumRowCount(20);
        noLircPredefinedsComboBox.setModel(noSendsLircPredefinedsComboBoxModel);
        noLircPredefinedsComboBox.setToolTipText("Number of times to send IR signal");
        noLircPredefinedsComboBox.setEnabled(false);

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

        lircCommandsComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "--" }));
        lircCommandsComboBox.setToolTipText("Predefined Command");
        lircCommandsComboBox.setEnabled(false);
        lircCommandsComboBox.setMinimumSize(new java.awt.Dimension(140, 28));
        lircCommandsComboBox.setPreferredSize(new java.awt.Dimension(140, 28));

        lircSendPredefinedButton.setText("Send");
        lircSendPredefinedButton.setEnabled(false);
        lircSendPredefinedButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                lircSendPredefinedButtonActionPerformed(evt);
            }
        });

        lircServerVersionLabel.setText("Lirc Server Version:");
        lircServerVersionLabel.setEnabled(false);

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

        jLabel44.setText("IP Name/Address");

        jLabel45.setText("TCP Port");

        jLabel46.setText("Transmitter");

        jLabel47.setText("Predefined commands");

        jLabel48.setText("#");

        jLabel49.setText("Remote");

        jLabel50.setText("Command");

        read_lirc_Button.setText("Read");
        read_lirc_Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                read_lirc_ButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout lircPanelLayout = new javax.swing.GroupLayout(lircPanel);
        lircPanel.setLayout(lircPanelLayout);
        lircPanelLayout.setHorizontalGroup(
            lircPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSeparator12, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 631, Short.MAX_VALUE)
            .addGroup(lircPanelLayout.createSequentialGroup()
                .addGroup(lircPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jSeparator14, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 619, Short.MAX_VALUE)
                    .addGroup(lircPanelLayout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(lircPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(lircPanelLayout.createSequentialGroup()
                                .addComponent(lircServerVersionLabel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(lircServerVersionText, javax.swing.GroupLayout.PREFERRED_SIZE, 458, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(lircPanelLayout.createSequentialGroup()
                                .addGroup(lircPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(lircPanelLayout.createSequentialGroup()
                                        .addGroup(lircPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(LircIPAddressTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addComponent(jLabel44))
                                        .addGap(18, 18, 18)
                                        .addGroup(lircPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addGroup(lircPanelLayout.createSequentialGroup()
                                                .addComponent(jLabel45)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(jLabel46))
                                            .addGroup(lircPanelLayout.createSequentialGroup()
                                                .addComponent(lircPortTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 52, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addGap(18, 18, 18)
                                                .addComponent(lircTransmitterComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addGap(18, 18, 18)
                                                .addComponent(lircStopIrButton, javax.swing.GroupLayout.PREFERRED_SIZE, 84, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addGap(18, 18, 18)
                                                .addComponent(read_lirc_Button))))
                                    .addGroup(lircPanelLayout.createSequentialGroup()
                                        .addComponent(noLircPredefinedsComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(18, 18, 18)
                                        .addGroup(lircPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(lircRemotesComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addComponent(jLabel49))
                                        .addGap(18, 18, 18)
                                        .addGroup(lircPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(jLabel50)
                                            .addComponent(lircCommandsComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                                .addGap(18, 18, 18)
                                .addComponent(lircSendPredefinedButton, javax.swing.GroupLayout.PREFERRED_SIZE, 68, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(jLabel47)
                            .addComponent(jLabel48))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        lircPanelLayout.setVerticalGroup(
            lircPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(lircPanelLayout.createSequentialGroup()
                .addGap(17, 17, 17)
                .addGroup(lircPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel44)
                    .addComponent(jLabel45)
                    .addComponent(jLabel46))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(lircPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(LircIPAddressTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lircPortTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lircTransmitterComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lircStopIrButton)
                    .addComponent(read_lirc_Button))
                .addGap(18, 18, 18)
                .addComponent(jSeparator12, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(3, 3, 3)
                .addComponent(jLabel47)
                .addGap(18, 18, 18)
                .addGroup(lircPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel48)
                    .addComponent(jLabel49)
                    .addComponent(jLabel50))
                .addGap(3, 3, 3)
                .addGroup(lircPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(noLircPredefinedsComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lircRemotesComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lircCommandsComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lircSendPredefinedButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 39, Short.MAX_VALUE)
                .addComponent(jSeparator14, javax.swing.GroupLayout.PREFERRED_SIZE, 6, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(lircPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lircServerVersionLabel)
                    .addComponent(lircServerVersionText))
                .addGap(29, 29, 29))
        );

        outputHWTabbedPane.addTab("LIRC", lircPanel);

        mainTabbedPane.addTab("Output HW", null, outputHWTabbedPane, "This pane sets the properties of the output hardware.");

        hexcalcPanel.setToolTipText("This pane consists of an interactive calculator for common computations on IR signals.");

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

        complement_decimal_TextField.setEditable(false);
        complement_decimal_TextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        complement_decimal_TextField.setText("255");
        complement_decimal_TextField.setPreferredSize(new java.awt.Dimension(100, 27));
        complement_decimal_TextField.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                generic_copy_menu(evt);
            }
        });

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

        jLabel6.setText("Decimal");

        jLabel7.setText("Hex");

        jLabel8.setText("Complement");

        jLabel14.setText("Reverse");

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

        jLabel22.setText("Frequency (Hz)");

        jLabel23.setText("# Periods (dec)");

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

        jLabel24.setText("Time (us)");

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

        jLabel25.setText("Prontocode");

        jSeparator6.setOrientation(javax.swing.SwingConstants.VERTICAL);

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

        jLabel9.setText("Input");

        jLabel15.setText("Rev.-Compl.");

        jLabel40.setText("EFC");

        jLabel41.setText("EFC^-1");

        jLabel42.setText("EFC5");

        jLabel43.setText("EFC5^-1");

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

        jLabel39.setText("# Periods (hex)");

        javax.swing.GroupLayout hexcalcPanelLayout = new javax.swing.GroupLayout(hexcalcPanel);
        hexcalcPanel.setLayout(hexcalcPanelLayout);
        hexcalcPanelLayout.setHorizontalGroup(
            hexcalcPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(hexcalcPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(hexcalcPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel9)
                    .addComponent(jLabel14)
                    .addComponent(jLabel15)
                    .addComponent(jLabel40)
                    .addComponent(jLabel41)
                    .addComponent(jLabel42)
                    .addComponent(jLabel43)
                    .addComponent(jLabel8))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 17, Short.MAX_VALUE)
                .addGroup(hexcalcPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(complement_decimal_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, hexcalcPanelLayout.createSequentialGroup()
                        .addGroup(hexcalcPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(hexcalcPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                .addComponent(decimal_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(reverse_complement_decimal_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(reverse_decimal_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(efc_decimal_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(from_efc_decimal_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(efc5_decimal_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(from_efc5_decimal_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(jLabel6))
                        .addGap(12, 12, 12)
                        .addGroup(hexcalcPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(complement_hex_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(reverse_hex_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(reverse_complement_hex_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(efc_hex_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(from_efc_hex_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(efc5_hex_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(from_efc5_hex_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(hex_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel7))
                        .addGap(29, 29, 29)))
                .addComponent(jSeparator6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(31, 31, 31)
                .addGroup(hexcalcPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(time_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(hexcalcPanelLayout.createSequentialGroup()
                        .addGroup(hexcalcPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(frequency_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel22)
                            .addComponent(jLabel24)
                            .addComponent(no_periods_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel23, javax.swing.GroupLayout.Alignment.TRAILING))
                        .addGap(18, 18, 18)
                        .addGroup(hexcalcPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(no_periods_hex_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel25)
                            .addComponent(prontocode_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel39)
                            .addGroup(hexcalcPanelLayout.createSequentialGroup()
                                .addGap(38, 38, 38)
                                .addComponent(period_selection_enable_CheckBox)))))
                .addContainerGap())
        );
        hexcalcPanelLayout.setVerticalGroup(
            hexcalcPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(hexcalcPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(hexcalcPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel22)
                    .addComponent(jLabel25)
                    .addComponent(jLabel6)
                    .addComponent(jLabel7))
                .addGroup(hexcalcPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel9)
                    .addComponent(frequency_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(prontocode_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(decimal_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(hex_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(hexcalcPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(hexcalcPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(complement_decimal_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(complement_hex_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel8))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, hexcalcPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel23)
                        .addComponent(jLabel39)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(hexcalcPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(hexcalcPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(reverse_decimal_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(reverse_hex_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel14))
                    .addComponent(no_periods_hex_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(no_periods_TextField, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGroup(hexcalcPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(hexcalcPanelLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(hexcalcPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(hexcalcPanelLayout.createSequentialGroup()
                                .addGroup(hexcalcPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(reverse_complement_decimal_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(reverse_complement_hex_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel15))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(hexcalcPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(efc_decimal_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(efc_hex_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel40))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(hexcalcPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(from_efc_decimal_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(from_efc_hex_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel41))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(hexcalcPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(efc5_decimal_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(efc5_hex_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel42))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(hexcalcPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(from_efc5_decimal_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(from_efc5_hex_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel43)))
                            .addGroup(hexcalcPanelLayout.createSequentialGroup()
                                .addGap(12, 12, 12)
                                .addComponent(jLabel24)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(time_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, hexcalcPanelLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(period_selection_enable_CheckBox)))
                .addContainerGap())
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, hexcalcPanelLayout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(jSeparator6, javax.swing.GroupLayout.PREFERRED_SIZE, 362, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(66, 66, 66))
        );

        mainTabbedPane.addTab("IRcalc", null, hexcalcPanel, "This pane consists of an interactive calculator for common computations on IR signals.");
        hexcalcPanel.getAccessibleContext().setAccessibleName("IRCalc");

        optionsPanel.setToolTipText("This pane sets some program options.");

        jLabel16.setText("IRP Protocols");

        IrpProtocolsTextField.setText(Props.get_instance().get_irpmaster_configfile());
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

        makehexIrpDirTextField.setText(Props.get_instance().get_makehex_irpdir());
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
                    .addGroup(optionsPanelLayout.createSequentialGroup()
                        .addGroup(optionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel1)
                            .addComponent(jLabel16)
                            .addComponent(jLabel11))
                        .addGap(18, 18, 18)
                        .addGroup(optionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(optionsPanelLayout.createSequentialGroup()
                                .addComponent(IrpProtocolsTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(irpProtocolsSelectButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(IrpProtocolsBrowseButton))
                            .addComponent(debug_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(optionsPanelLayout.createSequentialGroup()
                                .addGroup(optionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(makehexIrpDirTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(lafComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 108, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(optionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(optionsPanelLayout.createSequentialGroup()
                                        .addComponent(macro_select_Button)
                                        .addGap(12, 12, 12)
                                        .addComponent(makehexIrpDirBrowseButton))
                                    .addComponent(jLabel26)))))
                    .addComponent(verbose_CheckBox)
                    .addComponent(disregard_repeat_mins_CheckBox))
                .addContainerGap(83, Short.MAX_VALUE))
        );

        optionsPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {IrpProtocolsBrowseButton, makehexIrpDirBrowseButton});

        optionsPanelLayout.setVerticalGroup(
            optionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, optionsPanelLayout.createSequentialGroup()
                .addGap(50, 50, 50)
                .addGroup(optionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel16)
                    .addComponent(IrpProtocolsTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(irpProtocolsSelectButton)
                    .addComponent(IrpProtocolsBrowseButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(optionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(makehexIrpDirTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(makehexIrpDirBrowseButton)
                    .addComponent(macro_select_Button))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(optionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(debug_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel11))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(optionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(verbose_CheckBox)
                    .addComponent(lafComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel26))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(disregard_repeat_mins_CheckBox)
                .addGap(233, 233, 233))
        );

        mainTabbedPane.addTab("Options", null, optionsPanel, "This tab allows the change of options for the program.");

        console_TextArea.setColumns(20);
        console_TextArea.setEditable(false);
        console_TextArea.setLineWrap(true);
        console_TextArea.setRows(5);
        console_TextArea.setToolTipText("This is the console, where errors and messages go. Press right mouse button for menu.");
        console_TextArea.setWrapStyleWord(true);
        console_TextArea.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                console_TextAreaMousePressed(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                console_TextAreaMouseReleased(evt);
            }
        });
        jScrollPane1.setViewportView(console_TextArea);

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

        verbose_CheckBoxMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_V, java.awt.event.InputEvent.CTRL_MASK));
        verbose_CheckBoxMenuItem.setMnemonic('v');
        verbose_CheckBoxMenuItem.setText("Verbose");
        verbose_CheckBoxMenuItem.setToolTipText("Report actual command sent to devices");
        verbose_CheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                verbose_CheckBoxMenuItemActionPerformed(evt);
            }
        });
        jMenu1.add(verbose_CheckBoxMenuItem);

        disregard_repeat_mins_CheckBoxMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_D, 0));
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
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(mainTabbedPane, javax.swing.GroupLayout.PREFERRED_SIZE, 655, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(jScrollPane1))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(mainTabbedPane, javax.swing.GroupLayout.PREFERRED_SIZE, 395, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 166, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(51, 51, 51))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private class globalcache_thread extends Thread {
        private IrSignal code;
        private int module;
        private int connector;
        private int count;
        private JButton start_button;
        private JButton stop_button;

        public globalcache_thread(IrSignal code, int module, int connector, int count,
                JButton start_button, JButton stop_button) {
            super("globalcache_thread");
            this.code = code;
            this.module = module;
            this.connector = connector;
            this.count = count;
            this.start_button = start_button;
            this.stop_button = stop_button;
        }

        @Override
        public void run() {
            start_button.setEnabled(false);
            stop_button.setEnabled(true);
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


            start_button.setEnabled(true);
            stop_button.setEnabled(false);
            the_globalcache_protocol_thread = null;
        }
    }

    private class irtrans_thread extends Thread {
        private IrSignal code;
        private irtrans.led_t led;
        private int count;
        private JButton start_button;
        private JButton stop_button;

        public irtrans_thread(IrSignal code, irtrans.led_t led, int count,
                JButton start_button, JButton stop_button) {
            super("irtrans_thread");
            this.code = code;
            this.led = led;
            this.count = count;
            this.start_button = start_button;
            this.stop_button = stop_button;
        }

        @Override
        public void run() {
            start_button.setEnabled(false);
            stop_button.setEnabled(true);
            boolean success = false;
            try {
                success = irt.send_ir(code, led, count);
            } catch (IncompatibleArgumentException ex) {
                System.err.println(ex.getMessage());
            } catch (UnknownHostException ex) {
                System.err.println("IRTrans hostname not found.");
            } catch (IOException e) {
                System.err.println(e);
            //} catch (InterruptedException e) {
            //    System.err.println("*** Interrupted *** ");
            //    success = true;
            }

            if (!success)
                System.err.println("** Failed **");

            the_irtrans_thread = null;
            start_button.setEnabled(true);
            stop_button.setEnabled(false);
        }
    }

    private void do_exit() {
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));
        System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.err)));
        Props.get_instance().set_bounds(getBounds());
        Props.get_instance().set_hardwareIndex(Integer.toString(hardwareIndex));
        System.out.println("Exiting...");
        System.exit(0);
    }

    private void exitMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitMenuItemActionPerformed
        do_exit();
    }//GEN-LAST:event_exitMenuItemActionPerformed

    private void saveMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveMenuItemActionPerformed
        try {
            String result = Props.get_instance().save();
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
        Props.browse(Props.get_instance().get_helpfileUrl(), verbose);
}//GEN-LAST:event_contentMenuItemActionPerformed

    private void saveAsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveAsMenuItemActionPerformed
        try {
            String props = select_file("Select properties save", "xml", "XML Files", true, null).getAbsolutePath();
            Props.get_instance().save(props);
            System.err.println("Property file written to " + props + ".");
        } catch (IOException e) {
            System.err.println(e);
        } catch (NullPointerException e) {
        }
    }//GEN-LAST:event_saveAsMenuItemActionPerformed

    private void update_verbosity() {
        UserPrefs.get_instance().set_verbose(verbose);
        gc.set_verbosity(verbose);
        irt.set_verbosity(verbose);
        verbose_CheckBoxMenuItem.setSelected(verbose);
        verbose_CheckBox.setSelected(verbose);
    }

    private void verbose_CheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_verbose_CheckBoxMenuItemActionPerformed
        verbose = verbose_CheckBoxMenuItem.isSelected();
        update_verbosity();
    }//GEN-LAST:event_verbose_CheckBoxMenuItemActionPerformed


    private void copy_console_to_clipboard_MenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_copy_console_to_clipboard_MenuItemActionPerformed
        (new copy_clipboard_text()).to_clipboard(console_TextArea.getText());
    }//GEN-LAST:event_copy_console_to_clipboard_MenuItemActionPerformed

    private void clear_console_MenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clear_console_MenuItemActionPerformed
        console_TextArea.setText(null);
    }//GEN-LAST:event_clear_console_MenuItemActionPerformed

    private File getMakehexIrpFile() {
        String protocol_name = (String) protocol_ComboBox.getModel().getSelectedItem();
        return new File(Props.get_instance().get_makehex_irpdir(), protocol_name + "." + IrpFileExtension);
    }

    private String renderMakehexCode(int F_override) {
        Makehex makehex = new Makehex(getMakehexIrpFile());
        toggletype toggle = toggletype.decode_toggle((String) toggle_ComboBox.getModel().getSelectedItem());
        int tog = toggletype.toInt(toggle);
        int devno = deviceno_TextField.getText().trim().isEmpty() ? invalid_parameter : harcutils.parse_intnumber(deviceno_TextField.getText());
        int sub_devno = subdevice_TextField.getText().trim().isEmpty() ? invalid_parameter : harcutils.parse_intnumber(subdevice_TextField.getText());
        int cmd_no = F_override >= 0 ? F_override : harcutils.parse_intnumber(commandno_TextField.getText());

        return makehex.prontoString(devno, sub_devno, cmd_no, tog);
    }

    private void renderProtocolDocu() {
        if (this.irpmasterRenderer()) {
            String protocol_name = (String) protocol_ComboBox.getModel().getSelectedItem();
            protocol_raw_TextArea.setText(irpMaster.getDocumentation(protocol_name));
        } else
            System.err.println("Internal error.");
    }

    private IrSignal extract_code() throws NumberFormatException, IrpMasterException, RecognitionException {
        return extract_code(invalid_parameter);
    }

    private IrSignal extract_code(int F_override) throws NumberFormatException, IrpMasterException, RecognitionException {
        if (makehexRenderer()) {
            return Pronto.ccfSignal(renderMakehexCode(F_override));
        } else {
            String protocol_name = (String) protocol_ComboBox.getModel().getSelectedItem();
            long devno = deviceno_TextField.getText().trim().isEmpty() ? invalid_parameter : harcutils.parse_longnumber(deviceno_TextField.getText());
            long sub_devno = invalid_parameter;
            Protocol protocol = get_protocol(protocol_name);
            if (protocol.hasParameter("S") && !(protocol.hasParameterDefault("S") && subdevice_TextField.getText().trim().equals("")))
                sub_devno = harcutils.parse_longnumber(subdevice_TextField.getText());
            long cmd_no = F_override >= 0 ? (long) F_override : harcutils.parse_longnumber(commandno_TextField.getText());
            String tog = (String) toggle_ComboBox.getModel().getSelectedItem();
            toggletype toggle = toggletype.decode_toggle((String) toggle_ComboBox.getModel().getSelectedItem());
            String add_params = protocol_params_TextField.getText();
            //System.err.println(protocol_name + devno + " " + sub_devno + " " + cmd_no + toggle);

            if (protocol == null)
                return null;

            HashMap<String, Long> params = //parameters(deviceno, subdevice, cmdno, toggle, extra_params);
                    new HashMap<String, Long>();
            if (devno != invalid_parameter)
                params.put("D", devno);
            if (sub_devno != invalid_parameter)
                params.put("S", sub_devno);
            if (cmd_no != invalid_parameter)
                params.put("F", cmd_no);
            if (toggle != toggletype.dont_care)
                params.put("T", (long) toggletype.toInt(toggle));
            if (add_params != null && !add_params.trim().isEmpty()) {
                String[] str = add_params.trim().split("[ \t]+");
                for (String s : str) {
                    String[] q = s.split("=");
                    if (q.length == 2)
                        params.put(q[0], IrpUtils.parseLong(q[1]));
                }
            }
            IrSignal irSignal = protocol.renderIrSignal(params, !Props.get_instance().get_disregard_repeat_mins());
            return irSignal;//protocol.encode(protocol_name, devno, sub_devno, cmd_no, toggle, add_params, false);
        }
    }

    private void exportIrSignal(PrintStream printStream, Protocol protocol, HashMap<String, Long> params,
            boolean doXML, boolean doRaw, boolean doPronto, LircExport lircExport)
            throws IrpMasterException {
        IrSignal irSignal = protocol.renderIrSignal(params, !Props.get_instance().get_disregard_repeat_mins());
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

    private void export() throws NumberFormatException, IrpMasterException, RecognitionException, FileNotFoundException {
        String format = (String) exportFormatComboBox.getSelectedItem();
        boolean doXML = format.equalsIgnoreCase("XML");
        boolean doText = format.equalsIgnoreCase("text");
        boolean doTonto = format.equalsIgnoreCase("Pronto CCF");
        boolean doLirc = format.equalsIgnoreCase("lirc");
        boolean doWave = format.equalsIgnoreCase("wave");
        boolean doRaw = this.exportRawCheckBox.isSelected();
        boolean doPronto = this.exportProntoCheckBox.isSelected();
        String protocolName = (String) protocol_ComboBox.getModel().getSelectedItem();
        long devno = deviceno_TextField.getText().trim().isEmpty() ? invalid_parameter : harcutils.parse_longnumber(deviceno_TextField.getText());
        long sub_devno = invalid_parameter;
        if (!subdevice_TextField.getText().trim().equals(""))
            sub_devno = harcutils.parse_longnumber(subdevice_TextField.getText());
        long cmd_no_lower = harcutils.parse_longnumber(commandno_TextField.getText());
        long cmd_no_upper = (doWave || lastFTextField.getText().isEmpty()) ? cmd_no_lower : harcutils.parse_longnumber(lastFTextField.getText());
        toggletype toggle = toggletype.decode_toggle((String) toggle_ComboBox.getModel().getSelectedItem());
        String add_params = protocol_params_TextField.getText();
        String extension = doXML ? "xml"
                : doText  ? "txt"
                : doTonto ? "ccf"
                : doLirc  ? "lirc"
                : doWave  ? "wav" : "txt";
        String formatDescription = "Export files"; // FIXME

        if (automaticFileNamesCheckBox.isSelected()) {
            File exp = new File(Props.get_instance().get_exportdir());
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

        File file = automaticFileNamesCheckBox.isSelected()
                ? harcutils.create_export_file(Props.get_instance().get_exportdir(),
                protocolName + "_" + devno + (sub_devno != invalid_parameter ? ("_" + sub_devno) : ""),
                extension)
                : select_file("Select export file", extension, formatDescription, true, Props.get_instance().get_exportdir());

        if (file == null)
            return;

        // TODO: this code sucks.
        if (irpmasterRenderer()) {
            Protocol protocol = irpMaster.newProtocol(protocolName);
            HashMap<String, Long> params = Protocol.parseParams((int) devno, (int) sub_devno,
                    (int) cmd_no_lower, toggletype.toInt(toggle), add_params);
            if (doWave) {
                int repetitions = Integer.parseInt((String) exportRepetitionsComboBox.getSelectedItem());
                toggletype tt = toggletype.decode_toggle((String)this.toggle_ComboBox.getSelectedItem());
                if (tt != toggletype.dont_care)
                    params.put("T", (long) toggletype.toInt(tt));
                IrSignal irSignal = protocol.renderIrSignal(params, !Props.get_instance().get_disregard_repeat_mins());
                wav_export.export(irSignal, true, repetitions, file);
                System.err.println("Exporting to " + file);
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
                        toggletype tt = toggletype.decode_toggle((String)this.toggle_ComboBox.getSelectedItem());
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
                Makehex makehex = new Makehex(new File(Props.get_instance().get_makehex_irpdir(), protocol_name + "." + IrpFileExtension));
                for (int cmd_no = (int) cmd_no_lower; cmd_no <= cmd_no_upper; cmd_no++) {
                    String ccf = makehex.prontoString((int)devno, (int)sub_devno, (int)cmd_no, toggletype.toInt(toggle));
                    printStream.println("Device Code: " + devno + (sub_devno != invalid_parameter ? ("." + sub_devno) : "") + ", Function: " + cmd_no);
                    printStream.println(ccf);
                }
                printStream.close();
            }
        }
        
        lastExportFile = file.getAbsoluteFile();
        viewExportButton.setEnabled(true);
    }

    private void update_protocol_parameters() {
        String current_protocol = (String) protocol_ComboBox.getSelectedItem();
        boolean initialize = ! Props.get_instance().get_protocol().equalsIgnoreCase(current_protocol);
        Props.get_instance().set_protocol(current_protocol.toLowerCase());
        if (irpmasterRenderer()) {
            if (irpMaster == null)
                return;
            try {
                Protocol protocol = get_protocol((String) protocol_ComboBox.getModel().getSelectedItem());
                deviceno_TextField.setEnabled(protocol.hasParameter("D"));
                subdevice_TextField.setEnabled(protocol.hasParameter("S"));
                commandno_TextField.setEnabled(protocol.hasParameter("F"));
                toggle_ComboBox.setEnabled(protocol.hasParameter("T"));
                if (initialize) {
                    deviceno_TextField.setText(null);
                    commandno_TextField.setText(null);
                    this.subdevice_TextField.setText(null);
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
            String filename = select_file("Save console text as...", "txt", "Text file", true, null).getAbsolutePath();
            PrintStream ps = new PrintStream(new FileOutputStream(filename));
            ps.println(console_TextArea.getText());
        } catch (FileNotFoundException ex) {
            System.err.println(ex);
        } catch (NullPointerException e) {
        }
    }//GEN-LAST:event_consoletext_save_MenuItemActionPerformed

    private void verbose_CheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_verbose_CheckBoxActionPerformed
        verbose = this.verbose_CheckBox.isSelected();
	update_verbosity();
    }//GEN-LAST:event_verbose_CheckBoxActionPerformed

    private void exportdir_browse_ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportdir_browse_ButtonActionPerformed

        try {
	    String dir = select_file("Select export directory", null, "Directories", false, ((new File(Props.get_instance().get_exportdir())).getAbsoluteFile().getParent())).getAbsolutePath();
	    Props.get_instance().set_exportdir(dir);
	    exportdir_TextField.setText(dir);
	} catch (NullPointerException e) {
	}
    }//GEN-LAST:event_exportdir_browse_ButtonActionPerformed

    private void exportdir_TextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_exportdir_TextFieldFocusLost
        Props.get_instance().set_exportdir(exportdir_TextField.getText());
     }//GEN-LAST:event_exportdir_TextFieldFocusLost

    private void exportdir_TextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportdir_TextFieldActionPerformed
        Props.get_instance().set_exportdir(exportdir_TextField.getText());
     }//GEN-LAST:event_exportdir_TextFieldActionPerformed

    private void makehexIrpDirTextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_makehexIrpDirTextFieldFocusLost
        Props.get_instance().set_makehex_irpdir(makehexIrpDirTextField.getText());
    }//GEN-LAST:event_makehexIrpDirTextFieldFocusLost

    private void IrpProtocolsTextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_IrpProtocolsTextFieldFocusLost
        Props.get_instance().set_irpmaster_configfile(IrpProtocolsTextField.getText());
     }//GEN-LAST:event_IrpProtocolsTextFieldFocusLost

    private void IrpProtocolsTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_IrpProtocolsTextFieldActionPerformed
        Props.get_instance().set_irpmaster_configfile(IrpProtocolsTextField.getText());
     }//GEN-LAST:event_IrpProtocolsTextFieldActionPerformed

    private void select_period_time(boolean selectPeriod, boolean useHex) {
        no_periods_TextField.setEditable(selectPeriod && ! useHex);
        no_periods_hex_TextField.setEditable(selectPeriod && useHex);
        time_TextField.setEditable(!selectPeriod);
        //period_selection_enable_CheckBox.setSelected(selectPeriod);
        //time_selection_enable_CheckBox.setSelected(!selectPeriod);
    }

    private void prontocode_TextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_prontocode_TextFieldFocusLost
        update_from_frequencycode();
     }//GEN-LAST:event_prontocode_TextFieldFocusLost

    private void prontocode_TextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_prontocode_TextFieldActionPerformed
        update_from_frequencycode();
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
        update_from_frequency();
     }//GEN-LAST:event_frequency_TextFieldFocusLost

    private void frequency_TextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_frequency_TextFieldActionPerformed
        update_from_frequency();
     }//GEN-LAST:event_frequency_TextFieldActionPerformed

    private void no_periods_TextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_no_periods_TextFieldFocusLost
        no_periods_TextFieldActionPerformed(null);
     }//GEN-LAST:event_no_periods_TextFieldFocusLost

    private void no_periods_TextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_no_periods_TextFieldActionPerformed
        double no_periods = Double.parseDouble(no_periods_TextField.getText());
        no_periods_hex_TextField.setText(String.format("%04X", Math.round(no_periods)));
        int freq = Integer.parseInt(frequency_TextField.getText());
        time_TextField.setText(Integer.toString((int) (1000000 * ((double) no_periods) / (double) freq)));
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
             update_hexcalc(in, no_bytes);
         } catch (NumberFormatException e) {
             decimal_TextField.setText("*");
             hexcalc_silly_number(e);
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
             update_hexcalc(in, no_bytes);
         } catch (NumberFormatException e) {
             hex_TextField.setText("*");
             hexcalc_silly_number(e);
         }
     }//GEN-LAST:event_decimal_TextFieldActionPerformed

    private void lircPortTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_lircPortTextFieldActionPerformed
        Props.get_instance().set_lircPort(lircPortTextField.getText());
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
            Props.get_instance().set_lircIpName(lircIp);
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
                //lircTransmitterComboBox.setSelectedIndex(lircTransmitterDefaultIndex);
            } catch (IOException ex) {
                error(ex.getMessage());
            }
	}//GEN-LAST:event_LircIPAddressTextFieldActionPerformed

    private void irtrans_browse_ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_irtrans_browse_ButtonActionPerformed
        Props.browse(URI.create("http://" + irtrans_address_TextField.getText()), verbose);
     }//GEN-LAST:event_irtrans_browse_ButtonActionPerformed

    private void irtrans_address_TextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_irtrans_address_TextFieldActionPerformed
        Props.get_instance().set_irTransIpName(irtrans_address_TextField.getText());
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

    private void gcDiscoveredTypejTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_gcDiscoveredTypejTextFieldActionPerformed

    }//GEN-LAST:event_gcDiscoveredTypejTextFieldActionPerformed

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
                gcDiscoveredTypejTextField.setText(beacon.table.get("-Model"));
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
	    gc.stop_ir(get_gc_module(), get_gc_connector());
	} catch (UnknownHostException ex) {
	    System.err.println(ex.getMessage());
	} catch (IOException ex) {
	    System.err.println(ex.getMessage());
	} catch (InterruptedException ex) {
	    System.err.println(ex.getMessage());
	}
    }//GEN-LAST:event_gc_stop_ir_ActionPerformed

    private void gc_browse_ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_gc_browse_ButtonActionPerformed
        Props.browse(URI.create("http://" + gc_address_TextField.getText()), verbose);
    }//GEN-LAST:event_gc_browse_ButtonActionPerformed

    private void gc_address_TextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_gc_address_TextFieldActionPerformed
        Props.get_instance().set_globalcacheIpName(gc_address_TextField.getText());
        gc = new globalcache(gc_address_TextField.getText(), verbose_CheckBoxMenuItem.getState());
	try {
	    gc_module_ComboBox.setEnabled(false);
	    gc_connector_ComboBox.setEnabled(false);
            //gcDiscoveredTypejTextField.setText("<unknown>");
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
        File file = this.select_file("Select ict file", "ict", "ict Files", false, null);
	if (file != null) {
	    try {
		if (verbose)
		    System.err.println("Imported " + file.getName());
		IrSignal ip = ICT.parse(file);
		protocol_raw_TextArea.setText(ip.ccfString());
		/*//this.protocol_send_Button.setEnabled(true);
		this.protocol_decode_Button.setEnabled(true);
		this.protocol_clear_Button.setEnabled(true);
                //this.protocolCopyButton.setEnabled(true);
                this.protocolAnalyzeButton.setEnabled(true);*/
                enableProtocolButtons(true);
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
	/*/protocol_params_TextField.setText(null);*/
	//protocol_clear_Button.setEnabled(false);
        //protocolPlotButton.setEnabled(false);
        //protocolAnalyzeButton.setEnabled(false);
	//protocol_decode_Button.setEnabled(false);
	//protocol_send_Button.setEnabled(false);
        enableProtocolButtons(false);
    }//GEN-LAST:event_protocol_clear_ButtonActionPerformed

    private void protocol_stop_ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_protocol_stop_ButtonActionPerformed

        try {
	    if (the_globalcache_protocol_thread != null)
		this.the_globalcache_protocol_thread.interrupt();
	    gc.stop_ir(this.get_gc_module(), this.get_gc_connector());
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
	    IrSignal code = extract_code();
	    if (code == null)
		return;
	    protocol_raw_TextArea.setText(code.ccfString());
            enableProtocolButtons(true);
	    //protocol_decode_Button.setEnabled(true);
	    //protocol_clear_Button.setEnabled(true);
            //protocolPlotButton.setEnabled(true);
            //protocolAnalyzeButton.setEnabled(true);
	    //protocol_send_Button.setEnabled(true);
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
        boolean use_globalcache = protocol_outputhw_ComboBox.getSelectedIndex() == 0;
        boolean use_irtrans = protocol_outputhw_ComboBox.getSelectedIndex() == 1;
        boolean use_lirc = protocol_outputhw_ComboBox.getSelectedIndex() == 2;

        String ccf = protocol_raw_TextArea.getText();
        /* If raw code null, take code from the upper row, ignoring text areas*/
        IrSignal code = null;
        try {
            code = (ccf == null || ccf.trim().equals("")) ? extract_code() : Pronto.ccfSignal(ccf);
        } catch (NumberFormatException ex) {
            System.err.println(ex.getMessage());
        } catch (IrpMasterException ex) {
            System.err.println(ex.getMessage());
        } catch (RecognitionException ex) {
            System.err.println(ex.getMessage());
        }
        if (code == null)
            return;
        if (use_globalcache) {
            if (the_globalcache_protocol_thread != null) {
                System.err.println("Warning: the_globalcache_protocol_thread != null, waiting...");
                try {
                    the_globalcache_protocol_thread.join();
                } catch (InterruptedException ex) {
                    System.err.println("***** Interrupted *********");
                }
            }
            the_globalcache_protocol_thread = new globalcache_thread(code, get_gc_module(), get_gc_connector(), count, protocol_send_Button, protocol_stop_Button);
            the_globalcache_protocol_thread.start();
        } else if (use_irtrans) {
            //irt.send_ir(code, get_irtrans_led(), count);
            if (the_irtrans_thread != null) {
                System.err.println("Warning: the_irtrans_thread != null, waiting...");
                try {
                    the_irtrans_thread.join();
                } catch (InterruptedException ex) {
                    System.err.println("***** Interrupted *********");
                }
            }
            the_irtrans_thread = new irtrans_thread(code, get_irtrans_led(), count, protocol_send_Button, protocol_stop_Button);
            the_irtrans_thread.start();

        } else if (use_lirc) {
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
        } else
            System.err.println("This cannot happen, internal error.");
    }//GEN-LAST:event_protocol_send_ButtonActionPerformed

    private void commandno_TextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_commandno_TextFieldFocusLost
        possibly_enable_encode_send();
    }//GEN-LAST:event_commandno_TextFieldFocusLost

    private void commandno_TextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_commandno_TextFieldActionPerformed
        possibly_enable_encode_send();
    }//GEN-LAST:event_commandno_TextFieldActionPerformed

    private void deviceno_TextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_deviceno_TextFieldFocusLost
	possibly_enable_encode_send();
    }//GEN-LAST:event_deviceno_TextFieldFocusLost

    private void deviceno_TextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deviceno_TextFieldActionPerformed
        possibly_enable_encode_send();
    }//GEN-LAST:event_deviceno_TextFieldActionPerformed

    private void protocol_ComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_protocol_ComboBoxActionPerformed
        update_protocol_parameters();
    }//GEN-LAST:event_protocol_ComboBoxActionPerformed

    private void irpProtocolsBrowse(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_irpProtocolsBrowse
        Props.open(Props.get_instance().get_irpmaster_configfile(), verbose);
        System.err.println("If editing the file, changes will not take effect before you save the file AND restart IrMaster!");
    }//GEN-LAST:event_irpProtocolsBrowse

    private void irpProtocolsSelect(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_irpProtocolsSelect
        File f = select_file("Select protocol file for IrpMaster", "ini", "Configuration files", false, null);
        if (f != null) {
            Props.get_instance().set_irpmaster_configfile(f.getAbsolutePath());
            this.IrpProtocolsTextField.setText(f.getAbsolutePath());
        }
    }//GEN-LAST:event_irpProtocolsSelect

    private void makehexIrpDirSelect(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_makehexIrpDirSelect
        File f = select_file("Select direcory containing IRP files for Makehex", null, "Directories", false, null);
        if (f != null) {
            Props.get_instance().set_makehex_irpdir(f.getAbsolutePath());
            this.makehexIrpDirTextField.setText(f.getAbsolutePath());
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
            protocol_ComboBox.setSelectedItem(Props.get_instance().get_protocol());
            exportFormatComboBox.setEnabled(true);
            exportRawCheckBox.setEnabled(true);
            exportProntoCheckBox.setEnabled(true);
        } else {
            // Makehex
            String[] filenames = harcutils.get_basenames(Props.get_instance().get_makehex_irpdir(), IrpFileExtension, false);
            java.util.Arrays.sort(filenames, String.CASE_INSENSITIVE_ORDER);
            protocol_ComboBox.setModel(new DefaultComboBoxModel(filenames));
            String old_protocol = Props.get_instance().get_protocol();
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
        
        IRP_Label.setEnabled(irpmasterRenderer());
        //String old = (String) protocol_Comb7oBox.getSelectedItem();
        //boolean protocol_has_changed = ! Props.get_instance().get_protocol().equalsIgnoreCase((String) protocol_ComboBox.getSelectedItem());
        update_protocol_parameters();
        protocol_params_TextField.setText(null);
        protocol_raw_TextArea.setText(null);
        enableProtocolButtons(false);
        //protocolAnalyzeButton.setEnabled(false);
        //protocolPlotButton.setEnabled(false);
        //protocol_decode_Button.setEnabled(false);
        //protocol_clear_Button.setEnabled(false);
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
        (new copy_clipboard_text()).to_clipboard(this.protocol_raw_TextArea.getText());
    }//GEN-LAST:event_protocolPlotButtonActionPerformed

    private class WarDialerThread extends Thread {
        public WarDialerThread() {

        }

        @Override
        public void run() {
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
            //pauseButton.setEnabled(true);
            int beg = Integer.parseInt(commandno_TextField.getText());
            int end = Integer.parseInt(endFTextField.getText());
            int delay = Math.round((int) (Double.parseDouble(delayTextField.getText()) * 1000));
            int hw_index = war_dialer_outputhw_ComboBox.getSelectedIndex();
            for (int cmd = beg; cmd <= end; cmd++) {
                currentFTextField.setText(Integer.toString(cmd));
                try {
                    IrSignal code = extract_code(cmd);
                    boolean success;
                    switch (hw_index) {
                        case 0:
                            // GlobalCache
                            success = gc.send_ir(code, get_gc_module(), get_gc_connector(), 1);
                            break;
                        case 1:
                            // IrTrans
                            success = irt.send_ir(code, get_irtrans_led());
                            break;
                        case 2:
                            // LIRC
                            success = lircClient.send_ccf(code.ccfString(), 1);
                            break;
                        default:
                            System.err.println("Internal error, sorry.");
                            success = false;
                            break;
                    }
                    if (! success)
                        break;
                    Thread.sleep(delay);
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
        protocol_send_Button.setEnabled(state);
        //protocolPlotButton.setEnabled(state);
    }

    private void enableProtocolButtons() {
        enableProtocolButtons(!protocol_raw_TextArea.getText().isEmpty());
    }

    private void startButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_startButtonActionPerformed
        if (warDialerThread != null)
            System.err.println("Warning: warDialerThread != null");

        warDialerThread = new WarDialerThread();
        warDialerThread.start();
    }//GEN-LAST:event_startButtonActionPerformed

    private void stopButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_stopButtonActionPerformed
        //warDialerStopRequested = true;
        warDialerThread.interrupt();
    }//GEN-LAST:event_stopButtonActionPerformed

    private void pauseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pauseButtonActionPerformed

    }//GEN-LAST:event_pauseButtonActionPerformed

    private void consoleClearMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_consoleClearMenuItemActionPerformed
        console_TextArea.setText(null);
    }//GEN-LAST:event_consoleClearMenuItemActionPerformed

    private void console_TextAreaMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_console_TextAreaMousePressed
        if (evt.isPopupTrigger())
           consolePopupMenu.show(evt.getComponent(), evt.getX(), evt.getY());
    }//GEN-LAST:event_console_TextAreaMousePressed

    private void console_TextAreaMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_console_TextAreaMouseReleased
        console_TextAreaMousePressed(evt);
    }//GEN-LAST:event_console_TextAreaMouseReleased

    private void consoleCopyMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_consoleCopyMenuItemActionPerformed
        (new copy_clipboard_text()).to_clipboard(console_TextArea.getText());
    }//GEN-LAST:event_consoleCopyMenuItemActionPerformed

    private void consoleSaveMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_consoleSaveMenuItemActionPerformed
        consoletext_save_MenuItemActionPerformed(evt);
    }//GEN-LAST:event_consoleSaveMenuItemActionPerformed

    private void consoleCopySelectionMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_consoleCopySelectionMenuItemActionPerformed
        (new copy_clipboard_text()).to_clipboard(console_TextArea.getSelectedText());
    }//GEN-LAST:event_consoleCopySelectionMenuItemActionPerformed

    private void protocol_raw_TextAreaMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_protocol_raw_TextAreaMousePressed
        if (evt.isPopupTrigger())
           CCFCodePopupMenu.show(evt.getComponent(), evt.getX(), evt.getY());
    }//GEN-LAST:event_protocol_raw_TextAreaMousePressed

    private void protocol_raw_TextAreaMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_protocol_raw_TextAreaMouseReleased
        protocol_raw_TextAreaMousePressed(evt);
    }//GEN-LAST:event_protocol_raw_TextAreaMouseReleased

    private void makehexIrpDirBrowseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_makehexIrpDirBrowseButtonActionPerformed
        Props.open(makehexIrpDirTextField.getText(), verbose);
    }//GEN-LAST:event_makehexIrpDirBrowseButtonActionPerformed

    private void makehexIrpDirTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_makehexIrpDirTextFieldActionPerformed
        Props.get_instance().set_makehex_irpdir(makehexIrpDirTextField.getText());
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
        BufferedReader irpStream;
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(getMakehexIrpFile())));
            StringBuffer irp;
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
        (new copy_clipboard_text()).to_clipboard(protocol_raw_TextArea.getText());
    }//GEN-LAST:event_rawCodeCopyMenuItemActionPerformed

    private void rawCodePasteMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rawCodePasteMenuItemActionPerformed
        protocol_raw_TextArea.setText((new copy_clipboard_text()).from_clipboard());
        enableProtocolButtons();
    }//GEN-LAST:event_rawCodePasteMenuItemActionPerformed

    private void rawCodeSaveMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rawCodeSaveMenuItemActionPerformed
        if (protocol_raw_TextArea.getText().isEmpty()) {
            System.err.println("Nothing to save.");
            return;
        }
        File export = select_file("Select file to save", "", null, true, Props.get_instance().get_exportdir());
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
        Props.edit(lastExportFile, verbose);
    }//GEN-LAST:event_viewExportButtonActionPerformed

    private void debug_TextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_debug_TextFieldFocusLost
        debug_TextFieldActionPerformed(null);
    }//GEN-LAST:event_debug_TextFieldFocusLost

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        do_exit();
    }//GEN-LAST:event_formWindowClosing

    private void checkUpdatesMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkUpdatesMenuItemActionPerformed
        BufferedReader in = null;
        try {
            URL url = new URL(IrMasterUtils.currentVersionUrl);
            in = new BufferedReader(new InputStreamReader(url.openStream()));
            String current = in.readLine().trim();
            System.out.println(current.equals(IrMasterUtils.version_string)
                    ? "You are using the latest version of IrMaster, " + IrMasterUtils.version_string
                    : "Current version is " + current + ", your version is " + IrMasterUtils.version_string);
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
        (new copy_clipboard_text()).to_clipboard(jtf.getText());
    }//GEN-LAST:event_copyMenuItemActionPerformed

    private void cpCopyMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cpCopyMenuItemActionPerformed
        copyMenuItemActionPerformed(evt);
    }//GEN-LAST:event_cpCopyMenuItemActionPerformed

    private void pasteMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pasteMenuItemActionPerformed
        JMenuItem jmi = (JMenuItem)evt.getSource();
        JPopupMenu jpm = (JPopupMenu)jmi.getParent();
        JTextField jtf = (JTextField) jpm.getInvoker();
        if (jtf.isEditable()) {
            jtf.setText((new copy_clipboard_text()).from_clipboard());
            jtf.postActionEvent();
        }
    }//GEN-LAST:event_pasteMenuItemActionPerformed

    private void generic_copy_paste_menu(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_generic_copy_paste_menu
        if (evt.isPopupTrigger())
           this.copyPastePopupMenu.show(evt.getComponent(), evt.getX(), evt.getY());
    }//GEN-LAST:event_generic_copy_paste_menu

    private void generic_copy_menu(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_generic_copy_menu
        if (evt.isPopupTrigger())
           copyPopupMenu.show(evt.getComponent(), evt.getX(), evt.getY());
    }//GEN-LAST:event_generic_copy_menu

    private void openExportDirButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openExportDirButtonActionPerformed
        Props.open(Props.get_instance().get_exportdir(), verbose);
    }//GEN-LAST:event_openExportDirButtonActionPerformed

    private void exportFormatComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportFormatComboBoxActionPerformed
        enableExportFormatRelated();
    }//GEN-LAST:event_exportFormatComboBoxActionPerformed

    private void enableExportFormatRelated() {
        String format = (String) exportFormatComboBox.getSelectedItem();
        boolean isWave = format.equalsIgnoreCase("wave");
        boolean isLirc = format.equalsIgnoreCase("lirc");
        exportRawCheckBox.setEnabled(!(isWave || isLirc));
        exportProntoCheckBox.setEnabled(!(isWave || isLirc));
        lastFTextField.setEnabled(!isWave);
        exportGenerateTogglesCheckBox.setEnabled(!isWave);
        this.exportRepetitionsComboBox.setEnabled(isWave);
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
                    (String) this.irtransCommandsComboBox.getModel().getSelectedItem(),
                    get_irtrans_led(),
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
            lircClient.send_ir((String) this.lircRemotesComboBox.getModel().getSelectedItem(),
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
        Props.browse(IrMasterUtils.homepageUrl, verbose);
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
        Props.get_instance().set_disregard_repeat_mins(disregard_repeat_mins_CheckBoxMenuItem.isSelected());
        disregard_repeat_mins_CheckBox.setSelected(disregard_repeat_mins_CheckBoxMenuItem.isSelected());
    }//GEN-LAST:event_disregard_repeat_mins_CheckBoxMenuItemActionPerformed

    private void disregard_repeat_mins_CheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_disregard_repeat_mins_CheckBoxActionPerformed
        Props.get_instance().set_disregard_repeat_mins(this.disregard_repeat_mins_CheckBox.isSelected());
        disregard_repeat_mins_CheckBoxMenuItem.setSelected(disregard_repeat_mins_CheckBox.isSelected());
    }//GEN-LAST:event_disregard_repeat_mins_CheckBoxActionPerformed

    private void read_ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_read_ButtonActionPerformed
        irtrans_address_TextFieldActionPerformed(null);
    }//GEN-LAST:event_read_ButtonActionPerformed

    private void read_lirc_ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_read_lirc_ButtonActionPerformed
        LircIPAddressTextFieldActionPerformed(null);
    }//GEN-LAST:event_read_lirc_ButtonActionPerformed

    private void lafComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_lafComboBoxActionPerformed
        int index = this.lafComboBox.getSelectedIndex();
        try {
            UIManager.setLookAndFeel(lafInfo[index].getClassName());
            Props.get_instance().set_lookAndFeel(index);
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
    }//GEN-LAST:event_lafComboBoxActionPerformed

    private void gc_module_ComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_gc_module_ComboBoxActionPerformed
        Props.get_instance().set_globalcacheModule(Integer.parseInt((String)gc_module_ComboBox.getSelectedItem()));
    }//GEN-LAST:event_gc_module_ComboBoxActionPerformed

    private void gc_connector_ComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_gc_connector_ComboBoxActionPerformed
        Props.get_instance().set_globalcachePort(Integer.parseInt((String)gc_connector_ComboBox.getSelectedItem()));
    }//GEN-LAST:event_gc_connector_ComboBoxActionPerformed

    private void irtrans_led_ComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_irtrans_led_ComboBoxActionPerformed
        Props.get_instance().set_irTransPort(irtrans_led_ComboBox.getSelectedIndex());
    }//GEN-LAST:event_irtrans_led_ComboBoxActionPerformed

    private void no_periods_hex_TextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_no_periods_hex_TextFieldActionPerformed
        int no_periods = Integer.parseInt(no_periods_hex_TextField.getText(), 16);
        no_periods_TextField.setText(String.format("%d", no_periods));
        int freq = Integer.parseInt(frequency_TextField.getText());
        time_TextField.setText(Integer.toString((int) (1000000 * ((double) no_periods) / (double) freq)));
    }//GEN-LAST:event_no_periods_hex_TextFieldActionPerformed

    private void no_periods_hex_TextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_no_periods_hex_TextFieldFocusLost
        no_periods_hex_TextFieldActionPerformed(null);
    }//GEN-LAST:event_no_periods_hex_TextFieldFocusLost

    private void time_TextFieldMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_time_TextFieldMouseEntered
        select_period_time(false, false);
    }//GEN-LAST:event_time_TextFieldMouseEntered

    private void no_periods_TextFieldMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_no_periods_TextFieldMouseEntered
        select_period_time(true, false);
    }//GEN-LAST:event_no_periods_TextFieldMouseEntered

    private void no_periods_hex_TextFieldMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_no_periods_hex_TextFieldMouseEntered
        select_period_time(true, true);
    }//GEN-LAST:event_no_periods_hex_TextFieldMouseEntered

    private void protocol_raw_TextAreaMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_protocol_raw_TextAreaMouseExited
        enableProtocolButtons();
    }//GEN-LAST:event_protocol_raw_TextAreaMouseExited

    private void browseIRPMasterMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browseIRPMasterMenuItemActionPerformed
        Props.browse(Props.get_instance().get_irpmasterUrl(), verbose);
    }//GEN-LAST:event_browseIRPMasterMenuItemActionPerformed

    private void browseJP1WikiActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browseJP1WikiActionPerformed
        Props.browse("http://www.hifi-remote.com/wiki/index.php?title=Main_Page", verbose);
    }//GEN-LAST:event_browseJP1WikiActionPerformed

    private void browseIRPSpecMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browseIRPSpecMenuItemActionPerformed
        Props.browse("http://www.hifi-remote.com/wiki/index.php?title=IRP_Notation", verbose);
    }//GEN-LAST:event_browseIRPSpecMenuItemActionPerformed

    private void browseDecodeIRMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browseDecodeIRMenuItemActionPerformed
        Props.browse("http://www.hifi-remote.com/wiki/index.php?title=DecodeIR", verbose);
    }//GEN-LAST:event_browseDecodeIRMenuItemActionPerformed

    private void update_hexcalc(int in, int no_bytes) {
        int comp = no_bytes == 2 ? 65535 : 255;
        int rev = no_bytes == 2 ? ((Integer.reverse(in) >> 16) & 65535) : ((Integer.reverse(in) >> 24) & 255);
        String hex_format = no_bytes == 2 ? "%04X" : "%02X";
        //int comp_rev = in > 255

        complement_decimal_TextField.setText(Integer.toString(comp - in));
        complement_hex_TextField.setText(String.format(hex_format, comp - in));
        reverse_decimal_TextField.setText(Integer.toString(rev));
        reverse_hex_TextField.setText(String.format(hex_format, rev));
        reverse_complement_hex_TextField.setText(String.format(hex_format, comp-rev));
        reverse_complement_decimal_TextField.setText(Integer.toString(comp-rev));
        efc_decimal_TextField.setText(Integer.toString(EFC.hex2efc(in)));
        efc_hex_TextField.setText(String.format("%02X", EFC.hex2efc(in)));
        efc5_decimal_TextField.setText(Integer.toString(EFC.hex2efc5(in, no_bytes)));
        efc5_hex_TextField.setText(String.format(hex_format, EFC.hex2efc5(in, no_bytes)));
        from_efc_decimal_TextField.setText(Integer.toString(EFC.efc2hex(in)));
        from_efc_hex_TextField.setText(String.format("%02X", EFC.efc2hex(in)));
        from_efc5_decimal_TextField.setText(Integer.toString(EFC.efc52hex(in, no_bytes)));
        from_efc5_hex_TextField.setText(String.format(hex_format, EFC.efc52hex(in, no_bytes)));

        //test_efc_hex();
    }

    private void hexcalc_silly_number(NumberFormatException e) {
        System.err.println("Parse error " + e.getMessage());
        complement_decimal_TextField.setText("****");
        complement_hex_TextField.setText("****");
        reverse_decimal_TextField.setText("****");
        reverse_hex_TextField.setText("****");
    }

    private void update_from_frequency() {
        int freq = Integer.parseInt(frequency_TextField.getText());
        prontocode_TextField.setText(Pronto.formatInteger(Pronto.getProntoCode(freq)));//ir_code.ccf_integer(ir_code.get_frequency_code(freq)));
        update_from_frequency(freq);
    }

    private void update_from_frequencycode() {
        int freq = (int) Pronto.getFrequency(Integer.parseInt(prontocode_TextField.getText(),16));
        frequency_TextField.setText(Integer.toString(freq));
        update_from_frequency(freq);
    }

    private void update_from_frequency(int freq) {
        if (period_selection_enable_CheckBox.isSelected()) {
            double no_periods = Double.parseDouble(no_periods_TextField.getText());
            time_TextField.setText(Integer.toString((int)(1000000.0*no_periods/freq)));
        } else {
            int time = Integer.parseInt(time_TextField.getText());
            no_periods_TextField.setText(String.format("%.1f", (time*freq)/1000000.0));
        }
    }

    private void possibly_enable_encode_send() {
        boolean looks_ok = !commandno_TextField.getText().isEmpty();
        protocol_send_Button.setEnabled(looks_ok || !protocol_raw_TextArea.getText().isEmpty());
        protocol_generate_Button.setEnabled(looks_ok);
        //wav_export_Button.setEnabled(looks_ok);
    }

    private int get_gc_module() {
        return Integer.parseInt((String)gc_module_ComboBox.getSelectedItem());
    }

    private int get_gc_connector() {
        return Integer.parseInt((String) gc_connector_ComboBox.getSelectedItem());
    }

    private irtrans.led_t get_irtrans_led() {
        return irtrans.led_t.parse((String)irtrans_led_ComboBox.getSelectedItem());
    }

    /**
     * Normally not used, instead IrMaster.main is used instead.
     * @param args the command line arguments. Not used.
     */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                new GuiMain(false, 0).setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPopupMenu CCFCodePopupMenu;
    private javax.swing.JLabel DecodeIRVersion;
    private javax.swing.JLabel IRP_Label;
    private javax.swing.JTextField IRP_TextField;
    private javax.swing.JButton IrpProtocolsBrowseButton;
    private javax.swing.JTextField IrpProtocolsTextField;
    private javax.swing.JTextField LircIPAddressTextField;
    private javax.swing.JMenuItem aboutMenuItem;
    private javax.swing.JPanel analyzePanel;
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
    private javax.swing.JTextArea console_TextArea;
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
    private javax.swing.JTextField gcDiscoveredTypejTextField;
    private javax.swing.JTextField gc_address_TextField;
    private javax.swing.JButton gc_browse_Button;
    private javax.swing.JComboBox gc_connector_ComboBox;
    private javax.swing.JComboBox gc_module_ComboBox;
    private javax.swing.JPanel globalcache_Panel;
    private javax.swing.JMenu helpMenu;
    private javax.swing.JTextField hex_TextField;
    private javax.swing.JPanel hexcalcPanel;
    private javax.swing.JButton icf_import_Button;
    private javax.swing.JButton irpProtocolsSelectButton;
    private javax.swing.JComboBox irtransCommandsComboBox;
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
    private javax.swing.JLabel jLabel31;
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
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator10;
    private javax.swing.JSeparator jSeparator11;
    private javax.swing.JSeparator jSeparator12;
    private javax.swing.JPopupMenu.Separator jSeparator13;
    private javax.swing.JSeparator jSeparator14;
    private javax.swing.JPopupMenu.Separator jSeparator15;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JPopupMenu.Separator jSeparator3;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JPopupMenu.Separator jSeparator5;
    private javax.swing.JSeparator jSeparator6;
    private javax.swing.JPopupMenu.Separator jSeparator7;
    private javax.swing.JPopupMenu.Separator jSeparator8;
    private javax.swing.JPopupMenu.Separator jSeparator9;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JComboBox lafComboBox;
    private javax.swing.JTextField lastFTextField;
    private javax.swing.JComboBox lircCommandsComboBox;
    private javax.swing.JPanel lircPanel;
    private javax.swing.JTextField lircPortTextField;
    private javax.swing.JComboBox lircRemotesComboBox;
    private javax.swing.JButton lircSendPredefinedButton;
    private javax.swing.JLabel lircServerVersionLabel;
    private javax.swing.JLabel lircServerVersionText;
    private javax.swing.JButton lircStopIrButton;
    private javax.swing.JComboBox lircTransmitterComboBox;
    private javax.swing.JMenuItem listIrpMenuItem;
    private javax.swing.JButton macro_select_Button;
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
    private javax.swing.JButton openExportDirButton;
    private javax.swing.JPanel optionsPanel;
    private javax.swing.JTabbedPane outputHWTabbedPane;
    private javax.swing.JMenuItem pasteMenuItem;
    private javax.swing.JButton pauseButton;
    private javax.swing.JCheckBox period_selection_enable_CheckBox;
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
    private javax.swing.JMenuItem rawCodeCopyMenuItem;
    private javax.swing.JMenuItem rawCodeImportMenuItem;
    private javax.swing.JMenuItem rawCodePasteMenuItem;
    private javax.swing.JMenuItem rawCodeSaveMenuItem;
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
    private javax.swing.JTextField time_TextField;
    private javax.swing.JComboBox toggle_ComboBox;
    private javax.swing.JCheckBox verbose_CheckBox;
    private javax.swing.JCheckBoxMenuItem verbose_CheckBoxMenuItem;
    private javax.swing.JButton viewExportButton;
    private javax.swing.JPanel warDialerPanel;
    private javax.swing.JComboBox war_dialer_outputhw_ComboBox;
    // End of variables declaration//GEN-END:variables
    private AboutPopup aboutBox;
}
