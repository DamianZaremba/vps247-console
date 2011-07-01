package com.vps247.admin;

import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;

class VMPanel extends JPanel
{
  public final VM vm;
  public JTabbedPane tabs = new JTabbedPane();
  public JPanel summaryPanel = new JPanel(new GridBagLayout());
  public TitleValuePanel tvName;
  public TitleValuePanel tvStatus;
  public TitleValuePanel tvOS;
  public TitleValuePanel tvMemory;
  public JPanel loadingPanel = new JPanel(new BorderLayout());
  public JPanel thumbnailPanel;
  private Thread t;
  public BufferedImage noConsoleImage;
  public JButton startButton;
  public JButton stopButton;

  public VMPanel(VM vm)
  {
    super(new GridLayout(1, 1));
    VM f_vm = vm;
    this.vm = vm;

    URL imageFile = Manager.class.getResource("no_console.png");
    try {
      this.noConsoleImage = ImageIO.read(imageFile);
    } catch (IOException e) {
      System.out.println("Could not load image file!");
    }

    JLabel loadingLabel = new JLabel("Loading console...");
    loadingLabel.setHorizontalAlignment(0);
    this.loadingPanel.add(loadingLabel, "Center");

    JToolBar tb = new JToolBar(vm.name);
    this.startButton = new JButton("Start" + vm.name, Manager.playIcon);
    this.stopButton = new JButton("Stop " + vm.name, Manager.stopIcon);
    tb.add(this.startButton);
    tb.add(this.stopButton);

    this.stopButton.addActionListener(new ActionListener(f_vm) {
      public void actionPerformed(ActionEvent e) {
        if (JOptionPane.showConfirmDialog(Manager.manager, "Are you sure you want to shut down this VM?", "Shut down VM", 0) == 0) {
          VMPanel.this.startButton.setEnabled(false);

          VMPanel.this.stopButton.setText("Stopping");
          VMPanel.this.t = new Thread(new Runnable(this.val$f_vm) {
            public void run() {
              System.out.println("STOPPING");
              VPS247Api.postPath("/vms/" + this.val$f_vm.id + "/stop.json");
            }
          });
          VMPanel.this.t.start();
        }
      }
    });
    this.startButton.addActionListener(new ActionListener(f_vm) {
      public void actionPerformed(ActionEvent e) {
        VMPanel.this.startButton.setEnabled(false);
        VMPanel.this.stopButton.setEnabled(false);
        VMPanel.this.t = new Thread(new Runnable(this.val$f_vm) {
          public void run() {
            VPS247Api.postPath("/vms/" + this.val$f_vm.id + "/start");
          }
        });
        VMPanel.this.t.start();
      }
    });
    JPanel p = new JPanel(new BorderLayout());

    p.add(this.tabs, "Center");
    add(p);

    System.out.println("STATUS ===== " + vm.status);
  }
}