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

	private static void printVMsHostsDCs(List<Datacenter> dcs){
		//TODO:
	}
	
	private static void printCloudletList(List<Cloudlet> list, List<Vm> vms) {

		double total_over_time = 0.00;

		int vm_count = vms.size()*2;
		double[] vm_time = new double[vm_count];
		for(int i=0; i<vm_count; i++){
			vm_time[i]=0.00;
		}
		int size = list.size();
		Cloudlet cloudlet = null;

		String indent = "    ";
		Log.printLine();
		Log.printLine("========== OUTPUT ==========");
		Log.printLine("Cloudlet ID" + indent + "STATUS" + indent +
						"Data center ID" + indent + "VM ID" + indent + indent + 
						"Time" + indent + "Start Time" + indent + "Finish Time");

		DecimalFormat dft = new DecimalFormat("###.##");
		for (int i = 0; i < size; i++) {
			cloudlet = list.get(i);
			Log.print(indent + cloudlet.getCloudletId() + indent + indent);

			/* deprecation:
			 * if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS){
			 */
			if (cloudlet.getStatus() == Cloudlet.SUCCESS){
				Log.print("SUCCESS");
			}
			else{
				Log.print("FAILED");
			}
		//	{
				Log.printLine( indent + indent + 
							cloudlet.getResourceId() + 
								indent + indent + indent + 
							cloudlet.getVmId() +
								indent + indent + indent + 
							dft.format(cloudlet.getActualCPUTime()) +
								indent + indent + 
							dft.format(cloudlet.getExecStartTime()) + 
								indent + indent + indent + 
							dft.format(cloudlet.getFinishTime()));
		//	}

			double tmpTime = cloudlet.getFinishTime();
			total_over_time = tmpTime>total_over_time?tmpTime:total_over_time;

			int indx = cloudlet.getVmId();
			vm_time[indx] = vm_time[indx]>cloudlet.getActualCPUTime()?vm_time[indx]:cloudlet.getActualCPUTime();
		}

		double total_init_time = 0.00;
		double total_busy_time = 0.00;

		total_init_time = list.get(0).getExecStartTime();
		total_over_time -= total_init_time;

		for(int i=0; i<vm_count; i++){
			total_busy_time += (vm_time[i]-total_init_time);
		}
		Log.formatLine("Utilization = [ total busy time / (total finish time * number of VMs)] * 100");
		
		double utilization = 100 * total_busy_time/(total_over_time*vms.size());
		Log.formatLine("Utilization = [ %f / ( %f * %d)] * 100 = %f", 
							total_busy_time, total_over_time, vms.size(), utilization);

		System.out.println("");
		System.out.println("");
		System.out.println(indent+indent+"=============================================");
		System.out.println("");

		System.out.format("%s%s%s%s%s%s%s%s%s %n",
							indent, indent, "MakeSpan", 
							indent, indent, "Utilization", 
							indent, indent, "Busy");
		System.out.format("%s%s%.4f%s%s%.4f%s%s%s%.4f %n",
							indent, indent, total_over_time,
							indent, indent, utilization,
							indent, indent, indent, total_busy_time);
		System.out.println("");
		System.out.println(indent+indent+"=============================================");

		System.out.println("");
		System.out.println("");
	}

	private static void useLogfile(){
		OutputStream logFile = null;
		try{
			logFile = new FileOutputStream("log/ga.log");
		}
		catch(IOException e){
			e.printStackTrace();
		}
		Log.setOutput(logFile);
	}

	public static void main(String[] args) {
		boolean traced = false;
		int users = 1;

		useLogfile();
		Log.printLine("Starting " + Application.class.getName() + "...");

		Calendar calendar = Calendar.getInstance();	
		CloudSim.init(users, calendar, traced); 

		Establishment myEstConfig = new Establishment("etc/establishment.xml");
		
		CloudSim.startSimulation();

		List<Datacenter> dcArray = myEstConfig.datacenters;
		List<DatacenterBroker> brokerArray = myEstConfig.datacenterbrokers;
        List<Cloudlet> cloudletList = new LinkedList<Cloudlet>();
		List<Vm> vmList = new LinkedList<Vm>();

		for(int i=0; i<brokerArray.size(); i++){
			cloudletList.addAll(brokerArray.get(i).getCloudletReceivedList());
			vmList.addAll(brokerArray.get(i).getVmList());//getVmsCreatedList());//getVmList());
		}

        CloudSim.stopSimulation();
        printCloudletList(cloudletList, vmList);

        Log.printLine(Application.class.getName() + " finished!");
	}
}

