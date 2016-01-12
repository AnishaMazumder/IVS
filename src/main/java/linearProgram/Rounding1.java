
package linearProgram;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.jgraph.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;
import org.jgrapht.traverse.DepthFirstIterator;
import org.jgrapht.traverse.GraphIterator;

import java.util.Set;

import ilog.concert.*;
import ilog.cplex.*;
import ilog.cplex.IloCplex.UnknownObjectException;

public class Rounding1 {

	public static boolean DEBUG = true;
	public static boolean undirected=true;
	private int NODE_COUNT;// = 14;
	public double epsilon = 0.001;
	//cplex variable
	//	static IloCplex cplex = null;
	IloCplex cplex;
	private IloNumVar[] X_p;
	private IloNumVar[] X_q;
	private IloNumVar[] X_s;

	//non cplex variables
	private HashMap<Integer,Double> nodeCost;
	private HashMap<Integer,Double> nodeUtility;
	private Set<Integer> seedSetA;
	private boolean adjacencyMatrix[][];
	private int budget;

	public Rounding1(String filenameTopology,String filinameSeedsetA,int budget) {		
		this.budget = budget;
		nodeCost = new HashMap<Integer,Double>();
		nodeUtility = new HashMap<Integer,Double>();
		seedSetA = new HashSet<Integer>();
		populateInstanceConfig(filenameTopology,filinameSeedsetA);

	}

	double getNodeCost(int v){
		return nodeCost.get(v);
	}

	double getNodeUtility(int v){
		return nodeUtility.get(v);
	}
	private void populateInstanceConfig(String filenameTopology,String filinameSeedsetA) {
		try {
			FileReader fr = new FileReader(new File(filenameTopology).getAbsoluteFile());
			BufferedReader br = new BufferedReader(fr);			
			String line;
			line=br.readLine(); 
			NODE_COUNT = Integer.parseInt(line); //read the first line as the number of nodes
			adjacencyMatrix = new boolean[NODE_COUNT][NODE_COUNT];
			for(int i=0;i<NODE_COUNT;i++){ // read the node id:cost:id
				//System.out.println(line);
				line = br.readLine();
				String[] s= line.split(":");
				int nodeID = Integer.parseInt(s[0]);
				nodeCost.put(nodeID,Double.parseDouble(s[1]));
				nodeUtility.put(nodeID, Double.parseDouble(s[2]));
			}

			while ((line = br.readLine()) != null) {
				String[] s = line.split(":");
				adjacencyMatrix[Integer.parseInt(s[0])][Integer.parseInt(s[1])]= true;
				adjacencyMatrix[Integer.parseInt(s[1])][Integer.parseInt(s[0])]= true;
			}
			br.close();
			br = new BufferedReader(new FileReader(new File(filinameSeedsetA).getAbsoluteFile()));

			while ((line = br.readLine()) != null) {
				//System.out.println(line);
				seedSetA.add(Integer.parseInt(line));
			}
			br.close();

			cplex = new IloCplex();		
			//cplex.setOut(null);
			
			if(DEBUG)
			{	
				System.out.println("There are total "+NODE_COUNT+" nodes with"+seedSetA.size()+" nodes in seedSetA = ");
				System.out.println(seedSetA.toString());
			}
			// verifyInputFile(); // debug
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void optimizeAndRound() {
		try {		

			//			cplex.setOut(env.getNullStream());
			// create variables
			createX_pVariables();
			createX_qVariables();
			createX_sVariables();
			//createOtherVariables();
			//System.out.println(x[0].toString());

			// create constraints
			createConstraint1();
			createConstraint2();
			createConstraint3();
			createConstraint4();

			// create objective
			createObjective();
//			if(DEBUG)
//				System.out.println(cplex.toString());
			if ( cplex.solve() ) {
				if(DEBUG){
					System.out.println("----------------------------------------");
					System.out.println("Obj " + cplex.getObjValue());
					System.out.println();
				}
				
				
			}

		} catch (Exception exc) {
			System.err.println("Concert exception '" + exc.getStackTrace() + "' caught");
		}
		HashSet<Integer> nodesInS1 = round1();  // only LP values
		findPartition(nodesInS1,1);
		HashSet<Integer> nodesInS2 = round2(); // LP values/cost
		findPartition(nodesInS2,2);
		HashSet<Integer> nodesInS3 = round3(); // LP values*utility
		findPartition(nodesInS3,3);
	}
	private HashSet<Integer> round3() {
		// 
		try {
			HashSet<Integer> nodesInS = new HashSet<Integer>();
			int[] nodeIDs = new int[NODE_COUNT];
			double[] xsValues = new double[NODE_COUNT]; 
			for(int i=0;i<NODE_COUNT;i++){
				nodeIDs[i] = i;
				xsValues[i] = cplex.getValue(X_s[i]);
			}
			int temp;
			for(int i=0;i<NODE_COUNT;i++){
				for(int j =0;j<=i;j++){
					if((xsValues[nodeIDs[j]]*nodeUtility.get(nodeIDs[j]))<(xsValues[nodeIDs[i]]*nodeUtility.get(nodeIDs[i]))){
						temp =nodeIDs[i];
						nodeIDs[i] = nodeIDs[j];
						nodeIDs[j] = temp;
					}
				}
			}
			int remainingBudget = budget;
			
			for(int i=0;i<NODE_COUNT && remainingBudget>0;i++){ // take all nodes with highest X_s values till budget permits
				if(nodeCost.get(nodeIDs[i])<=remainingBudget &&!seedSetA.contains(nodeIDs[i])){
				nodesInS.add(nodeIDs[i]);
				remainingBudget-=nodeCost.get(nodeIDs[i]);
				}
			}
			System.out.println("\n Final remaining budget = "+ remainingBudget + " size of nodesInS " +nodesInS.size());
			
			return nodesInS;
			
		} catch (IloException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	private HashSet<Integer> round2() {

		try {
			HashSet<Integer> nodesInS = new HashSet<Integer>();
			int[] nodeIDs = new int[NODE_COUNT];
			double[] xsValues = new double[NODE_COUNT]; 
			for(int i=0;i<NODE_COUNT;i++){
				nodeIDs[i] = i;
				xsValues[i] = cplex.getValue(X_s[i]);
			}
			int temp;
			for(int i=0;i<NODE_COUNT;i++){
				for(int j =0;j<=i;j++){
					if((xsValues[nodeIDs[j]]/nodeCost.get(nodeIDs[j]))<(xsValues[nodeIDs[i]]/nodeCost.get(nodeIDs[i]))){
						temp =nodeIDs[i];
						nodeIDs[i] = nodeIDs[j];
						nodeIDs[j] = temp;
					}
				}
			}
			int remainingBudget = budget;
			
			for(int i=0;i<NODE_COUNT && remainingBudget>0;i++){ // take all nodes with highest X_s values till budget permits
				if(nodeCost.get(nodeIDs[i])<=remainingBudget&&!seedSetA.contains(nodeIDs[i])){
				nodesInS.add(nodeIDs[i]);
				remainingBudget-=nodeCost.get(nodeIDs[i]);
				}
			}
			System.out.println("\n Final remaining budget = "+ remainingBudget + " size of nodesInS " +nodesInS.size());
			
			return nodesInS;
			
		} catch (IloException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	private void findPartition(HashSet<Integer> nodesInS1, int roundingStrategy) {
		 SimpleGraph<Integer, DefaultEdge> g = new SimpleGraph<Integer,DefaultEdge>(DefaultEdge.class);
		for(int i=0;i<NODE_COUNT;i++){
			if(!nodesInS1.contains(i)){
				g.addVertex(i);
			}
		}
		for(int i=0;i<NODE_COUNT;i++){
			for(int j=0;j<i;j++){
				if(adjacencyMatrix[i][j]&&!nodesInS1.contains(i)&&!nodesInS1.contains(j))
					g.addEdge(i, j);
			}
		}
//		System.out.println("SeedSet intersection s1 = "+nodesInS1.retainAll(seedSetA));
		g.addVertex(NODE_COUNT); // add a dummy node
		//add dummy edges
		Iterator<Integer> iterator = seedSetA.iterator(); 
		while (iterator.hasNext()){
			g.addEdge(NODE_COUNT, (Integer) iterator.next());
		}
		 GraphIterator<Integer, DefaultEdge> graphIterator = 
	                new DepthFirstIterator<Integer, DefaultEdge>(g,NODE_COUNT); // do BFS starting from dummy node
		 HashSet<Integer> nodesInP = new HashSet<Integer>();
	        while (graphIterator.hasNext()) {
	            nodesInP.add( graphIterator.next() );
	        }
	        nodesInP.remove(NODE_COUNT); //remove dummy node from P
	        
	        try{
	        	PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("roundingOutput.txt", true)));
	        	out.println("\n\n Rounding "+ roundingStrategy+" output");
	        	if(DEBUG)
	        	System.out.println("\n\n Rounding "+ roundingStrategy+" output");
	        	double objValue = 0.0;
	        	iterator = nodesInP.iterator();
	        	while(iterator.hasNext()){
	        		objValue+=nodeUtility.get(iterator.next());
	        	}
	        	out.println("\n"+objValue);
	        	PrintWriter outobj = new PrintWriter(new BufferedWriter(new FileWriter("objective values.txt", true)));
				outobj.println("Rounding"+roundingStrategy+" obj value ="+objValue);
				outobj.close();
	        	if(DEBUG)
		        	System.out.println("\n"+objValue);
	        	if(DEBUG)
		        	System.out.print(" X_p::"); // print the values of X_p in one line for all nodes
	        	for(int i=0;i<NODE_COUNT;i++){
	        		if(nodesInP.contains(i))
	        			{
	        				out.print("1.0"+"\t");
	        				if(DEBUG)
	        		        	System.out.print("1.0"+"\t");
	        			}
	        		else
	        			{
	        				out.print("0.0"+"\t");
	        				if(DEBUG)
	        		        	System.out.print("0.0"+"\t");
	        			}
	        	}
	        	out.print("\n X_q::"); // print the values of X_q in one line for all nodes
	        	if(DEBUG)
		        	System.out.print("\n X_q::");
	        	for(int i=0;i<NODE_COUNT;i++){
	        		if(!nodesInP.contains(i)&&!nodesInS1.contains(i))
	        			{
	        				out.print("1.0"+"\t");
	        				if(DEBUG)
	        		        	System.out.print("1.0"+"\t");
	        			}
	        		else
	        			{
	        				out.print("0.0"+"\t");
	        				if(DEBUG)
	        		        	System.out.print("0.0"+"\t");
	        			}
	        	}
	        	out.print("\n X_s::"); // print the values of X_s in one line for all nodes
	        	if(DEBUG)
		        	System.out.print("\n X_s::");
	        	for(int i=0;i<NODE_COUNT;i++){
	        		if(nodesInS1.contains(i))
	        			{
	        				out.print("1.0"+"\t");
	        				if(DEBUG)
	        		        	System.out.print("1.0"+"\t");
	        			}
	        		else
	        			{
	        				out.print("0.0"+"\t");
	        				if(DEBUG)
	        		        	System.out.print("0.0"+"\t");
	        			}
	        	}
	        	out.close();
	        }catch(Exception e){
	        	System.out.println(e.toString());
	        }
	        
	        		
	}

	/*
	 * Take only the nodes which have the highest lp values as long as budget is satisfied
	 */
	private HashSet<Integer> round1() {

		try {
			HashSet<Integer> nodesInS = new HashSet<Integer>();
			int[] nodeIDs = new int[NODE_COUNT];
			double[] xsValues = new double[NODE_COUNT]; 
			for(int i=0;i<NODE_COUNT;i++){
				nodeIDs[i] = i;
				xsValues[i] = cplex.getValue(X_s[i]);
			}
			int temp;
			for(int i=1;i<NODE_COUNT;i++){
				for(int j =0;j<i;j++){
					if(xsValues[nodeIDs[j]]<xsValues[nodeIDs[i]]){
						temp =nodeIDs[i];
						nodeIDs[i] = nodeIDs[j];
						nodeIDs[j] = temp;
					}
				}
			}
			int remainingBudget = budget;
			
			for(int i=0;i<NODE_COUNT && remainingBudget>=0;i++){ // take all nodes with highest X_s values till budget permits
				if(nodeCost.get(nodeIDs[i])<=remainingBudget&&!seedSetA.contains(nodeIDs[i])){
				nodesInS.add(nodeIDs[i]);
				remainingBudget-=nodeCost.get(nodeIDs[i]);
				}
			}
			System.out.println("\n Final remaining budget = "+ remainingBudget + " size of nodesInS " +nodesInS.size());
			System.out.println(nodesInS.toString());
			return nodesInS;
			
		} catch (IloException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		
	}

	public void createObjective() {
		try {
			IloLinearNumExpr expr = cplex.linearNumExpr();

			Iterator<Entry<Integer, Double>> it = nodeUtility.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<Integer, Double> pair = (Map.Entry<Integer, Double>)it.next();
				expr.addTerm((double) pair.getValue(), X_p[(int) pair.getKey()]);
				//		        System.out.println(pair.getKey() + " = " + pair.getValue());
			}

			cplex.addMinimize(expr, ": Obejective to minimize sum(u_v*X_p)");

		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}
	public void createConstraint1() {
		try {
			for (int i =  0; i < NODE_COUNT; i++) {
				for(int j =0; j<NODE_COUNT;j++)
				{
					if(adjacencyMatrix[i][j]==true) //edge (i,j) exists
					{
						IloLinearNumExpr expr1=cplex.linearNumExpr();
						expr1.addTerm(1.0,X_p[i]);
						expr1.addTerm(1.0, X_q[j]);
						cplex.addLe(expr1, 1, ": Contraint 1 for edge (" + i + "," + j +")" );

					}		
				}
				//cplex.addGe(expr1, 0, ": Contraint 1 for node" + i );
			}	         
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}		
	}
	public void createConstraint2() {
		try {
			for (int i =  0; i < NODE_COUNT; i++) {
				IloLinearNumExpr expr2=cplex.linearNumExpr();
				expr2.addTerm(1.0,X_p[i]);
				expr2.addTerm(1.0, X_q[i]);
				expr2.addTerm(1.0, X_s[i]);
				cplex.addEq(expr2, 1.0, ": Contraint 2 for node" + i );

			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}		
	}

	public void createConstraint3() {
		try {

			Iterator<Integer> iterator = seedSetA.iterator(); 
			while (iterator.hasNext()){
				IloLinearNumExpr expr3=cplex.linearNumExpr();
				int nodeID = (Integer)iterator.next();
				expr3.addTerm(1.0,X_p[nodeID]);
				cplex.addEq(expr3, 1.0, ": Contraint 3 for node " + nodeID);
				//			   System.out.println("Value: "+iterator.next() + " ");  
			}

		} catch (Exception e) {
			System.out.println(e.getMessage());
		}		
	}
	public void createConstraint4() {

		try {
			IloLinearNumExpr expr4=cplex.linearNumExpr();
			for (int i =  0; i < NODE_COUNT; i++) {
				expr4.addTerm((double)nodeCost.get(i),X_s[i]);
			}
			cplex.addLe(expr4, budget, ": Contraint 4 " );
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}		
	}

	public void createX_pVariables() {		
		try {
			double[]    lb = new double[NODE_COUNT];
			double[]    ub = new double[NODE_COUNT];
			for(int i=0;i<NODE_COUNT;i++)
			{
				lb[i]=0;
				ub[i]=1;
			}			
			X_p = cplex.numVarArray(NODE_COUNT, lb, ub);	
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}	

	}
	public void createX_qVariables() {		
		try {
			double[]    lb = new double[NODE_COUNT];
			double[]    ub = new double[NODE_COUNT];
			for(int i=0;i<NODE_COUNT;i++)
			{
				lb[i]=0;
				ub[i]=1;
			}			
			X_q = cplex.numVarArray(NODE_COUNT, lb, ub);	
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}	

	}
	public void createX_sVariables() {		
		try {
			double[]    lb = new double[NODE_COUNT];
			double[]    ub = new double[NODE_COUNT];
			for(int i=0;i<NODE_COUNT;i++)
			{
				lb[i]=0;
				ub[i]=1;
			}			
			X_s = cplex.numVarArray(NODE_COUNT, lb, ub);	
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}	

	}
	public void printValue()
	{
		try {
			if(DEBUG)
				System.out.println("Inside print");
			IloLinearNumExpr expr = cplex.linearNumExpr();

			for(int i=0;i<NODE_COUNT;i++)
			{

				expr.addTerm(nodeUtility.get(i), X_p[i]);
			}
			System.out.println("\n obj value ="+cplex.getValue(expr));
			//		System.out.println(cplex.toString());
			DecimalFormat numberFormat = new DecimalFormat("0.000");
			try 
			{
				PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("LPoutputfile.txt", true)));
				out.println("obj value ="+cplex.getValue(expr));
				PrintWriter outobj = new PrintWriter(new BufferedWriter(new FileWriter("objective values.txt", true)));
				outobj.println("LP obj value ="+cplex.getValue(expr));
				outobj.close();
				if(DEBUG)
					System.out.print("\n");
				out.print("\n");
				int tempCount = 0;
				for(int i=0;i<NODE_COUNT;i++){
					out.print(" X_p::"+numberFormat.format(cplex.getValue(X_p[i]))+"\t");
					if(DEBUG){
						System.out.print(" X_p::"+numberFormat.format(cplex.getValue(X_p[i]))+"\t");
						if(cplex.getValue(X_p[i])>0 && cplex.getValue(X_p[i]) <1)
							tempCount++;
					}
				}
				if(DEBUG){
					System.out.println("\n Count of fractional X_p values = " + tempCount);
					out.println("\n Count of fractional X_p values = " + tempCount);
					tempCount=0;
				}
				if(DEBUG)
					System.out.print("\n");
				out.print("\n");
				for(int i=0;i<NODE_COUNT;i++){
					out.print(" X_q::"+numberFormat.format(cplex.getValue(X_q[i]))+"\t");
					if(DEBUG){	
						System.out.print(" X_q::"+numberFormat.format(cplex.getValue(X_q[i]))+"\t");
						if(cplex.getValue(X_q[i])>0 && cplex.getValue(X_q[i]) <1)
							tempCount++;
					}
				}
				if(DEBUG){
					System.out.println("\n Count of fractional X_q values = " + tempCount);
					out.println("\n Count of fractional X_q values = " + tempCount);
					tempCount=0;
				}
				if(DEBUG)
					System.out.print("\n");
				out.print("\n");
				for(int i=0;i<NODE_COUNT;i++){
					out.print(" X_s::"+numberFormat.format(cplex.getValue(X_s[i]))+"\t");
					if(DEBUG){
						System.out.print(" X_s::"+numberFormat.format(cplex.getValue(X_s[i]))+"\t");
						if(cplex.getValue(X_s[i])>0 && cplex.getValue(X_s[i]) <1)
							tempCount++;
					}

				}
				if(DEBUG){
					System.out.println("\n Count of fractional X_s values = " + tempCount);
					out.println("\n Count of fractional X_s values = " + tempCount);
					tempCount=0;
				}
				out.close();

			}catch (IOException e) {
				//exception handling left as an exercise for the reader
				e.printStackTrace();
			}
		} catch (UnknownObjectException e) {
			e.printStackTrace();
		} catch (IloException e) {
			e.printStackTrace();
		}
	}

}
