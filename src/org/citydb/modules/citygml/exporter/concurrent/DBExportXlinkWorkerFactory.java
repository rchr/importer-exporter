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
package org.citydb.modules.citygml.exporter.concurrent;

import java.sql.SQLException;

import org.citydb.api.concurrent.Worker;
import org.citydb.api.concurrent.WorkerFactory;
import org.citydb.api.event.EventDispatcher;
import org.citydb.config.Config;
import org.citydb.database.DatabaseConnectionPool;
import org.citydb.log.Logger;
import org.citydb.modules.citygml.common.database.xlink.DBXlink;

public class DBExportXlinkWorkerFactory implements WorkerFactory<DBXlink> {
	private final Logger LOG = Logger.getInstance();
	
	private final DatabaseConnectionPool dbConnectionPool;
	private final Config config;
	private final EventDispatcher eventDispatcher;

	public DBExportXlinkWorkerFactory(DatabaseConnectionPool dbConnectionPool, Config config, EventDispatcher eventDispatcher) {
		this.dbConnectionPool = dbConnectionPool;
		this.config = config;
		this.eventDispatcher = eventDispatcher;
	}

	@Override
	public Worker<DBXlink> createWorker() {
		DBExportXlinkWorker dbWorker = null;

		try {
			dbWorker = new DBExportXlinkWorker(dbConnectionPool, config, eventDispatcher);
		} catch (SQLException e) {
			LOG.error("Failed to create XLink export worker: " + e.getMessage());
		}

		return dbWorker;
	}
}
