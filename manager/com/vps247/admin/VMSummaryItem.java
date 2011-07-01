package com.vps247.admin;

import com.citrix.xenserver.console.VNCCanvas;
import com.citrix.xenserver.console.XenConsole;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import javax.imageio.ImageIO;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

class VMSummaryItem extends JPanel
{
  public final VM vm;
  public BufferedImage noConsoleImage;
  public JLabel titleLabel;
  public TitleValuePanel tvName;
  public TitleValuePanel tvStatus;
  public TitleValuePanel tvOS;
  public TitleValuePanel tvMemory;
  public JPanel thumbnailPanel;

  public VMSummaryItem(VM vm)
  {
    super(new BorderLayout());
    VM f_vm = vm;
    this.vm = vm;

    URL imageFile = Manager.class.getResource("no_console.png");
    try {
      this.noConsoleImage = ImageIO.read(imageFile);
    } catch (IOException e) {
      System.out.println("Could not load image file!");
    }

    JPanel attGridContainer = new JPanel(new BorderLayout());

    JPanel attGrid = new JPanel(new GridLayout(0, 1));
    this.tvName = new TitleValuePanel("  Name", vm.name);
    attGrid.add(this.tvName);

    this.tvStatus = new TitleValuePanel("  Status", vm.status);
    attGrid.add(this.tvStatus);

    this.tvOS = new TitleValuePanel("  Operating System", vm.os);
    attGrid.add(this.tvOS);

    this.tvMemory = new TitleValuePanel("  Memory", vm.memory + " MB");
    attGrid.add(this.tvMemory);

    Box b = new Box(0);
    JButton manageButton = new JButton("Manage");
    b.add(manageButton);
    manageButton.addActionListener(new ActionListener(f_vm) {
      public void actionPerformed(ActionEvent arg0) {
        Manager.mainTree.setSelectionPath(new TreePath(this.val$f_vm.treeNode.getPath()));
      }
    });
    attGrid.add(b);

    attGridContainer.add(attGrid, "North");

    add(attGridContainer, "Center");

    this.thumbnailPanel = new JPanel(f_vm) {
      public void paintComponent(Graphics g) {
        System.out.println("REPAINTING THUMBNAILPANEL IN VMSUMMARYPANEL.JAVA");
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D)g;
        if ((this.val$f_vm.xenConsole != null) && (this.val$f_vm.xenConsole.canvas_ != null) && (this.val$f_vm.xenConsole.canvas_.image_ != null)) {
          int w = this.val$f_vm.xenConsole.canvas_._streamWidth;
          int h = this.val$f_vm.xenConsole.canvas_._streamHeight;
          double aspect_ratio = h / w / 0.75D;
          int th = (int)(150.0D * aspect_ratio);
          int y = 75 - th / 2;
          System.out.println("Width = " + w + ", Height = " + h + ", Aspect ratio is " + aspect_ratio);

          g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
          g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);

          g2.drawImage(this.val$f_vm.xenConsole.canvas_.image_, 1, y + 1, 199, y + th - 1, 0, 0, w, h, null);
        }
        else {
          g2.setColor(Color.BLACK);
          g2.fillRect(0, 0, 200, 150);
        }
      }

      public Dimension getPreferredSize() {
        return new Dimension(200, 150);
      }
      public Dimension getMaximumSize() {
        return new Dimension(200, 150);
      }
      public Dimension getMinimumSize() {
        return new Dimension(200, 150);
      }
    };
    add(this.thumbnailPanel, "West");

    JPanel separator = new JPanel() {
      public Dimension getPreferredSize() {
        return new Dimension(getParent().getWidth(), 1);
      }
      public Dimension getMaximumSize() {
        return new Dimension(getParent().getWidth(), 1);
      }
      public Dimension getMinimumSize() {
        return new Dimension(getParent().getWidth(), 1);
      }
    };
    separator.setBackground(Color.GRAY);
    add(separator, "South");

    this.titleLabel = new JLabel(vm.name);

    this.titleLabel.setFont(new Font("sans-serif", 1, 13));
    this.titleLabel.setForeground(Color.WHITE);

    JPanel titleBackground = new JPanel(new BorderLayout())
    {
      public Dimension getPreferredSize() {
        return new Dimension(getParent().getWidth(), 25);
      }
      public Dimension getMaximumSize() {
        return new Dimension(getParent().getWidth(), 25);
      }
      public Dimension getMinimumSize() {
        return new Dimension(getParent().getWidth(), 25);
      }
    };
    this.titleLabel.setHorizontalAlignment(2);
    titleBackground.setBackground(Color.GRAY);
    titleBackground.add(this.titleLabel, "West");

    add(titleBackground, "North");
  }

  public Dimension getPreferredSize() {
    return new Dimension(getParent().getWidth(), 176);
  }
}