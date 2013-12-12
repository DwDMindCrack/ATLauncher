/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.atlauncher.gui;

import com.atlauncher.App;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;

/**
 *
 * @author Dan
 */
public class NavTitle extends JComponent {

    private Color colour;

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(200, 50);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(200, 50);
    }

    public NavTitle(Color colour) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        this.colour = colour;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setColor(colour);
        g2.fillRect(40, 0, 150, 50);
        g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new Color(255, 255, 255));

        FontMetrics fm = getFontMetrics(getFont());
        int width = fm.stringWidth("DwD Launcher");
        int height = fm.getHeight();

        g2.drawString("DwD Launcher", getWidth()-(width+30), 2+height);
        
        
        
        g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
        fm = getFontMetrics(getFont());
        width = fm.stringWidth("Version "+App.settings.getVersion().toString());
        height = height+fm.getHeight();

        g2.drawString("Version "+App.settings.getVersion().toString(), getWidth()-width, 2+height);

    }
}
