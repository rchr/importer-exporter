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
package org.citydb.modules.citygml.exporter.database.uid;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.citydb.modules.citygml.common.database.cache.BranchCacheTable;
import org.citydb.modules.citygml.common.database.cache.CacheTable;
import org.citydb.modules.citygml.common.database.cache.CacheTableManager;
import org.citydb.modules.citygml.common.database.cache.model.CacheTableModelEnum;
import org.citydb.modules.citygml.common.database.uid.UIDCacheEntry;
import org.citydb.modules.citygml.common.database.uid.UIDCachingModel;
import org.citygml4j.model.citygml.CityGMLClass;

public class GeometryGmlIdCache implements UIDCachingModel {
	private final ReentrantLock mainLock = new ReentrantLock(true);
	private final int partitions;
	private final CacheTableModelEnum cacheTableModel;
	private final CacheTableManager cacheTableManager;

	private BranchCacheTable branchTable;

	private CacheTable[] backUpTables;
	private PreparedStatement[] psLookupDbIds;
	private PreparedStatement[] psLookupGmlIds;
	private PreparedStatement[] psDrains;
	private ReentrantLock[] locks;
	private int[] batchCounters;

	private int batchSize;

	public GeometryGmlIdCache(CacheTableManager cacheTableManager, int partitions, int batchSize) throws SQLException {
		this.cacheTableManager = cacheTableManager;
		this.partitions = partitions;
		this.batchSize = batchSize;

		cacheTableModel = CacheTableModelEnum.GMLID_GEOMETRY;
		backUpTables = new CacheTable[partitions];
		psLookupDbIds = new PreparedStatement[partitions];
		psLookupGmlIds = new PreparedStatement[partitions];
		psDrains = new PreparedStatement[partitions];
		locks = new ReentrantLock[partitions];
		batchCounters = new int[partitions];

		for (int i = 0; i < partitions; i++)
			locks[i] = new ReentrantLock(true);
	}

	@Override
	public void drainToDB(ConcurrentHashMap<String, UIDCacheEntry> map, int drain) throws SQLException {
		int drainCounter = 0;			

		// firstly, try and write those entries which have already been requested
		Iterator<Map.Entry<String, UIDCacheEntry>> iter = map.entrySet().iterator();
		while (drainCounter <= drain && iter.hasNext()) {
			Map.Entry<String, UIDCacheEntry> entry = iter.next();
			if (entry.getValue().isRequested()) { 
				String gmlId = entry.getKey();

				// determine partition for gml:id
				int partition = Math.abs(gmlId.hashCode() % partitions);
				initializePartition(partition);

				// get corresponding prepared statement
				PreparedStatement psDrain = psDrains[partition];

				psDrain.setString(1, gmlId);
				psDrain.setLong(2, entry.getValue().getId());
				psDrain.setLong(3, entry.getValue().getRootId());
				psDrain.setInt(4, entry.getValue().isReverse() ? 1 : 0);
				psDrain.setString(5, entry.getValue().getMapping());
				psDrain.setInt(6, entry.getValue().getType().ordinal());

				psDrain.addBatch();
				if (++batchCounters[partition] == batchSize) {
					psDrain.executeBatch();
					batchCounters[partition] = 0;
				}

				iter.remove();
				++drainCounter;
			}
		}

		// secondly, drain remaining entries until drain limit
		iter = map.entrySet().iterator();
		while (drainCounter <= drain && iter.hasNext()) {
			Map.Entry<String, UIDCacheEntry> entry = iter.next();				
			String gmlId = entry.getKey();

			// determine partition for gml:id
			int partition = Math.abs(gmlId.hashCode() % partitions);
			initializePartition(partition);

			// get corresponding prepared statement
			PreparedStatement psDrain = psDrains[partition];

			psDrain.setString(1, gmlId);
			psDrain.setLong(2, entry.getValue().getId());
			psDrain.setLong(3, entry.getValue().getRootId());
			psDrain.setInt(4, entry.getValue().isReverse() ? 1 : 0);
			psDrain.setString(5, entry.getValue().getMapping());
			psDrain.setInt(6, entry.getValue().getType().ordinal());

			psDrain.addBatch();
			if (++batchCounters[partition] == batchSize) {
				psDrain.executeBatch();
				batchCounters[partition] = 0;
			}

			iter.remove();
			++drainCounter;
		}

		// finally execute batches
		for (int i = 0; i < psDrains.length; i++)
			if (psDrains[i] != null && batchCounters[i] > 0)
				psDrains[i].executeBatch();
	}

	@Override
	public UIDCacheEntry lookupDB(String key) throws SQLException { 
		// determine partition for gml:id
		int partition = Math.abs(key.hashCode() % partitions);
		initializePartition(partition);

		// lock partition
		final ReentrantLock tableLock = this.locks[partition];
		tableLock.lock();

		try {
			ResultSet rs = null;
			try {
				psLookupGmlIds[partition].setString(1, key);
				rs = psLookupGmlIds[partition].executeQuery();

				if (rs.next()) {
					long id = rs.getLong(1);
					long rootId = rs.getLong(2);
					boolean reverse = rs.getBoolean(3);
					String mapping = rs.getString(4);
					int type = rs.getInt(5);

					return new UIDCacheEntry(id, rootId, reverse, mapping, CityGMLClass.fromInt(type));
				}

				return null;
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
		} finally {
			tableLock.unlock();
		}
	}

	@Override
	public String lookupDB(long id, CityGMLClass type) throws SQLException {
		// since we cannot determine the partition by id we have to check all of them. 
		// this is definitely a drawback of using partitions  			
		for (int i = 0; i < partitions; i++) {
			final ReentrantLock tableLock = locks[i];
			tableLock.lock();

			try {
				if (backUpTables[i] == null)
					continue;

				ResultSet rs = null;
				try {
					psLookupDbIds[i].setLong(1, id);
					rs = psLookupDbIds[i].executeQuery();

					while (rs.next()) {
						CityGMLClass dbType = CityGMLClass.fromInt(rs.getInt(2));
						if (!dbType.isInstance(type))
							continue;

						return rs.getString(1);
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
			} finally {
				tableLock.unlock();
			}
		}

		return null;
	}

	@Override
	public void close() throws SQLException {
		for (PreparedStatement ps : psDrains)
			if (ps != null)
				ps.close();

		for (PreparedStatement ps : psLookupDbIds)
			if (ps != null)
				ps.close();

		for (PreparedStatement ps : psLookupGmlIds)
			if (ps != null)
				ps.close();
	}

	@Override
	public String getType() {
		return "geometry";
	}

	private void initializePartition(int partition) throws SQLException {
		if (branchTable == null) {
			mainLock.lock();

			try {
				if (branchTable == null)
					branchTable = cacheTableManager.createAndIndexBranchCacheTable(cacheTableModel);
			} finally {
				mainLock.unlock();
			}
		}

		if (backUpTables[partition] == null) {
			final ReentrantLock tableLock = locks[partition];
			tableLock.lock();

			try {
				if (backUpTables[partition] == null) {
					CacheTable tempTable = partition == 0 ? branchTable.getMainTable() : branchTable.branchAndIndex();

					Connection conn = tempTable.getConnection();
					String tableName = tempTable.getTableName();

					backUpTables[partition] = tempTable;
					psLookupDbIds[partition] = conn.prepareStatement("select GMLID, TYPE from " + tableName + " where ID=?");
					psLookupGmlIds[partition] = conn.prepareStatement("select ID, ROOT_ID, REVERSE, MAPPING, TYPE from " + tableName + " where GMLID=?");
					psDrains[partition] = conn.prepareStatement("insert into " + tableName + " (GMLID, ID, ROOT_ID, REVERSE, MAPPING, TYPE) values (?, ?, ?, ?, ?, ?)");
				}
			} finally {
				tableLock.unlock();
			}
		}
	}	
}
