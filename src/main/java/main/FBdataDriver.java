package main;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;

import org.jgraph.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import linearProgram.LinearProgram;
import linearProgram.Rounding1;

public class FBdataDriver {

	public static void main(String args[]) throws IOException {
		for(int count=0;count<1;count++){
			for(int counter = 0; counter<1;counter++){
				//		String fileName = "Facebook/Data/Data_"+counter+".txt";
				String filenameTopology = "FBTopology.txt";
				String filenameSeedSetA = "FBSeedSetA.txt";
				//String fileName="MyArpanet_graph.txt";

				try {
					BufferedReader brsd;
					int budget;
					////			brsd = new BufferedReader(new FileReader("test_seedSetAs.txt"));
					brsd = new BufferedReader(new FileReader("Data/FB/facebook_combined.txt"));
					SimpleGraph<Integer, DefaultEdge> g = new SimpleGraph<Integer,DefaultEdge>(DefaultEdge.class);
					String sdline;
					for(int i=0;i<4039;i++)
						g.addVertex(i);
					while ((sdline = brsd.readLine()) != null) {


						String[] sdlinearray=sdline.split(" ");
						g.addEdge(Integer.parseInt(sdlinearray[0]), Integer.parseInt(sdlinearray[1]));
					}
					brsd.close();
					int nodeCount = 4039;
					PrintWriter pr = new PrintWriter("FBTopology.txt");
					pr.println(nodeCount);
					for(int i=0;i<nodeCount;i++){
//						pr.println(i+":"+"1:"+g.degreeOf(i));
						pr.println(i+":"+g.degreeOf(i)+":"+g.degreeOf(i));
					}
					brsd = new BufferedReader(new FileReader("Data/FB/facebook_combined.txt"));
					while ((sdline = brsd.readLine()) != null) {
						String[] sdlinearray=sdline.split(" ");
						pr.println(sdlinearray[0]+":"+sdlinearray[1]);
					}
					brsd.close();
					pr.close();

					budget = (int) (nodeCount * 0.2);
					//create seedsetA
					pr = new PrintWriter("FBSeedSetA.txt");
					Random ran = new Random();
					for(int i=0;i<nodeCount;i++){
						if(ran.nextDouble()>0.5){
							pr.write(i+"\n");
						}
					}
					pr.close();
					LinearProgram mdpp = new LinearProgram(filenameTopology,filenameSeedSetA,budget);
					mdpp.optimize();
					mdpp.printValue();

					Rounding1 roun = new Rounding1(filenameTopology,filenameSeedSetA,budget);
					roun.optimizeAndRound();
					roun.printValue();

					//*/
					//			brsd.close();
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				//		if(DEBUG)
				System.out.println("\n Done :)");
				//OptimalStorageVerifier_LargestComponent ofv = new OptimalStorageVerifier_LargestComponent();		
				//ofv.verify("inputBLC/" + fileName, "optimalOutputBLC/CAP_1_" + fileName);			
				//		System.out.println("--------------------------------------------------");		
			}
			//		System.out.println("Total successful runs = "+total_successful_runs);
		}
	}
}
