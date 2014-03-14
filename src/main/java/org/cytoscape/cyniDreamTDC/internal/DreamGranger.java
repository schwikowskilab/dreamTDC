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
import java.util.Map.Entry;
import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.cytoscape.cyni.CyniTable;
import org.jblas.DoubleMatrix;
import org.jblas.MatrixFunctions;

import cern.colt.matrix.tdouble.DoubleMatrix2D;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.impl.*;




/**
 * The BasicInduction provides a very simple Induction, suitable as
 * the default Induction for Cytoscape data readers.
 */
public class DreamGranger {
	
	
	private CyniTable table;
	private int nThreads;
	private boolean cancelled;
	private double[][] futureData;
	private double[][] pastData;
	/**
	 * Creates a new EqualDiscretization object.
	 */
	public DreamGranger(CyniTable table, int nThreads) {
		
		this.table = table;
		cancelled = false;
		this.nThreads = nThreads;
		futureData = new double[table.nRows()][table.nRows()];
		pastData = new double[table.nRows()][table.nRows()];
	}
	
	public double[][] getGrangerResults(String dimension1,List<String> listDimensions2,List<String> listDimensions3 ) 
	{
		File file;
		Map<String,DreamTDCTable> mapTables = new HashMap<String,DreamTDCTable>();
		String nodesIds[];
		double finalData[][] = new double[table.nRows()][table.nRows()];
		
		
		for(String dimension3 : listDimensions3)
			mapTables.put(dimension3, new DreamTDCTable(table,dimension1,listDimensions2,dimension3));
		//if(dimension1.matches("IGF1"))
		//{
		
		ExecutorService executor = Executors.newFixedThreadPool(nThreads);
		
		for(int i=0;i< table.nRows();i++)
		{
			if(cancelled)
				break;
			executor.execute(new ThreadedLasso(i,mapTables,listDimensions3,table.nRows()));
		}
		
		
		executor.shutdown();
		// Wait until all threads are finish
		try {
         	executor.awaitTermination(7, TimeUnit.DAYS);
        } catch (Exception e) {}
		
		if(cancelled)
			return null;
		
		for(int i = 0;i<table.nRows();i++)
		{
			for(int j = 0;j<table.nRows();j++)
				finalData[i][j] = (pastData[i][j] + futureData[i][j])/2;
		}
		//}
		double max ;
		DoubleMatrix tempData = new DoubleMatrix(finalData);
		max = tempData.max();
		if(max != 0)
		{
			tempData = MatrixFunctions.abs(tempData);
			max = tempData.max();
			tempData.divi(max);
			return tempData.toArray2();
		}
		/*nodesIds = new String[400];
		for(int i =0;i<table.nRows();i++)
			nodesIds[i] = (String)table.getRowLabel(i);
		file = new File("/home/oguitart/temp/grangerData." + dimension1);
		writeData(file,finalData,nodesIds);*/
		return finalData;
	}
	
	
	private class ThreadedLasso implements Runnable {
		private int position;
		private int size;
		private Map<String,DreamTDCTable> mapTables;
		private DoubleMatrix2D weightFuture;
		private DoubleMatrix2D weightPast;
		private List<String> listDim3;
		
		
		
		ThreadedLasso(int position,Map<String,DreamTDCTable> mapTables,List<String> listDim3, int size)
		{
			this.position = position;
			this.mapTables = mapTables;
			this.size = size;
			this.listDim3 = listDim3;
			weightFuture = new DenseDoubleMatrix2D(size-1, listDim3.size());
			weightPast = new DenseDoubleMatrix2D(size-1, listDim3.size());
		}
		
		public void run() {
			
			int total = size*mapTables.size()-1;
			double[][] xMatrix = new double[total][];
			double[] wLasso;
			double[] yVector = null;
			ArrayList<Integer> pivotIndexes = new ArrayList<Integer>();
			int nRows = 0;
			int thr =0, nonAR = 0;
			double wSum = 0;
			int times;
			DenseDoubleMatrix1D vectorData ;
			DenseDoubleMatrix2D mainData;
			
			
			
			for(String dim : listDim3)
			{
				DreamTDCTable table = mapTables.get(dim);
				for(int i =0;i< size;i++)
				{
					if(i != position)
					{
						xMatrix[nRows] = table.get1DRow(i);
						//System.out.println("row[" + nRows+"]= " + Arrays.toString(xMatrix[nRows]));
						nRows++;
					}
				}
			}
			nonAR = nRows;
			//System.out.println("num rows nonAR: " + nonAR);
			for(int it= 0; it<listDim3.size();it++)
			{
				thr = (it+1) * (size-1);
				wSum = 0.0;
				//System.out.println("time: " + listDim3.get(it));
				yVector = mapTables.get(listDim3.get(it)).get1DRow(position);
				
				if(cancelled)
					break;
				
				nRows = nonAR;
				pivotIndexes.clear();
				for(String dim : listDim3)
				{
					if(!dim.matches(listDim3.get(it)))
					{
						pivotIndexes.add(nRows);
						xMatrix[nRows] = mapTables.get(dim).get1DRow(position);
						//System.out.println("row[" + nRows+"]= " + Arrays.toString(xMatrix[nRows]));
						nRows++;
					}
				}
				
				//System.out.println("Yrow[" + nRows+"]= " + Arrays.toString(yVector));
				vectorData = new DenseDoubleMatrix1D(yVector);
				mainData = new DenseDoubleMatrix2D(xMatrix);
				//System.out.println("num rows XMatrix: " + mainData.rows());
				//System.out.println("num cols XMatrix: " + mainData.columns());
				wLasso =grangerLasso(mainData,vectorData,pivotIndexes);
				//System.out.println("weights: " + Arrays.toString(wLasso));
				
				for(int i=0;i<wLasso.length;i++)
					wSum += Math.abs(wLasso[i]);
				
				if(wSum > 0)
				{
					for(int i=0;i<wLasso.length;i++)
						wLasso[i] = wLasso[i]/ wSum;
				}
				
				times = it +1;
				for(int i=0;i<times;i++)
				{
					for(int j=0;j<(size-1);j++)
						weightPast.setQuick(j   , it,weightPast.getQuick(j , it)+wLasso[j + i*(size-1)]);
				}
				if(times > 1)
				{
					for(int i=0;i<(size-1);i++)
						weightPast.setQuick(i, it,weightPast.getQuick(i, it)/times);
				}
				
				if(thr < nonAR)
				{
					times = listDim3.size() - times  ;
					for(int i=0;i<times;i++)
					{
						for(int j=0;j<(size-1);j++)
							weightFuture.setQuick(j   , it,weightFuture.getQuick(j , it)+wLasso[j + i*(size-1)+thr]);
					}
					if(times > 1)
					{
						for(int i=0;i<(size-1);i++)
							weightFuture.setQuick(i, it,weightFuture.getQuick(i, it)/times);
					}
				}
				/*double temp1 [] = new double[size-1];
				double temp2 [] = new double[size-1];
				
				for(int i=0;i<(size-1);i++)
				{
					
						temp1[i] = weightPast.getQuick(i, it);
						temp2[i] = weightFuture.getQuick(i, it);
					
				}
				
				System.out.println("FinalweightsPast: " + Arrays.toString(temp1));
				System.out.println("FinalweightsFuture: " + Arrays.toString(temp2));*/
			}
			
			
			int pos=0;
			double sumPast , sumFuture;
			for(int i=0;i<(size-1);i++)
			{
				sumPast = 0.0;
				sumFuture = 0.0;
				if(pos == position)
				{
					pos++;
				}
				for(int j = 0;j<listDim3.size();j++)
				{
					sumPast += weightPast.getQuick(i, j);
					sumFuture += weightFuture.getQuick(i, j);
				}
				pastData[pos][position] = sumPast/listDim3.size();
				if(listDim3.size() > 1)
					futureData[position][pos] = sumFuture/(listDim3.size()-1);
				pos++;
				
			}
			
			/*System.out.println("Past:");
			for(int i=0;i<size;i++)
			{
				System.out.println("row[" + position + "]=" + pastData[i][position]);
			}
			
			System.out.println("Past:");
			
				System.out.println("row[" + position + "]=" + Arrays.toString(futureData[position]));*/
			
			
		}
		
		double[] grangerLasso(DoubleMatrix2D X, DoubleMatrix1D y,  List<Integer> pivots)
		{
			double[] weigths = new double[X.rows()];
			double[] weigthsNew = null;
			double b = 0.0;
			double S[] = new double[X.columns()];
			int maxIter=1000;
			double eps=0.0000000001;//1e-10;
			double llMax, llMin =0.0,llCur;
			double sum = 0.0;
			double max = 0.0;
			
			if(X.columns() != y.size())
				return null;
			
			b = y.zSum()/(double)y.size();
			
			for(int i =0 ;i< X.rows();i++)
			{
				sum = 0.0;
				for(int j =0 ;j< X.columns();j++)
				{
					sum += ((y.getQuick(j) -b)*X.getQuick(i, j));
				}
				sum = sum /X.columns();
				if(max < Math.abs(sum))
					max = Math.abs(sum);
			}
			llMax = max;
			//System.out.println( " eps: "+ eps);
			
			
			
			
			//Arrays.fill(S, b);
			//System.out.println( " S: "+ Arrays.toString(S));
			//System.out.println( " n: " + X.columns() + " p: " + X.rows());
			for(int iter=0;iter<20;iter++)
			{
				if(cancelled)
					break;
				llCur = (llMax+llMin)/2.0;
				//System.out.println("llmax: " + llMax + " llMin: "+ llMin + " llCuur " + llCur);
				sum = 0.0;
				Arrays.fill(weigths, 0.0);
				Arrays.fill(S, b);
				/*weigthsNew =*/ optLASSO(X.toArray(),y.toArray(),llCur, S, X.columns(),X.rows(),maxIter,eps,weigths,b);
				for(Integer index : pivots)
					sum += weigths[index];
				if(sum == 0.0 )
					 llMax = llCur;
				else
					llMin = llCur;		
			}
			//System.out.println("weights2: " + Arrays.toString(weigthsNew));
			for(Integer index : pivots)
				weigths[index] = 0;
			return weigths;
		}
		

	}
	
	double sthresh( double x, double a )
	{
		double temp;
	  if( Math.abs(x) < a ) temp = 0;
	  else if( x < 0 ) temp = x + a;
	  else temp = x - a;
	  return temp;
	}

	// Updates fits and L2-norm penalties
	void updateFits( double[][] X, double[] S, int np, int jp, double wj_diffp )
	{
	  int n = np;
	  int j = jp;
	  double wjd = wj_diffp;

	  for( int i = 0; i < n; ++i )
	    S[i] += (X[j][i] * wjd);
	}

	// Computes the objective function value
	double objVal( double[] S, double[] z, double[] w,
		     double lambdap, int np, int pp )
	{
	  int n = np;
	  int p = pp;
	  double lambda = lambdap;
	  double res;

	  // Loss term
	  double loss = 0.0;
	  for( int i = 0; i < n; ++i )
	    {
	      double r = (z[i] - S[i]);
	      loss += (r * r);
	    }

	  // Regularization term
	  double regL1 = 0.0;
	  for( int j = 0; j < p; ++j )
	    regL1 += Math.abs( w[j] );

	  res = (0.5 * loss / n) + (lambda * regL1);
	  return res;
	}

	// Computes the new value for coordinate *jp
	double computeCoord( double[][] X, double[] z, double lambdap, double[] S,
			   int np, int pp, int jp, double[] w,
			   double[] work_zj )
	{
	  // Dereference
	  int n = np;  int j = jp;
	  double lambda = lambdap;
	  double res;

	  // Compute the working space values
	  for( int i = 0; i < n; ++i )
	    work_zj[i] = S[i] - X[j][i] * w[j];

	  // Compute the numerator
	  double num = 0.0;
	  for( int i = 0; i < n; ++i )
	  {
		 // if(j == 238)
			//  System.out.println("X: " + X[j][i] + " Z " + z[i] + " work: " +  work_zj[i] + " S: " +  S[i] + " W: " + w[j]);
	    num += (X[j][i] * (z[i] - work_zj[i]));
	  }

	  // Normalize the numerator
	  num /= n;
	  if(j == 238)
	  {
		  //System.out.println("num[" + j + "]="+ num + " lambda: " + lambda);
		  //System.out.println("W[" + j + "]="+ w[j]);
	  }
	  num = sthresh( num, lambda );
	  if( num == 0.0 ) { res = 0.0; return res; }

	  // Compute the denominator
	  double denom = 0.0;
	  for( int i = 0; i < n; ++i )
	    denom += X[j][i] * X[j][i];

	  //System.out.println("denom: " + denom);
	  // Normalize the denominator
	  denom /= n;

	  res = num / denom;
	  return res;
	}

	// Optimizes the a LASSO objective via coordinate descent
	void optLASSO( double[][] X, double[] z, double lambdap,
		       double[] S, int np, int pp,
		       int max_iter, double eps,
		       double[] w, double b )
	{
	  // Dereference
	  int n = np; int p = pp;
	  double lambda = lambdap;
	 
	  // Working storage
	  double[] work_zj = new double[n];//(double*) R_alloc( n, sizeof( double ) );

	 
	  //System.out.println( "Running base optimization with lambda = "+lambda );

	  // Compute the initial objective function value
	  double fprev;
	  fprev = objVal( S, z, w, lambda, np, pp );

	  //  Rprintf( "nTop = %d, obj = %f\n", nTop, fprev );

	  // Perform coordinate descent
	  int iter; double f = 0;
	  for( iter = 1; iter <= (max_iter); ++iter )
	  {
		  if(cancelled)
				break;
	      //      Rprintf( "\n=== Iteration %d ===\n", iter );

	      // Update the weights
	    for( int j = 0; j < p; ++j )
		{
		  //	  Rprintf( "== Coord %d ==\n", j );
	    
	    	//if(j == 238)
	  		//  System.out.println("b: " + Arrays.toString(S));
		  // Perform the update
		  double wj_old = w[j];
		  w[j] = computeCoord( X, z, lambda, S, np, pp, j, w, work_zj );
		  
		  //	  Rprintf( "computeCoord returned %f\n", w[j] );

		  // Update fits and L2-norm penalty term accordingly
		  double wj_diff = w[j] - wj_old;
		  if( wj_diff != 0.0 )
		    updateFits( X, S, np, j, wj_diff);
		}

	      // Update the bias term
	    double b_num = 0.0;
	    double b_denom = n;
	    for( int i = 0; i < n; ++i )
		{
		  double s = S[i] - b;
		  b_num += (z[i] - s);
		}
	      
	      double b_old = b;
	      b = b_num / b_denom;
	      double b_diff = b - b_old;

	      // Update the fits accordingly
	    if( b_diff != 0 )
		{
		  for( int i = 0; i < n; ++i )
		    S[i] += b_diff;
		}

	      // Compute the objective function value and check the stopping criterion
	      f = objVal( S, z, w, lambda, np, pp );
	      //      Rprintf( "f = %f\n", f );
	      if( Math.abs( f - fprev ) / Math.abs( fprev ) < eps ) 
	    	  break;
	      else 
	    	  fprev = f;
	  }
	  if( iter > (max_iter) ) 
		  --iter;	// Corner case: loop didn't end via break
	  //System.out.println( "f = %f after iteration "+ f + " "+ iter );
	  //return w;
	}
	
	private void writeData(File file, double[][] data, String[] ids)
	{
		File newFile = new File(file.getPath()+ ".pid");
		FileWriter fw;
		String line = "";
		
		try{
			fw = new FileWriter(newFile);
			for(int i = -1 ; i< table.nRows();i++)
			{
				if(i == -1)
					line = "Key\t";
				else
					line = ids[i] + "\t";
				for(int j=0; j< table.nRows();j++)
				{
					if(i == -1 )
					{
						line += ids[j];
						line += "\t";
					}
					else
					{
						line += data[i][j];
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
