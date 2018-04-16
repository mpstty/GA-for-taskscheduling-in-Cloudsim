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
import org.dom4j.*;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.lists.VmList;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.lists.CloudletList;

public class Population {
	/* for special test */
	private int currentOpt = 0;
	private int currentAge = 0;
	private int replacement = 0;

	private Parameter parameters = null;
	private Random random = new Random();
	private List<Individual> individuals = new ArrayList<Individual>();
	private List<Individual> indvSelects = new ArrayList<Individual>();

	private double[] indvFitnessPortions = null;

	public Population(){
		//nothing to do
	}

	public Population(Parameter par){
		this.parameters = par;
	}

	public int GetAge(){
		return currentAge;
	}

	public int SetAge(int i){
		currentAge = i;
		return currentAge;
	}

	public List<Individual> GetIndividuals(){
		return individuals;
	}

	public List<Individual> GetSelecteds(){
		return indvSelects;
	}

	private void cleanup(){
		currentAge = 0;
		individuals.clear();	
		return ;
	}

	private Population increase(Individual... indvss){
		for(Individual tmpIndv: indvss){
			tmpIndv.GenIdx();
			GetIndividuals().add(tmpIndv);
		}
		return this;
	}

	private Population increase(List<Individual> indvlist){
		for(Individual tmpIndv: indvlist){
			tmpIndv.GenIdx();
			GetIndividuals().add(tmpIndv);
		}
		return this;
	}

	private Population decrease(Individual... indvss){
		for(Individual indv: indvss){
			for(int i=0; i<GetIndividuals().size(); i++){
				Individual orig = GetIndividuals().get(i);
				if(indv.Idx()==orig.Idx()){
					GetIndividuals().remove(i);
				}
			}
		}
		return this;
	}
	
	public boolean Sort(){
		Collections.sort(individuals, new Comparator<Individual>(){
									@Override
									public int compare(Individual i1, Individual i2){
										return Double.compare(i1.Duration(), i2.Duration());
									}
								}
						);
		
		return true;
	}


	public void Initialize(){
		currentAge = 0;
		
		for(int i=0; i<parameters.GetPopulationSize(); i++){
			Individual indv = new Individual(parameters.GetVMs(), 
												parameters.GetCloudlets());
			indv.Evaluate();
			GetIndividuals().add(indv);
		}

		Individual ibest = FindTheBest(GetIndividuals());
		System.out.println("#### Initial Best Solution: age=("+currentAge+") ####");
		System.out.println("");
		ibest.Show();
		return ;
	}

	private boolean ContainsId(List<Individual> ilist, int iid){
		List<Individual> tmpIList = (ilist==null)?GetIndividuals():ilist;
		for(Individual indv: tmpIList){
			if(indv.Idx()==iid){
				return true;
			}
		}
		return false;
	}
	
	//TODO: enlarge the sub-population size
	public List<Individual> Selection(){
		Individual ib1st = null;
		Individual ib2nd = null;
		GetSelecteds().clear();
		
		switch (parameters.GetSelectionPolicy()){
			case "Randomly":
				int rn = 0;
				rn = random.nextInt(GetIndividuals().size());
				ib1st = GetIndividuals().get(rn);
				rn = random.nextInt(GetIndividuals().size());
				ib2nd = GetIndividuals().get(rn);
				break;	
			case "Best-Two":
				ib1st = FindTheBest(GetIndividuals());
				ib2nd = FindSndBest(GetIndividuals());
				break;
			case "Roulette-Wheel":
				ib1st = selectionRouletteWheel();
				ib2nd = selectionRouletteWheel();
				break;
			case "Tournament":
				ib1st = selectionTournament();
				ib2nd = selectionTournament();
				break;
			default:
				System.out.println("#ERROR: no such selection policy ("+
						parameters.GetSelectionPolicy()+")");
				return null;
		}

		ib1st = ib1st.Duplicate();
		ib2nd = ib2nd.Duplicate();
		indvSelects.add(ib1st);
		indvSelects.add(ib2nd); 
		return GetSelecteds();
	} 

	private void GenFitnessPortions(){
		double totFitvalue = 0.00;
		int curSize = GetIndividuals().size();
		indvFitnessPortions = new double[curSize+1];
		for(Individual indv: GetIndividuals()){
			totFitvalue += indv.Fitness();
		}
		indvFitnessPortions[0] = 0.00;

		for (int i=1; i<=curSize; i++) {
			indvFitnessPortions[i] = indvFitnessPortions[i-1] + 
						(GetIndividuals().get(i-1).Fitness()/totFitvalue);
		}

		double lastProba = indvFitnessPortions[curSize];
		if (Math.abs(lastProba - 1.0)>0.0001){//TODO: preciseness
			System.out.println("#ERROR: The sum of probabilities is not 1.00 but " + 
								lastProba);//TODO: throw new Exception("");
		}
	}

	private Individual selectionRouletteWheel(){

		/*TODO: add replacement to control */
		if(replacement != 0){
			GenFitnessPortions();
		}
		
		Individual ibest = null;
		double ranProba = random.nextDouble(); 
		int curSize = GetIndividuals().size();
		for(int k=1; k<=curSize; k++){
			if(ranProba>=indvFitnessPortions[k-1] && ranProba<=indvFitnessPortions[k]){
				ibest = GetIndividuals().get(k-1);
				break;
			}
		}
		return ibest;
    }

	private Individual selectionTournament(){//tournament selection
		Individual ibest = null;
		Individual icheck = null;
		int rn = 0;

/* for particular test
		if(currentOpt==5){
			parameters.SetTournamentRate(0.75);
			parameters.SetTournamentSize(2);
		}
*/
		List<Individual> tournamentIndvs = new ArrayList<Individual>();
		ibest = null;
		for(int j=0; j<parameters.GetTournamentSize(); j++){
				rn = random.nextInt(GetIndividuals().size());
				icheck = GetIndividuals().get(rn);
			tournamentIndvs.add(icheck);
		}
		double ppp = random.nextDouble();
		if(ppp<parameters.GetTournamentRate()){
			ibest = FindTheBest(tournamentIndvs);
		}
		else{
			ibest = FindSndBest(tournamentIndvs);
		}
		return ibest;
	}

	public List<Individual> Crossover(){
		double iproba = 1.00;
		int isize = GetSelecteds().size();

		if(isize<2){
			System.out.println("#ERROR: nothing to be crossover");
			return GetSelecteds();
		}

		for(int i=0; i<isize; i+=2){
			if(parameters.GetCrossoverRate()<random.nextDouble()){
				continue;
			}
			Individual b1 = GetSelecteds().get(i);
			Individual b2 = GetSelecteds().get(i+1);
			switch (parameters.GetCrossoverPolicy()){
				case "One-Point":
					b1.crossoverOnePoint(b2);
					break;
				case "Two-Point":
					b1.crossoverTwoPoint(b2);
					break;
				case "Uniformly":
					b1.crossoverUniformly(b2);
					break;
				default:
					break;

			}
		}
		return GetSelecteds();
	} 


	public List<Individual> Mutation(){

		for(Individual indv: GetSelecteds()){
			if(parameters.GetMutationRate()<random.nextDouble()){
				continue;
			}
			switch(parameters.GetMutationPolicy()){
				case "Randomchange":
					indv.mutationRandomChange();
					break;
				case "Interchange":
					indv.Evaluate();//crossing genes
					indv.mutationInterChange();
					break;
				default:
					break;
			}
		}
		return GetSelecteds();
	}

	/*best in original cover worst in next */
	public Population Replace(){
		Individual iworst = FindTheWorst(GetIndividuals());
        Individual ibest = FindTheBest(GetSelecteds());
        if(ibest.Fitness()>iworst.Fitness()){
			ibest.Duplicate(iworst);
        }
		replacement = 1;
        return this;
	}


	public Population Evolve(){
		
		int i=0;
		Population nextpop = new Population(parameters);
		Individual indvAdd = null;

		/* for special test */
		int currentOpt = parameters.GetOption();
		/* for special test */
		if(currentOpt==5){
			indvAdd = FindTheBest(GetIndividuals()).Duplicate();
			nextpop.increase(indvAdd);
		}

		if(parameters.GetSelectionPolicy() == "Roulette-Wheel"){
			GenFitnessPortions();
		}

		int totalSize = 0;
		while(totalSize<parameters.GetPopulationSize()){
			indvAdd = null;
			Selection();
			/* for special test */
			if(currentOpt==5){
				for(Individual indv: GetSelecteds()){
					indvAdd = indv.Duplicate();//avoid reference
					nextpop.increase(indvAdd);
				}
			}

			Crossover();
			Mutation();
			for(Individual indv: GetSelecteds()){
				indv.Evaluate();
				nextpop.increase(GetSelecteds());
			}
			//Replace();
			if(currentOpt==2){
				Replace();
			}
			/* special
			indvAdd = FindTheBest(GetSelecteds()).Duplicate();
			nextpop.increase(indvAdd);
			totalSize+=1;
			*/
			totalSize += GetSelecteds().size();	
		}
		nextpop.SetAge(++i);
		return nextpop;
	}
	
	public Individual FindTheWorst(List<Individual> ilist){
		double rmin = 0.00;
		Individual indv = null;

		for(Individual tmpIndv: ilist){
			double tmpTime = tmpIndv.Duration();
			if(tmpTime>rmin){
				indv=tmpIndv;
				rmin=tmpTime;
			}
		}
		return indv;
	}

	public Individual FindTheBest(List<Individual> ilist){
		double rmax = Double.MAX_VALUE;
		Individual indv = null;
		for(Individual tmpIndv: ilist){
			double tmpTime = tmpIndv.Duration();
			if(tmpTime<rmax){
				indv=tmpIndv;
				rmax=tmpTime;
			}
		}
		return indv;
	}
	public Individual FindSndBest(List<Individual> ilist) {
        int b1 = 0;
        int b2 = 0;
        for (int i = 0; i < ilist.size(); i++) {
            if (ilist.get(i).Fitness() > ilist.get(b1).Fitness()) {
                b2 = b1;
                b1 = i;
            } 
			else if (ilist.get(i).Fitness() > ilist.get(b2).Fitness()) {
                b2 = i;
            }
        }
        return ilist.get(b2);
	}
}
