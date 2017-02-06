/**
	Copyright 2016 Mario Pascucci <mpascucci@gmail.com>
	This file is part of LDrawLib

	LDrawLib is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	LDrawLib is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with LDrawLib.  If not, see <http://www.gnu.org/licenses/>.
 
 */


package bricksnspace.ldrawlib;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipException;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.TableColumnModel;




/**
 * A dialog in Swing UI to manage LDraw libraries and folder used to search and get LDraw parts
 * 
 * @author Mario Pascucci
 *
 */
public class LDLibManageDlg extends JDialog implements ActionListener {

	private static final long serialVersionUID = 8988123312521694767L;

	private JPanel contentPanel;
	private JTable table;
	private JButton addButton,delButton,upButton,downButton;
	private JButton okButton;

	private LDrawLib ldl;
	private LDLibTableModel tableModel;
	private JFileChooser jfc;

	
	/**
	 * Shows LDraw library management dialog
	 * @param owner parent of dialog
	 * @param title text in dialog title
	 * @param modal modal if true
	 * @param l current LDrawLib instance 
	 */
	public LDLibManageDlg(Frame owner, String title, boolean modal, LDrawLib l) {
		super(owner, title, modal);
		ldl = l;
		initialize();
	}

	/**
	 * Shows LDraw library management dialog
	 * @param owner parent of dialog
	 * @param title text in dialog title
	 * @param modal modal if true
	 * @param l current LDrawLib instance 
	 */
	public LDLibManageDlg(Dialog owner, String title, boolean modal, LDrawLib l) {
		super(owner, title, modal);
		ldl = l;
		initialize();
	}

	
	
	
	private void initialize() {

		JPanel userPane;
		JScrollPane scrollPane;
		
		setLocationByPlatform(true);
		getContentPane().setLayout(new BorderLayout());
		contentPanel = new JPanel();
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(new BorderLayout(4, 4));
		
		scrollPane = new JScrollPane();
		
		contentPanel.add(scrollPane, BorderLayout.CENTER);

		tableModel = new LDLibTableModel(ldl);
		table = new JTable(tableModel);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		table.setAutoCreateRowSorter(true);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		TableColumnModel tcm = table.getColumnModel();
		tcm.getColumn(0).setPreferredWidth(25);
		tcm.getColumn(1).setPreferredWidth(250);
		tcm.getColumn(2).setPreferredWidth(20);
		tcm.getColumn(3).setPreferredWidth(20);
		tcm.getColumn(4).setPreferredWidth(20);
		
		userPane = new JPanel();
		contentPanel.add(userPane, BorderLayout.EAST);
		userPane.setLayout(new BoxLayout(userPane, BoxLayout.Y_AXIS));

		addButton = new JButton("Add...");
		delButton = new JButton("Delete");
		upButton = new JButton("Move Up");
		downButton = new JButton("Move Down");
		addButton.addActionListener(this);
		delButton.addActionListener(this);
		upButton.addActionListener(this);
		downButton.addActionListener(this);
		
		userPane.add(addButton);
		userPane.add(delButton);
		userPane.add(upButton);
		userPane.add(downButton);

		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
		getContentPane().add(buttonPane, BorderLayout.SOUTH);

		okButton = new JButton("OK");
		buttonPane.add(okButton);
		okButton.addActionListener(this);
		getRootPane().setDefaultButton(okButton);

		scrollPane.setPreferredSize(new Dimension(600,200));
		scrollPane.setViewportView(table);
		
		pack();
		
		jfc = new JFileChooser(".");
		jfc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		FileFilter ff = new FileNameExtensionFilter("Zipfile", "zip");
		jfc.setFileFilter(ff);
		jfc.setDialogTitle("Select LDraw Library");
		jfc.setDialogType(JFileChooser.OPEN_DIALOG);
		jfc.setMultiSelectionEnabled(false);
	}
	
	
	

	@Override
	public void actionPerformed(ActionEvent ev) {

		if (ev.getSource() == addButton) {
			int res = jfc.showOpenDialog(this);
			if (res != JFileChooser.APPROVE_OPTION)
				return;
			try {
				ldl.addLDLib(jfc.getSelectedFile().getPath());
				tableModel.refreshTable();
			} catch (ZipException e) {
				Logger.getGlobal().log(Level.SEVERE,"Zip library read error", e);
			} catch (IOException e) {
				Logger.getGlobal().log(Level.SEVERE,"Zip library read error", e);
			}
		}
		else if (ev.getSource() == delButton) {
			// delete selected
			int res = JOptionPane.showConfirmDialog(this, "Removing LDraw library from list?", "Please confirm", JOptionPane.YES_NO_OPTION);
			if (res != JOptionPane.YES_OPTION)
				return;
			int r = table.getSelectedRow();
			if (r >= 0) {
				ldl.removeLDLib(r);
				tableModel.refreshTable();
			}
		}
		else if (ev.getSource() == upButton) {
			// increase priority
			int r = table.getSelectedRow();
			if (r >= 0) {
				ldl.upPriority(r);
				tableModel.refreshTable();
				if (r > 0)
					table.setRowSelectionInterval(r-1, r-1);
				else 
					table.setRowSelectionInterval(r, r);
			}
		}
		else if (ev.getSource() == downButton) {
			// decrease priority
			int r = table.getSelectedRow();
			if (r >= 0) {
				ldl.downPriority(r);
				tableModel.refreshTable();
				if (r+1 < ldl.count())
					table.setRowSelectionInterval(r+1, r+1);
				else 
					table.setRowSelectionInterval(r, r);
			}
		}
		else if (ev.getSource() == okButton) {
			setVisible(false);
		}
	}

	

}
