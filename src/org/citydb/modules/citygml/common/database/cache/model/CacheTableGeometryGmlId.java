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
package org.citydb.modules.citygml.common.database.cache.model;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.citydb.database.adapter.AbstractSQLAdapter;


public class CacheTableGeometryGmlId extends CacheTableModel {
	private static CacheTableGeometryGmlId instance;

	private CacheTableGeometryGmlId() {
	}

	public synchronized static CacheTableGeometryGmlId getInstance() {
		if (instance == null)
			instance = new CacheTableGeometryGmlId();

		return instance;
	}
	
	@Override
	public CacheTableModelEnum getType() {
		return CacheTableModelEnum.GMLID_GEOMETRY;
	}

	@Override
	public void createIndexes(Connection conn, String tableName, String properties) throws SQLException {
		Statement stmt = null;

		try {
			stmt = conn.createStatement();

			stmt.executeUpdate("create index idx_" + tableName + " on " + tableName + " (GMLID) " + properties);
			stmt.executeUpdate("create index idx2_" + tableName + " on " + tableName + " (ID) " + properties);
		} finally {
			if (stmt != null) {
				stmt.close();
				stmt = null;
			}
		}
	}

	@Override
	protected String getColumns(AbstractSQLAdapter sqlAdapter) {
		StringBuilder builder = new StringBuilder("(")
		.append("GMLID ").append(sqlAdapter.getCharacterVarying(256)).append(", ")
		.append("ID ").append(sqlAdapter.getInteger()).append(", ")
		.append("ROOT_ID ").append(sqlAdapter.getInteger()).append(", ")
		.append("REVERSE ").append(sqlAdapter.getNumeric(1, 0)).append(", ")
		.append("MAPPING ").append(sqlAdapter.getCharacterVarying(256)).append(", ")
		.append("TYPE ").append(sqlAdapter.getNumeric(3))
		.append(")");

		return builder.toString();
	}
}
