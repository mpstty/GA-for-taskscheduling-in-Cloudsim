/*created by mazw at 20180302*/

package lab.cloudsim.taskscheduling; 

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Iterator;
import java.util.Random;
import java.io.File;
import java.lang.reflect.*;
import java.util.function.*;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.CloudletScheduler;
import org.cloudbus.cloudsim.CloudletSchedulerSpaceShared;
import org.cloudbus.cloudsim.CloudletSchedulerTimeShared;

import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.DatacenterCharacteristics;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;

import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelFull;

import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmAllocationPolicy;
import org.cloudbus.cloudsim.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.VmScheduler;
import org.cloudbus.cloudsim.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.VmSchedulerSpaceShared;

import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

public class Establishment {

	private Document docConfig = null;
	public List<Datacenter> datacenters = null;
	public List<DatacenterBroker> datacenterbrokers = null;

	private static int totalHosts = 0;
	private static int totalVMs = 0;
	private static int totalCloudlets = 0;

	public void parseConfigFile(String filename) throws DocumentException {
		File inFile = new File(filename);
		SAXReader reader = new SAXReader();
		docConfig = reader.read(inFile);
	}

	public Document getDocument(){
		return docConfig;
	}

	public List<Datacenter> createDatacenters() {

		List<Node> nodeList = docConfig.selectNodes("//establishment/datacenter[@valid='true']");
		List<Datacenter> dcArray = new ArrayList<Datacenter>();

		int i=0;
		for (Iterator<Node> iter = nodeList.iterator(); iter.hasNext(); ) {
			Node node=iter.next();
			dcArray.add(createDatacenter(node));
		}
		return dcArray;
	}

	public Datacenter createDatacenter(Node current){
		String name = current.selectSingleNode("name").getText();
		//System.out.println("    Creating " + current.getName() + ": " + name + "...");
		String arch = current.selectSingleNode("arch").getText();
		String os = current.selectSingleNode("os").getText();
		String vmm = current.selectSingleNode("vmm").getText();
		double timezone = Double.parseDouble(current.selectSingleNode("timezone").getText());
		double costPerProc = Double.parseDouble(current.selectSingleNode("cost/proc").getText());
		double costPerMem = Double.parseDouble(current.selectSingleNode("cost/memory").getText());
		double costPerStorage = Double.parseDouble(current.selectSingleNode("cost/storage").getText());
		double costPerBw = Double.parseDouble(current.selectSingleNode("cost/bandwidth").getText());
		LinkedList<Storage> storageList = new LinkedList<Storage>();

		String policyName = current.selectSingleNode("policy").getText();
		int count = Integer.parseInt(current.selectSingleNode("count").getText());

		List<Host> hostList = new ArrayList<Host>();
		List<Node> nodeList = current.selectNodes("hosts[@valid='true']");
		for (Iterator<Node> iter = nodeList.iterator(); iter.hasNext(); ) {
			Node node=iter.next();
			hostList.addAll(createHosts(node));
		}

		DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
                arch, os, vmm, hostList, timezone, costPerProc, costPerMem, costPerStorage, costPerBw);

		Datacenter datacenter = null;
		VmAllocationPolicy policy = null;
		try{
			Class<?> PolicyClass = Class.forName(policyName); 
			Constructor<?> PolicyConstruct = PolicyClass.getConstructor(List.class);
			policy = (VmAllocationPolicy)PolicyConstruct.newInstance(hostList);
			datacenter = new Datacenter(name, characteristics, policy, storageList, 0);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return datacenter;
	}

	public List<Host> createHosts(Node current) {
		int count = Integer.parseInt(current.selectSingleNode("count").getText());
		int pes = Integer.parseInt(current.selectSingleNode("pes").getText());
		int mips = Integer.parseInt(current.selectSingleNode("mips").getText());
		int ram = Integer.parseInt(current.selectSingleNode("ram").getText());
		int storage = Integer.parseInt(current.selectSingleNode("storage").getText());
		int bw = Integer.parseInt(current.selectSingleNode("bandwidth").getText());
		String policyName = current.selectSingleNode("policy").getText();

		List<Pe> tmpPeList = new ArrayList<Pe>();
		for(int j=0; j<pes; j++) {
			tmpPeList.add(new Pe(j, new PeProvisionerSimple(mips))); 
		}
		List<Host> hostList = new ArrayList<Host>();
		int hostId = 0;
		for(int i=0; i<count; i++){
			List<Pe> peList = new ArrayList<Pe>();
			peList.addAll(tmpPeList);

			VmScheduler policy = null;
			try {
				Class<?> PolicyClass = Class.forName(policyName); 
				Constructor<?> PolicyConstruct = PolicyClass.getConstructor(List.class);
				policy = (VmScheduler)PolicyConstruct.newInstance(peList);

			}
			catch (Exception e) {
				e.printStackTrace();
			}

			hostId = i + totalHosts;
			hostList.add(
    			new Host(
    				hostId,
    				new RamProvisionerSimple(ram),
    				new BwProvisionerSimple(bw),
    				storage,
    				peList,
    				policy	
    			)
    		); 
		}
		totalHosts += hostList.size();
		return hostList;
	}

	public List<DatacenterBroker> createDatacenterBrokers() {
		List<Node> nodeList = docConfig.selectNodes("//establishment/broker[@valid='true']");
		List<DatacenterBroker> dcBrokerArray = new ArrayList<DatacenterBroker>();

		int i=0;
		for (Iterator<Node> iter = nodeList.iterator(); iter.hasNext(); ) {
			Node node=iter.next();
			dcBrokerArray.add(createDatacenterBroker(node));
		}
		return dcBrokerArray;
	}

	public DatacenterBroker createDatacenterBroker(Node current){
		String name = current.selectSingleNode("name").getText();
		Node policyNode = current.selectSingleNode("policy");
		String policyName = policyNode.getText();
		DatacenterBroker policy = null;
		Class<?> PolicyClass = null; 
		try {
			PolicyClass = Class.forName(policyName); 
			Constructor<?> PolicyConstruct = PolicyClass.getConstructor(String.class);
			policy = (DatacenterBroker)PolicyConstruct.newInstance(name);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		DatacenterBroker broker = policy;
		List<Vm> vmList = new LinkedList<Vm>();
		List<Node> nodeList = current.selectNodes("vms[@valid='true']");
	//	int idShift = 0;
		for (Iterator<Node> iter = nodeList.iterator(); iter.hasNext(); ) {
			Node node=iter.next();
			vmList.addAll(createVMs(node, broker));
		}

		List<Cloudlet> cloudletList = new LinkedList<Cloudlet>();
		nodeList = current.selectNodes("cloudlets[@valid='true']");
//		idShift = 0;
		for (Iterator<Node> iter = nodeList.iterator(); iter.hasNext(); ) {
			Node node=iter.next();
			cloudletList.addAll(createCloudlets(node, broker));
		}

		broker.submitVmList(vmList);
		broker.submitCloudletList(cloudletList);

		/*Post Steps*/
		policyNode = current.selectSingleNode("bindvmcloudlet");
		if(policyNode!=null){
			policyName = policyNode.getText();
			PolicyClass = null; 
			try {
				PolicyClass = Class.forName(policyName); 
				Constructor<?> PolicyConstruct = PolicyClass.getConstructor(
														List.class, List.class);
				Object obj = PolicyConstruct.newInstance(vmList, cloudletList);
				Method PolicyApproach = PolicyClass.getMethod("Execute");
				List<Integer>  results = (List<Integer>)PolicyApproach.invoke(obj);	
				for(int i=0; i<results.size(); i+=2){
					int vmId = results.get(i);
					int ctId = results.get(i+1);
					broker.bindCloudletToVm(ctId, vmId);
				}
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		return broker;	
	}

	public List<Vm> createVMs(Node current, DatacenterBroker broker) {
		int count = Integer.parseInt(current.selectSingleNode("count").getText());
		String policyName = current.selectSingleNode("policy").getText();

		int imagesize = Integer.parseInt(current.selectSingleNode("imagesize").getText());
		int mips = Integer.parseInt(current.selectSingleNode("mips").getText());
		int ram = Integer.parseInt(current.selectSingleNode("ram").getText());
		int bw = Integer.parseInt(current.selectSingleNode("bandwidth").getText());
		int pes = Integer.parseInt(current.selectSingleNode("pes").getText());
		String vmm = current.selectSingleNode("vmm").getText();
		int userId = broker.getId();

		int vmId = 0;
		LinkedList<Vm> vmList = new LinkedList<Vm>();
		for(int i=0;i < count;i++){
			CloudletScheduler policy = null;
			try {
				Class<?> PolicyClass = Class.forName(policyName); 
				//if(configNode!=null && configNode.hasContent()){
				//	Constructor<?> PolicyConstruct = PolicyClass.getConstructor(Node.class);
				//	policy = (CloudletScheduler)PolicyConstruct.newInstance(configNode);
				//}
				//else{
					Constructor<?> PolicyConstruct = PolicyClass.getConstructor();
					policy = (CloudletScheduler)PolicyConstruct.newInstance();
				//}
			}
			catch (Exception e) {
				e.printStackTrace();
			}
			vmId = i + totalVMs;
			mips+=50;//2018
			Vm vm = new Vm(vmId, userId, mips, pes, ram, bw, imagesize, vmm, policy);
			vmList.add(vm);
		}
		totalVMs += vmList.size();
		return vmList;
	}

	public List<Cloudlet> createCloudlets(Node current, DatacenterBroker broker) {
		int count = Integer.parseInt(current.selectSingleNode("count").getText());
		String policyName = current.selectSingleNode("policy").getText();
		int inputsize = Integer.parseInt(current.selectSingleNode("inputsize").getText());
		int outputsize = Integer.parseInt(current.selectSingleNode("outputsize").getText());
		int length = Integer.parseInt(current.selectSingleNode("length").getText());
		int pes = Integer.parseInt(current.selectSingleNode("pes").getText());
		int userId = broker.getId();

		UtilizationModel policy = null;
		try {
			Class<?> PolicyClass = Class.forName(policyName); 
			Constructor<?> PolicyConstruct = PolicyClass.getConstructor();
			policy = (UtilizationModel)PolicyConstruct.newInstance();
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		Random ranDom = new Random();
		int cloudletId = 0;
		LinkedList<Cloudlet> cloudletList = new LinkedList<Cloudlet>();
		for(int i=0; i < count; i++){
			int ranLen = 0;
			//length = ranDom.nextInt(1000)+1000;//random 2018
			//ranLen += ranDom.nextInt(length);
			ranLen = length+100*i;
			cloudletId = totalCloudlets + i;
			Cloudlet cloudlet = new Cloudlet(cloudletId, ranLen, pes, inputsize, 
												outputsize, policy, policy, policy);
			cloudlet.setUserId(userId);
			cloudletList.add(cloudlet);
		}
		totalCloudlets += cloudletList.size();
		return cloudletList;
	}
	
	public Establishment(String filepath) {
		try{
			parseConfigFile(filepath);
			
			datacenters = createDatacenters();
			datacenterbrokers = createDatacenterBrokers();
		}
		catch (DocumentException e){
			e.printStackTrace();
		}
	}
}
