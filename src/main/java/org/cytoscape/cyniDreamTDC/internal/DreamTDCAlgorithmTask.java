/*
  File: EqualDiscretizationTask.java

  Copyright (c) 2006, 2010, The Cytoscape Consortium (www.cytoscape.org)

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



import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyNetworkTableManager;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.cyni.*;
import org.cytoscape.view.layout.CyLayoutAlgorithm;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.subnetwork.CyRootNetworkManager;
import org.cytoscape.property.CyProperty;
import org.cytoscape.view.vizmap.VisualMappingManager;





/**
 * The CyniSampleAlgorithmTask provides a simple example on how to create a cyni task
 */
public class DreamTDCAlgorithmTask extends AbstractCyniTask {
	
	private final CyTable mytable;
	private final List<String> attributeArray;
	private CyLayoutAlgorithmManager layoutManager;
	private CyniNetworkUtils netUtils;
	private String hugoColumn;
	private String mode,algoMode;
	private File file;
	private boolean nodeWithMultipleIds;
	private boolean threeDformat;
	private DreamPrior prior;
	private DreamGranger dreamGranger;
	private String filesPath ;
	private boolean edgesDirected;
	private static int iteration = 0;
	

	/**
	 * Creates a new object.
	 */
	public DreamTDCAlgorithmTask(final String name, final DreamTDCAlgorithmContext context, CyNetworkFactory networkFactory, CyNetworkViewFactory networkViewFactory,
			CyNetworkManager networkManager,CyNetworkTableManager netTableMgr, CyRootNetworkManager rootNetMgr, VisualMappingManager vmMgr,
			CyNetworkViewManager networkViewManager,CyLayoutAlgorithmManager layoutManager, 
			CyCyniMetricsManager metricsManager, CyTable selectedTable, String filesPath)
	{
		super(name, context,networkFactory,networkViewFactory,networkManager, networkViewManager,netTableMgr,rootNetMgr, vmMgr);
		
		this.mytable = selectedTable;
		this.filesPath = filesPath;
		this.mode = context.mode.getSelectedValue();
		this.algoMode = context.algoMode.getSelectedValue();
		if(mode.matches(DreamTDCAlgorithmContext.MODE_DATABASE))
			this.hugoColumn = context.hugoColumn1.getSelectedValue();
		else
			this.hugoColumn = context.hugoColumn2.getSelectedValue();
		this.attributeArray = context.attributeList.getSelectedValues();
		this.layoutManager = layoutManager;
		this.netUtils = new CyniNetworkUtils(networkViewFactory,networkManager,networkViewManager,netTableMgr,rootNetMgr,vmMgr);
		
		if(mytable.getColumn(hugoColumn).getType() == List.class)
			nodeWithMultipleIds = true;
		else
			nodeWithMultipleIds = false;
		if(context.dataFormat.getSelectedValue().matches(DreamTDCAlgorithmContext.THREE_DIMENSIONS_FORMAT))
			threeDformat = true;
		else
			threeDformat = false;
		
		if(context.edgesOptions.getSelectedValue().matches(DreamTDCAlgorithmContext.DIRECTED))
			edgesDirected = true;
		else
			edgesDirected = false;
		
		if(mode == DreamTDCAlgorithmContext.MODE_OWN_DATA)
			file = context.networkZipFile;
		iteration++;
	}

	/**
	 *  Perform actualtask.
	 */
	@Override
	final protected void doCyniTask(final TaskMonitor taskMonitor) {
		
		Double progress = 0.0d;
		CyNetwork networkSelected = null;
		String networkName;
		CyRow row;
		double step;
		CyLayoutAlgorithm layout;
		CyNetworkView newNetworkView ;
		CyTable  edgeTable;
		double priorData[][] = null;
		double grangerData[][] = null;
		Map<Integer,List<String>> indexHugoIdsMap = new HashMap<Integer,List<String>> ();
		Map<String,List<String>> twoDimensionsMap ;
		Map<String,List<String>> threeDimensionsMap = null ;
		Map<String,List<String>> mapDim1ToDim2 = new HashMap<String,List<String>> ();
		Map<String,double[][]> mapGrangertables = new HashMap<String,double[][]> ();
		Set<String> dimensions2 = new HashSet();
		Set<String> dimensions3 = new HashSet();
		ArrayList<String> dimension2List = new ArrayList<String>();
		ArrayList<String> dimension3List = new ArrayList<String>();
		DreamFileUtils fileUtils = null;
		
		//String queryTest = "ACACA ACACB AKT1 AKT1S1 AKT2 AKT3 BAD CDKN1B CHEK1 CHEK2 EGFR EIF4EBP1 ERBB2 ERBB3 ESR1 FOXO3 GSK3A GSK3B JUN MAP2K1 MAPK1 MAPK14 MAPK3 MAPK8 MET MTOR NDRG1 PDK1 PEA15 PRKAA1 PRKAA2 PRKCA PRKCB PRKCD PRKCE PRKCH PRKCQ RAF1 RB1 RELA RICTOR RPS6 RPS6KA1 RPS6KB1 SRC STAT3 WWTR1 YAP1 YBX1";
		//String str[]= queryTest.split(" ");
		List<String> patternList =new ArrayList<String>();
		List<String> priorFiles;
		Set<String> hugoIds = new HashSet<String>();
		
		taskMonitor.setTitle("Dream DC_TDC Algorithm");
		//patternList = Arrays.asList(str);
		
		if(!algoMode.matches(DreamTDCAlgorithmContext.MODE_NO_DATA_SET) )
		{
			if(threeDformat)
			{
				threeDimensionsMap = new HashMap<String,List<String>> ();
				String array[];
				for(String dimension : attributeArray)
				{
					
					array = dimension.split("/");
					if(array.length != 3)
						continue;
					if(mapDim1ToDim2.get(array[0]) == null)
					{
						mapDim1ToDim2.put(array[0], new ArrayList<String>());
						mapDim1ToDim2.get(array[0]).add(array[1]);
					}
					else
					{
						if(!mapDim1ToDim2.get(array[0]).contains(array[1]))
							mapDim1ToDim2.get(array[0]).add(array[1]);
					}
					dimensions3.add(array[2]);
					if(!dimension3List.contains(array[2]))
						dimension3List.add(array[2]);
					
					if(threeDimensionsMap.get(array[0]) == null)
					{
						ArrayList<String> listCols = new ArrayList<String>();
						listCols.add(dimension);
						threeDimensionsMap.put(array[0], listCols);
					}
					else
						threeDimensionsMap.get(array[0]).add(dimension);
					
				}
				System.out.println("times: " + dimension3List);
				//dimension2List.addAll(dimensions2);
				//dimension3List.addAll(dimensions3);
			}
			else
			{			
				twoDimensionsMap = new HashMap<String,List<String>> ();
				String array[];
				for(String dimension : attributeArray)
				{
					array = dimension.split("/");
					if(array.length != 2)
						continue;
					dimensions2.add(array[0]);
					dimensions3.add(array[1]);
					if(twoDimensionsMap.get(array[0]) == null)
					{
						ArrayList<String> listCols = new ArrayList<String>();
						listCols.add(dimension);
						twoDimensionsMap.put(array[0], listCols);
					}
					else
						twoDimensionsMap.get(array[0]).add(dimension);				
				}
				dimension2List.addAll(dimensions2);
				dimension3List.addAll(dimensions3);
			}
		}
		// Create the CyniTable
		CyniTable data = new CyniTable(mytable,attributeArray.toArray(new String[0]), false, false, selectedOnly);
		    
		if(!algoMode.matches(DreamTDCAlgorithmContext.MODE_NO_DATA_SET) )
		{
			if(data.hasAnyMissingValue())
			{
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						JOptionPane.showMessageDialog(null, "The data selected contains missing values.\n " +
								"Therefore, this algorithm can not proceed with these conditions.\n" +
								"Please, use one of the imputation data algorithms to estimate the missing values.", "Warning", JOptionPane.WARNING_MESSAGE);
					}
				});
				newNetwork.dispose();
				return;
			}
		
			dreamGranger = new DreamGranger(data, nThreads);
		}
		
		priorData = new double[data.nRows()][data.nRows()];
		
		if(algoMode.matches(DreamTDCAlgorithmContext.MODE_NO_DATA_SET) )
		{
			threeDformat = false;
			grangerData = new double[data.nRows()][data.nRows()];
		}
			
		if(!algoMode.matches(DreamTDCAlgorithmContext.MODE_NO_PRIOR_DATA) )
		{
			for(int i=0;i < data.nRows();i++)
			{
				if (cancelled)
					break;
				row = mytable.getRow(data.getRowLabel(i));
				if(nodeWithMultipleIds)
				{
					List<String> tempList = row.getList(hugoColumn,String.class);
					indexHugoIdsMap.put(i, tempList);
					for(String id : tempList)
						hugoIds.add(id);
				}
				else
				{
					String hugo = row.get(hugoColumn, String.class);
					indexHugoIdsMap.put(i, new ArrayList<String>());
					indexHugoIdsMap.get(i).add(hugo);
					hugoIds.add(hugo);
				}
			}
			
			if(mode.matches(DreamTDCAlgorithmContext.MODE_DATABASE))
	    		fileUtils = new DreamFileUtils(filesPath, null);
			if(mode.matches(DreamTDCAlgorithmContext.MODE_OWN_DATA) && file.exists())
	    		fileUtils = new DreamFileUtils(filesPath, file);
			
			patternList.addAll( hugoIds);
			if(fileUtils != null && !cancelled)
			{

				priorFiles = fileUtils.getFilesWithPatterns(patternList);
				
				System.out.println("number of files found: " + priorFiles.size());
				
				taskMonitor.setStatusMessage("Processing Prior Data ...");
				taskMonitor.setProgress(0.1);
				prior = new DreamPrior(fileUtils);
				priorData = prior.getPriorResults(priorFiles,patternList,indexHugoIdsMap, data);
			}
		}
		
		if(!algoMode.matches(DreamTDCAlgorithmContext.MODE_NO_DATA_SET) )
		{
			if(threeDformat)
			{
				double temp [][];
				int i =0;
				step = (0.6-0.1)/mapDim1ToDim2.size();
				for(String nameDimension : mapDim1ToDim2.keySet())
				{
					taskMonitor.setStatusMessage("Calculating Regression for " + nameDimension);
					taskMonitor.setProgress(0.1 + step*i);
					i++;
					if (cancelled)
						break;
					List<String> dim3List = getDimension3List(data,nameDimension, mapDim1ToDim2.get(nameDimension),dimension3List);
					temp = dreamGranger.getGrangerResults(nameDimension, mapDim1ToDim2.get(nameDimension), dim3List);
					if(temp != null)
						mapGrangertables.put(nameDimension, temp);
				}
				
			}
			else
			{
				taskMonitor.setStatusMessage("Calculating Regression on provided data ...");
				taskMonitor.setProgress(0.4);
				grangerData = dreamGranger.getGrangerResults(null, dimension2List, dimension3List);
			}
		}
		
		if(cancelled)
			return;
		//Check if a network is associated to the selected table
		networkSelected = netUtils.getNetworkAssociatedToTable(mytable);
		
		
		if(threeDformat)
		{
			taskMonitor.setStatusMessage("Generating networks ...");
			taskMonitor.setProgress(0.7);
			
			for(String names : mapGrangertables.keySet())
			{
				if(mapGrangertables.get(names) == null)
					break;
				//Create new network
				CyNetwork newNetwork = netFactory.createNetwork();
		
				
				//Set the name of the network, another name could be chosen
				networkName = "Cyni Dream DC_TDC " + names + " "+ iteration;
				if (newNetwork != null && networkName != null) {
					CyRow netRow = newNetwork.getRow(newNetwork);
					netRow.set(CyNetwork.NAME, networkName);
				}
				edgeTable = newNetwork.getDefaultEdgeTable();
				edgeTable.createColumn("Edge Score", Double.class, false);	
				netUtils.addColumns(networkSelected,newNetwork,mytable,CyNode.class, CyNetwork.LOCAL_ATTRS);
				
				fillNetwork(newNetwork,priorData,mapGrangertables.get(names), data, threeDimensionsMap.get(names),edgesDirected);
				
				//Display the new network
				if (!cancelled)
				{
					newNetworkView = netUtils.displayNewNetwork(newNetwork, networkSelected,edgesDirected);
					layout = layoutManager.getDefaultLayout();
					Object context = layout.getDefaultLayoutContext();
					insertTasksAfterCurrentTask(layout.createTaskIterator(newNetworkView, context, CyLayoutAlgorithm.ALL_NODE_VIEWS,""));
				}
			}
		}
		else
		{
			taskMonitor.setStatusMessage("Generating network ...");
			taskMonitor.setProgress(0.7);
			//Create new network
			CyNetwork newNetwork = netFactory.createNetwork();
	
			
			//Set the name of the network, another name could be chosen
			networkName = "Cyni Dream DC_TDC " + iteration;
			if (newNetwork != null && networkName != null) {
				CyRow netRow = newNetwork.getRow(newNetwork);
				netRow.set(CyNetwork.NAME, networkName);
			}
			
			edgeTable = newNetwork.getDefaultEdgeTable();
			edgeTable.createColumn("Edge Score", Double.class, false);	
			netUtils.addColumns(networkSelected,newNetwork,mytable,CyNode.class, CyNetwork.LOCAL_ATTRS);
			
			fillNetwork(newNetwork,priorData,grangerData, data, null,edgesDirected);
			
			//Display the new network
			if (!cancelled)
			{
				newNetworkView = netUtils.displayNewNetwork(newNetwork, networkSelected,edgesDirected);
				taskMonitor.setProgress(1.0d);
				layout = layoutManager.getDefaultLayout();
				Object context = layout.getDefaultLayoutContext();
				insertTasksAfterCurrentTask(layout.createTaskIterator(newNetworkView, context, CyLayoutAlgorithm.ALL_NODE_VIEWS,""));
			}
			
		}
		
		taskMonitor.setProgress(1.0d);
	}
	
	private List<String> getDimension3List(CyniTable table, String dimension1, List<String> dimensions2, List<String> dimensions3)
	{
		ArrayList<String> finalList = new ArrayList<String>();
		ArrayList<String> removeList = new ArrayList<String>();
		String colName;
		
		finalList.addAll(dimensions3);
		
		for(String dim3 : dimensions3)
		{
			for(String dim2 : dimensions2)
			{
				if(dimension1 != null)
					colName = dimension1 + "/";
				else
					colName="";
				colName +=  dim2 + "/" + dim3;
				if(table.getColIndex(colName) == -1)
				{
					removeList.add(dim3);
					break;
				}
			}
		}
		
		finalList.removeAll(removeList);
		
		return finalList;
	}
	
	private void fillNetwork(CyNetwork network, double[][] table1, double[][] table2, CyniTable data, List<String> colsToKeep, boolean directed)
	{
		CyNode mapRowNodes[];
		CyNode node1, node2;
		CyEdge edge;
		double score = 0.0;
		boolean addEdge = false;
		int j=0;
		int numNodes = 0;
		int nRows = data.nRows();
		CyNetwork networkSelected = netUtils.getNetworkAssociatedToTable(mytable);
		
		mapRowNodes = new CyNode[nRows];
		
		for (int i = 0; i < nRows; i++) 
		{
			if (cancelled)
				break;

			if(directed)
				j= 0;
			else
				j = i+1;
			for (; j < nRows; j++) 
			{
				if (cancelled)
					break;
				if(i == j)
					continue;
				score = (table1[i][j] + table2[i][j])/2.0;
				
				if(!directed && score == 0.0)
				{
					score = (table1[j][i] + table2[j][i])/2.0;
				}
				
				if(Math.abs(score) > 0.0)
				{
		
					if(mapRowNodes[i] == null)
					{
						node1 = network.addNode();
						netUtils.cloneNodeRow(network,mytable.getRow(data.getRowLabel(i)), node1);
						if(network.getRow(node1).get(CyNetwork.NAME,String.class ) == null || network.getRow(node1).get(CyNetwork.NAME,String.class ).isEmpty() == true)
						{
							if(mytable.getPrimaryKey().getType().equals(String.class) && networkSelected == null)
								network.getRow(node1).set(CyNetwork.NAME,mytable.getRow(data.getRowLabel(i)).get(mytable.getPrimaryKey().getName(),String.class));
							else
								network.getRow(node1).set(CyNetwork.NAME, "Node " + numNodes);
						}
						if(network.getRow(node1).get(CyNetwork.SELECTED,Boolean.class ) == true)
							network.getRow(node1).set(CyNetwork.SELECTED, false);
						mapRowNodes[i] =node1;
						numNodes++;
					}
					if(mapRowNodes[j] == null)
					{
						node2 = network.addNode();
						netUtils.cloneNodeRow(network,mytable.getRow(data.getRowLabel(j)),node2);
						if(network.getRow(node2).get(CyNetwork.NAME,String.class ) == null || network.getRow(node2).get(CyNetwork.NAME,String.class ).isEmpty() == true)
						{
							if(mytable.getPrimaryKey().getType().equals(String.class) && networkSelected == null)
								network.getRow(node2).set(CyNetwork.NAME,mytable.getRow(data.getRowLabel(j)).get(mytable.getPrimaryKey().getName(),String.class));
							else
								network.getRow(node2).set(CyNetwork.NAME, "Node " + numNodes);
						}
						if(network.getRow(node2).get(CyNetwork.SELECTED,Boolean.class ) == true)
							network.getRow(node2).set(CyNetwork.SELECTED, false);
						mapRowNodes[j] = node2;
						numNodes++;
					}
								
					
						
					edge = network.addEdge(mapRowNodes[i], mapRowNodes[j], directed);
					network.getRow(edge).set("Edge Score",score);
					//network.getRow(edge).set(CyEdge.INTERACTION,((Double)matrix.getScore(i, j)).toString());
					network.getRow(edge).set("name", network.getRow(mapRowNodes[i]).get("name", String.class)
							+ " (DC_TDC) " + network.getRow( mapRowNodes[j]).get("name", String.class));
					
				}
			}
		}
		
		if(colsToKeep != null)
		{
			ArrayList<String> listCols = new ArrayList<String>();
			CyTable nodeTable = network.getDefaultNodeTable();
			
			listCols.addAll(attributeArray);
			listCols.removeAll(colsToKeep);
			
			for(String colName : listCols)
				nodeTable.deleteColumn(colName);
						
		}
	}
	
	@Override
	public void cancel() {
		cancelled = true;
		if(dreamGranger != null)
			dreamGranger.setCancel();
		if(prior != null)
			prior.setCancel();
	}
}
