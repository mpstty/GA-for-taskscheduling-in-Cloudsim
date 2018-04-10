/*created by mazw at 20180312*/

package lab.cloudsim.taskscheduling.GA;

import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Random;
import java.util.Comparator;
import java.util.Collections;
import java.util.function.*;
import org.dom4j.*;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.lists.VmList;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.lists.CloudletList;

public class Scheduler {

	List<Vm> vmList = null;
	List<Cloudlet> ctList = null;
	Parameter parameters = null;

	public Scheduler(List<Vm> vmlist, List<Cloudlet> ctlist){
		parameters = new Parameter("etc/genetic_algorithm.xml");
		parameters.Apply(vmlist, ctlist);
	}

	//public Individual Execute(){
	public List<Integer> Execute(){
		Population lastpop = null; 
		Population nextpop = null;
		Individual bestSolution = null;
		Individual currSolution = null;
		int counter = 0;

		lastpop = new Population(parameters);
		lastpop.Initialize(); //allocate the optimum

		bestSolution = lastpop.FindTheBest(lastpop.GetIndividuals());
		while(counter<parameters.GetGenerationSize()){
			nextpop = lastpop.Evolve();
			currSolution = nextpop.FindTheBest(nextpop.GetIndividuals());
			if(currSolution.Fitness() > bestSolution.Fitness()){
				bestSolution = currSolution.Duplicate();
				counter = 0; //reset
			}
			else{
				counter += 1;
			}
			lastpop = nextpop;
		}
		
		System.out.println("");
		System.out.println("");
		System.out.println("");
		System.out.println("########## Final Result: age=("+counter+") ###########");
		System.out.println("");
		bestSolution.Show();
		return bestSolution.Return();
	}
}
