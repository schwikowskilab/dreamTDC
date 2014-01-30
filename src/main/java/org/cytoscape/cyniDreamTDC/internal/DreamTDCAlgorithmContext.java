package org.cytoscape.cyniDreamTDC.internal;

import java.io.File;
import java.io.IOException;
import java.util.*;

import org.cytoscape.cyni.CyniAlgorithmContext;
import org.cytoscape.model.CyTable;
import org.cytoscape.work.TunableValidator.ValidationState;
import org.cytoscape.work.util.*;
import org.apache.commons.lang3.StringUtils;

import org.cytoscape.work.Tunable;
import org.cytoscape.work.TunableValidator;

public class DreamTDCAlgorithmContext extends CyniAlgorithmContext implements TunableValidator {
	@Tunable(description="Prior Data Mode:",groups="Algorithm Prior Data Definition", xorChildren=true, gravity=1.0)
	public ListSingleSelection<String> mode = new ListSingleSelection<String>(MODE_DATABASE,MODE_OWN_DATA,MODE_NO_PRIOR_DATA);
	
	@Tunable(description="Select column containing HUGO Ids:",groups={"Algorithm Prior Data Definition","HUGO ID Definition"},gravity=2.0,xorKey="Use Pathway Commons database prior data")
	public ListSingleSelection<String> hugoColumn ;
	
	@Tunable(description="Zip File containing all networks:",groups={"Algorithm Prior Data Definition","Network Data Set Definition"},gravity=3.0,params="input=true",xorKey="Use your own set of prior network data")
	public File networkZipFile ;
	
	@Tunable(description="No network prior data will be used:",groups={"Algorithm Prior Data Definition","No Network Prior Data"},gravity=4.0,xorKey="Don't use prior data")
	public boolean noDatabase = true ;
	
	@Tunable(description="Data Attributes", groups="Sources for Network Inference",gravity=5.0,listenForChange = "DataFormat")
	public ListMultipleSelection<String> attributeList;
	
	public ListSingleSelection<String> dataFormat = new ListSingleSelection<String>(THREE_DIMENSIONS_FORMAT,TWO_DIMENSIONS_FORMAT);
	@Tunable(description="Data Columns Name Format", groups="Sources for Network Inference",gravity=6.0)
	public ListSingleSelection<String> getDataFormat()
	{
		return dataFormat;
	}
	
	public void setDataFormat(ListSingleSelection<String> format)
	{
		dataFormat = format;
		if(dataFormat.getSelectedValue().matches(TWO_DIMENSIONS_FORMAT))
			attributes = cols2dFormat;
		if(dataFormat.getSelectedValue().matches(THREE_DIMENSIONS_FORMAT))
			attributes = cols3dFormat;
		if(attributes.size() > 0)
		{
			attributeList.setPossibleValues(attributes);
			attributeList.setSelectedValues(attributeList.getPossibleValues());
		}
		else
		{
			attributeList = new  ListMultipleSelection<String>("No sources available");
		}
	}
	
	@Tunable(description="Network generation option", groups="Sources for Network Inference",gravity=7.0, dependsOn="DataFormat=Dimension1/Dimension2/Dimension3")
	public ListSingleSelection<String> outputOptions = new ListSingleSelection<String>(COMBINE_ALL,ONLY_ONE_NETWORK);
	
	
	
	private List<String> attributesHugo;
	private List<String> attributes;
	private List<String> cols2dFormat;
	private List<String> cols3dFormat;
	public static String MODE_DATABASE = "Use Pathway Commons database prior data";
	public static String MODE_OWN_DATA = "Use your own set of prior network data";
	public static String MODE_NO_PRIOR_DATA = "Don't use prior data";
	public static String TWO_DIMENSIONS_FORMAT = "Dimension1/Dimension2";
	public static String THREE_DIMENSIONS_FORMAT = "Dimension1/Dimension2/Dimension3";
	public static String COMBINE_ALL = "Generate network for each Dimension1 found";
	public static String ONLY_ONE_NETWORK = "Generate just one network";

	public DreamTDCAlgorithmContext(CyTable table ) {
		super(true);
		attributesHugo = getAllAttributesStrings(table);
		if(getAllAttributesLists(table).size() > 0)
			attributesHugo.addAll(getAllAttributesLists(table));
		if(attributesHugo.size() > 0)
		{
			hugoColumn = new ListSingleSelection<String>(attributesHugo);
			
		}
		else
		{
			hugoColumn = new  ListSingleSelection<String>("No sources available");
		}
		attributes = getAllAttributesNumbers(table);
		cols2dFormat =  new ArrayList<String>();
		cols3dFormat =  new ArrayList<String>();
		for(String col : attributes)
		{
			int num = StringUtils.countMatches(col, "/");
			if(num == 1)
				cols2dFormat.add(col);
			if(num == 2)
				cols3dFormat.add(col);
		}
		if(dataFormat.getSelectedValue().matches(TWO_DIMENSIONS_FORMAT))
			attributes = cols2dFormat;
		if(dataFormat.getSelectedValue().matches(THREE_DIMENSIONS_FORMAT))
			attributes = cols3dFormat;
		if(attributes.size() > 0)
		{
			attributeList = new  ListMultipleSelection<String>(attributes);
			attributeList.setSelectedValues(attributeList.getPossibleValues());
		}
		else
		{
			attributeList = new  ListMultipleSelection<String>("No sources available");
		}
		mode.setSelectedValue(MODE_DATABASE);
		
	}
	
	@Override
	public ValidationState getValidationState(final Appendable errMsg) {
		if (mode.getSelectedValue().matches(MODE_DATABASE) && hugoColumn.getSelectedValue().matches("No sources available") )
		{
			try {
				errMsg.append("There is no column with HUGO IDs");
			} catch (IOException e) {
				e.printStackTrace();
				return ValidationState.INVALID;
			}
			return ValidationState.INVALID;
		}
		
		if (mode.getSelectedValue().matches(MODE_OWN_DATA) && !networkZipFile.exists() )
		{
			try {
				errMsg.append("Zip file with set of networks needs to be specified");
			} catch (IOException e) {
				e.printStackTrace();
				return ValidationState.INVALID;
			}
			return ValidationState.INVALID;
		}
		
		if(attributeList.getPossibleValues().get(0).matches("No sources available") || attributeList.getSelectedValues().size() == 0) {
			try {
				errMsg.append("No sources selected to apply the algorithm or there are no available. Please, select sources from the list if available.");
			} catch (IOException e) {
				e.printStackTrace();
				return ValidationState.INVALID;
			}
			return ValidationState.INVALID;
			
		}
		
		return ValidationState.OK;
	}
}
