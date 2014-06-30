package fr.systemsbiology.cyniDreamTDC.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.zip.ZipInputStream;

import fr.systemsbiology.cyni.CyniAlgorithmContext;
import org.cytoscape.model.CyTable;
import org.cytoscape.work.TunableValidator.ValidationState;
import org.cytoscape.work.util.*;
import org.apache.commons.lang3.StringUtils;

import org.cytoscape.work.Tunable;
import org.cytoscape.work.TunableValidator;

public class DreamTDCAlgorithmContext extends CyniAlgorithmContext implements TunableValidator {
	
	@Tunable(description="Algorithm Mode:",groups="Algorithm Definition",  gravity=0.5)
	public ListSingleSelection<String> algoMode = new ListSingleSelection<String>(MODE_PRIOR_AND_DATA,MODE_NO_DATA_SET,MODE_NO_PRIOR_DATA);
	
	@Tunable(description="Prior Data Mode:",groups="Prior Data Definition", xorChildren=true,dependsOn="algoMode!=Do not use prior data", gravity=1.0)
	public ListSingleSelection<String> mode = new ListSingleSelection<String>(MODE_DATABASE,MODE_OWN_DATA);
	
	@Tunable(description="Select column containing HUGO Ids:",groups={"Prior Data Definition","HUGO ID Definition"},dependsOn="algoMode!=Do not use prior data",gravity=2.0,xorKey="Use Human Pathway Commons database prior data")
	public ListSingleSelection<String> hugoColumn1 ;
	
	@Tunable(description="Zip File containing all networks in sif format:",groups={"Prior Data Definition","Network Data Set Definition"},dependsOn="algoMode!=Do not use prior data",gravity=3.0,params="input=true",xorKey="Use your own set of prior network data")
	public File networkZipFile ;
	@Tunable(description="Select column containing the mapping Ids:",groups={"Prior Data Definition","Network Data Set Definition"},dependsOn="algoMode!=Do not use prior data",gravity=3.5,xorKey="Use your own set of prior network data")
	public ListSingleSelection<String> hugoColumn2 ;
	
	
	@Tunable(description="New edges type", groups="Network Output Options",gravity=4.5)
	public ListSingleSelection<String> edgesOptions = new ListSingleSelection<String>(DIRECTED,UNDIRECTED);
	
	@Tunable(description="Data Attributes", groups="Sources for Network Inference",dependsOn="algoMode!=Only use prior data",gravity=5.0,listenForChange = "DataFormat")
	public ListMultipleSelection<String> attributeList;
	
	public ListSingleSelection<String> dataFormat = new ListSingleSelection<String>(THREE_DIMENSIONS_FORMAT,TWO_DIMENSIONS_FORMAT);
	@Tunable(description="Data Columns Name Format", groups="Sources for Network Inference",dependsOn="algoMode!=Only use prior data",gravity=6.0)
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
	
	
	
	
	
	private List<String> attributesHugo;
	private List<String> attributes;
	private List<String> cols2dFormat;
	private List<String> cols3dFormat;
	public static String MODE_DATABASE = "Use Human Pathway Commons database prior data";
	public static String MODE_OWN_DATA = "Use your own set of prior network data";
	public static String MODE_NO_PRIOR_DATA = "Do not use prior data";
	public static String MODE_NO_DATA_SET = "Only use prior data";
	public static String MODE_PRIOR_AND_DATA = "Use prior data and input table data";
	public static String TWO_DIMENSIONS_FORMAT = "Stimulus/Time";
	public static String THREE_DIMENSIONS_FORMAT = "Network/Stimulus/Time";
	public static String UNDIRECTED = "Undirected Edges";
	public static String DIRECTED = "Directed Edges";

	public DreamTDCAlgorithmContext(CyTable table ) {
		super(true);
		attributesHugo = getAllAttributesStrings(table);
		if(getAllAttributesLists(table).size() > 0)
			attributesHugo.addAll(getAllAttributesLists(table));
		if(attributesHugo.size() > 0)
		{
			hugoColumn1 = new ListSingleSelection<String>(attributesHugo);
			hugoColumn2 = new ListSingleSelection<String>(attributesHugo);
			
		}
		else
		{
			hugoColumn1 = new  ListSingleSelection<String>("No sources available");
			hugoColumn2 = new  ListSingleSelection<String>("No sources available");
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
		if(!algoMode.getSelectedValue().matches(MODE_NO_PRIOR_DATA))
		{
			if (mode.getSelectedValue().matches(MODE_DATABASE) && hugoColumn1.getSelectedValue().matches("No sources available") )
			{
				try {
					errMsg.append("There is no column with HUGO IDs");
				} catch (IOException e) {
					e.printStackTrace();
					return ValidationState.INVALID;
				}
				return ValidationState.INVALID;
			}
			
			if (mode.getSelectedValue().matches(MODE_OWN_DATA) && hugoColumn2.getSelectedValue().matches("No sources available") )
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
					errMsg.append("Zip file doesn't seem to exist. Please specify the zip file containing the set of networks");
				} catch (IOException e) {
					e.printStackTrace();
					return ValidationState.INVALID;
				}
				return ValidationState.INVALID;
			}
			
			if (mode.getSelectedValue().matches(MODE_OWN_DATA) && !networkZipFile.getName().endsWith("zip") )
			{
				try {
					errMsg.append("The chosen file doesn't seem to be a zip file. Please select a zip file.");
				} catch (IOException e) {
					e.printStackTrace();
					return ValidationState.INVALID;
				}
				return ValidationState.INVALID;
			}
			if (mode.getSelectedValue().matches(MODE_OWN_DATA) )
			{
				
				try {
					ZipInputStream zis = new ZipInputStream(new FileInputStream(networkZipFile));
					zis.getNextEntry();
					if(!zis.getNextEntry().getName().endsWith(".sif"))
					{
						errMsg.append("The zip file doesn't contain sif files. Please select a zip file that contains networks in sif format and saved in a .sif file");
						return ValidationState.INVALID;
					}
				} catch (IOException e) {
					e.printStackTrace();
					return ValidationState.INVALID;
				}
				
			}
		}
		
		if(!algoMode.getSelectedValue().matches(MODE_NO_DATA_SET))
		{
			if(attributeList.getPossibleValues().get(0).matches("No sources available") || attributeList.getSelectedValues().size() == 0) {
				try {
					errMsg.append("No sources selected to apply the algorithm or there are no available. Please, select sources from the list if available.");
				} catch (IOException e) {
					e.printStackTrace();
					return ValidationState.INVALID;
				}
				return ValidationState.INVALID;
				
			}
		}
		
		return ValidationState.OK;
	}
}
