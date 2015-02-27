/*
  File: DreamPrior.java

  Copyright (c) 2010-2015, The Cytoscape Consortium (www.cytoscape.org)

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
package fr.systemsbiology.cyniDreamTDC.internal;


import java.util.*;
import java.io.*;
import java.util.zip.*;
import org.jblas.DoubleMatrix;
import org.jblas.MatrixFunctions;
import org.apache.commons.io.IOUtils;
import cern.colt.matrix.tdouble.impl.*;
import fr.systemsbiology.cyni.*;



/**
 * The BasicInduction provides a very simple Induction, suitable as
 * the default Induction for Cytoscape data readers.
 */
public class DreamPrior {
	
	
	private DreamFileUtils fileUtils;
	private boolean cancelled;
	/**
	 * Creates a new EqualDiscretization object.
	 */
	public DreamPrior(DreamFileUtils fileUtils) {
		
		this.fileUtils = fileUtils;
		cancelled = false;
	}
	
	public double[][] getPriorResults(List<String> listFiles,List<String> listStringIds, Map<Integer,List<String>> indexHugoIds , CyniTable table) 
	{
		String textFile;
		File file;
		Set<String> nodes = new HashSet<String>();
		Map<String,Map<String,Integer>> edges = new HashMap<String,Map<String,Integer>>();
		Map<String,Integer> degrees = new HashMap<String,Integer>();
		Map<String,Set<String>> nodesMap = new HashMap<String,Set<String>>();
		String nodesIds[] ;
		ArrayList<Integer> listIds = new ArrayList<Integer>();
		SparseDoubleMatrix2D data , prior, finalData;
		DoubleMatrix tempData ;
		int counts[][];
		double sum;
		
		prior = new SparseDoubleMatrix2D(listStringIds.size(),listStringIds.size());
		finalData = new SparseDoubleMatrix2D(indexHugoIds.size(),indexHugoIds.size());
		counts = new int [listStringIds.size()][listStringIds.size()];
		
		
		nodes.clear();
		edges.clear();
		listIds.clear();
		degrees.clear();
			
		createNetworkMap(listFiles,nodesMap);
		//System.out.println("Done with create network map");
		parseNetwork(nodes,edges,degrees, nodesMap);
		//System.out.println("nodes found: " + nodes.size() + " edges: " + edges.size());
		if(nodes.size() == 0)
			return finalData.toArray();
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
		//System.out.println("prior point 1");
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
		//System.out.println("prior point 2");
		tempData = new DoubleMatrix(data.toArray());
		tempData.muli(-0.1);
		data.assign(MatrixFunctions.expm(tempData).toArray2());
		//writeData(file,data,nodesIds);
		//System.out.println("prior point 3");
		if(listIds.size() >0)
		{
			sum = 0.0;
			for(Integer id1 : listIds)
			{
				for(Integer id2 : listIds)
				{
					if(id1 != id2)
						sum += data.getQuick(id1, id2);
				}
			}
			if(sum != 0.0)
			{
				for(Integer id1 : listIds)
				{
					for(Integer id2 : listIds)
					{
						if(id1 == id2)
							prior.setQuick(listStringIds.indexOf(nodesIds[id1]), listStringIds.indexOf(nodesIds[id2]), 0);
						else
						{
							int index1 = listStringIds.indexOf(nodesIds[id1]);
							int index2 = listStringIds.indexOf(nodesIds[id2]);
							double temp = prior.getQuick(index1, index2);
							counts[index1][index2]++;
							prior.setQuick(index1, index2, temp + data.getQuick(id1, id2));
							//prior.setQuick(listStringIds.indexOf(nodesIds[id1]), listStringIds.indexOf(nodesIds[id2]), data.getQuick(id1, id2));
						}
							
					}
				}
			}
		}

			//System.out.println("prior point 4");
		for(int i=0;i< prior.rows();i++)
			for(int j=0;j<prior.columns();j++)
				if(counts[i][j] != 0)
					prior.setQuick(i,j,prior.getQuick(i, j)/counts[i][j]);
		
		if(prior.getMaxLocation()[0] != 0)
		{
			//tempData = new DoubleMatrix(prior.toArray());
			double max = prior.getMaxLocation()[0];
			for(int i=0;i<prior.rows();i++)
				for(int j=0 ;j< prior.columns();j++)
					prior.setQuick(i, j, prior.getQuick(i, j)/max);
			//tempData.divi(prior.getMaxLocation()[0]);
			//prior.assign(tempData.toArray2());
		}
		//System.out.println("prior point 5");
		for(int i= 0;i< indexHugoIds.size();i++)
		{
			for(int j= 0;j< indexHugoIds.size();j++)
			{
				if(cancelled)
					break;
				sum = 0.0;
				if(indexHugoIds.get(i).size() == 0 || indexHugoIds.get(j).size() ==0)
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
			double max ;
			tempData = new DoubleMatrix(finalData.toArray());
			tempData = MatrixFunctions.abs(tempData);
			max = tempData.max();
			tempData.divi(max);
			finalData.assign(tempData.toArray2());
		}
		
		/*nodesIds = new String[400];
		for(int i =0;i<table.nRows();i++)
			nodesIds[i] = (String)table.getRowLabel(i);
		file = new File("/home/oguitart/temp/priorData.txt");
		writeData(file,finalData,nodesIds);*/
		return finalData.toArray();
	}
	
	private void createNetworkMap(List<String> listFiles,Map<String,Set<String>> nodesMap)
	{
		File file;
		String textFile;
		boolean sifFile;
		String networkCols[];
		String line;
		String source, target;
		Set<String> option1,option2;
		
		Collections.sort(listFiles);
		for(String fileName : listFiles)
		{
			file = new File(fileName);
			
			if(cancelled)
				break;
			
			textFile = fileUtils.getTextFile(file);
			
			if(fileName.endsWith(".sif"))
				sifFile = true;
			else
				sifFile = false;
			
			Scanner scanner = new Scanner(textFile);
			while (scanner.hasNextLine()) {
			    line = scanner.nextLine();
			    networkCols = line.split("\t");
			    
			    if(networkCols.length != 3)
			    	continue;
			    
			    if(sifFile)
			    {
			    	source = networkCols[0];
			    	target = networkCols[2];
			    }
			    else
			    {
			    	source = networkCols[0];
			    	target = networkCols[1];
			    }
			    
			    option1 = nodesMap.get(source);
			    option2 = nodesMap.get(target);
			    if(option1 != null)
			    {
			    	if(option1.contains(target))
			    		continue;
			    }
			    if(option2 != null)
			    {
			    	if(option2.contains(source))
			    		continue;
			    }
			    
			    if(option1 != null)
			    	option1.add(target);
			    else	
			    {
			    	option1 = new HashSet<String>();
			    	option1.add(target);
			    	nodesMap.put(source, option1);
			    }
			}
			
		}
	}
	
	private void parseNetwork(Set<String> nodes, Map<String,Map<String,Integer>> edges,Map<String,Integer> degree, Map<String,Set<String>> nodesMap)
	{
		String networkCols[];
		String line;
		String source;
		
		for(Map.Entry<String,Set<String>> entry : nodesMap.entrySet())
		{
			source = entry.getKey();
			
			for(String target : entry.getValue())
			{
			//target = entry.getValue();
		    
			    if(edges.get(source) == null)
			    	edges.put(source,new HashMap<String,Integer>());
			    if(edges.get(target) == null)
			    	edges.put(target,new HashMap<String,Integer>());
			    if(edges.get(source) != null && edges.get(source).get(target) != null)
			    	continue;
			    
			    nodes.add(source);
			    nodes.add(target);
			    edges.get(source).put(target,1);
			    edges.get(target).put( source,1);
			    
			    if(degree.get(source) == null)
			    	degree.put(source, 1);
			    else
			    	degree.put(source, degree.get(source)+1);
			    if(degree.get(target) == null)
			    	degree.put(target, 1);
			    else
			    	degree.put(target, degree.get(target)+1);
				}
		}
	}
	
	private void writeData(File file,SparseDoubleMatrix2D data, String[] ids)
	{
		File newFile = new File(file.getPath()+ ".data");
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
	
	public void setCancel()
	{
		cancelled = true;
	}
	
}
