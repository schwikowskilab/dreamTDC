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

import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyNetworkTableManager;
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
	private boolean nodeWithMultipleIds;
	
	public static final String DEFAULT_CONFIG_DIR = CyProperty.DEFAULT_PROPS_CONFIG_DIR ;
	
	private static final String DEF_USER_DIR = System.getProperty("user.home");
	
	private String filesPath = join(File.separator, DEF_USER_DIR, DEFAULT_CONFIG_DIR, "3", "dreamTDC/");

	/**
	 * Creates a new object.
	 */
	public DreamTDCAlgorithmTask(final String name, final DreamTDCAlgorithmContext context, CyNetworkFactory networkFactory, CyNetworkViewFactory networkViewFactory,
			CyNetworkManager networkManager,CyNetworkTableManager netTableMgr, CyRootNetworkManager rootNetMgr, VisualMappingManager vmMgr,
			CyNetworkViewManager networkViewManager,CyLayoutAlgorithmManager layoutManager, 
			CyCyniMetricsManager metricsManager, CyTable selectedTable)
	{
		super(name, context,networkFactory,networkViewFactory,networkManager, networkViewManager,netTableMgr,rootNetMgr, vmMgr);
		
		this.mytable = selectedTable;
		this.hugoColumn = context.hugoColumn.getSelectedValue();
		this.attributeArray = context.attributeList.getSelectedValues();
		this.layoutManager = layoutManager;
		this.netUtils = new CyniNetworkUtils(networkViewFactory,networkManager,networkViewManager,netTableMgr,rootNetMgr,vmMgr);
		
		if(mytable.getColumn(hugoColumn).getType() == List.class)
			nodeWithMultipleIds = true;
		else
			nodeWithMultipleIds = false;
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
		CyLayoutAlgorithm layout;
		CyNetworkView newNetworkView ;
		Map<Integer,List<String>> indexHugoIdsMap = new HashMap<Integer,List<String>> ();
		
		String queryTest = "ACACA ACACB AKT1 AKT1S1 AKT2 AKT3 BAD CDKN1B CHEK1 CHEK2 EGFR EIF4EBP1 ERBB2 ERBB3 ESR1 FOXO3 GSK3A GSK3B JUN MAP2K1 MAPK1 MAPK14 MAPK3 MAPK8 MET MTOR NDRG1 PDK1 PEA15 PRKAA1 PRKAA2 PRKCA PRKCB PRKCD PRKCE PRKCH PRKCQ RAF1 RB1 RELA RICTOR RPS6 RPS6KA1 RPS6KB1 SRC STAT3 WWTR1 YAP1 YBX1";
		String str[]= queryTest.split(" ");
		List<String> patternList = Arrays.asList(str);
		List<String> priorFiles;
		Set<String> hugoIds = new HashSet<String>();
		
		// Create the CyniTable
	    CyniTable data = new CyniTable(mytable,attributeArray.toArray(new String[0]), false, false, selectedOnly);
		
		DreamFileUtils fileUtils = new DreamFileUtils(filesPath);
		
		for(int i=0;i < data.nRows();i++)
		{
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
			
		//patternList.addAll( hugoIds);
		priorFiles = fileUtils.getFilesWithPatterns(patternList);
		
		System.out.println("number of files found: " + priorFiles.size());
		
		DreamPrior prior = new DreamPrior(fileUtils);
		prior.getPriorResults(priorFiles,patternList,indexHugoIdsMap);
        //step = 1.0 /  attributeArray.size();
        
        taskMonitor.setStatusMessage("Algorithm running ...");
		taskMonitor.setProgress(progress);
		
		//Create new network
		CyNetwork newNetwork = netFactory.createNetwork();
		
		
		//Check if a network is associated to the selected table
		networkSelected = netUtils.getNetworkAssociatedToTable(mytable);
		
		
		
		
		//Set the name of the network, another name could be chosen
		networkName = "Cyni Dream DC_TDC " + newNetwork.getSUID();
		if (newNetwork != null && networkName != null) {
			CyRow netRow = newNetwork.getRow(newNetwork);
			netRow.set(CyNetwork.NAME, networkName);
		}
		
		
		/*****************************************************/
		//
		// Add the different nodes and edges according to the table data
	    //
		//
		/*****************************************************/
		
		//Display the new network
		if (!cancelled)
		{
			newNetworkView = netUtils.displayNewNetwork(newNetwork, networkSelected,false);
			taskMonitor.setProgress(1.0d);
			layout = layoutManager.getDefaultLayout();
			Object context = layout.getDefaultLayoutContext();
			insertTasksAfterCurrentTask(layout.createTaskIterator(newNetworkView, context, CyLayoutAlgorithm.ALL_NODE_VIEWS,""));
		}
		
		taskMonitor.setProgress(1.0d);
	}
	
	private static String join(String separator, String... parts) {
		StringBuilder builder = new StringBuilder();
		boolean isFirst = true;
		for (String part : parts) {
			if (!isFirst) {
				builder.append(separator);
			} else {
				isFirst = false;
			}
			builder.append(part);
		}
		return builder.toString();
	}
	
	
}
