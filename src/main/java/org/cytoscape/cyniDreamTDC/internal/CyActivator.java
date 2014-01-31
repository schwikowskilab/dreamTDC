package org.cytoscape.cyniDreamTDC.internal;

import org.cytoscape.application.swing.CySwingApplication;



import org.cytoscape.application.swing.CyAction;
import org.cytoscape.cyni.*;

import org.osgi.framework.BundleContext;

import org.cytoscape.property.CyProperty;
import org.cytoscape.service.util.AbstractCyActivator;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import org.apache.commons.io.FileUtils;


public class CyActivator extends AbstractCyActivator {
	
	public static final String DEFAULT_CONFIG_DIR = CyProperty.DEFAULT_PROPS_CONFIG_DIR ;
	
	private static final String DEF_USER_DIR = System.getProperty("user.home");
	private String filesPath = join(File.separator, DEF_USER_DIR, DEFAULT_CONFIG_DIR, "3", "dreamTDC/");
	
	public CyActivator() {
		super();
	}


	public void start(BundleContext bc) {

		//Define new Cyni Algorithm
		DreamTDCAlgorithm test = new DreamTDCAlgorithm(filesPath);
		//Register new Cyni Algorithm
		registerService(bc,test,CyCyniAlgorithm.class, new Properties());

		

	}
	@Override
	public void shutDown() {
		File testFile = new File(filesPath);
		try{
	    	if(testFile.exists())
	    		FileUtils.deleteDirectory(testFile);
		}catch (IOException e) {
			
		}
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

