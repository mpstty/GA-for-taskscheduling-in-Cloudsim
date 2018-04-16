/*created by mazw at 20180305*/

package lab.cloudsim.taskscheduling;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

public class Application {
	private double totalStartTime = 0.00;
	private double totalDoneTime = 0.00;
	private double totalActualTime = 0.00;
	private int totalVMs = 0;//vms.size();//+1;//*2;
	private final String indent = "    ";
/*
	private List<Cloudlet> ctsList = null;
	private List<Vm> vmsList = null;
*/
	private List<Cloudlet> cloudletList = null;
	private List<Vm> vmList = null;

	public Application(){
		cloudletList = new LinkedList<Cloudlet>();
		vmList = new LinkedList<Vm>();
	}

	private void PrintPerformances(){
		Log.formatLine("Utilization = [ Total_Busy_Time / (Total_Finish_Time * Number of VMs)] * 100");
		
		double utilization = 100 * totalActualTime/(totalDoneTime*totalVMs);
		Log.formatLine("Utilization = [ %f / ( %f * %d)] * 100 = %f", 
							totalActualTime, totalDoneTime, totalVMs, utilization);

		System.out.println("");
		System.out.println("");
		System.out.println(indent+indent+"=============================================");
		System.out.println("");

		System.out.format("%s%s%s%s%s%s%s%s%s %n",
							indent, indent, "MakeSpan", 
							indent, indent, "Utilization", 
							indent, indent, "Busy");
		System.out.format("%s%s%.4f%s%s%.4f%s%s%s%.4f %n",
							indent, indent, totalDoneTime,
							indent, indent, utilization,
							indent, indent, indent, totalActualTime);
		System.out.println("");
		System.out.println(indent+indent+"=============================================");

		System.out.println("");
		System.out.println("");
	}
	
	private void PrintCloudlets(){
		int size = cloudletList.size();
		Cloudlet cloudlet = null;
		String status = "";

		double startTime = 0.00;
		double doneTime = 0.00;
		double actualTime = 0.00;
		int vid = 0;

		totalStartTime = cloudletList.get(0).getExecStartTime();
		totalVMs = vmList.size();
		double[] vtimeList = new double[totalVMs];
		for(int i=0; i<totalVMs; i++) vtimeList[i] = 0.00;

		Log.printLine();
		Log.print(indent + indent + indent + indent);
		Log.printLine("========== OUTPUT ==========");
		Log.printLine(	"Cloudlet ID"	+ indent + 
						"STATUS"		+ indent +
						"DC ID"			+ indent + 
						"VM ID"			+ indent +  
						"Time"			+ indent + 
						"Start "		+ indent + 
						"Finish ");

		DecimalFormat dft = new DecimalFormat("###.##");
		for (int i = 0; i < size; i++) {
			cloudlet = cloudletList.get(i);
			status = (cloudlet.getStatus() == Cloudlet.SUCCESS)?"SUCCESS":"FAILED";

			startTime = cloudlet.getExecStartTime();
			doneTime = cloudlet.getFinishTime();
			actualTime = cloudlet.getActualCPUTime();
			vid = cloudlet.getVmId();

			Log.printLine(indent +
				cloudlet.getCloudletId()	+ indent + indent + 
				status						+ indent + indent +
				cloudlet.getResourceId()	+ indent + indent +
				vid							+ indent + 
				dft.format(actualTime)		+ indent + 
				dft.format(startTime)		+ indent + 
				dft.format(doneTime));
/* */

			doneTime = cloudlet.getFinishTime();
			totalDoneTime = doneTime>totalDoneTime?doneTime:totalDoneTime;
			vtimeList[vid] = vtimeList[vid]>actualTime?vtimeList[vid]:actualTime;
		}
		totalDoneTime -= totalStartTime;
		for(int i=0; i<totalVMs; i++){
			totalActualTime += (vtimeList[i]-totalStartTime);
		}
	}

	private void Logfile(String filename){
		OutputStream logFile = null;
		try{
			logFile = new FileOutputStream(filename);
		}
		catch(IOException e){
			e.printStackTrace();
		}
		Log.setOutput(logFile);
	}

	private void Run(String confs){
		boolean traced = false;
		int users = 1;

		Log.printLine("Starting " + Application.class.getName() + "...");

		Calendar calendar = Calendar.getInstance();	
		CloudSim.init(users, calendar, traced); 

		Establishment myEstConfig = new Establishment(confs);
		
		CloudSim.startSimulation();

//		List<Datacenter> dcArray = myEstConfig.datacenters;
		List<DatacenterBroker> brokerArray = myEstConfig.datacenterbrokers;

		for(int i=0; i<brokerArray.size(); i++){
			cloudletList.addAll(brokerArray.get(i).getCloudletReceivedList());
			vmList.addAll(brokerArray.get(i).getVmList());
		}
        CloudSim.stopSimulation();
        Log.printLine(Application.class.getName() + " finished!");
	}

	public static void main(String[] args) {

		Application tsapp = new Application();//cloudletList, vmList);
		tsapp.Logfile(args[0]);
		tsapp.Run(args[1]);
        tsapp.PrintCloudlets();
        tsapp.PrintPerformances();
	}
}

