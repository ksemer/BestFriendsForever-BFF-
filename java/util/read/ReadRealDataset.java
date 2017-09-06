package util.read;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class ReadRealDataset {
	private static String path = "/home/ksemer/workspaces/BFF/real_june/o2/";
	private static String dataset = "oregon2";
	private static int ITERATIONS = 5;
	static DecimalFormat df;
	static DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols(Locale.US);

	public static void main(String[] args) throws IOException {
		// otherSymbols.setDecimalSeparator('.');
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
		init("_su.txt");

		System.out.println("average sim");
		init("_sa.txt");

		System.out.println("min sim");
		init("_sm.txt");

		System.out.println("dens jac");
		init("_du.txt");

		System.out.println("average dens");
		init("_da.txt");

		System.out.println("min dens");
		init("_dm.txt");
	}

	public static void init(String extra) throws IOException {

		double res;
		String out;

		for (int m = 1; m <= 4; m++) {
			out = "";

			for (int k = 2; k <= 8; k += 2) {
				res = read(m, k, path + dataset + "_m=" + m + "_k=" + k + extra);
				out += ("(" + k * 10 + "," + df.format(res) + ")");
			}
			System.out.println(out);
		}
	}

	public static void initR(String extra) throws IOException {
		String ex;
		double res_;
		String out;

		for (int m = 1; m <= 4; m++) {
			out = "";

			for (int k = 2; k <= 8; k += 2) {
				res_ = 0;

				for (int it = 1; it <= ITERATIONS; it++) {
					ex = extra;
					ex = ex.replace("*", "" + it);
					res_ += read(m, k, path + "random/" + dataset + "_m=" + m + "_k=" + k + ex);
				}

				out += ("(" + k * 10 + "," + df.format(res_ / ITERATIONS) + ")");
			}
			System.out.println(out);
		}
	}

	public static double read(int m, int k, String path) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(path));
		String line = null;
		String[] token;
		double score = 0;

		while ((line = br.readLine()) != null) {

			if (line.contains("Score")) {
				token = line.split("Score: ");
				token = token[1].split("\\s+");
				score = Double.parseDouble(token[0]);
			}
		}

		br.close();
		return score;
	}
}