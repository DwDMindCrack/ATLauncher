/**
 * Copyright 2013 by ATLauncher and Contributors
 *
 * This work is licensed under the Creative Commons Attribution-ShareAlike 3.0
 * Unported License. To view a copy of this license, visit
 * http://creativecommons.org/licenses/by-sa/3.0/.
 */
package com.atlauncher.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import com.atlauncher.App;
import com.atlauncher.utils.Utils;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

@SuppressWarnings("serial")
public class LauncherFrame extends JFrame {

    // Size of initial window
    private final BorderLayout LAYOUT_MANAGER = new BorderLayout(0,0);
    private final Color BASE_COLOR = new Color(40, 45, 50);
    private static JPanel tabbedPane;
    private static JPanel loginPane;
    private static NewsPanel newsPanel;
    private static PacksPanel packsPanel;
    private AddonsPanel addonsPanel;
    private InstancesPanel instancesPanel;
    private static AccountPanel accountPanel;
    private static SettingsPanel settingsPanel;
    private BottomBar bottomBar;
    static Point mouseDownCompCoords;
    private NavPanel navPanel;

    public LauncherFrame(boolean show) {
        App.settings.log("Launcher opening");
        App.settings.setParentFrame(this);
        setSize(new Dimension(900, 500));
        setTitle("DwD Launcher %VERSION% - Powered by ATLauncher");
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setUndecorated(true);
        setIconImage(Utils.getImage("/resources/Icon.png"));
        setLayout(LAYOUT_MANAGER);
        setContentPane(new JLabel(new ImageIcon(getClass().getResource("/resources/Background.png"))));

        setLayout(LAYOUT_MANAGER);

        App.settings.log("Setting up Look & Feel");
        setupLookAndFeel(); // Setup the look and feel for the Launcher
        App.settings.log("Finished Setting up Look & Feel");

        
        setupTabs();
        setupNav();
        setupLogin();

        //setupBottomBar();


        add(navPanel, BorderLayout.NORTH);
        add(tabbedPane,BorderLayout.WEST);
        add(loginPane,BorderLayout.EAST);
        //add(bottomBar,BorderLayout.SOUTH);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent arg0) {
                dispose();
            }
        });

        if (show) {
            App.settings.log("Showing Launcher");
            pack();
            setVisible(true);

            //App.settings.setConsoleVisible(false);
        }
        mouseDownCompCoords = null;

        setupMovable();

        App.settings.addConsoleListener(new ComponentAdapter() {
            @Override
            public void componentHidden(ComponentEvent e) {
                App.settings.log("Hiding console");
                App.settings.setConsoleVisible(false);
            }
        });
    }

    private void setupNav() {
        navPanel = new NavPanel();
    }

    /**
     * Setup the individual tabs used in the Launcher sidebar
     */
    private void setupTabs() {
        // Parent pane
        tabbedPane = new JTabPanel();

        newsPanel = new NewsPanel();
        App.settings.setNewsPanel(newsPanel);
        tabbedPane.add(newsPanel);
        
        packsPanel = new PacksPanel();
        App.settings.setPacksPanel(packsPanel);
        addonsPanel = new AddonsPanel();
        instancesPanel = new InstancesPanel();
        App.settings.setInstancesPanel(instancesPanel);
        accountPanel = new AccountPanel();
        settingsPanel = new SettingsPanel();

        tabbedPane.setOpaque(false);
        //tabbedPane.setBackground(new Color(255,255,255,100));
    }
    
    private void setupLogin() {
        loginPane = new LoginPanel();
        App.settings.setLoginPane((LoginPanel) loginPane);
    }

    /**
     * Setup the bottom bar of the Launcher
     */
    private void setupBottomBar() {
        bottomBar = new BottomBar();
        App.settings.setBottomBar(bottomBar);
    }

    /**
     * Setup the Java Look and Feel to make things look pretty
     */
    private void setupLookAndFeel() {
        try {
            System.out.println(UIManager.getInstalledLookAndFeels());
            for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            App.settings.logStackTrace(e);
        }

        // For some reason Mac OS makes text bigger then it should be
        if (Utils.isMac()) {
            UIManager.getLookAndFeelDefaults().put("defaultFont",
                    new Font("SansSerif", Font.PLAIN, 11));
        }
/*
        UIManager.put("control", BASE_COLOR);
        UIManager.put("text", Color.WHITE);
        UIManager.put("nimbusBase", Color.BLACK);
        UIManager.put("nimbusFocus", BASE_COLOR);
        UIManager.put("nimbusBorder", BASE_COLOR);
        UIManager.put("nimbusLightBackground", BASE_COLOR);
        UIManager.put("info", BASE_COLOR);
        UIManager.put("nimbusSelectionBackground", new Color(100, 100, 200));
        UIManager
                .put("Table.focusCellHighlightBorder", BorderFactory.createEmptyBorder(2, 5, 2, 5));*/
    }
    
    private void setupMovable() {
        addMouseListener(new MouseListener() {
            @Override
            public void mouseReleased(MouseEvent e) {
                mouseDownCompCoords = null;
            }

            @Override
            public void mousePressed(MouseEvent e) {
                mouseDownCompCoords = e.getPoint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseClicked(MouseEvent e) {
            }
        });

        addMouseMotionListener(new MouseMotionListener() {
            @Override
            public void mouseMoved(MouseEvent e) {
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                Point currCoords = e.getLocationOnScreen();
                setLocation(currCoords.x - mouseDownCompCoords.x, currCoords.y - mouseDownCompCoords.y);
            }
        });
    }
    
    public static JPanel getNewsPanel() {
        return newsPanel;
    }
    
    public static JPanel getModsPanel() {
        return packsPanel;
    }
    
    public static JPanel getAccountsPanel() {
        return accountPanel;
    }
    
    public static JPanel getSettingsPanel() {
        return settingsPanel;
    }
    
    public static JPanel getTabPanel() {
        return tabbedPane;
    }
}