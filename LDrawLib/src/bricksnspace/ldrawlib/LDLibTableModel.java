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


import javax.swing.table.AbstractTableModel;



/**
 * Table model for LDraw libraries management 
 * @author Mario Pascucci
 *
 */
public class LDLibTableModel extends AbstractTableModel {

	private static final long serialVersionUID = -6551135347388914647L;

	private static String[] columnNames = {
		"Priority",
		"Path",
		"Type",
		"Official",
		"Enabled"
		};

	
	private LDrawLib ldl;
	
	public LDLibTableModel(LDrawLib ldl) {
	
		super();
		if (ldl == null)
			throw new IllegalArgumentException("LDraw library can't be null");
		this.ldl = ldl;
	}
	
	
	@Override
	public int getColumnCount() {

		return columnNames.length;
	}
	
	
	@Override
	public int getRowCount() {

		return ldl.count();
	}
	
	
	
	@Override
	public boolean isCellEditable(int row, int col) {

		if (col == 0 || col == 1 || col == 2) 
	        return false;
	    else 
	        return true;
	}
	
	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Class getColumnClass(int c) {
	
		switch (c) {
		case 0:
		case 2:
			return Integer.class;
		case 1:
			return String.class;
		case 3:
		case 4:
			return Boolean.class;
		}
		return String.class;
	}
	
	
	@Override
	public String getColumnName(int col) {

	    return columnNames[col];
	}
	
	
	@SuppressWarnings("boxing")
	public void setValueAt(Object value, int row, int col) {
		
		switch (col) {
		case 3:
			ldl.setOfficial(row, (Boolean) value);
			break;
		case 4:
			if ((Boolean) value) 
				ldl.enable(row);
			else
				ldl.disable(row);
			break;
		}
	    fireTableRowsUpdated(row, row);
	 }
	
	
	
	public void refreshTable() {
		
		fireTableDataChanged();
	}
	
	
	
	@SuppressWarnings("boxing")
	@Override
	public Object getValueAt(int row, int col) {
		if (getRowCount() == 0)
			return "";
		switch (col) {
		case 0:
			return row;
		case 1:
			return ldl.getPath(row);
		case 2:
			return ldl.getType(row)==LDLibrary.FOLDER?"Folder":"Zipfile";
		case 3:
			return ldl.isOfficial(row);
		case 4:
			return ldl.isEnabled(row);
		}
		return null;
	}
	
	

}
