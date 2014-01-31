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
import org.apache.commons.io.IOUtils;



/**
 * The BasicInduction provides a very simple Induction, suitable as
 * the default Induction for Cytoscape data readers.
 */
public class DreamFileUtils {
	
	
	private String pathDirectory;
	private File chosenFile;
	/**
	 * Creates a new EqualDiscretization object.
	 */
	public DreamFileUtils(String pathDirectory, File file) {
		
		this.pathDirectory = 	pathDirectory;
		this.chosenFile = file;
		File testFile = new File(pathDirectory);
    	if(!testFile.exists())
    		testFile.mkdirs();
	}
	
	
	public List<String> getFilesWithPatterns(List<String> patterns)
	{
		ArrayList<String> files = new ArrayList<String>();
		ArrayList<String> allFiles = new ArrayList<String>();
		File spfFile;
		ZipInputStream zis;
		
		ZipEntry entry;
		String textFile = null;
		boolean patternFound = false;
		byte[] Unzipbuffer = new byte[(int) Math.pow(2, 16)];
		
		
		
		try{
			if(chosenFile == null)
				zis = new ZipInputStream(getClass().getResourceAsStream("/splitdir_net.zip"));
			else
				zis = new ZipInputStream(new FileInputStream(chosenFile));
			while((entry = zis.getNextEntry()) != null)
			{
				spfFile = new File(pathDirectory + entry.getName());
				if(entry.isDirectory())
				{
					spfFile.mkdir();
					continue;
				}
				
				allFiles.add(spfFile.getPath());
				if(!spfFile.exists())
				{
	                FileOutputStream fout = new FileOutputStream(spfFile);
	                int Unziplength = 0;
	                while ((Unziplength = zis.read(Unzipbuffer)) > 0) {
	                    fout.write(Unzipbuffer, 0, Unziplength);
	                }
	                zis.closeEntry();
	                fout.close();
				}
			}
			
		} catch (IOException e) {
		
		}
		
		String patternArray[] =  new String[patterns.size()];
		patterns.toArray(patternArray);
		for(String fileName : allFiles)
		{
			spfFile = new File(fileName);
			textFile = getTextFile(spfFile);
	    	patternFound = false;
	    	for(int i=0; (i< patternArray.length && !patternFound);i++)
	    	{
	    		for(int j=(i+1); (j< patternArray.length && !patternFound);j++)
	    		{
	    			
	    			if(textFile.contains( patternArray[i]) && textFile.contains(patternArray[j]))
	    			{
	    				//System.out.println("pat1: " + patternArray[i] + " pat2: " + patternArray[j] + " file: " + fileName);
	    				files.add(fileName);
	    				patternFound = true;
	    			}
	    		}
	    	}
		}
		
		return files;
	}

	public String getTextFile(File file)
	{
		FileInputStream fisTargetFile = null;
		String text = "";
		
		try {
    		fisTargetFile = new FileInputStream(file);
    	} catch (FileNotFoundException e) {
    		System.out.println("File not found: " + file.getName());
    	} 
    	
    	try {
    		if(fisTargetFile != null)
    			text = IOUtils.toString(fisTargetFile, "UTF-8");
    	} catch (IOException ex) {
    		
    		System.out.println("IOException " + file.getName());
    	}
		
    	return text;
	}
	
	
}
