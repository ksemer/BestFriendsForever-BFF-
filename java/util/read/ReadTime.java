package util.read;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class ReadTime {

	private static boolean toSeconds = false;
	private static boolean isReal = true;
	private static String path = "results/";
	// "/home/ksemer/workspaces/RESULTS_VLDB/results_c1/";
	// "/home/ksemer/workspaces/BFF/real_june/o2/";
	// "/home/ksemer/workspaces/BFF/real_june/new_set_o2/o2/";
	// "/home/ksemer/workspaces/BFF/dataset_synth_improv/synthetic_31-05/pa=09/o2/";
	// "/home/ksemer/workspaces/BFF/dataset_synth_improv/synthetic_31-05/competitor_pa=09/o2/";

	private static String dataset = "synthetic_pr:0.5_ds:100_n:20000";
	// "rand_24_snap:40_24_it_0";
	// "synthetic_pr:0.9_s:_
	private static int ITERATION_DATA = 5;
	private static DecimalFormat df;
	private static DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols(Locale.US);

	public static void main(String[] args) throws IOException {
		otherSymbols.setDecimalSeparator('.');
		df = new DecimalFormat("#.##", otherSymbols);
		df.setRoundingMode(RoundingMode.CEILING);

		if (toSeconds)
			System.out.println("Times to seconds");
		else
			System.out.println("Times to msec");

		System.out.println("BFF");
		initbff(".txt");

		System.out.println("Contiguous");
		init(".txt");

		System.out.println("ATLEAST");
		init("_atleast.txt");

		System.out.println("Random");
		initR("_it=*.txt");

		System.out.println("AGGR");
		init("_aggr.txt");

		System.out.println("average sim");
		init("_sa.txt");

		System.out.println("min sim");
		init("_sd.txt");

		System.out.println("dens");
		init("_du.txt");

		System.out.println("jaccard");
		init("_sn.txt");

		System.out.println("average dens");
		init("_da.txt");

		System.out.println("min dens");
		init("_dm.txt");
	}

	private static void initbff(String extra) throws IOException {

		int time, sec;
		String out;

		for (int m = 1; m <= 9; m++) {
			out = "";

			time = read(path + dataset + "_" + m + extra);

			if (toSeconds)
				sec = 1000;
			else
				sec = 1;

			out += ("(" + m + "," + (time / sec) + ")");

			if (m == 4)
				m = 6;

			System.out.println(out);
		}
	}

	private static void init(String extra) throws IOException {

		int[][] res = new int[4][5];
		int time, j, sec;
		String out;

		for (int m = 1; m <= 9; m++) {
			out = "";
			j = 0;

			for (int k = 2; k <= 8; k += 2) {
				for (int it = 0; it < ITERATION_DATA; it++) {

					if (isReal)
						time = read(path + dataset + "_m=" + m + "_k=" + k + extra);
					else
						time = read(path + dataset + k + "_it_" + it + "_m=" + m + "_k=" + k + extra);
					res[m - 1][j] += time;
				}

				if (toSeconds)
					sec = 1000;
				else
					sec = 1;

				out += ("(" + k * 10 + "," + (res[m - 1][j] / ITERATION_DATA / sec) + ")");
			}
			System.out.println(out);
		}
	}

	private static void initR(String extra) throws IOException {
		String ex;
		int[][] res = new int[4][5];
		int time, j, sec;
		StringBuilder out = new StringBuilder("");

		for (int m = 1; m <= 9; m++) {
			j = 0;

			for (int k = 2; k <= 8; k += 2) {

				for (int IT = 0; IT < ITERATION_DATA; IT++) {

					for (int it = 1; it <= 5; it++) {
						ex = extra;
						ex = ex.replace("*", "" + it);

						if (isReal)
							time = read(path + "random/" + dataset + "_m=" + m + "_k=" + k + ex);
						else
							time = read(path + "random/" + dataset + "_it_" + IT + "_m=" + m + "_k=" + k + ex);
						res[m - 1][j] += time;
					}
				}

				if (toSeconds)
					sec = 1000;
				else
					sec = 1;

				out.append("(" + (k * 10) + "," + df.format(res[m - 1][j] / (ITERATION_DATA * 5) / sec) + ")\n");
			}
		}
		System.out.println(out.toString());
	}

	private static int read(String path) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(path));
		String line;
		String[] token;
		int time = -1;

		while ((line = br.readLine()) != null) {

			if (line.contains("time:")) {
				token = line.split("\\s+");
				time = Integer.parseInt(token[2].trim());
				break;
			}
		}
		br.close();

		if (time == -1)
			System.out.println("error");
		return time;
	}
}
