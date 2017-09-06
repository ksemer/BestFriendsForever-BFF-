package util.read;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class Read1 {
	private static String path = "C:\\Users\\ksemer\\Desktop\\pa05\\o2\\";
	private static String dataset = "rew_rand_";
	private static int ITERATION_DATA = 10;
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

		double[][] result = new double[4][5];
		double res;
		String out;
		int j;

		for (int m = 1; m <= 4; m++) {
			out = "";
			j = 0;

			for (int k = 2; k <= 8; k += 2) {
				for (int it = 0; it < ITERATION_DATA; it++) {
					res = read(m, k, path + dataset + k + "_it_" + it + "_m=" + m + "_k=" + k + extra);
					result[m - 1][j] += res;

					if (k == 2) {
						res = read(m, 3, path + dataset + "3_it_" + it + "_m=" + m + "_k=3" + extra);
						result[m - 1][j + 1] += res;
					}
				}

				out += ("(" + k * 10 + "," + df.format(result[m - 1][j] / ITERATION_DATA) + ")");

				if (k == 2) {
					out += ("(30," + df.format(result[m - 1][j] / ITERATION_DATA) + ")");

					j += 2;
				} else
					j++;
			}
			System.out.println(out);
		}
	}

	public static void initR(String extra) throws IOException {
		String ex;
		double results[][] = new double[4][5];
		String out;
		int j;

		for (int m = 1; m <= 4; m++) {
			out = "";
			j = 0;

			for (int k = 2; k <= 8; k += 2) {

				for (int IT = 0; IT < ITERATION_DATA; IT++) {

					double res_;

					for (int it = 1; it <= 5; it++) {
						ex = extra;
						ex = ex.replace("*", "" + it);
						res_ = read(m, k, path + "random/" + dataset + k + "_it_" + IT + "_m=" + m + "_k=" + k + ex);
						results[m - 1][j] += res_;

						if (k == 2) {
							res_ = read(m, 3, path + "random/" + dataset + "3_it_" + IT + "_m=" + m + "_k=3" + ex);
							results[m - 1][j + 1] += res_;
						}
					}
				}

				out += ("(" + k * 10 + "," + df.format(results[m - 1][j] / (ITERATION_DATA * 5)) + ")");

				if (k == 2) {
					out += ("(" + "30," + df.format(results[m - 1][j + 1] / (ITERATION_DATA * 5)) + ")");

					j += 2;
				} else
					j++;
			}
			System.out.println(out);
		}
	}

	public static double read(int m, int k, String path) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(path));
		String line = null;
		double density = -1;
		String[] token;

		while ((line = br.readLine()) != null) {

			if (line.contains("Iteration")) {
				token = line.split("\\s+");
				density = Double.parseDouble(token[3].trim());
			}
		}

		br.close();
		return density;
	}
}