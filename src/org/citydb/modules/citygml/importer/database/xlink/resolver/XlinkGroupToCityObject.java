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
package org.citydb.modules.citygml.importer.database.xlink.resolver;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.citydb.modules.citygml.common.database.cache.CacheTable;
import org.citydb.modules.citygml.common.database.uid.UIDCacheEntry;
import org.citydb.modules.citygml.common.database.xlink.DBXlinkGroupToCityObject;
import org.citydb.modules.common.filter.ImportFilter;
import org.citydb.modules.common.filter.feature.FeatureClassFilter;
import org.citygml4j.model.citygml.CityGMLClass;

public class XlinkGroupToCityObject implements DBXlinkResolver {
	private final Connection batchConn;
	private final CacheTable cacheTable;
	private final DBXlinkResolverManager resolverManager;

	private PreparedStatement psSelectTmp;
	private PreparedStatement psGroupMemberToCityObject;
	private PreparedStatement psGroupParentToCityObject;
	
	private int parentBatchCounter;
	private int memberBatchCounter;

	private FeatureClassFilter featureClassFilter;

	public XlinkGroupToCityObject(Connection batchConn, CacheTable cacheTable, ImportFilter importFilter, DBXlinkResolverManager resolverManager) throws SQLException {
		this.batchConn = batchConn;
		this.cacheTable = cacheTable;
		this.resolverManager = resolverManager;
		this.featureClassFilter = importFilter.getFeatureClassFilter();
		
		init();
	}

	private void init() throws SQLException {
		psSelectTmp = cacheTable.getConnection().prepareStatement("select GROUP_ID from " + cacheTable.getTableName() + " where GROUP_ID=? and IS_PARENT=?");
		psGroupMemberToCityObject = batchConn.prepareStatement("insert into GROUP_TO_CITYOBJECT (CITYOBJECT_ID, CITYOBJECTGROUP_ID, ROLE) values " +
		"(?, ?, ?)");
		psGroupParentToCityObject = batchConn.prepareStatement("update CITYOBJECTGROUP set PARENT_CITYOBJECT_ID=? where ID=?");
	}

	public boolean insert(DBXlinkGroupToCityObject xlink) throws SQLException {
		// for groupMembers, we do not only lookup gmlIds within the document, but also within
		// the whole database!
		UIDCacheEntry cityObjectEntry = resolverManager.getDBId(xlink.getGmlId(), CityGMLClass.ABSTRACT_CITY_OBJECT, true);
		if (cityObjectEntry == null || cityObjectEntry.getId() == -1)
			return false;

		if (featureClassFilter.filter(cityObjectEntry.getType()))
			return true;

		// be careful with cyclic groupings!
		if (cityObjectEntry.getType() == CityGMLClass.CITY_OBJECT_GROUP) {
			ResultSet rs = null;

			try {
				psSelectTmp.setLong(1, cityObjectEntry.getId());
				psSelectTmp.setLong(2, xlink.isParent() ? 1 : 0);
				rs = psSelectTmp.executeQuery();			

				if (rs.next()) {
					resolverManager.propagateXlink(xlink);
					return true;
				}

			} finally {
				if (rs != null) {
					try {
						rs.close();
					} catch (SQLException sqlEx) {
						//
					}

					rs = null;
				}
			}
		}

		if (xlink.isParent()) {
			psGroupParentToCityObject.setLong(1, cityObjectEntry.getId());
			psGroupParentToCityObject.setLong(2, xlink.getGroupId());
			
			psGroupParentToCityObject.addBatch();
			if (++parentBatchCounter == resolverManager.getDatabaseAdapter().getMaxBatchSize()) {
				psGroupParentToCityObject.executeBatch();
				parentBatchCounter = 0;
			}
		} else {
			psGroupMemberToCityObject.setLong(1, cityObjectEntry.getId());
			psGroupMemberToCityObject.setLong(2, xlink.getGroupId());
			psGroupMemberToCityObject.setString(3, xlink.getRole());

			psGroupMemberToCityObject.addBatch();
			if (++memberBatchCounter == resolverManager.getDatabaseAdapter().getMaxBatchSize()) {
				psGroupMemberToCityObject.executeBatch();
				memberBatchCounter = 0;
			}
		}
		
		return true;
	}


	@Override
	public void executeBatch() throws SQLException {
		psGroupMemberToCityObject.executeBatch();
		psGroupParentToCityObject.executeBatch();		
		parentBatchCounter = 0;
		memberBatchCounter = 0;
	}

	@Override
	public void close() throws SQLException {
		psGroupMemberToCityObject.close();
		psGroupParentToCityObject.close();
		psSelectTmp.close();
	}

	@Override
	public DBXlinkResolverEnum getDBXlinkResolverType() {
		return DBXlinkResolverEnum.GROUP_TO_CITYOBJECT;
	}

}
