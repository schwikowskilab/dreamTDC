/*
  File: CyActivator.java

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

import org.cytoscape.application.swing.CySwingApplication;



import org.cytoscape.application.swing.CyAction;
import fr.systemsbiology.cyni.*;

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

