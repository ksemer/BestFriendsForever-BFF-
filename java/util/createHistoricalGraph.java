package util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Create a version graph using datasets from SNAP
 * 
 * @author ksemer
 */
@SuppressWarnings("unused")
public class createHistoricalGraph {

	private static String BASE = "/home/ksemer/workspaces/BFF/data/";

	public static void main(String[] args) throws IOException, ParseException {
		// snap("oregon1", "oregon1");
		// snap("oregon2", "oregon2");
		// snap("soc", "soc");
		// snap("caida", "caida");
		// snap("amazon", "amazon");
		// phat("1500", "phat1500");
		// phat("700", "phat700");
		// phat("300", "phat300");
		twitter("hashtags.csv", "twitter");
	}

	/**
	 * HasTag dataset from twitter
	 * 
	 * @param name
	 * @param output
	 * @throws IOException
	 * @throws ParseException
	 */
	private static void twitter(String name, String output) throws IOException, ParseException {
		FileWriter w = new FileWriter(output), w1 = new FileWriter("ids_hashtags");
		BufferedReader br = new BufferedReader(new FileReader(BASE + name));
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		Calendar cal = Calendar.getInstance();

		Map<String, Integer> hashTags = new HashMap<>();
		Map<String, List<Integer>> hashTagsPerTweet = new HashMap<>();
		Map<String, Integer> timeOfTweet = new HashMap<>();
		Map<Date, Integer> dateToID = new HashMap<>();
		Date date;
		String line = null, tweet, hashTag, timestamp;
		String[] token;
		int hashTagID = 0, t = 0;

		br.readLine();
		while ((line = br.readLine()) != null) {
			token = line.split(",");
			tweet = token[1];
			hashTag = token[2].toLowerCase();

			if (!hashTagsPerTweet.containsKey(tweet))
				hashTagsPerTweet.put(tweet, new ArrayList<>());

			if (!hashTags.containsKey(hashTag)) {
				hashTags.put(hashTag, hashTagID);
				hashTagID++;
			}

			timestamp = token[0].replaceAll("\"", "");
			date = dateFormat.parse(timestamp);

			if (!dateToID.containsKey(date)) {
				dateToID.put(date, t);
				t++;
			}

			if (!timeOfTweet.containsKey(tweet))
				timeOfTweet.put(tweet, dateToID.get(date));

			hashTagsPerTweet.get(tweet).add(hashTags.get(hashTag));
		}
		br.close();
		System.out.println("HashTags size: " + hashTags.size());
		System.out.println("Dates size: " + dateToID.size());
		hashTagsPerTweet.forEach((k, v) -> {
			for (int i = 0; i < v.size(); i++)
				for (int j = i + 1; j < v.size(); j++)
					try {
						w.write(v.get(i) + "\t" + v.get(j) + "\t" + timeOfTweet.get(k) + "\n");
					} catch (IOException e) {}
		});
		w.close();

		Map<Integer, String> idToHashtags = hashTags.entrySet().stream()
				.collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
		
		idToHashtags.forEach((k, v) -> {
			try {
				w1.write(k + "\t" + v + "\n");
			} catch (IOException e) {}
		});
		w1.close();
	}

	/**
	 * For phat datasets from PKDD
	 * 
	 * @param name
	 * @param output
	 * @throws IOException
	 */
	private static void phat(String name, String output) throws IOException {
		File folder = new File(BASE);
		File[] listOfFiles = folder.listFiles();
		FileWriter w = new FileWriter(output);
		int t = 0;

		for (File f : listOfFiles) {
			if (f.getName().toLowerCase().contains(name)) {
				System.out.println(f.getName());
				t++;

				BufferedReader br = new BufferedReader(new FileReader(BASE + f.getName()));
				String line = null;
				String[] token = null;

				while ((line = br.readLine()) != null) {
					if (!line.startsWith("e"))
						continue;

					token = line.split("\\s+");
					w.write(token[1] + "\t" + token[2] + "\t" + t + "\n");
				}
				br.close();
			}
		}
		w.close();
	}

	/**
	 * For datasets from snapshots
	 * 
	 * @param name
	 * @param output
	 * @throws IOException
	 */
	private static void snap(String name, String output) throws IOException {
		File folder = new File(BASE);
		File[] listOfFiles = folder.listFiles();
		FileWriter w = new FileWriter(output);
		int t = 0;

		for (File f : listOfFiles) {
			if (f.getName().toLowerCase().contains(name)) {
				System.out.println(f.getName());
				t++;

				BufferedReader br = new BufferedReader(new FileReader(BASE + f.getName()));
				String line = null;
				String[] token = null;

				while ((line = br.readLine()) != null) {
					if (line.startsWith("#"))
						continue;

					token = line.split("\\s+");
					w.write(token[0] + "\t" + token[1] + "\t" + t + "\n");
				}
				br.close();
			}
		}
		w.close();
	}
}