/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.atlauncher.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 *
 * @author Dan
 */
public class NavItem extends JComponent {

    private Color colour, activeColour;
    private Color nonactiveColour = new Color(188, 188, 188, 179);
    private String icon;
    private boolean active;
    private JPanel panel;

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(50, 50);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(50, 50);
    }

    public NavItem(Color colour, String icon, JPanel linkedPanel) {
        this.activeColour = colour;
        this.icon = icon;
        this.panel = linkedPanel;
        this.colour = nonactiveColour;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(colour);
        g.fillRect(0, 0, 50, 50);
        if (!this.icon.isEmpty()) {
            try {
                g.drawImage(ImageIO.read(getClass().getResource("/resources/nav" + icon + ".png")), 10, 10, this);
            } catch (Exception e) {
            }
        }
    }

    public void setActive(boolean active) {
        this.active = active;
        if (active) {
            colour = activeColour;
        } else {
            colour = nonactiveColour;
        }
        repaint();
    }
    
    public JPanel getPanel() {
        return panel;
    }
}
