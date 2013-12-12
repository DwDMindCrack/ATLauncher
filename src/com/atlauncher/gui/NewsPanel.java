/**
 * Copyright 2013 by ATLauncher and Contributors
 *
 * This work is licensed under the Creative Commons Attribution-ShareAlike 3.0
 * Unported License. To view a copy of this license, visit
 * http://creativecommons.org/licenses/by-sa/3.0/.
 */
package com.atlauncher.gui;

import java.awt.BorderLayout;
import java.io.File;
import java.io.IOException;

import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.atlauncher.App;
import com.atlauncher.utils.Utils;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.border.EmptyBorder;
import sun.font.TrueTypeFont;

@SuppressWarnings("serial")
public class NewsPanel extends JPanel {

    private JEditorPane newsArea;

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(600, 400);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(600, 400);
    }

    public NewsPanel() {
        setBorder(new EmptyBorder(0,0,10,10));
        setLayout(new BorderLayout(0,0));
        setBackground(new Color(226, 0, 0));
        loadContent();
        setSize(500, 400);
    }

    private void loadContent() {
        JLabel title = new JLabel();
        title.setText(" News");
        title.setBorder(new EmptyBorder(10,10,10,10));

        try {
            Font customFont = Font.createFont(Font.TRUETYPE_FONT, getClass().getResourceAsStream("/resources/weblysleekuil.ttf"));
            customFont = customFont.deriveFont(24f);
            title.setFont(customFont);
        } catch (FontFormatException | IOException ex) {
            Logger.getLogger(NewsPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
        add(title, BorderLayout.NORTH);

        newsArea = new JEditorPane();
        newsArea.setEditable(false);
        newsArea.setBorder(BorderFactory.createEmptyBorder());
        newsArea.setSelectionColor(new Color(255, 255, 255, 150));
        newsArea.setOpaque(false);
        newsArea.setBackground(new Color(0, 0, 0, 0));

        HTMLEditorKit kit = new HTMLEditorKit();
        StyleSheet styleSheet = kit.getStyleSheet();
        styleSheet.addRule("A {color:#0088CC}");
        styleSheet.addRule("#newsHeader {font-weight:bold;font-size:14px;color:#339933;}");
        styleSheet.addRule("#newsBody {font-size:10px;padding-left:20px;}");
        newsArea.setEditorKit(kit);

        newsArea.addHyperlinkListener(new HyperlinkListener() {
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    Utils.openBrowser(e.getURL());
                }
            }
        });
        JScrollPane scrollPane = new JScrollPane(newsArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollPane, BorderLayout.CENTER);

        scrollPane.getViewport().setBackground(new Color(0, 0, 0, 0));
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setViewportBorder(BorderFactory.createEmptyBorder());
        scrollPane.setOpaque(false);
        scrollPane.setBackground(new Color(0, 0, 0, 0));
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        String news = "<html>";
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new File(App.settings.getConfigsDir(), "news.xml"));
            document.getDocumentElement().normalize();
            NodeList nodeList = document.getElementsByTagName("article");
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;
                    if (i == nodeList.getLength() - 1) {
                        news += "<p id=\"newsHeader\">"
                                + element.getAttribute("posted")
                                + " - <a href=\""
                                + element.getAttribute("link")
                                + "\">"
                                + element.getAttribute("title")
                                + "</a> ("
                                + element.getAttribute("comments")
                                + " "
                                + (Integer.parseInt(element.getAttribute("comments")) == 1 ? "comment"
                                : "comments") + ")</p>" + "<p id=\"newsBody\">"
                                + element.getTextContent() + "</p><br/>";
                    } else {
                        news += "<p id=\"newsHeader\">"
                                + element.getAttribute("posted")
                                + " - <a href=\""
                                + element.getAttribute("link")
                                + "\">"
                                + element.getAttribute("title")
                                + "</a> ("
                                + element.getAttribute("comments")
                                + " "
                                + (Integer.parseInt(element.getAttribute("comments")) == 1 ? "comment"
                                : "comments") + ")</p>" + "<p id=\"newsBody\">"
                                + element.getTextContent() + "</p><br/><hr/>";
                    }
                }
            }
        } catch (SAXException e) {
            App.settings.getConsole().logStackTrace(e);
        } catch (ParserConfigurationException e) {
            App.settings.getConsole().logStackTrace(e);
        } catch (IOException e) {
            App.settings.getConsole().logStackTrace(e);
        }
        newsArea.setText(news + "</html>");
        newsArea.setCaretPosition(0);
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
    }

    @Override
    public boolean isOptimizedDrawingEnabled() {
        return false;
    }

    public void reload() {
        removeAll();
        loadContent();
        validate();
        repaint();
    }
}
