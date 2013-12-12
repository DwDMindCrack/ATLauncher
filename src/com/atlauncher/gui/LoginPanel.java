/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.atlauncher.gui;

import com.atlauncher.App;
import com.atlauncher.data.Account;
import com.atlauncher.data.Instance;
import com.atlauncher.data.Pack;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

/**
 *
 * @author Dan
 */
public class LoginPanel extends JPanel {

    private JLabel userSkin;
    private JPanel rightPanel;
    private JPanel bottomPanel;
    private JPanel topPanel;
    private JComboBox<Account> accountsComboBox;
    private JComboBox<Instance> instancesComboBox;
    private JButton playButton;
    private JLabel usernameLabel;
    private JTextField usernameField;
    private JLabel passwordLabel;
    private JPasswordField passwordField;
    private JLabel rememberLabel;
    private JCheckBox rememberField;
    private JPanel buttons;
    private JButton leftButton;
    private JButton rightButton;
    private JMenuItem updateSkin;
    private JPopupMenu contextMenu;
    private Account fillerAccount;
    private final Insets TOP_INSETS = new Insets(0, 0, 20, 0);
    private final Insets BOTTOM_INSETS = new Insets(10, 0, 0, 0);
    private final Insets LABEL_INSETS = new Insets(3, 0, 3, 10);
    private final Insets FIELD_INSETS = new Insets(3, 0, 3, 0);

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(270, 400);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(270, 400);
    }

    public LoginPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(40, 0, 30, 30));
        setOpaque(false);

        bottomPanel = new JPanel();
        bottomPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.insets = TOP_INSETS;
        gbc.anchor = GridBagConstraints.CENTER;

        accountsComboBox = new JComboBox<Account>();
        bottomPanel.add(accountsComboBox);

        instancesComboBox = new JComboBox<Instance>();
        bottomPanel.add(instancesComboBox);
        
        reload();

        playButton = new JButton();
        playButton.setText("Play!");
        playButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final Instance instance = ((Instance) instancesComboBox.getSelectedItem());
                instance.launch();
            }
        });


        bottomPanel.add(playButton);

        contextMenu = new JPopupMenu();

        updateSkin = new JMenuItem(App.settings.getLocalizedString("account.reloadskin"));
        updateSkin.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final Account account = ((Account) accountsComboBox.getSelectedItem());
                account.updateSkin();
                userSkin.setIcon(account.getMinecraftSkin());
            }
        });
        contextMenu.add(updateSkin);

        if (App.settings.getAccount() == null) {
            userSkin = new JLabel(new Account("default").getMinecraftSkin());
        } else {
            userSkin = new JLabel(App.settings.getAccount().getMinecraftSkin());
        }
        userSkin.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (((Account) accountsComboBox.getSelectedItem()) != fillerAccount) {
                    if (e.getButton() == MouseEvent.BUTTON3) {
                        contextMenu.show(userSkin, e.getX(), e.getY());
                    }
                }
            }
        });
        userSkin.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        add(userSkin, BorderLayout.CENTER);

        add(bottomPanel, BorderLayout.SOUTH);

    }

    public void reload() {
        accountsComboBox.removeAllItems();
        for (Account account : App.settings.getAccounts()) {
            accountsComboBox.addItem(account);
        }
        if (App.settings.getAccounts().isEmpty()) {
            accountsComboBox.addItem(new Account(App.settings.getLocalizedString("account.add")));
        }

        accountsComboBox.setSelectedIndex(0);
        accountsComboBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    Account account = (Account) accountsComboBox.getSelectedItem();
                    userSkin.setIcon(account.getMinecraftSkin());
                }
            }
        });

        instancesComboBox.removeAllItems();
        for (Instance instance : App.settings.getInstances()) {
            if (instance.isPlayable()) {
                instancesComboBox.addItem(instance);
            }
        }
        if (instancesComboBox.getItemCount() > 0) {
            instancesComboBox.setSelectedIndex(0);
        }
    }
}
