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
package org.citydb.api.database;

import java.sql.Connection;
import java.sql.SQLException;

import org.citydb.api.geometry.GeometryObject;

public interface DatabaseGeometryConverter {
	public GeometryObject getEnvelope(Object geomObj) throws SQLException;
	public GeometryObject getPoint(Object geomObj) throws SQLException;
	public GeometryObject getMultiPoint(Object geomObj) throws SQLException;
	public GeometryObject getCurve(Object geomObj) throws SQLException;
	public GeometryObject getMultiCurve(Object geomObj) throws SQLException;
	public GeometryObject getPolygon(Object geomObj) throws SQLException;
	public GeometryObject getMultiPolygon(Object geomObj) throws SQLException;
	public GeometryObject getGeometry(Object geomObj) throws SQLException;
	public Object getDatabaseObject(GeometryObject geomObj, Connection connection) throws SQLException;
	public int getNullGeometryType();
	public String getNullGeometryTypeName();
}
