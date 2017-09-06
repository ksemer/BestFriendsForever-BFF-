package util.read;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class Tex {
	private static String path = "res";

	public static void main(String[] args) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(path));
		String line = null;
		String[] cont = new String[5];
		String[] atleast = new String[5];
		String[] random = new String[5];
		String[] aggr = new String[5];
		String[] jac = new String[5];
		String[] asim = new String[5];
		String[] msim = new String[5];
		String[] djac = new String[5];
		String[] aden = new String[5];
		String[] mden = new String[5];
		int i = -1;
		boolean contin = false, atleas = false, ran = false, agg = false, j = false, as = false, ms = false, dj = false,
				ad = false, md = false;

		while ((line = br.readLine()) != null) {
			line = line.toLowerCase();

			if (line.contains("contiguous"))
				contin = true;
			else if (line.contains("atleast"))
				atleas = true;
			else if (line.contains("random"))
				ran = true;
			else if (line.contains("aggr"))
				agg = true;
			else if (line.contains("jaccard"))
				j = true;
			else if (line.contains("average sim"))
				as = true;
			else if (line.contains("min sim"))
				ms = true;
			else if (line.contains("dens jac"))
				dj = true;
			else if (line.contains("average dens"))
				ad = true;
			else if (line.contains("min dens"))
				md = true;
			else {
				if (contin)
					cont[i] = line;
				else if (atleas)
					atleast[i] = line;
				else if (ran)
					random[i] = line;
				else if (agg)
					aggr[i] = line;
				else if (j)
					jac[i] = line;
				else if (as)
					asim[i] = line;
				else if (ms)
					msim[i] = line;
				else if (dj)
					djac[i] = line;
				else if (ad)
					aden[i] = line;
				else if (md)
					mden[i] = line;
			}

			if (i == 3) {
				i = -1;
				contin = false;
				atleas = false;
				ran = false;
				agg = false;
				j = false;
				as = false;
				ms = false;
				dj = false;
				ad = false;
				md = false;

			} else if (i == -1) {
				i = 0;
				continue;
			} else
				i++;
		}
		br.close();

		for (i = 0; i < 4; i++) {
			System.out.println("\t\\addplot[color=darkgreen, mark=triangle*,line width=1.5pt,mark size=2pt]coordinates{"
					+ cont[i] + "};");
			System.out.println(
					"\t\\addplot[color=blue, mark=*,line width=1.5pt,mark size=2pt]coordinates{" + atleast[i] + "};");
			System.out.println("\t\\addplot[color=red, mark=square*,line width=1.5pt,mark size=2pt]coordinates{"
					+ random[i] + "};");
			System.out.println(
					"\t\\addplot[color=black, mark=x,line width=1.5pt,mark size=3pt]coordinates{" + aggr[i] + "};");
			System.out.println(
					"\t\\addplot[color=yellow, mark=x,line width=1.5pt,mark size=3pt]coordinates{" + jac[i] + "};");
			System.out.println(
					"\t\\addplot[color=orange, mark=*,line width=1.5pt,mark size=3pt]coordinates{" + asim[i] + "};");
			System.out.println("\t\\addplot[color=pink, mark=triangle,line width=1.5pt,mark size=3pt]coordinates{"
					+ msim[i] + "};");
			System.out.println("\t\\addplot[color=purple, mark=square,line width=1.5pt,mark size=3pt]coordinates{"
					+ djac[i] + "};");
			System.out.println(
					"\t\\addplot[color=gray, mark=*,line width=1.5pt,mark size=3pt]coordinates{" + aden[i] + "};");
			System.out.println(
					"\t\\addplot[color=cyan, mark=x,line width=1.5pt,mark size=3pt]coordinates{" + mden[i] + "};");
			System.out.println("");
		}

	}
}