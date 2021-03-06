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
package org.citydb.modules.database.gui.operations;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.citydb.api.controller.ViewController;
import org.citydb.api.database.DatabaseSrs;
import org.citydb.api.event.global.DatabaseConnectionStateEvent;
import org.citydb.api.geometry.BoundingBox;
import org.citydb.api.registry.ObjectRegistry;
import org.citydb.config.Config;
import org.citydb.config.language.Language;
import org.citydb.config.project.database.DBOperationType;
import org.citydb.config.project.database.Workspace;
import org.citydb.config.project.general.FeatureClassMode;
import org.citydb.database.DatabaseConnectionPool;
import org.citydb.gui.components.StatusDialog;
import org.citydb.gui.components.bbox.BoundingBoxClipboardHandler;
import org.citydb.gui.components.bbox.BoundingBoxPanelImpl;
import org.citydb.log.Logger;
import org.citydb.util.gui.GuiUtil;

public class BoundingBoxOperation extends DatabaseOperationView {
	private final ReentrantLock mainLock = new ReentrantLock();
	private final Logger LOG = Logger.getInstance();
	private final DatabaseOperationsPanel parent;
	private final ViewController viewController;
	private final DatabaseConnectionPool dbConnectionPool;
	private final Config config;

	private JPanel component;
	private JLabel featureLabel;
	private JComboBox<FeatureClassMode> featureComboBox;
	private BoundingBoxPanelImpl bboxPanel;
	private JButton bboxButton;

	public BoundingBoxOperation(DatabaseOperationsPanel parent, Config config) {
		this.parent = parent;
		this.config = config;
		viewController = ObjectRegistry.getInstance().getViewController();
		dbConnectionPool = DatabaseConnectionPool.getInstance();

		init();
	}

	private void init() {
		component = new JPanel();
		component.setLayout(new GridBagLayout());

		featureLabel = new JLabel();
		bboxPanel = new BoundingBoxPanelImpl(config);
		bboxPanel.setEditable(false);
		bboxButton = new JButton();

		featureComboBox = new JComboBox<FeatureClassMode>();
		for (FeatureClassMode type : FeatureClassMode.values())
			featureComboBox.addItem(type);

		component.add(featureLabel, GuiUtil.setConstraints(0,0,0.0,0.0,GridBagConstraints.BOTH,10,5,0,5));
		component.add(featureComboBox, GuiUtil.setConstraints(1,0,1.0,0.0,GridBagConstraints.BOTH,10,5,0,5));

		GridBagConstraints c = GuiUtil.setConstraints(0,2,0.0,0.0,GridBagConstraints.BOTH,5,5,5,5);
		c.gridwidth = 2;
		component.add(bboxPanel, c);

		c = GuiUtil.setConstraints(0,3,1.0,0.0,GridBagConstraints.NONE,10,5,5,5);
		c.gridwidth = 2;
		component.add(bboxButton, c);

		bboxButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Thread thread = new Thread() {
					public void run() {
						doOperation();
					}
				};
				thread.setDaemon(true);
				thread.start();
			}
		});
	}

	@Override
	public String getLocalizedTitle() {
		return Language.I18N.getString("db.label.operation.bbox");
	}

	@Override
	public Component getViewComponent() {
		return component;
	}

	@Override
	public String getToolTip() {
		return null;
	}

	@Override
	public Icon getIcon() {
		return null;
	}

	@Override
	public DBOperationType getType() {
		return DBOperationType.BOUNDING_BOX;
	}

	@Override
	public void doTranslation() {
		featureLabel.setText(Language.I18N.getString("db.label.operation.bbox.feature"));
		bboxButton.setText(Language.I18N.getString("db.button.bbox"));
	}

	@Override
	public void setEnabled(boolean enable) {
		featureLabel.setEnabled(enable);
		featureComboBox.setEnabled(enable);
		bboxPanel.setEnabled(enable);
		bboxButton.setEnabled(enable);
	}

	@Override
	public void loadSettings() {
		featureComboBox.setSelectedItem(config.getProject().getDatabase().getOperation().getBoundingBoxFeatureClass());
		bboxPanel.getSrsComboBox().setSelectedItem(config.getProject().getDatabase().getOperation().getBoundingBoxSRS());
	}

	@Override
	public void setSettings() {
		config.getProject().getDatabase().getOperation().setBoundingBoxFeatureClass((FeatureClassMode)featureComboBox.getSelectedItem());
		config.getProject().getDatabase().getOperation().setBoundingBoxSRS(bboxPanel.getSrsComboBox().getSelectedItem());
	}

	private void doOperation() {
		final ReentrantLock lock = this.mainLock;
		lock.lock();

		try {
			Workspace workspace = parent.getWorkspace();
			if (workspace == null)
				return;

			viewController.clearConsole();
			viewController.setStatusText(Language.I18N.getString("main.status.database.bbox.label"));

			LOG.info("Calculating bounding box...");			
			if (dbConnectionPool.getActiveDatabaseAdapter().hasVersioningSupport() && !parent.existsWorkspace())
				return;

			final StatusDialog bboxDialog = new StatusDialog(viewController.getTopFrame(), 
					Language.I18N.getString("db.dialog.bbox.window"), 
					Language.I18N.getString("db.dialog.bbox.title"), 
					null,
					Language.I18N.getString("db.dialog.bbox.details"), 
					true);

			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					bboxDialog.setLocationRelativeTo(viewController.getTopFrame());
					bboxDialog.setVisible(true);
				}
			});

			bboxDialog.getButton().addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							dbConnectionPool.getActiveDatabaseAdapter().getUtil().interruptDatabaseOperation();
						}
					});
				}
			});

			BoundingBox bbox = null;
			try {
				FeatureClassMode featureClass = (FeatureClassMode)featureComboBox.getSelectedItem();
				bbox = dbConnectionPool.getActiveDatabaseAdapter().getUtil().calcBoundingBox(workspace, featureClass);

				if (bbox != null) {
					if (bbox.getLowerLeftCorner().getX() != Double.MAX_VALUE && 
							bbox.getLowerLeftCorner().getY() != Double.MAX_VALUE &&
							bbox.getUpperRightCorner().getX() != -Double.MAX_VALUE && 
							bbox.getUpperRightCorner().getY() != -Double.MAX_VALUE) {

						DatabaseSrs dbSrs = dbConnectionPool.getActiveDatabaseAdapter().getConnectionMetaData().getReferenceSystem();
						DatabaseSrs targetSrs = bboxPanel.getSrsComboBox().getSelectedItem();

						if (targetSrs.isSupported() && targetSrs.getSrid() != dbSrs.getSrid()) {
							try {
								bbox = dbConnectionPool.getActiveDatabaseAdapter().getUtil().transformBoundingBox(bbox, dbSrs, targetSrs);
							} catch (SQLException e) {
								//
							}					
						}

						bboxPanel.setBoundingBox(bbox);	
						bbox.setSrs(targetSrs);
						BoundingBoxClipboardHandler.getInstance(config).putBoundingBox(bbox);
						LOG.info("Bounding box for feature " + featureClass + " successfully calculated.");							
					} else {
						bboxPanel.clearBoundingBox();							
						LOG.warn("The bounding box could not be calculated.");
						LOG.warn("Either the database does not contain " + featureClass + " features or their ENVELOPE attribute is not set.");
					}

				} else
					LOG.warn("Calculation of bounding box aborted.");

				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						bboxDialog.dispose();
					}
				});

			} catch (SQLException sqlEx) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						bboxDialog.dispose();
					}
				});

				bboxPanel.clearBoundingBox();

				String sqlExMsg = sqlEx.getMessage().trim();
				String text = Language.I18N.getString("db.dialog.error.bbox");
				Object[] args = new Object[]{ sqlExMsg };
				String result = MessageFormat.format(text, args);

				JOptionPane.showMessageDialog(
						viewController.getTopFrame(), 
						result, 
						Language.I18N.getString("common.dialog.error.db.title"),
						JOptionPane.ERROR_MESSAGE);

				LOG.error("SQL error: " + sqlExMsg);
			} finally {		
				viewController.setStatusText(Language.I18N.getString("main.status.ready.label"));
			}

		} finally {
			lock.unlock();
		}
	}

	@Override
	public void handleDatabaseConnectionStateEvent( DatabaseConnectionStateEvent event) {
		if (event.wasConnected())
			bboxPanel.clearBoundingBox();
	}

}
