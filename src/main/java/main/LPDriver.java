package main;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import linearProgram.LinearProgram;

public class LPDriver {
	private static boolean DEBUG = false;
	
	public static void main(String args[]) throws IOException {
		for(int count=0;count<5;count++){
		for(int counter = 0; counter<1;counter++){
//		String fileName = "Facebook/Data/Data_"+counter+".txt";
		String filenameTopology = "testTopology.txt";
		String filenameSeedSetA = "testSeedSetA.txt";
		//String fileName="MyArpanet_graph.txt";
		
		try {
			BufferedReader brsd;
			int budget;
////			brsd = new BufferedReader(new FileReader("test_seedSetAs.txt"));
			brsd = new BufferedReader(new FileReader(filenameTopology));
//			String sdline;
//			while ((sdline = brsd.readLine()) != null) {
//				
//			
//			String[] sdlinearray=sdline.split(":");
//			ArrayList<Integer> seedsA=new ArrayList<Integer>();
//			
//			for(String s:sdlinearray)
//				seedsA.add(Integer.parseInt(s));
//			if(DEBUG)
//			System.out.println("for seedsA = " +seedsA.toString());
//			Integer[] seedsAarray = new Integer[seedsA.size()];
//			for(int i=0;i<seedsA.size();i++)
//				seedsAarray[i]=seedsA.get(i);
			int nodeCount = Integer.parseInt(brsd.readLine());
			budget = (int) (nodeCount * 0.2);
			LinearProgram mdpp = new LinearProgram(filenameTopology,filenameSeedSetA,budget);
			mdpp.optimize();
			mdpp.printValue();
			//*/
//			}
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
