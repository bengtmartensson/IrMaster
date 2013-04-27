/*
 Copyright (C) 2013 Bengt Martensson.

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

import java.awt.Component;
import java.io.File;
import java.util.HashMap;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * This class packs a number of file selectors.
 *
 */
public class SelectFile {

    private static HashMap<String, String> filechooserdirs = new HashMap<String, String>();

    /**
     * Version of the file selector with only one file extension.
     *
     * @param parent
     * @param title
     * @param save
     * @param defaultdir
     * @param showHiddenFiles 
     * @param extension
     * @param fileTypeDesc
     * @return Selected File, or null.
     */
    public static File selectFile(Component parent, String title, boolean save, String defaultdir, boolean showHiddenFiles, String extension, String fileTypeDesc) {
        return selectFile(parent, title, save, defaultdir, showHiddenFiles, new String[]{extension, fileTypeDesc});
    }

    /**
     * Packs a file selector. The finally selected direcory will be remembered, and used as initial direcory for subsequent invocations with the same title.
     *
     * @param parent Component, to which the popup will be positioned.
     * @param title Title of the popup. Also identifies the file selector.
     * @param save True iff the file is to be written.
     * @param defaultdir Default direcory if not stored in the class' static memory.
     * @param showHiddenFiles If true show also "hidden files".
     * @param filetypes Variable number of file extensions, as pair of strings.
     * @return Selected File, or null.
     */
    public static File selectFile(Component parent, String title, boolean save, String defaultdir, boolean showHiddenFiles, String[]... filetypes) {
        String startdir = filechooserdirs.containsKey(title) ? filechooserdirs.get(title) : defaultdir;
        JFileChooser chooser = new JFileChooser(startdir);
        chooser.setFileHidingEnabled(!showHiddenFiles);
        chooser.setDialogTitle(title);
        if (filetypes[0][0] == null || filetypes[0][0].isEmpty()) {
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        } else {
            chooser.setFileFilter(new FileNameExtensionFilter(filetypes[0][1], filetypes[0][0]));
            for (int i = 1; i < filetypes.length; i++) {
                chooser.addChoosableFileFilter(new FileNameExtensionFilter(filetypes[i][1], filetypes[i][0]));
            }
        }
        int result = save ? chooser.showSaveDialog(parent) : chooser.showOpenDialog(parent);

        if (result == JFileChooser.APPROVE_OPTION) {
            filechooserdirs.put(title, chooser.getSelectedFile().getAbsoluteFile().getParent());
            return chooser.getSelectedFile();
        } else {
            return null;
        }
    }

    private SelectFile() {
    }
}
