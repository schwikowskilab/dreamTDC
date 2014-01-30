/*
  File: CyniSampleAlgorithm.java

  Copyright (c) 2010, The Cytoscape Consortium (www.cytoscape.org)

  This library is free software; you can redistribute it and/or modify it
  under the terms of the GNU Lesser General Public License as published
  by the Free Software Foundation; either version 2.1 of the License, or
  any later version.

  This library is distributed in the hope that it will be useful, but
  WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
  MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  The software and
  documentation provided hereunder is on an "as is" basis, and the
  Institute for Systems Biology and the Whitehead Institute
  have no obligations to provide maintenance, support,
  updates, enhancements or modifications.  In no event shall the
  Institute for Systems Biology and the Whitehead Institute
  be liable to any party for direct, indirect, special,
  incidental or consequential damages, including lost profits, arising
  out of the use of this software and its documentation, even if the
  Institute for Systems Biology and the Whitehead Institute
  have been advised of the possibility of such damage.  See
  the GNU Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General Public License
  along with this library; if not, write to the Free Software Foundation,
  Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
*/
package org.cytoscape.cyniDreamTDC.internal;


import java.util.*;
import java.io.*;
import java.util.zip.*;
import org.jblas.DoubleMatrix;
import org.jblas.MatrixFunctions;
import org.apache.commons.io.IOUtils;
import org.cytoscape.model.CyTable;
import org.cytoscape.cyni.*;

import cern.colt.matrix.tdouble.impl.*;
import cern.colt.matrix.tobject.impl.DenseObjectMatrix2D;



/**
 * The BasicInduction provides a very simple Induction, suitable as
 * the default Induction for Cytoscape data readers.
 */
public class DreamTDCTable  {
	
	double[][] doubleData;
	Map<String,Integer> mapColumnNames;
	
	/**
	 * Creates a new EqualDiscretization object.
	 */
	public DreamTDCTable(CyniTable table, String dimension1, List<String> dimension2, String dimension3) {
		
		String colName;
		doubleData = new double[table.nRows()][dimension2.size()];
		mapColumnNames = new HashMap<String,Integer>();
		if(dimension1 != null)
			colName = dimension1 + "/";
		else
			colName="";
		for(int i=0; i<table.nRows();i++)
		{
			double[] temp = new double[dimension2.size()];
			for(int j=0; j<dimension2.size();j++)
			{
				//System.out.println(colName + dimension2.get(j) + "/" + dimension3);
				temp[j] = table.doubleValue(i, table.getColIndex(colName + dimension2.get(j) + "/" + dimension3));
				//System.out.println("value: " + temp[j]);
			}
			doubleData[i] = temp;
		}
	}
	
	public double[][] getMatrix()
	{
		return doubleData;
	}
	
	public double[] get1DRow(int row)
	{
		if(doubleData[row] == null)
			System.out.println("DreamTDCTable null row  " + row );
		return doubleData[row];
	}
	
	public double[] get1DRow(String id)
	{
		return doubleData[mapColumnNames.get(id)];
	}
	
	
	
	
	
}
