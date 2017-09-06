package util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Computes the overlap of all BFF solutions
 * 
 * @author ksemer
 *
 */
public class Jaccard {
	private static String BASE = "/home/ksemer/workspaces/BFF/results_c1/dblp10";

	public static void main(String[] args) throws IOException {
		double Jaccard[][] = new double[4][4];
		String metric[] = { "{\\problemmm}", "{\\problemma}", "{\\problemam}", "{\\problemaa}", "TAM", "TMA", "AMA",
				"MAM" };

		for (int m = 1; m <= 4; m++)
			for (int m_ = 1; m_ <= 4; m_++)
				Jaccard[m - 1][m_ - 1] = jaccard(m, m_) / getSet(m).size();

		for (int i = 0; i < Jaccard.length; i++) {
			System.out.print("{\\small " + metric[i] + "}");
			for (int j = 0; j < Jaccard[i].length; j++)
				System.out.print(" & " + String.format(Locale.ENGLISH, "%.2f", Jaccard[i][j]));
			System.out.print("\\\\ \n");
		}
	}

	private static double jaccard(int m, int m_) throws IOException {
		Set<Integer> set1 = getSet(m), set2 = getSet(m_);
		set1.retainAll(set2);
		return set1.size();
	}

	private static Set<Integer> getSet(int m) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(BASE + "_" + m + ".txt"));
		String line = null;
		String[] token = null;
		boolean flag = false;
		Set<Integer> set = new HashSet<>();

		while ((line = br.readLine()) != null) {
			if (line.startsWith("="))
				flag = true;
			else if (flag && !line.startsWith("Total")) {
				token = line.split("\\s+");
				set.add(Integer.parseInt(token[0]));
			}
		}
		br.close();
		return set;
	}
}