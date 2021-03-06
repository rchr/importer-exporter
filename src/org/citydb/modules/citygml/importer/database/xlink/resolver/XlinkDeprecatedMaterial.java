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
import java.sql.SQLException;

import org.citydb.modules.citygml.common.database.uid.UIDCacheEntry;
import org.citydb.modules.citygml.common.database.xlink.DBXlinkDeprecatedMaterial;
import org.citydb.modules.citygml.importer.database.content.DBSequencerEnum;
import org.citygml4j.model.citygml.CityGMLClass;

public class XlinkDeprecatedMaterial implements DBXlinkResolver {
	private final Connection batchConn;
	private final DBXlinkResolverManager resolverManager;

	private PreparedStatement psSurfaceData;
	private PreparedStatement psTextureParam;
	
	private int batchCounter;

	public XlinkDeprecatedMaterial(Connection batchConn, DBXlinkResolverManager resolverManager) throws SQLException {
		this.batchConn = batchConn;
		this.resolverManager = resolverManager;

		init();
	}

	private void init() throws SQLException {
		psSurfaceData = batchConn.prepareStatement("insert into SURFACE_DATA (select ?, GMLID, NAME, NAME_CODESPACE, DESCRIPTION, IS_FRONT, OBJECTCLASS_ID, " +
				"X3D_SHININESS, X3D_TRANSPARENCY, X3D_AMBIENT_INTENSITY, X3D_SPECULAR_COLOR, X3D_DIFFUSE_COLOR, X3D_EMISSIVE_COLOR, X3D_IS_SMOOTH, " +
				"TEX_IMAGE_ID, TEX_TEXTURE_TYPE, TEX_WRAP_MODE, TEX_BORDER_COLOR, " +
				"GT_PREFER_WORLDFILE, GT_ORIENTATION, GT_REFERENCE_POINT from SURFACE_DATA where ID=?)");

		psTextureParam = batchConn.prepareStatement("insert into TEXTUREPARAM (select ?, IS_TEXTURE_PARAMETRIZATION, WORLD_TO_TEXTURE, TEXTURE_COORDINATES, ? from TEXTUREPARAM where SURFACE_DATA_ID=?)");
	}

	public boolean insert(DBXlinkDeprecatedMaterial xlink) throws SQLException {
		UIDCacheEntry surfaceDataEntry = resolverManager.getDBId(xlink.getGmlId(), CityGMLClass.APPEARANCE);
		if (surfaceDataEntry == null || surfaceDataEntry.getId() == -1)
			return false;

		long newSurfaceDataId = resolverManager.getDBId(DBSequencerEnum.SURFACE_DATA_ID_SEQ);

		psSurfaceData.setLong(1, newSurfaceDataId);
		psSurfaceData.setLong(2, surfaceDataEntry.getId());
		psSurfaceData.addBatch();

		psTextureParam.setLong(1, xlink.getSurfaceGeometryId());
		psTextureParam.setLong(2, newSurfaceDataId);
		psTextureParam.setLong(3, surfaceDataEntry.getId());
		psTextureParam.addBatch();
		
		if (++batchCounter == resolverManager.getDatabaseAdapter().getMaxBatchSize())
			executeBatch();

		return true;
	}

	@Override
	public void executeBatch() throws SQLException {
		psSurfaceData.executeBatch();
		psTextureParam.executeBatch();
		batchCounter = 0;
	}

	@Override
	public void close() throws SQLException {
		psSurfaceData.close();
		psTextureParam.close();
	}

	@Override
	public DBXlinkResolverEnum getDBXlinkResolverType() {
		return DBXlinkResolverEnum.XLINK_DEPRECATED_MATERIAL;
	}

}
