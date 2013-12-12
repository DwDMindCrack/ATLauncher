/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.atlauncher.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/**
 *
 * @author Dan
 */
public class JTabPanel extends JPanel {
    
    @Override
    public Dimension getMinimumSize() {
        return new Dimension(600, 400);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(600, 400);
    }
    
    public JTabPanel() {
        FlowLayout layoutManager = new FlowLayout(FlowLayout.LEFT);
        layoutManager.setHgap(0);
        layoutManager.setVgap(10);
        setLayout(layoutManager);
        setOpaque(false);
    }
    
}
