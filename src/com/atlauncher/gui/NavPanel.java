/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.atlauncher.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 *
 * @author Dan
 */
public class NavPanel extends JPanel {

    private static ArrayList<NavItem> navItems = new ArrayList<>();
    private JPanel currentPanel;

    public NavPanel() {
        FlowLayout layoutManager = new FlowLayout(FlowLayout.LEFT);
        layoutManager.setVgap(0);
        layoutManager.setHgap(6);
        setLayout(layoutManager);
        setSize(new Dimension(900, 50));
        setOpaque(false);

        JComponent title = new NavTitle(new Color(128, 128, 128, 179));
        add(title);

        NavItem newsButton = new NavItem(new Color(226, 0, 0, 179), "News", LauncherFrame.getNewsPanel());
        add(newsButton);
        navItems.add(newsButton);
        newsButton.setActive(true);
        currentPanel = newsButton.getPanel();

        NavItem modsButton = new NavItem(new Color(0, 226, 0, 179), "Mods", LauncherFrame.getModsPanel());
        add(modsButton);
        navItems.add(modsButton);

        NavItem accountButton = new NavItem(new Color(0, 0, 226, 179), "Profile", LauncherFrame.getAccountsPanel());
        add(accountButton);
        navItems.add(accountButton);

        NavItem settingsButton = new NavItem(new Color(226, 226, 0, 179), "Settings", LauncherFrame.getSettingsPanel());
        add(settingsButton);
        navItems.add(settingsButton);



        setupMouseListener();

    }

    private void setupMouseListener() {
        addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                
            }

            @Override
            public void mousePressed(MouseEvent e) {
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                boolean changed = false;
                for (NavItem item : navItems) {
                    if (item.contains(SwingUtilities.convertPoint(e.getComponent(),e.getPoint(),item))) {
                        item.setActive(true);
                        if(!currentPanel.equals(item.getPanel())) {
                            currentPanel = item.getPanel();
                            changed = true;
                        }
                    } else {
                        item.setActive(false);
                    }
                }
                
                if(changed) {
                    JPanel panel = LauncherFrame.getTabPanel();
                    panel.removeAll();
                    panel.add(currentPanel);
                    panel.validate();
                    panel.repaint();
                }
                
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }
        });
    }

    public static void disableAll() {
    }
}
