package util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Latex {
	private static String BASE = "/home/ksemer/workspaces/BFF/FINAL_EXPERIMENT/";
	private static int ITERATIONS = 5;

	private static String legend[] = { "Contiguous", "AtLeastK", "Random" };
	private static String xlabel = "k (in \\% of snapshots)";
	private static String ySclabel[] = { "$f_{mm}$", "$f_{ma}$", "$f_{am}$", "$f_{aa}$" };
	private static String subN[] = { "{\\algotbffmm}", "{\\algotbffmm}", "{\\algotbffaa}", "{\\algotbffaa}" };
	private static String xVal = "20, 40, 60, 80";
	private static String lineMark = ",line width=1.5pt,mark size=2pt";
	private static String scatter[] = { "color=darkgreen, mark=triangle*", "color=blue, mark=*",
			"color=red, mark=square*" };
	private static String bar[] = { "color=darkgreen, fill=darkgreen", "color=blue, fill=blue", "color=red, fill=red" };

	public static void main(String[] args) throws IOException {
		FileWriter w = new FileWriter("score.plots");
		FileWriter w1 = new FileWriter("size.plots");
		// createFigure("cont", "{Synthetic1}", w, w1, 2, 8);
		// createFigure("rand", "{Synthetic2}", w, w1, 2, 8);

		createFigure("synth_un", "{Synth un}", w, w1, 2, 8);
		createFigure("DBLP_Graph_DB1+ALL", "{\\dbten}", w, w1, 2, 8);
		createFigure("oregon1", "{\\orf}", w, w1, 2, 8);
		createFigure("oregon2", "{\\ort}", w, w1, 2, 8);
		createFigure("soc", "{\\soc}", w, w1, 1, 2);
		createFigure("caida", "{\\cai}", w, w1, 24, 96);
		createFigure("as", "{\\as}", w, w1, 146, 584);
		createFigure("twitter", "{\\twitter}", w, w1, 3, 12);
		w.close();
		w1.close();
	}

	private static void createFigure(String name, String fig_name, FileWriter w, FileWriter w1, int s, int e)
			throws IOException {
		double[] res;
		w.write("\\begin{figure*}[t!]\n\\centering\n\\resizebox{0.75\\textwidth}{!}{\n\\begin{tabular}{cccc}\n");
		w1.write("\\begin{figure*}[t!]\n\\centering\n\\resizebox{1.0\\textwidth}{!}{\n\\begin{tabular}{cccc}\n");
		for (int m = 1; m <= 4; m++) {
			writeTkz(w, 1, m - 1, true, false);
			writeTkz(w1, 1, m - 1, false, true);

			// for each strategy
			for (int strategy = 0; strategy < 3; strategy++) {
				w.write("\t\\addplot[" + scatter[strategy] + lineMark + "]coordinates{");
				w1.write("\t\\addplot[" + bar[strategy] + "]coordinates{");
				double score, size;
				int K = 20;

				// for each k
				for (int k = s; k <= e; k += s) {
					if (strategy != 2) {
						res = readData(name, m, k, strategy, -1);
						score = res[1];
						size = res[2];
					} else {
						score = 0;
						size = 0;
						for (int i = 1; i <= ITERATIONS; i++) {
							res = readData(name, m, k, strategy, i);
							score += res[1];
							size += res[2];
						}
						score = score / ITERATIONS;
						size = size / ITERATIONS;
						score = Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", score));
						size = Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", size));
					}
					w.write("(" + K + "," + score + ")");
					w1.write("(" + K + "," + size + ")");
					K += 20;
				}
				w.write("};\n");
				w1.write("};\n");
			}
			writeTkz(w, 2, -1, false, false);
			writeTkz(w1, 2, -1, false, true);

			if (m < 4) {
				w.write("&\n");
				w1.write("&\n");
			}
		}
		w.write("\\end{tabular}}\n\\caption{" + fig_name
				+ " dataset: scores of aggregate density functions $f$}\n\\label{fig:}\n\\vspace{-0.25cm}\n\\end{figure*}\n\n");
		w1.write("\\end{tabular}}\n\\caption{" + fig_name
				+ " dataset: sizes of aggregate density functions $f$}\n\\label{fig:}\n\\vspace{-0.25cm}\n\\end{figure*}\n\n");
	}

	private static void writeTkz(FileWriter w, int t, int m, boolean isScore, boolean bar) throws IOException {
		if (t == 1) {
			w.write("\\subfloat[\\textit{" + subN[m] + "}]\n");
			w.write("{\\begin{tikzpicture}\n");
			w.write("\\begin{axis}[\n");
			w.write("\txtick = {" + xVal + "},\n");
			w.write("\txlabel={" + xlabel + "},\n");
			w.write("\tylabel={");
			if (!isScore)
				w.write("Size of ");
			w.write(ySclabel[m]);

			if (!isScore)
				w.write(" solution},\n");
			else
				w.write("},\n");

			w.write("\tlegend style={legend columns=3, anchor=north,at={(0.5,1.12)}},\n");
			if (!bar) {
				// w.write("\tytick distance=1,\n");
			} else {
				w.write("\tenlarge x limits=0.2,\n");
				w.write("\tybar,\n]\n");
			}

			// w.write("\tlegend pos=north east,\n");
			if (!bar)
				w.write("\tymajorgrids=true,\n\tgrid style=dashed,\n]\n");
		} else {
			w.write("\\legend{" + legend[0] + "," + legend[1] + "," + legend[2] + "}\n");
			w.write("\\end{axis}\n\\end{tikzpicture}}\n");
		}
	}

	private static double[] readData(String name, int m, int k, int type, int it) throws IOException {
		String path = null;

		if (type == 0)
			path = name + "_" + k + "_m=" + m + "_k=" + k + ".txt";
		if (type == 1)
			path = name + "_" + k + "_m=" + m + "_k=" + k + "_union.txt";
		else if (type == 2)
			path = "/random/" + name + "_" + k + "_m=" + m + "_k=" + k + "_it=" + it + ".txt";

		// double vll = READ(path, 1);
		// int x = 5;
		// if (vll < 2)
		// x--;
		// double o = READ(path, 2);
		// double o1 = READ(path, 3);
		// double o2 = READ(path, 4);
		// double o3 = READ(path, 5);
		//
		// if (o < 2)
		// x--;
		// if (o1 < 2)
		// x--;
		// if (o2 < 2)
		// x--;
		// if (o3 < 2)
		// x--;
		//
		// vll = vll / x;
		// System.out.println(path);
		// System.out.println(vll);

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

	
//	private static double READ(String path, int num) throws NumberFormatException, IOException {
//		BufferedReader br = new BufferedReader(new FileReader(BASE + "/" + num + "/" + path));
//		String line = null, token[];
//		double[] val = new double[3];
//		List<Integer> iterations = new ArrayList<>();
//		List<Integer> sizes = new ArrayList<>();
//		List<Double> scores = new ArrayList<>();
//
//		int size, iteration;
//		double score;
//		while ((line = br.readLine()) != null) {
//			if (line.contains("Iteration")) {
//				token = line.split(" ");
//
//				iteration = Integer.parseInt(token[1]);
//				score = Double.parseDouble(token[3]);
//				size = Integer.parseInt(token[5]);
//
//				if (score > val[1]) {
//					val[1] = score;
//					val[2] = size;
//				}
//
//				iterations.add(iteration);
//				sizes.add(size);
//				scores.add(score);
//
//				val[0] = iteration;
//			} else if (line.matches("^\\s*$"))
//				break;
//		}
//		br.close();
//		return val[1];
//	}
}