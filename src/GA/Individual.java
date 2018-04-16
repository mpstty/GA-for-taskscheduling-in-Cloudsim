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
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.lists.VmList;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.lists.CloudletList;

public class Individual {//implements Cloneable{
	private class Atom {//implements Clonable{
		private int Id = 0;
		private Vm resource = null;
		private Cloudlet task = null;
		private String binString = "";
		private double procTime = 0.00;

		public Atom(Vm k, Cloudlet s){
			resource = k;
			task = s;
			Id = s.getCloudletId();
		}

		private Atom(Atom old){
			Id = old.Id;
			resource = old.resource;
			task = old.task;
			binString = old.binString;
			procTime = old.procTime;
		}

		public Atom Duplicate(){
			return new Atom(this);
		}

		public int GetId(){
			return Id;
		}

/*
		public void SetId(iid){
			Id = iid;
		}
*/

		public Vm GetVM(){
			return resource;
		}

		public void SetVM(Vm k){
			resource = k;
		}

		public Cloudlet GetCloudlet(){
			return task;
		}

		public double GetProcTime(){
			return procTime;
		}

		public double Estimate(){
			Vm tmpVm = GetVM();
			Cloudlet tmpCt = GetCloudlet();
			procTime = tmpCt.getCloudletLength()/(tmpVm.getMips()*tmpVm.getNumberOfPes());
			return procTime;
		}

		public void SetCloudlet(Cloudlet s){
			task = s;
		}

		public void Show(){
			System.out.print(GetVM().getId()+"|");
		}

		private String toBinString(int number, int bitscnt){
			String result = "";
			String fills = ""; 
			int count=0;
			while( number > 0 ) {
				result = (number & 1) + result;
				number >>= 1;
				count++;
			}
			while(count<bitscnt){
				fills += "0";
				count++; 
			}
			result = fills +result; 
			return result;
		}

		public String Encode(int length){
			Vm tmpVm = GetVM();
			Cloudlet tmpCt = GetCloudlet();
			binString += toBinString(tmpVm.getId(), length);
			binString += ",";
			binString += toBinString(tmpCt.getCloudletId(), length);
			return binString;
		}

		public void SwapWith(Atom old){
			Vm tmpVm = old.resource;	
			old.SetVM(this.GetVM());
			this.SetVM(tmpVm);
			return ;
		}
	}//end of Atom
	private List<Atom> atomList = new ArrayList<Atom>();
	private Map<Integer, ArrayList<Atom>> geneMap = new HashMap<Integer, ArrayList<Atom>>();
	private Random random = new Random();

	private static int globalIdx = 0; 

	private int selfIdx = 0;
	private double fitValue = 0.00;
	private double doneTime = 0.00;
	private String binaryString = "";

	private Individual(Individual old){//duplication
		//new this
		old.Cover(this);
	}

	public Individual Duplicate(Individual... indvs){
		Individual dup = null;
		if(indvs.length==0){
			dup = new Individual(this);
		}
		else{
			dup = indvs[0];	
			this.Cover(dup);
		}
		dup.Evaluate();
		return dup;
	}

	private Individual Cover(Individual old){
		old.GetAtoms().clear();
		for(Atom atom: GetAtoms()){
			old.GetAtoms().add(atom.Duplicate());
		}
		//Todo: copy the geneMap
		old.fitValue=fitValue;
		old.doneTime=doneTime;
		return old;
	}

	//TODO: make it for derive
	public Individual(List<Vm> vmlist, List<Cloudlet> ctlist){
		for(Cloudlet ct: ctlist){
			int rn = random.nextInt(vmlist.size());
			Vm vm = vmlist.get(rn);
			Atom atom = new Atom(vm, ct);
			atomList.add(atom);
		}
		
		selfIdx=globalIdx++;
/*
		Collections.sort(atomList, new Comparator<Atom>(){
										@Override
										public int compare(Atom g1, Atom g2){
											return g1.GetId() - g2.GetId();
										}
									}
						);
*/
	}


	public List<Atom> GetAtoms(){
		return atomList;
	}
	
	public Map<Integer, ArrayList<Atom>> GetGeneMap (){
		return geneMap;
	}
	
	public int GenIdx(){
		selfIdx = globalIdx++;
		return selfIdx;
	}
	public int Idx(){ //Key()
		return selfIdx;
	}

	public double Evaluate(){
		binaryString = "";

		/* build genemap */
		geneMap.clear();
		ArrayList<Atom> tmpAtomList = null;
		for(Atom atom: GetAtoms()){
			Integer key = new Integer(atom.GetVM().getId());
			if(geneMap.containsKey(key)){
				tmpAtomList = geneMap.get(key);
				tmpAtomList.add(atom);
			}
			else{
				tmpAtomList = new ArrayList<Atom>();
				tmpAtomList.add(atom);
				geneMap.put(key, tmpAtomList);
			}
		}

		/* calculate fitness */
		double tmpTime = 0.00;
		double needTime = 0.00;
		doneTime = 0.00;
		for(Entry<Integer, ArrayList<Atom>> entry: geneMap.entrySet()) {
			needTime = 0.00;
			tmpAtomList = entry.getValue();
			for(Atom atom: tmpAtomList){
				needTime += atom.Estimate();
				/*
				tmpTime = gene.Estimate();
				needTime = needTime>tmpTime?needTime:tmpTime;
				*/
			}
			doneTime = doneTime>needTime?doneTime:needTime;
		}
		/*assume normal time*/
		fitValue=Math.abs(doneTime-0.00)<=0.001?1.00:1.00/doneTime;
		return fitValue;
	}

	public double Duration(){
		return doneTime;
	}

	public double Fitness(){
		return fitValue;
	}

	public void Show(){
		int i=0;
		System.out.format("## Solution[%d]: Duration=%f, Fitness=%f %n",
							Idx(),  Duration(), Fitness());
		System.out.println(" ");

		for(Entry<Integer, ArrayList<Atom>> entry: geneMap.entrySet()){
			double tmpTime = 0.00;
			System.out.print("VM["+entry.getKey()+"]: ");
			for(Atom atom: entry.getValue()){
				tmpTime += atom.GetProcTime();
				System.out.format("%04d ",atom.GetCloudlet().getCloudletId());
			}
			System.out.format("; Time=(%.4f)", tmpTime);
			System.out.println(" ");
		}
	}

	public boolean crossoverOnePoint(Individual oppr){
		Individual b1 = this;
		Individual b2 = oppr;
		int size=0, p1=0, p2=0, pp=0;

		size = b1.GetAtoms().size();
		p1 = random.nextInt(size);
		for(int j=0; j<=p1; j++){
			Atom g1 = b1.GetAtoms().get(j);
			Atom g2 = b2.GetAtoms().get(j);
			g1.SwapWith(g2);
		}
		return true;
	}

	public boolean crossoverTwoPoint(Individual oppr){
		Individual b1 = this;
		Individual b2 = oppr;
		int size=0, p1=0, p2=0, pp=0;
		size = b1.GetAtoms().size();
		p1 = random.nextInt(size);
		p2 = random.nextInt(size);
		if(p1>p2){ pp=p2; p2=p1; p1=pp; }

		for(int j=0; j<=p1; j++){
			Atom g1 = b1.GetAtoms().get(j);
			Atom g2 = b2.GetAtoms().get(j);
			g1.SwapWith(g2);
		}

		for(int j=p2; j<size; j++){
			Atom g1 = b1.GetAtoms().get(j);
			Atom g2 = b2.GetAtoms().get(j);
			g1.SwapWith(g2);
		}
		return true;
	}

	public boolean crossoverUniformly(Individual oppr){
		double aProba = 0.5;
		double bProba = 0.5;
		Individual b1 = this;
		Individual b2 = oppr;
		if(random.nextDouble() < aProba){
			for(int j=0; j<b1.GetAtoms().size(); j++){
				if(random.nextFloat() < bProba){
					b1.GetAtoms().get(j).SwapWith(b2.GetAtoms().get(j));
				}
			}
		}
		return true;
	}

	public boolean mutationRandomChange(){
		Individual b1 = this;
		int size = b1.GetAtoms().size();
		int p1 = random.nextInt(size);
		int p2 = 0;
		do{
			p2 = random.nextInt(b1.GetAtoms().size());
		}while(p1==p2);

		Atom g1 = b1.GetAtoms().get(p1);
		Atom g2 = b1.GetAtoms().get(p2);
		g1.SwapWith(g2);
		return true;
	}

	public boolean mutationInterChange(){
		int size = GetGeneMap().size();
		
		int p1 = random.nextInt(size);
		int p2 = p1;
		while(p1 == p2){
			p2 = random.nextInt(size);
		}
		
		List<Atom> as1 = GetGeneMap().get(new Integer(p1));
		List<Atom> as2 = GetGeneMap().get(new Integer(p2));

		//TODO: find VM via id
		Vm vm1 = as1.get(0).GetVM();
		Vm vm2 = as2.get(0).GetVM();
		for(Atom atom: as1){
			atom.SetVM(vm2);	
		}
		for(Atom atom: as2){
			atom.SetVM(vm1);
		}
		return true;
	}

	public List<Integer> Return(){
		List<Integer> results = new ArrayList<Integer>();
		for(Entry<Integer, ArrayList<Atom>> entry: geneMap.entrySet()){
			for(Atom atom: entry.getValue()){
				results.add(new Integer(atom.GetVM().getId()));
				results.add(new Integer(atom.GetCloudlet().getCloudletId()));
			}
		}
		return results;
	}
}//end of Individual
