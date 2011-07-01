package com.vps247.admin;

import java.awt.Dimension;
import java.awt.Font;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

class TitleValuePanel extends Box
{
  private String title;
  private String value;
  private JLabel titleLabel;
  private JLabel valueLabel;
  private ImageIcon icon;

  TitleValuePanel(String title, String value)
  {
    super(0);

    this.title = title;
    this.value = value;
    this.titleLabel = new JLabel(title + ":");
    this.valueLabel = new JLabel(value);
    this.valueLabel.setFont(new Font("sans-serif", 0, 12));
    this.titleLabel.setMinimumSize(new Dimension(140, 20));
    this.titleLabel.setPreferredSize(new Dimension(140, 20));
    this.titleLabel.setMaximumSize(new Dimension(140, 20));
    this.valueLabel.setText(value);
    add(this.titleLabel);
    add(this.valueLabel);
  }

  public void setValue(String value)
  {
    this.value = value;
    this.valueLabel.setText(value);
    validate();
  }

  public void setIcon(ImageIcon icon) {
    this.icon = icon;
    this.valueLabel.setIcon(this.icon);
  }
}