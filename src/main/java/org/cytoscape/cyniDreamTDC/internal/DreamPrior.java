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
import cern.colt.matrix.tdouble.impl.*;



/**
 * The BasicInduction provides a very simple Induction, suitable as
 * the default Induction for Cytoscape data readers.
 */
public class DreamPrior {
	
	
	private DreamFileUtils fileUtils;
	/**
	 * Creates a new EqualDiscretization object.
	 */
	public DreamPrior(DreamFileUtils fileUtils) {
		
		this.fileUtils = fileUtils;
	}
	
	public double[][] getPriorResults(List<String> listFiles,List<String> listStringIds, Map<Integer,List<String>> indexHugoIds ) 
	{
		String textFile;
		File file;
		Set<String> nodes = new HashSet<String>();
		Map<String,Map<String,Integer>> edges = new HashMap<String,Map<String,Integer>>();
		Map<String,Integer> degrees = new HashMap<String,Integer>();
		String nodesIds[];
		ArrayList<Integer> listIds = new ArrayList<Integer>();
		SparseDoubleMatrix2D data , prior, finalData;
		DoubleMatrix tempData ;
		double sum;
		
		prior = new SparseDoubleMatrix2D(listStringIds.size(),listStringIds.size());
		finalData = new SparseDoubleMatrix2D(indexHugoIds.size(),indexHugoIds.size());
		
		for(String fileName : listFiles)
		{
			file = new File(fileName);
			
			textFile = fileUtils.getTextFile(file);
			nodes.clear();
			edges.clear();
			listIds.clear();
			degrees.clear();
			parseNetwork(nodes,edges,degrees, textFile);
			nodesIds =  new String[nodes.size()];
			nodes.toArray(nodesIds);
			data = new SparseDoubleMatrix2D(nodes.size(),nodes.size());
			
			Arrays.sort(nodesIds);
			
			for(int i=0;i< nodesIds.length;i++)
			{
				data.setQuick(i, i,  degrees.get(nodesIds[i]).doubleValue());
				if(listStringIds.contains(nodesIds[i]))
					listIds.add(i);
					
			}
			for(int i=0;i< nodesIds.length;i++)
			{
				for(int j=(i+1);j< nodesIds.length;j++)
				{
					if(j==i)
						continue;
					if(edges.get(nodesIds[i]).get(nodesIds[j]) != null)
					{
						data.setQuick(i, j,-1.0);
						data.setQuick(j, i,-1.0);
					}
				}
			}
			tempData = new DoubleMatrix(data.toArray());
			tempData.muli(-0.1);
			data.assign(MatrixFunctions.expm(tempData).toArray2());
			//writeData(file,data,nodesIds);
			if(listIds.size() >0)
			{
				for(Integer id1 : listIds)
				{
					for(Integer id2 : listIds)
					{
						prior.setQuick(listStringIds.indexOf(nodesIds[id1]), listStringIds.indexOf(nodesIds[id2]), data.getQuick(id1, id2));
					}
				}
			}
		}
		
		if(prior.getMaxLocation()[0] != 0)
		{
			tempData = new DoubleMatrix(prior.toArray());
			tempData.divi(prior.getMaxLocation()[0]);
			prior.assign(tempData.toArray2());
		}
		
		for(int i= 0;i< indexHugoIds.size();i++)
		{
			for(int j= 0;j< indexHugoIds.size();j++)
			{
				sum = 0.0;
				if(indexHugoIds.get(i).size() == 0 || indexHugoIds.get(i).size() ==0)
					continue;
				for(String hugoId1 : indexHugoIds.get(i))
				{
					for(String hugoId2 : indexHugoIds.get(j))
					{
						sum += prior.getQuick(listStringIds.indexOf(hugoId1), listStringIds.indexOf(hugoId2));
					}
					
				}
				sum = sum / (indexHugoIds.get(i).size()*indexHugoIds.get(j).size());
				finalData.setQuick(i, j, sum);
			}
		}
			
		if(finalData.getMaxLocation()[0] != 0)
		{
			tempData = new DoubleMatrix(finalData.toArray());
			tempData.divi(finalData.getMaxLocation()[0]);
			finalData.assign(tempData.toArray2());
		}
		
		return finalData.toArray();
	}
	
	private void parseNetwork(Set<String> nodes, Map<String,Map<String,Integer>> edges,Map<String,Integer> degree, String network )
	{
		String networkCols[];
		String line;
		
		
		Scanner scanner = new Scanner(network);
		while (scanner.hasNextLine()) {
		    line = scanner.nextLine();
		    networkCols = line.split("\t");
		    
		    if(networkCols.length != 3)
		    	continue;
		    
		    if(edges.get(networkCols[0]) == null)
		    	edges.put(networkCols[0],new HashMap<String,Integer>());
		    if(edges.get(networkCols[1]) == null)
		    	edges.put(networkCols[1],new HashMap<String,Integer>());
		    if(edges.get(networkCols[0]) != null && edges.get(networkCols[0]).get(networkCols[1]) != null)
		    	continue;
		    
		    nodes.add(networkCols[0]);
		    nodes.add(networkCols[1]);
		    edges.get(networkCols[0]).put( networkCols[1],1);
		    edges.get(networkCols[1]).put( networkCols[0],1);
		    
		    if(degree.get(networkCols[0]) == null)
		    	degree.put(networkCols[0], 1);
		    else
		    	degree.put(networkCols[0], degree.get(networkCols[0])+1);
		    if(degree.get(networkCols[1]) == null)
		    	degree.put(networkCols[1], 1);
		    else
		    	degree.put(networkCols[1], degree.get(networkCols[1])+1);
		}
	}
	
	private void writeData(File file,SparseDoubleMatrix2D data, String[] ids)
	{
		File newFile = new File(file.getPath()+ ".pid");
		FileWriter fw;
		String line = "";
		
		try{
			fw = new FileWriter(newFile);
			for(int i = -1 ; i< data.rows();i++)
			{
				if(i == -1)
					line = "Key\t";
				else
					line = ids[i] + "\t";
				for(int j=0; j< data.columns();j++)
				{
					if(i == -1 )
					{
						line += ids[j];
						line += "\t";
					}
					else
					{
						line += data.getQuick(i, j);
						line += "\t";
					}
				}
				line += "\n";
				fw.write(line);
			}
			
			fw.flush();
			fw.close();
		} catch (IOException ex) {
    		
    		System.out.println("IOException " + newFile.getName());
    	}
	}
	
	
}
