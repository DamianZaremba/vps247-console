package com.vps247.admin;

import com.citrix.xenserver.console.VNCCanvas;
import com.citrix.xenserver.console.VNCStream;
import com.citrix.xenserver.console.XenConsole;
import java.awt.Color;
import java.io.PrintStream;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.tree.DefaultMutableTreeNode;
import org.json.JSONObject;

public class VM
{
  public String name;
  public int id;
  public String status = "";
  public String os;
  public int memory;
  public XenConsole xenConsole;
  public VMPanel vmPanel;
  public String last_reboot_at;
  public JPanel summaryPanel;
  public VMSummaryItem summaryItem;
  public DefaultMutableTreeNode treeNode;

  public VM()
  {
    this.summaryPanel = new JPanel();

    this.summaryPanel.setBorder(BorderFactory.createLineBorder(Color.blue, 2));
  }

  public String toString() {
    return this.name;
  }

  public boolean loadConsole() {
    String json = VPS247Api.getPath("/vms/" + this.id + "/console.json");
    try
    {
      JSONObject o = new JSONObject(json);
      String url = o.getString("url");
      String session = o.getString("session");
      if (this.xenConsole == null) {
        this.xenConsole = new XenConsole(url, session, this.name);
      }

      if (!url.equals("null"))
      {
        this.xenConsole.stream_ = new VNCStream(this.xenConsole.canvas_, this.xenConsole, this.xenConsole);
        this.xenConsole.canvas_.setStream(this.xenConsole.stream_);
        this.xenConsole.path = url;
        this.xenConsole.auth = session;
        this.xenConsole.connect();
        return true;
      }
      return false;
    }
    catch (Exception localException)
    {
    }
    return false;
  }

  public void setStatus(String new_status) {
    if ((!this.status.equals("Halted")) && (new_status.equals("Halted")) && (this.xenConsole != null)) {
      this.xenConsole.stream_.disconnect();
      this.xenConsole.noConnect = true;
      System.out.println("Halted");
    }
    else if ((!this.status.equals("Running")) && (new_status.equals("Running")) && (this.xenConsole != null)) {
      loadConsole();
      this.xenConsole.noConnect = false;
      this.xenConsole.connect();
      System.out.println("Started");
    }
    if ((new_status.equals("Running")) && (this.xenConsole != null) && (this.xenConsole.path != null) && (this.xenConsole.path.equals("null"))) {
      loadConsole();
    }
    if ((this.xenConsole != null) && (this.xenConsole.path != null)) {
      System.out.println(this.xenConsole.path);
    }
    if (((this.status.equals("Provisioning")) || (this.status.equals("")) || (this.status == null)) && (!new_status.equals("Provisioning"))) {
      this.vmPanel.tabs.addTab("Console", Manager.consoleIcon, this.vmPanel.loadingPanel);
    }
    this.status = new_status;

    System.out.println("NEW STATUS " + new_status);
    System.out.println("BEFORE - " + this.vmPanel.stopButton.getText());

    if (new_status.equals("Halted")) { this.vmPanel.startButton.setEnabled(true); this.vmPanel.stopButton.setEnabled(false);
    } else if (new_status.equals("Running")) { System.out.println("IS RUNNING!"); this.vmPanel.startButton.setEnabled(false); this.vmPanel.stopButton.setText("Running"); this.vmPanel.stopButton.setEnabled(true); } else {
      System.out.println("BOTH"); this.vmPanel.startButton.setEnabled(false); this.vmPanel.stopButton.setEnabled(false);
    }
    System.out.println("AFTER - " + this.vmPanel.stopButton.getText());

    this.vmPanel.repaint();
  }

  public void setMemory(int memory)
  {
    this.memory = memory;
    this.vmPanel.tvMemory.setValue(memory + " MB");
    this.summaryItem.tvMemory.setValue(memory + " MB");
  }
}