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
package org.citydb.modules.citygml.importer.database.content;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DBWaterBodToWaterBndSrf implements DBImporter {
	private final Connection batchConn;
	private final DBImporterManager dbImporterManager;

	private PreparedStatement psWaterBodToWaterBndSrf;
	private int batchCounter;

	public DBWaterBodToWaterBndSrf(Connection batchConn, DBImporterManager dbImporterManager) throws SQLException {
		this.batchConn = batchConn;
		this.dbImporterManager = dbImporterManager;

		init();
	}

	private void init() throws SQLException {
		psWaterBodToWaterBndSrf = batchConn.prepareStatement("insert into WATERBOD_TO_WATERBND_SRF (WATERBOUNDARY_SURFACE_ID, WATERBODY_ID) values (?, ?)");
	}

	public void insert(long waterSurfaceId, long waterBodyId) throws SQLException {
		psWaterBodToWaterBndSrf.setLong(1, waterSurfaceId);
		psWaterBodToWaterBndSrf.setLong(2, waterBodyId);

		psWaterBodToWaterBndSrf.addBatch();
		if (++batchCounter == dbImporterManager.getDatabaseAdapter().getMaxBatchSize())
			dbImporterManager.executeBatch(DBImporterEnum.WATERBOD_TO_WATERBND_SRF);
	}

	@Override
	public void executeBatch() throws SQLException {
		psWaterBodToWaterBndSrf.executeBatch();
		batchCounter = 0;
	}

	@Override
	public void close() throws SQLException {
		psWaterBodToWaterBndSrf.close();
	}

	@Override
	public DBImporterEnum getDBImporterType() {
		return DBImporterEnum.WATERBOD_TO_WATERBND_SRF;
	}
}
