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
import java.io.File;
import org.dom4j.*;
import org.dom4j.io.SAXReader; 
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.lists.VmList;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.lists.CloudletList;


public class Parameter {
	private String useOption = "";
	private int populationSize = 0;
	private int generationSize = 0;
	private double tournamentRate = 0.00;
	private int tournamentSize = 0;
	private double crossoverRate = 0.00;
	private double mutationRate = 0.00;
	private String selectionPolicy = "";
	private String crossoverPolicy = "";
	private String mutationPolicy = "";
	private List<Vm> vmsList = null;
	private List<Cloudlet> cloudletsList = null;
	Document docConfig = null;

	public Parameter(String filename) {
		try{
			parse(filename);
		}
		catch (DocumentException e){
			e.printStackTrace(); 
			//TODO: throw something back to the caller
		}
	}

	public void Apply(List<Vm> vmlist, List<Cloudlet> ctlist){
		vmsList = vmlist;	
		cloudletsList = ctlist;
	}
	
	
	private void parse(String filename) throws DocumentException {
		File inFile = new File(filename);
		SAXReader reader = new SAXReader();
		docConfig = reader.read(inFile);
		String pText = "";

		Node root = docConfig.selectSingleNode("//parameters");
		
		Node tmpNode = root.selectSingleNode("option");
		if(tmpNode==null)useOption="";
		else useOption = tmpNode.getText();

		pText = root.selectSingleNode("population/size").getText();
		populationSize = Integer.parseInt(pText);

		pText = root.selectSingleNode("population/generation").getText();
		generationSize = Integer.parseInt(pText);

		pText = root.selectSingleNode("tournament/probability").getText();
		tournamentRate = Double.parseDouble(pText);

		pText = root.selectSingleNode("tournament/size").getText();
		tournamentSize = Integer.parseInt(pText);

		selectionPolicy = root.selectSingleNode("selection/policy").getText();

		crossoverPolicy = root.selectSingleNode("crossover/policy").getText();
		pText = root.selectSingleNode("crossover/rate").getText();
		crossoverRate = Double.parseDouble(pText);

		mutationPolicy = root.selectSingleNode("mutation/policy").getText();
		pText = root.selectSingleNode("mutation/rate").getText();
		mutationRate = Double.parseDouble(pText);
	}

	public List<Vm> GetVMs(){
		return vmsList;
	}

	public List<Cloudlet> GetCloudlets(){
		return cloudletsList;
	}

	public int IncreasePopulationSize(int len){
		populationSize += len;
		return populationSize;
	}

	public int GetPopulationSize(){
		return populationSize;
	}

	public int GetTournamentSize(){
		return tournamentSize;
	}

	public int SetTournamentSize(int pp){
		tournamentSize=pp;
		return tournamentSize;
	}

	public double GetTournamentRate(){
		return tournamentRate;
	}

	public double SetTournamentRate(double pp){
		tournamentRate=pp;
		return tournamentRate;
	}

	public String GetSelectionPolicy(){
		return selectionPolicy;
	}

	public String SetSelectionPolicy(String sepo){
		selectionPolicy = sepo;
		return selectionPolicy;
	}

	public String GetCrossoverPolicy(){
		return crossoverPolicy;
	}

	public String GetMutationPolicy(){
		return mutationPolicy;
	}

	public double GetCrossoverRate(){
		return crossoverRate;
	}

	public double GetMutationRate(){
		return mutationRate;
	}

	public int GetGenerationSize(){
		return generationSize;
	}

	/* for EFFICIENT test (temporary) */
	public int GetOption(){
		int currentOpt = 0;
		switch (useOption){
			case "ga":
				currentOpt = 1;
                SetSelectionPolicy("Randomly");
				break;
			case "btga":
				currentOpt = 2;
                SetSelectionPolicy("Best-Two");
				break;
			case "rwga":
                currentOpt = 3;
                SetSelectionPolicy("Roulette-Wheel");
				break;
			case "tsga":
                currentOpt = 4;
                SetSelectionPolicy("Tournament");
				break;
			case "tsga-A":
                currentOpt = 5;
                SetSelectionPolicy("Tournament");
				break;
			default:
				break;
		}

		return currentOpt;
	}

	public String SetOption(String opt){
		useOption = opt;
		return useOption;
	}
}
