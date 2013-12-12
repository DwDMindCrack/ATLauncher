/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.atlauncher.gui;

import java.awt.Graphics;
import java.awt.Image;
import javax.swing.JComponent;

/**
 *
 * @author Dan
 */
public class BackgroundImage extends JComponent {

    private Image image;

    public BackgroundImage(Image image) {
        this.image = image;
    }

    @Override
    protected void paintComponent(Graphics g) {
        g.drawImage(image, 0, 0, null);
    }
}
