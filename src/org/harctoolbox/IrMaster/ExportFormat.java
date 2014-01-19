/*
 Copyright (C) 2013, 2014 Bengt Martensson.

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

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 */
public class ExportFormat {

    private final String name;
    private final boolean multiSignalFormat;
    private final String extension;
    private final boolean supportsText;
    private final boolean simpleSequence;
    private Document xslt;

    public String getName() {
        return name;
    }

    public boolean getMultiSignalFormat() {
        return multiSignalFormat;
    }

    public String getExtension() {
        return extension;
    }

    public boolean getSupportsText() {
        return supportsText;
    }

    public boolean getSimpleSequence() {
        return simpleSequence;
    }

    public Document getXslt() {
        return xslt;
    }

    public ExportFormat(String name, boolean multiSignalFormat, String extension, boolean supportsText, boolean simpleSequence) {
        this.name = name;
        this.multiSignalFormat = multiSignalFormat;
        this.extension = extension;
        this.supportsText = supportsText;
        this.simpleSequence = simpleSequence;
        xslt = null;
    }

    private ExportFormat(Element el) {
        this.name = el.getAttribute("name");
        this.multiSignalFormat = Boolean.parseBoolean(el.getAttribute("multiSignal"));
        this.extension = el.getAttribute("extension");
        this.supportsText = false;
        this.simpleSequence = Boolean.parseBoolean(el.getAttribute("simpleSequence"));

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);
        factory.setNamespaceAware(false);
        //Document doc = null;
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            xslt = builder.newDocument();
        } catch (ParserConfigurationException ex) {
        }
        Node stylesheet = el.getElementsByTagName("xsl:stylesheet").item(0);
        xslt.appendChild(xslt.importNode(stylesheet, true));
    }

    public static LinkedHashMap<String, ExportFormat> parseExportFormats(File file) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);
        factory.setNamespaceAware(true);
        factory.setXIncludeAware(true);
        Document doc = null;
        DocumentBuilder builder = factory.newDocumentBuilder();
        doc = builder.parse(file);

        LinkedHashMap<String, ExportFormat> result = new LinkedHashMap<String, ExportFormat>();
        NodeList nl = doc.getElementsByTagName("exportformat");
        for (int i = 0; i < nl.getLength(); i++) {
            ExportFormat ef = new ExportFormat((Element) nl.item(i));
            result.put(ef.getName(), ef);
        }
        return result;
    }

    public static void main(String[] args) {
        try {
            LinkedHashMap<String, ExportFormat> formats = new LinkedHashMap<String, ExportFormat>();
            formats.put("xml", new ExportFormat("xml", true, ".xml", true, false));
            formats.put("text", new ExportFormat("text", true, ".txt", true, false));
            formats.put("lirc", new ExportFormat("lirc", true, ".lirc", false, true));
            formats.put("wave", new ExportFormat("wave", false, ".wav", false, true));
            formats.putAll(parseExportFormats(new File("exportformats.xml")));
            System.err.println();
        } catch (ParserConfigurationException ex) {
            System.err.println(ex.getMessage());
        } catch (SAXException ex) {
            System.err.println(ex.getMessage());
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
        }
    }
}
