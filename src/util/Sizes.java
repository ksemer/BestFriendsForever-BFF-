package util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Sizes {
	private static String BASE = "SIGMODPR2/";
	private static String legend[] = { "Union-Best", "Contiguous", "Random" };

	public static void main(String[] args) throws IOException {
		FileWriter w = new FileWriter("allstats.txt");

		writeSizes("dblp10", "$DBLP_{10}$", w, 2, 8);
		// writeSizes("or1","$Oregon_1$", w, 2, 8);
		// writeSizes("or2","$Oregon_2$", w, 2, 8);
		// writeSizes("caida","$Caida$", w, 24, 96);
		// writeSizes("as","$AS$", w, 146, 584);
		w.close();
	}

	private static void writeSizes(String name, String fig_name, FileWriter w, int s, int e) throws IOException {
		double val[];

		for (int strategy = 0; strategy < 3; strategy++) {
			w.write(legend[strategy] + "\n");
			for (int k = s; k <= e; k += s) {
				w.write("$f_{m,m}$\t$f_{m,a}$\t$f_{a,m}$\t$f_{a,a}$\t\n");
				for (int i = 1; i <= 4; i++)
					w.write("\tSize"/* \tScore\tIterations */);
				w.write("\n");

				w.write("k = " + k + "\t");
				for (int m = 1; m <= 4; m++) {
					double score, size;

					if (strategy != 2) {
						val = readData(name, m, k, strategy, -1);
						score = val[1];
						size = val[2];
					} else {
						score = 0;
						size = 0;
						for (int i = 1; i <= 10; i++) {
							val = readData(name, m, k, strategy, i);
							score += val[1];
							size += val[2];
						}
						score = score / 10;
						size = size / 10;
						score = Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", score));
					}
					w.write(size + /* "\t" + val[1] + "\t" + val[0] + */"\t");
				}
				w.write("\n");
			}
			w.write("\n\n");
		}
	}

	private static double[] readData(String name, int m, int k, int type, int it) throws IOException {
		String path = null;

		if (type == 1)
			path = name + "_m=" + m + "_k=" + k + "_union.txt";
		else if (type == 2)
			path = name + "_m=" + m + "_k=" + k + "_it=" + it + ".txt";
		else if (type == 0)
			path = name + "_m=" + m + "_k=" + k + ".txt";

		BufferedReader br = new BufferedReader(new FileReader(BASE + path));
		String line = null, token[];
		double[] val = new double[3];
		List<Integer> iterations = new ArrayList<>();
		List<Integer> sizes = new ArrayList<>();
		List<Double> scores = new ArrayList<>();

		int size, iteration;
		double score;
		while ((line = br.readLine()) != null) {
			if (line.contains("Iteration")) {
				token = line.split(" ");

				iteration = Integer.parseInt(token[1]);
				score = Double.parseDouble(token[3]);
				size = Integer.parseInt(token[5]);

				if (score > val[1]) {
					val[1] = score;
					val[2] = size;
				}

				iterations.add(iteration);
				sizes.add(size);
				scores.add(score);

				val[0] = iteration;
			} else if (line.matches("^\\s*$"))
				break;
		}
		br.close();
		return val;
	}
}