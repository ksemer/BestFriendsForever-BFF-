package util;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Creates latex code for Table 2 in BFF paper
 * 
 * @author ksemer
 */
public class Scores {
	private static String BASE = "/home/ksemer/workspaces/BFF/authors_results/";

	public static void main(String[] args) throws IOException {
		// System.out.print("$DBLP_{10}$");
		// showStats("dblp10");
		System.out.print("$DBLP_{5}$");
		showStats("dblp5");
		// System.out.print("$DBLP_{3}$");
		// showStats("dblp3");
		// System.out.print("$Oregon_{1}$");
		// showStats("oregon1");
		// System.out.print("$Oregon_{2}$");
		// showStats("oregon2");
		// System.out.print("$Slashdot$");
		// showStats("soc");
		// System.out.print("$Caida$");
		// showStats("caida");
		// System.out.print("$AS$");
		// showStats("as");
		// System.out.print("$Amazon$");
		// showStats("amazon");
		// System.out.print("$phat300$");
		// showStats("phat300");
		// System.out.print("$phat700$");
		// showStats("phat700");
		// System.out.print("$phat1500$");
		// showStats("phat1500");
		// System.out.print("$Twitter$");
		// showStats("twitter");
		// System.out.print("$YT$");
		// showStats("yt_days_version");
	}

	private static void showStats(String dataset) throws IOException {
		double stats[][] = new double[9][2];
		int metric[] = { 0, 1, 6, 5, 8, 7, 2, 4, 3 };

		for (int m = 1; m <= 9; m++)
			stats[m - 1] = getStats(dataset, m, null);

		for (int i = 0; i < metric.length; i++)
			System.out.print(" & " + NumberFormat.getNumberInstance(Locale.US).format((int) stats[metric[i]][0]) + " & "
					+ stats[metric[i]][1]);
		System.out.print("\\\\ \n");
	}

	private static double[] getStats(String dataset, int m, FileWriter w) throws IOException {
		BufferedReader br = null;
		double[] st = new double[2];

		try {
			br = new BufferedReader(new FileReader(BASE + dataset + "_" + m + ".txt"));
		} catch (FileNotFoundException e) {
			// w.write("-1\n");
			st[0] = -1;
			st[1] = -1;
			return st;
		}

		String line = null;
		String[] token = null;

		while ((line = br.readLine()) != null) {
			if (line.startsWith("Size")) {
				line = line.replaceAll("\\s+", "");
				token = line.split(":");
				st[0] = Integer.parseInt(token[1]);
			} else if (line.startsWith("Score")) {
				line = line.replaceAll("\\s+", "");
				token = line.split(":");
				st[1] = Double.parseDouble(token[1]);
			} else if (line.startsWith("Total"))
				continue;
		}
		br.close();
		return st;
	}
}