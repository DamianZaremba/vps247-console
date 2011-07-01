package com.vps247.admin;

import com.citrix.xenserver.console.VNCStream;
import com.citrix.xenserver.console.XenConsole;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintStream;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import org.json.JSONArray;
import org.json.JSONObject;

public class Manager extends JFrame
{
  private static final long serialVersionUID = 1L;
  public VM[] vms;
  public JTabbedPane tabs = new JTabbedPane();
  public boolean running = true;
  public VMSummaryPanel summaryPanel = new VMSummaryPanel();
  public JScrollPane summaryScrollPane;
  public static ImageIcon playIcon;
  public static ImageIcon stopIcon;
  public static ImageIcon consoleIcon;
  public static ImageIcon infoIcon;
  public static ImageIcon vps247Icon;
  public static ImageIcon brickIcon;
  public static ImageIcon brickPlayIcon;
  public static ImageIcon brickStopIcon;
  public static ImageIcon brickWorkingIcon;
  public static JScrollPane treeScrollPane;
  public static JTree mainTree;
  public static DefaultMutableTreeNode vmsNode;
  public static DefaultMutableTreeNode topNode;
  public VM current_vm;
  public static JMenuBar mainMenu;
  public static Manager manager;
  public String[] arguments;

  public static void main(String[] args)
    throws Throwable
  {
    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    manager = new Manager(args);
  }

  public VM find_vm_by_id(int id)
  {
    for (int n = 1; n < this.tabs.getTabCount(); n++) {
      VMPanel vmp = (VMPanel)this.tabs.getComponentAt(n);
      VM vm = vmp.vm;
      if (vm.id == id) {
        return vm;
      }
    }
    return null;
  }

  public Manager(String[] args) {
    manager = this;
    this.arguments = args;
    System.out.println(" ### ARGS: " + args[0]);
    playIcon = new ImageIcon(getClass().getResource("play.png"));
    stopIcon = new ImageIcon(getClass().getResource("stop.png"));
    consoleIcon = new ImageIcon(getClass().getResource("console.png"));
    infoIcon = new ImageIcon(getClass().getResource("information.png"));
    brickIcon = new ImageIcon(getClass().getResource("brick.png"));
    brickPlayIcon = new ImageIcon(getClass().getResource("brick_play.png"));
    brickStopIcon = new ImageIcon(getClass().getResource("brick_stop.png"));
    brickWorkingIcon = new ImageIcon(getClass().getResource("brick_working.png"));
    vps247Icon = new ImageIcon(getClass().getResource("vps247.png"));

    JMenuBar mainMenu = new JMenuBar();

    JMenu fileMenu = new JMenu("File");
    fileMenu.setMnemonic(70);

    JMenuItem fileExitMenuItem = new JMenuItem("Exit");
    fileExitMenuItem.setMnemonic(88);
    fileMenu.add(fileExitMenuItem);

    fileExitMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent arg0) {
        if (JOptionPane.showConfirmDialog(Manager.manager, "Are you sure you want to exit?", "Exit", 0) == 0)
          System.exit(0);
      }
    });
    mainMenu.add(fileMenu);
    setJMenuBar(mainMenu);

    setIconImage(vps247Icon.getImage());

    setDefaultCloseOperation(3);
    setVisible(true);
    setTitle("vps247 management console");
    setSize(800, 600);

    this.tabs.setUI(new BasicTabbedPaneUI() {
      protected int calculateTabAreaHeight(int tabPlacement, int horizRunCount, int maxTabHeight) {
        return 0;
      }
    });
    topNode = new DefaultMutableTreeNode("vps247");

    vmsNode = new DefaultMutableTreeNode("Virtual Machines");

    topNode.add(vmsNode);
    mainTree = new JTree(topNode);

    mainTree.setShowsRootHandles(true);

    mainTree.addTreeSelectionListener(new TreeSelectionListener() {
      public void valueChanged(TreeSelectionEvent e) {
        System.out.println("VALUE_CHANGED");
        DefaultMutableTreeNode node = (DefaultMutableTreeNode)Manager.mainTree.getLastSelectedPathComponent();
        DefaultMutableTreeNode parent = (DefaultMutableTreeNode)node.getParent();
        if ((parent != null) && (parent == Manager.vmsNode)) {
          System.out.println("Got vps247");
          VM vm = (VM)node.getUserObject();
          Manager.this.tabs.setSelectedComponent(vm.vmPanel);
        } else if (node == Manager.vmsNode) {
          Manager.this.tabs.setSelectedIndex(0);
        } else if (parent == null) {
          Manager.this.tabs.setSelectedIndex(0);
        }
      }
    });
    treeScrollPane = new JScrollPane(mainTree);

    Dimension minimumSize = new Dimension(150, 50);
    treeScrollPane.setMinimumSize(minimumSize);
    this.tabs.setMinimumSize(minimumSize);

    this.tabs.addTab("Global summary", new JPanel());

    String s_vms = VPS247Api.getPath("/vms.json");
    System.out.println(s_vms);
    try
    {
      JSONObject o = new JSONObject("{'vms':" + s_vms + "}");
      System.out.println(o.toString(2));

      JSONArray v = o.getJSONArray("vms");
      System.out.println(v.toString(2));
      this.vms = new VM[v.length()];
      for (int n = 0; n < v.length(); n++) {
        JSONObject vo = v.getJSONObject(n).getJSONObject("vm");
        if (vo.getString("id").equals(args[1])) {
          addVm(vo);
        }
      }
      mainTree.expandRow(0);
      mainTree.expandRow(1);
    }
    catch (Exception e)
    {
      System.out.println("Error while parsing JSON response.");
      e.printStackTrace();
    }

    mainTree.setSelectionPath(new TreePath(vmsNode.getPath()));

    add(this.tabs);
    this.tabs.setVisible(true);
    this.tabs.setSelectedIndex(1);
    VM lvm = ((VMPanel)this.tabs.getSelectedComponent()).vm;

    if (lvm.xenConsole == null) {
      System.out.println("STARTING CONSOLE");
      this.tabs.invalidate();
      lvm.loadConsole();
      lvm.vmPanel.tabs.setComponentAt(0, lvm.xenConsole);
    }

    this.current_vm = lvm;
    validate();

    while (this.running)
      try
      {
        Thread.sleep(2000L);
        if ((this.current_vm.xenConsole.stream_ == null) || ((!this.current_vm.xenConsole.stream_._connected) && (!this.current_vm.xenConsole.connecting)))
        {
          System.out.println("*********************** RECONN");
          if (this.current_vm.loadConsole())
          {
            this.current_vm.vmPanel.tabs.setComponentAt(0, this.current_vm.xenConsole);
          }
        }
        s_vms = VPS247Api.getPath("/vms.json?minimal=true");
        try {
          System.out.println(s_vms);
          JSONObject o = new JSONObject("{'vms':" + s_vms + "}");
          JSONArray v = o.getJSONArray("vms");
          for (int n = 0; n < v.length(); n++) {
            JSONObject vo = v.getJSONObject(n).getJSONObject("vm");
            int id = vo.getInt("id");
            System.out.println(id);
            VM vm = find_vm_by_id(id);
            if (vm != null) {
              System.out.println(vm.name);
              System.out.println("Current = " + vm.last_reboot_at + ", Now = " + vo.getString("last_reboot_at"));
              if (!vm.last_reboot_at.equals(vo.getString("last_reboot_at")))
              {
                if (vm.xenConsole != null) {
                  vm.xenConsole.ConnectionClosed();
                  vm.xenConsole.connect();
                  System.out.println("Should reconnect");
                }

              }

              vm.last_reboot_at = vo.getString("last_reboot_at");
              System.out.println("Setting status" + vo.getString("status"));
              vm.setStatus(vo.getString("status"));
            }

          }

        }
        catch (Exception e)
        {
          e.printStackTrace();
        }

        System.out.println("blah");
      } catch (InterruptedException e) {
        break;
      }
  }

  public VM addVm(JSONObject o)
  {
    try
    {
      System.out.println(o.getString("name"));
      VM vm = new VM();
      vm.id = o.getInt("id");
      vm.name = o.getString("name");
      vm.os = o.getString("os_name");
      vm.memory = o.getInt("memory_mb");
      vm.vmPanel = new VMPanel(vm);

      VMSummaryItem summaryItem = new VMSummaryItem(vm);
      this.summaryPanel.add(summaryItem);
      vm.summaryItem = summaryItem;

      vm.setStatus(o.getString("status"));

      vm.last_reboot_at = o.getString("last_reboot_at");
      ImageIcon icon;
      ImageIcon icon;
      if (o.getString("status").equals("Running")) {
        icon = playIcon;
      }
      else
      {
        ImageIcon icon;
        if (o.getString("status").equals("Halted"))
          icon = stopIcon;
        else {
          icon = stopIcon;
        }
      }
      this.tabs.addTab(vm.name, icon, vm.vmPanel);
      vm.treeNode = new DefaultMutableTreeNode(vm);

      vmsNode.add(vm.treeNode);
      ((DefaultTreeModel)mainTree.getModel()).insertNodeInto(vm.treeNode, vmsNode, 0);

      VM f_vm = vm;
      Thread t = new Thread(new Runnable()
      {
        public void run()
        {
        }
      });
      t.start();
      return vm;
    } catch (Exception e) {
      e.printStackTrace();
    }return null;
  }
}