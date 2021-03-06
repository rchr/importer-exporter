/*
 * 3D City Database - The Open Source CityGML Database
 * http://www.3dcitydb.org/
 * 
 * (C) 2013 - 2015,
 * Chair of Geoinformatics,
 * Technische Universitaet Muenchen, Germany
 * http://www.gis.bgu.tum.de/
 * 
 * The 3D City Database is jointly developed with the following
 * cooperation partners:
 * 
 * virtualcitySYSTEMS GmbH, Berlin <http://www.virtualcitysystems.de/>
 * M.O.S.S. Computer Grafik Systeme GmbH, Muenchen <http://www.moss.de/>
 * 
 * The 3D City Database Importer/Exporter program is free software:
 * you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 */
package org.citydb.gui.factory.popup;

import java.awt.Component;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPasswordField;
import javax.swing.KeyStroke;
import javax.swing.TransferHandler;
import javax.swing.text.JTextComponent;

import org.citydb.api.event.Event;
import org.citydb.api.event.EventHandler;
import org.citydb.api.event.global.GlobalEvents;
import org.citydb.api.registry.ObjectRegistry;
import org.citydb.config.language.Language;

@SuppressWarnings("serial")
public class StandardEditingPopupMenu extends AbstractStandardPopupMenu implements EventHandler {
	private JMenuItem cut;
	private JMenuItem copy;
	private JMenuItem paste;
	private JMenuItem selectAll;

	public StandardEditingPopupMenu() {
		ObjectRegistry.getInstance().getEventDispatcher().addEventHandler(GlobalEvents.SWITCH_LOCALE, this);
	}

	public void init(Component component) {
		cut = new JMenuItem();
		copy = new JMenuItem();		
		paste = new JMenuItem();
		selectAll = new JMenuItem();

		cut.setActionCommand((String)TransferHandler.getCutAction().getValue(Action.NAME));
		copy.setActionCommand((String)TransferHandler.getCopyAction().getValue(Action.NAME));
		paste.setActionCommand((String)TransferHandler.getPasteAction().getValue(Action.NAME));

		cut.addActionListener(new TransferActionListener());
		copy.addActionListener(new TransferActionListener());
		paste.addActionListener(new TransferActionListener());

		if (component instanceof JTextComponent) {
			selectAll.setAction(new TextSelectAllAction());

			if (component instanceof JPasswordField) {
				cut.setEnabled(false);
				copy.setEnabled(false);
			}
		} else if (component instanceof JList)
			selectAll.setAction(new ListSelectAllAction());		

		cut.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		copy.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		paste.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		selectAll.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

		add(cut);
		add(copy);
		add(paste);
		addSeparator();
		add(selectAll);
	}
	
	public void prepare(boolean enable) {
		cut.setEnabled(enable);
		paste.setEnabled(enable);
	}

	public void doTranslation() {
		cut.setText(Language.I18N.getString("common.popup.textfield.cut"));		
		copy.setText(Language.I18N.getString("common.popup.textfield.copy"));		
		paste.setText(Language.I18N.getString("common.popup.textfield.paste"));		
		selectAll.setText(Language.I18N.getString("common.popup.textfield.selectAll"));		
	}

	private final class TextSelectAllAction extends AbstractAction {
		public void actionPerformed(ActionEvent e) {
			final Component c = getInvoker();
			if (c instanceof JTextComponent) {
				JTextComponent text = (JTextComponent)c;
				text.requestFocus();
				text.setText(text.getText());
				text.selectAll();
			}
		}
	}

	private final class ListSelectAllAction extends AbstractAction {
		public void actionPerformed(ActionEvent e) {
			final Component c = getInvoker();
			if (c instanceof JList) {
				JList<?> list = (JList<?>)c;
				int end = list.getModel().getSize() - 1;
				if (end >= 0)
					list.setSelectionInterval(0, end);
			}
		}
	}

	private final class TransferActionListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			final Component c = getInvoker();
			if (c instanceof JComponent) {
				JComponent invoker = (JComponent)c;				
				String action = (String)e.getActionCommand();
				Action a = invoker.getActionMap().get(action);
				if (a != null)
					a.actionPerformed(new ActionEvent(invoker, ActionEvent.ACTION_PERFORMED, null));
			}
		}
	}

	@Override
	public void handleEvent(Event event) throws Exception {
		doTranslation();
	}

}
