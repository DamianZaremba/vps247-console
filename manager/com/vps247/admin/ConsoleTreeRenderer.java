package com.vps247.admin;

import java.awt.Component;
import java.io.PrintStream;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

class ConsoleTreeRenderer extends DefaultTreeCellRenderer
{
  public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus)
  {
    System.out.println("DOING TREE");
    super.getTreeCellRendererComponent(
      tree, value, sel, 
      expanded, leaf, row, 
      hasFocus);
    DefaultMutableTreeNode node = (DefaultMutableTreeNode)value;
    if ((leaf) && (node.getParent() == Manager.vmsNode)) {
      VM vm = (VM)node.getUserObject();
      if (vm.status.equals("Running"))
        setIcon(Manager.brickPlayIcon);
      else if (vm.status.equals("Halted"))
        setIcon(Manager.brickStopIcon);
      else {
        setIcon(Manager.brickWorkingIcon);
      }
      setToolTipText("VM");
    } else if (node == Manager.vmsNode) {
      setIcon(Manager.brickIcon);
    } else if (node == Manager.topNode) {
      setIcon(Manager.vps247Icon);
    }
    else {
      setToolTipText(null);
    }

    return this;
  }
}