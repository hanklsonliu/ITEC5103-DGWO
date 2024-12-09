package gwoPsoOptimzer;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelFull;

import java.text.DecimalFormat;
import java.util.*;

public class CloudSimGWOTest1T {
	protected static List<Cloudlet> cloudletList;
    protected static List<Vm> vmList;
    protected static final int WOLF_POPULATION = 100;
    protected static final int MAX_ITERATIONS = 50;
    
    // GWO parameters
    protected static double a;
    protected static final int SEARCH_AGENTS = 30;
    protected static final int DIMENSION = 50;
    
    public CloudSimGWOTest1T (List<Cloudlet> cloudletList, List<Vm> vmList) {
    	this.cloudletList = cloudletList;
		this.vmList = vmList;
		applyGWO();
    }
    
    /*
    public static void main(String[] args) {
        try {
            int num_user = 1;
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = false;
            
            // Initialize CloudSim
            CloudSim.init(num_user, calendar, trace_flag);
            
            // Create Datacenter
            Datacenter datacenter0 = createDatacenter("Datacenter_0");
            
            // Create Broker
            DatacenterBroker broker = createBroker();
            int brokerId = broker.getId();
            
            // Create VMs and Cloudlets
            vmList = createVMs(brokerId);
            cloudletList = createCloudlets(brokerId);
            
            // Submit VM list to broker
            broker.submitVmList(vmList);
            
            // Apply GWO Algorithm
            List<Cloudlet> optimizedCloudlets = applyGWO();
            
            // Submit cloudlet list to broker
            broker.submitCloudletList(optimizedCloudlets);
            
            // Start simulation
            CloudSim.startSimulation();
            
            // Stop simulation
            CloudSim.stopSimulation();
            
            // Print results
            List<Cloudlet> newList = broker.getCloudletReceivedList();
            printCloudletList(newList);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }*/
    
    public static List<Cloudlet> applyGWO() {
        double[][] positions = new double[SEARCH_AGENTS][DIMENSION];
        double[][] alphaPos = new double[1][DIMENSION];
        double[][] betaPos = new double[1][DIMENSION];
        double[][] deltaPos = new double[1][DIMENSION];
        double alphaScore = Double.POSITIVE_INFINITY;
        double betaScore = Double.POSITIVE_INFINITY;
        double deltaScore = Double.POSITIVE_INFINITY;
        
        // Initialize positions randomly
        Random rand = new Random();
        for (int i = 0; i < SEARCH_AGENTS; i++) {
            for (int j = 0; j < DIMENSION; j++) {
                positions[i][j] = rand.nextInt(vmList.size());
            }
        }
        
        // Main loop
        for (int iteration = 0; iteration < MAX_ITERATIONS; iteration++) {
            // Update a
            a = 2 - iteration * (2.0 / MAX_ITERATIONS);
            
            // Evaluate each search agent
            for (int i = 0; i < SEARCH_AGENTS; i++) {
                double fitness = calculateFitness(positions[i]);
                
                // Update Alpha, Beta, and Delta
                if (fitness < alphaScore) {
                    alphaScore = fitness;
                    System.arraycopy(positions[i], 0, alphaPos[0], 0, DIMENSION);
                } else if (fitness > alphaScore && fitness < betaScore) {
                    betaScore = fitness;
                    System.arraycopy(positions[i], 0, betaPos[0], 0, DIMENSION);
                } else if (fitness > alphaScore && fitness > betaScore && fitness < deltaScore) {
                    deltaScore = fitness;
                    System.arraycopy(positions[i], 0, deltaPos[0], 0, DIMENSION);
                }
            }
            
            // Update positions
            for (int i = 0; i < SEARCH_AGENTS; i++) {
                for (int j = 0; j < DIMENSION; j++) {
                    double r1 = rand.nextDouble();
                    double r2 = rand.nextDouble();
                    
                    double A1 = 2 * a * r1 - a;
                    double C1 = 2 * r2;
                    
                    double D_alpha = Math.abs(C1 * alphaPos[0][j] - positions[i][j]);
                    double X1 = alphaPos[0][j] - A1 * D_alpha;
                    
                    r1 = rand.nextDouble();
                    r2 = rand.nextDouble();
                    
                    double A2 = 2 * a * r1 - a;
                    double C2 = 2 * r2;
                    
                    double D_beta = Math.abs(C2 * betaPos[0][j] - positions[i][j]);
                    double X2 = betaPos[0][j] - A2 * D_beta;
                    
                    r1 = rand.nextDouble();
                    r2 = rand.nextDouble();
                    
                    double A3 = 2 * a * r1 - a;
                    double C3 = 2 * r2;
                    
                    double D_delta = Math.abs(C3 * deltaPos[0][j] - positions[i][j]);
                    double X3 = deltaPos[0][j] - A3 * D_delta;
                    
                    positions[i][j] = (X1 + X2 + X3) / 3;
                    
                    // Ensure position is within bounds
                    positions[i][j] = Math.max(0, Math.min(vmList.size() - 1, Math.round(positions[i][j])));
                }
            }
        }
        
        // Apply the best solution (Alpha position)
        return applySchedulingSolution(alphaPos[0]);
    }
    
    public static double calculateFitness(double[] position) {
        double totalExecutionTime = 0;
        double[] vmLoadTime = new double[vmList.size()];
        
        for (int i = 0; i < position.length; i++) {
            int vmIndex = (int) position[i];
            Cloudlet cloudlet = cloudletList.get(i);
            Vm vm = vmList.get(vmIndex);
            
            // Calculate execution time for this cloudlet on the assigned VM
            double executionTime = cloudlet.getCloudletLength() / vm.getMips();
            vmLoadTime[vmIndex] += executionTime;
            totalExecutionTime = Math.max(totalExecutionTime, vmLoadTime[vmIndex]);
        }
        
        return totalExecutionTime;
    }
    
    public static List<Cloudlet> applySchedulingSolution(double[] solution) {
        for (int i = 0; i < solution.length; i++) {
            Cloudlet cloudlet = cloudletList.get(i);
            int vmIndex = (int) solution[i];
            cloudlet.setVmId(vmList.get(vmIndex).getId());
        }
        return cloudletList;
    }
    
    static Datacenter createDatacenter(String name) {
        Scanner scanner = new Scanner(System.in);
        int hostID = 0;
        int ram = 16000;
        int storage = 100000;
        int bw = 100000;
        int mips = 100000;
        int hostNum = 0;
        int resourceScaler = 1;
        List<Host> hostList = new ArrayList<Host>();
        
        System.out.print("How many host machines for the Datacentre?: ");
        hostNum = scanner.nextInt();
        
        for (int i = 0; i < hostNum; i++) {
            List<Pe> peList = new ArrayList<>();
            peList.add(new Pe(0, new PeProvisionerSimple(mips * resourceScaler)));

            hostList.add(new Host(
                hostID, 
                new RamProvisionerSimple(ram * resourceScaler),
                new BwProvisionerSimple(bw * resourceScaler), 
                storage * resourceScaler,
                peList,
                new VmSchedulerTimeShared(peList)
            ));
            hostID++;
            resourceScaler++;
        }

        String arch = "X_86";
        String os = "Linux";
        String vmm = "Xen";
        double timeZone = 10.0;
        double cost = 3.00;
        double costPerMem = 0.05;
        double costPerStorage = 0.001;
        double costPerBw = 0.1;
        
        LinkedList<Storage> storageList = new LinkedList<Storage>();
        
        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
            arch, os, vmm, hostList, timeZone, cost, costPerMem, costPerStorage, costPerBw
        );
        
        try {
            return new Datacenter(name, characteristics, new VmAllocationPolicySimple(hostList), storageList, 0);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    static DatacenterBroker createBroker() {
        DatacenterBroker broker = null;
        try {
            broker = new DatacenterBroker("Broker");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return broker;
    }
    
    static List<Vm> createVMs(int brokerId) {
        Scanner scanner = new Scanner(System.in);
        List<Vm> vms = new ArrayList<Vm>();
        
        System.out.print("How many Virtual Machines?: ");
        int userVM = scanner.nextInt();
        
        int vmID = 0;
        int mips = 1000;
        long size = 1000;
        int ram = 1024;
        long bw = 1000;
        int pesNumber = 1;
        String vmm = "Xen";
        int multiplier = 1;
        
        for (int i = 0; i < userVM; i++) {
            vms.add(new Vm(vmID, brokerId, mips * multiplier, pesNumber * multiplier, 
                          ram * multiplier, bw * multiplier, size * multiplier, 
                          vmm, new CloudletSchedulerTimeShared()));
            vmID++;
            multiplier++;
            if (multiplier > 4) {
                multiplier = 1;
                ram = 512;
                mips = 500;
                bw = 500;
                pesNumber = 1;
                size = 500;
            }
        }
        return vms;
    }
    
    static List<Cloudlet> createCloudlets(int brokerId) {
        Scanner scanner = new Scanner(System.in);
        List<Cloudlet> cloudlets = new ArrayList<Cloudlet>();
        
        System.out.print("How many cloudlets? (Please enter a value under 500): ");
        int userCloud = scanner.nextInt();
        
        while (userCloud > 500) {
            System.out.println("You have entered a value that exceeds the limitation. Please re-enter here:");
            userCloud = scanner.nextInt();
        }
        
        int cid = 0;
        long length = 4000;
        long fileSize = 300;
        long outputSize = 300;
        int pesNumber = 1;
        UtilizationModel utilizationModel = new UtilizationModelFull();
        
        for (int i = 0; i < userCloud; i++) {
            Cloudlet cloudlet = new Cloudlet(cid, length, pesNumber, fileSize, 
                                           outputSize, utilizationModel, 
                                           utilizationModel, utilizationModel);
            cloudlet.setUserId(brokerId);
            cloudlets.add(cloudlet);
            cid++;
        }
        return cloudlets;
    }
    
    private static void printCloudletList(List<Cloudlet> list) {
        int size = list.size();
        Cloudlet cloudlet;
        
        String indent = "    ";
        System.out.println();
        System.out.println("========== OUTPUT ==========");
        System.out.println("Cloudlet ID" + indent + "STATUS" + indent + "Data center ID" + indent + "VM ID" + indent + "Time" + indent + "Start Time" + indent + "Finish Time");
        
        DecimalFormat dft = new DecimalFormat("###.##");
        for (int i = 0; i < size; i++) {
            cloudlet = list.get(i);
            System.out.printf(indent + cloudlet.getCloudletId() + indent + indent);
            
            if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS) {
                System.out.printf("SUCCESS");
                System.out.printf(indent + indent + cloudlet.getResourceId() + indent + indent + indent + cloudlet.getVmId() +
                    indent + indent + dft.format(cloudlet.getActualCPUTime()) + indent + indent + dft.format(cloudlet.getExecStartTime()) +
                    indent + indent + dft.format(cloudlet.getFinishTime()) + "\n");
            }
        }
    }
}