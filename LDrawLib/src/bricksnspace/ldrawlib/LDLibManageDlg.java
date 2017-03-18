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
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.border.BevelBorder;
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
	private JProgressBar pgr;

	private LDrawLib ldl;
	private LDLibTableModel tableModel;
	private JFileChooser jfc;

	private JButton refreshButton;

	
	private Timer tmr = null;
	private LDrawDBImportTask task = null;

	private JButton chgDefault;
	

	
	
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
		
		pgr = new JProgressBar(SwingConstants.HORIZONTAL,0,100);
		pgr.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));//EmptyBorder(6, 6, 6, 6));
		pgr.setMinimum(0);
		pgr.setMaximum(100);
		pgr.setStringPainted(true);

		
		userPane = new JPanel();
		contentPanel.add(userPane, BorderLayout.EAST);
		userPane.setLayout(new BoxLayout(userPane, BoxLayout.Y_AXIS));

		addButton = new JButton("Add...");
		delButton = new JButton("Delete");
		upButton = new JButton("Move Up");
		downButton = new JButton("Move Down");
		if (ldl.isDatabaseEnabled()) {
			refreshButton = new JButton("Refresh");
			refreshButton.addActionListener(this);
		}
		chgDefault = new JButton("Set Official...");
		addButton.addActionListener(this);
		delButton.addActionListener(this);
		upButton.addActionListener(this);
		downButton.addActionListener(this);
		chgDefault.addActionListener(this);
		
		userPane.add(addButton);
		userPane.add(delButton);
		userPane.add(upButton);
		userPane.add(downButton);
		if  (ldl.isDatabaseEnabled()) {
			userPane.add(refreshButton);
		}
		userPane.add(Box.createVerticalStrut(5));
		userPane.add(new JSeparator(SwingConstants.HORIZONTAL));
		userPane.add(Box.createVerticalStrut(5));
		userPane.add(chgDefault);
		userPane.add(Box.createVerticalGlue());

		JPanel buttonPane = new JPanel();
		buttonPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		//buttonPane.setLayout(new FlowLayout(FlowLayout.CENTER));
		buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
		getContentPane().add(buttonPane, BorderLayout.SOUTH);
		
		buttonPane.add(pgr);
		buttonPane.add(Box.createHorizontalGlue());

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
		
		if (ev.getSource() == tmr) {
			pgr.setValue(task.getProgress());
			if (task.isDone() || task.isCancelled()) {
				try {
					tmr.stop();
					setEnabled(true);
					int i = task.get(10, TimeUnit.MILLISECONDS);
					JOptionPane.showMessageDialog(this, "Imported/updated "+i+" LDraw parts.", 
								"LDraw part database", JOptionPane.INFORMATION_MESSAGE);
					tmr = null;
					task = null;
				}
				catch (ExecutionException ex) {
					Logger.getGlobal().log(Level.SEVERE,"[LDLibManageDlg] Exception in update database", ex);
					JOptionPane.showMessageDialog(this, "Error in database update.", 
							"LDraw part database", JOptionPane.ERROR_MESSAGE);
				} catch (InterruptedException e1) {
					try {
						ldl.getLdrDB().abortUpdate();
					} catch (SQLException e) {
						Logger.getGlobal().log(Level.SEVERE,"[LDLibManageDlg] Exception in rollback interrupted update", e);
					}
					Logger.getGlobal().log(Level.SEVERE,"[LDLibManageDlg] Database update interrupted", e1);
					JOptionPane.showMessageDialog(this, "Update interrupted.", 
							"LDraw part database", JOptionPane.INFORMATION_MESSAGE);
				} catch (TimeoutException e1) {
					Logger.getGlobal().log(Level.SEVERE,"[LDLibManageDlg] Timeout in retrieve update results", e1);
				}
				pgr.setValue(0);
				tableModel.refreshTable();
			}
		}
		else if (ev.getSource() == refreshButton) {
			if (ldl.isDatabaseEnabled()) {
				int idx = table.getSelectedRow();
				if (idx < 0)
					return;
				int res = JOptionPane.showConfirmDialog(this, "Refresh part in database?", "Please confirm", JOptionPane.YES_NO_OPTION);
				if (res != JOptionPane.YES_OPTION)
					return;
				tmr = new Timer(200, this);
				task = new LDrawDBImportTask(ldl, idx);
				task.execute();
				tmr.start();
				setEnabled(false);
			}
		}
		else if (ev.getSource() == addButton) {
			int res = jfc.showOpenDialog(this);
			if (res != JFileChooser.APPROVE_OPTION)
				return;
			try {
				int idx = ldl.addLDLib(jfc.getSelectedFile().getPath(),true);
				if (ldl.isDatabaseEnabled()) {
					tmr = new Timer(200, this);
					task = new LDrawDBImportTask(ldl, idx);
					task.execute();
					tmr.start();
					setEnabled(false);
				}
			} catch (ZipException e) {
				Logger.getGlobal().log(Level.SEVERE,"Zip library read error", e);
			} catch (IOException e) {
				Logger.getGlobal().log(Level.SEVERE,"I/O error", e);
			}
		}
		else if (ev.getSource() == delButton) {
			// delete selected
			int r = table.getSelectedRow();
			if (r <= 0)	// do not remove default library
				return;
			int res = JOptionPane.showConfirmDialog(this, "Removing LDraw library from list?", "Please confirm", JOptionPane.YES_NO_OPTION);
			if (res != JOptionPane.YES_OPTION)
				return;
			if (r >= 1) {
				ldl.removeLDLib(r);
				tableModel.refreshTable();
			}
		}
		else if (ev.getSource() == upButton) {
			// increase priority
			int r = table.getSelectedRow();
			if (r >= 2) {
				ldl.upPriority(r);
				tableModel.refreshTable();
				if (r > 1)
					table.setRowSelectionInterval(r-1, r-1);
				else 
					table.setRowSelectionInterval(r, r);
			}
		}
		else if (ev.getSource() == downButton) {
			// decrease priority
			int r = table.getSelectedRow();
			if (r >= 1) {
				ldl.downPriority(r);
				tableModel.refreshTable();
				if (r+1 < ldl.count())
					table.setRowSelectionInterval(r+1, r+1);
				else 
					table.setRowSelectionInterval(r, r);
			}
		}
		else if (ev.getSource() == chgDefault) {
			int res = jfc.showOpenDialog(this);
			if (res != JFileChooser.APPROVE_OPTION)
				return;
			try {
				if (!ldl.replaceOfficial(jfc.getSelectedFile().getPath())) {
					JOptionPane.showInternalMessageDialog(this, "Selected library does not appear to be official", "Library error", JOptionPane.ERROR_MESSAGE);
					return;
				}
				if (ldl.isDatabaseEnabled()) {
					tmr = new Timer(200, this);
					task = new LDrawDBImportTask(ldl, LDrawLib.OFFICIALINDEX);
					task.execute();
					tmr.start();
					setEnabled(false);
				}
			} catch (ZipException e) {
				Logger.getGlobal().log(Level.SEVERE,"Zip library read error", e);
			} catch (IOException e) {
				Logger.getGlobal().log(Level.SEVERE,"I/O error", e);
			}

		}
		else if (ev.getSource() == okButton) {
			setVisible(false);
		}
	}

	

}
