import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class Read {
	private static String path = "/home/ksemer/workspaces/BFF/dataset_synth_improv/synthetic_31-05/pa=09/o2/";
	private static String dataset = "rand_";
	private static int ITERATION_DATA = 10;
	private static int DENSE = 100;
	private static boolean precision = true, recall = true;
	static DecimalFormat df;
	static DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols(Locale.US);

	public static void main(String[] args) throws IOException {
		otherSymbols.setDecimalSeparator('.');
		df = new DecimalFormat("#.##", otherSymbols);
		df.setRoundingMode(RoundingMode.CEILING);

		System.out.println("Contiguous");
		init(".txt");
		
		System.out.println("ATLEAST");
		init("_atleast.txt");
		
		System.out.println("Random");
		initR("_it=*.txt");
		
		System.out.println("AGGR");
		init("_aggr.txt");
		
		System.out.println("jaccard");
		init("_sim.txt");
		
		System.out.println("average sim");
		init("_sa.txt");
		
		System.out.println("min sim");
		init("_sd.txt");
		
		System.out.println("dens jac");
		init("_dj.txt");
		
		System.out.println("average dens");
		init("_da.txt");
		
		System.out.println("min dens");
		init("_dm.txt");
	}
	
	public static void init(String extra) throws IOException {
		
		double[][] prec = new double[4][5], rec = new double[4][5];
		double[] res;
		String out;
		int j;
		
		for (int m = 1; m <= 4; m++) {
			out = "";
			j = 0;
			
			for (int k = 2; k <= 8; k+=2) {
				for (int it = 0; it < ITERATION_DATA; it++) {
					res = read(m, k, path + dataset + k  + "_it_" + it + "_m=" + m + "_k=" + k + extra);
					
					prec[m-1][j]+= res[0];
					rec[m-1][j]+= res[1];

					if (k == 2) {
						res = read(m, 3, path + dataset + "3_it_" + it + "_m=" + m + "_k=3" + extra);
						prec[m-1][j+1]+= res[0];
						rec[m-1][j+1]+= res[1];
					}
				}
				

				if (precision && recall) {
					double f = ((2 * (prec[m - 1][j] * rec[m - 1][j])) / (prec[m - 1][j] + rec[m - 1][j]))
							/ ITERATION_DATA;

					if (Double.compare(f, Double.NaN) == 0)
						f = 0;
					
					out += ("(" + k * 10 + "," + df.format(f) + ")");
				} else  if (precision)
					out+=("(" + k * 10 + "," + df.format(prec[m-1][j]/ITERATION_DATA) + ")");
				else if (recall)
					out+=("(" + k * 10 + "," + df.format(rec[m-1][j]/ITERATION_DATA) + ")");

				if (k == 2) {
//					if (precision)
//						out+=("(30," + df.format(prec[m-1][j]/ITERATION_DATA) + ")");
//					
//					if (recall)
//						out+=("(30," + df.format(rec[m-1][j]/ITERATION_DATA) + ")");
					j+=2;
				} else
					j++;
			}
			System.out.println(out);
		}
	}
	
	public static void initR(String extra) throws IOException {
		String ex;
		double[][] prec = new double[4][5], rec = new double[4][5];
		String out;
		int j;
		
		for (int m = 1; m <= 4; m++) {
			out = "";
			j = 0;
			
			for (int k = 2; k <= 8; k+=2) {
				
				for (int IT = 0; IT < ITERATION_DATA; IT++) {

					double res[] = new double[2], res_[];
					res[0] = 0;
					res[1] = 0;
					
					for (int it = 1; it <= 5; it++) {
						ex = extra;
						ex = ex.replace("*", "" + it);
						res_ = read(m, k, path + "random/" + dataset + k  + "_it_" + IT + "_m=" + m + "_k=" + k + ex);
						res[0]+= res_[0];
						res[1]+= res_[1];
						prec[m-1][j]+= res_[0];
						rec[m-1][j]+= res_[1];
						
						if (k == 2) {
							res_ = read(m, 3, path + "random/" + dataset + "3_it_" + IT + "_m=" + m + "_k=3" + ex);
							prec[m-1][j+1]+= res_[0];
							rec[m-1][j+1]+= res_[1];
						}
					}
				}
				
				if (precision && recall) {
					double f = ((2 * (prec[m - 1][j] * rec[m - 1][j])) / (prec[m - 1][j] + rec[m - 1][j]))
							/ (ITERATION_DATA * 5);

					if (Double.compare(f, Double.NaN) == 0)
						f = 0;
					out += ("(" + k * 10 + "," + df.format(f) + ")");
				} else if (precision)
					out+=("(" + k * 10 + "," + df.format(prec[m-1][j]/(ITERATION_DATA * 5)) + ")");
				else if (recall)
					out+=("(" + k * 10 + "," + df.format(rec[m-1][j]/(ITERATION_DATA * 5)) + ")");

				if (k == 2) {
//					if (precision)
//						out+=("(" + "30," + df.format(prec[m-1][j+1]/(ITERATION_DATA * 5)) + ")");
//					
//					if (recall)
//						out+=("(" + "30," + df.format(rec[m-1][j+1]/(ITERATION_DATA * 5)) + ")");
					j+=2;
				} else	
					j++;
			}
			System.out.println(out);
		}
	}
	
	public static double[] read(int m, int k, String path) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(path));
		String line = null;
		int nodesReturned = -1, denseNodes = -1;
		String[] token;
		double[] res = new double[2];
		
		while((line = br.readLine()) != null) {
			
			if (line.contains("Iteration")) {
				token = line.split("\\s+");			
				nodesReturned = Integer.parseInt(token[5].trim());
			}
			
			if (line.contains("Dense Nodes A")) {
				token = line.split(":");
				token = token[1].split("/");
				denseNodes = Integer.parseInt(token[0].trim());
//				System.out.println(nodesReturned + "\t" + denseNodes);
				if (nodesReturned == 0)
					res[0] = 0.0;
				else
					res[0] =  (double) denseNodes / nodesReturned;
				res[1] = (double) denseNodes / DENSE;

				if (path.contains("_it=")) {
					br.close();
					return res;
				} else {
					br.close();
					return res;
				}
			}
		}
		br.close();
		return null;
	}
}