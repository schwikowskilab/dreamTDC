package org.cytoscape.cyniDreamTDC.internal;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.cytoscape.cyni.CyniAlgorithmContext;
import org.cytoscape.model.CyTable;
import org.cytoscape.work.util.*;

import org.cytoscape.work.Tunable;
import org.cytoscape.work.TunableValidator;

public class DreamTDCAlgorithmContext extends CyniAlgorithmContext implements TunableValidator {
	@Tunable(description="P Mode:",groups="Algorithm Prior Data Definition", xorChildren=true)
	public ListSingleSelection<String> mode = new ListSingleSelection<String>(MODE_DATABASE,MODE_OWN_DATA,MODE_NO_PRIOR_DATA);
	
	@Tunable(description="Select column containing HUGO Ids:",groups={"Algorithm Prior Data Definition","HUGO ID Definition"},xorKey="Use Pathway Commons database prior data")
	public ListSingleSelection<String> hugoColumn ;
	
	@Tunable(description="Zip File containing all networks:",groups={"Algorithm Prior Data Definition","Network Data Set Definition"},params="input=true",xorKey="Use your own set of prior network data")
	public File networkZipFile ;
	
	@Tunable(description="No network prior data will be used:",groups={"Algorithm Prior Data Definition","No Network Prior Data"},xorKey="Don't use prior data")
	public boolean noDatabase = true ;
	
	
	private List<String> attributes;
	public static String MODE_DATABASE = "Use Pathway Commons database prior data";
	public static String MODE_OWN_DATA = "Use your own set of prior network data";
	public static String MODE_NO_PRIOR_DATA = "Don't use prior data";

	public DreamTDCAlgorithmContext(CyTable table ) {
		super(true);
		attributes = getAllAttributesStrings(table);
		if(getAllAttributesLists(table).size() > 0)
			attributes.addAll(getAllAttributesLists(table));
		if(attributes.size() > 0)
		{
			hugoColumn = new ListSingleSelection<String>(attributes);
			
		}
		else
		{
			hugoColumn = new  ListSingleSelection<String>("No sources available");
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
		
		return ValidationState.OK;
	}
}
