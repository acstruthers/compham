package xyz.struthers.rhul.ham.analysis;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.opencsv.CSVReader;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TObjectFloatHashMap;

/**
 * 
 * 
 * @author Adam Struthers
 * @since 2019-07-23
 */
public class AnalyseExogeneous {

	// the maximum value of each bin for the exogeneous income ratio histogram
	public static final float[] BIN_MAX = { 0.05f, 0.1f, 0.15f, 0.2f, 0.25f, 0.3f, 0.35f, 0.4f, 0.45f, 0.5f, 0.55f,
			0.6f, 0.65f, 0.7f, 0.75f, 0.8f, 0.85f, 0.9f, 0.95f, 1f };

	public static final float BIN_SIZE = 0.05f;

	public AnalyseExogeneous() {
		super();
	}

	private void loadExegeneousDataCsv(String fileResourceLocation, String dataSourceName, int[] columnsToImport,
			Map<String, List<String>> titles, Map<String, List<String>> units,
			Map<String, TObjectFloatHashMap<Date>> data) {

		TIntArrayList adiCount = new TIntArrayList(
				new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 });
		TIntArrayList businessCount = new TIntArrayList(
				new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 });
		TIntArrayList householdCount = new TIntArrayList(
				new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 });

		CSVReader reader = null;
		try {
			InputStream is = this.getClass().getResourceAsStream(fileResourceLocation);
			reader = new CSVReader(new InputStreamReader(is));
			String[] line = reader.readNext(); // read and discard header row
			while ((line = reader.readNext()) != null) {
				float exogeneousRatio = 0f;
				int binNumber = 0;
				try {
					float exogeneousIncome = Float.valueOf(line[2].replace(",", ""));
					float totalIncome = Float.valueOf(line[3].replace(",", ""));
					exogeneousRatio = totalIncome <= 0f ? 0f : exogeneousIncome / totalIncome;
					binNumber = this.getBin(exogeneousRatio);
				} catch (NumberFormatException e) {
					// do nothing and leave it as zero.
				}

				switch (line[1]) {
				case "A": // ADI
					adiCount.get(offset)
					break;
				case "B": // business

					break;
				default: // household

					break;
				}
				if (header) {
					if (line[0].equals("Title")) {
						// store title
						titles.put(dataSourceName, new ArrayList<String>(columnsToImport.length));
						for (int i = 0; i < columnsToImport.length; i++) {
							titles.get(dataSourceName).add(line[columnsToImport[i]]);
						}
					} else if (line[0].equals("Units")) {
						// store unit types
						units.put(dataSourceName, new ArrayList<String>(columnsToImport.length));
						for (int i = 0; i < columnsToImport.length; i++) {
							units.get(dataSourceName).add(line[columnsToImport[i]]);
						}
					} else if (line[0].equals("Series ID")) {
						// store series ID as key with blank collections to populate with data below
						for (int i = 0; i < columnsToImport.length; i++) {
							seriesId[i] = line[columnsToImport[i]];
							data.put(line[columnsToImport[i]], new TObjectFloatHashMap<Date>());
						}
						header = false;
					}
				} else {
					for (int i = 0; i < columnsToImport.length; i++) {
						// parse the body of the data
						float value = 0f;
						try {
							value = Float.valueOf(line[columnsToImport[i]].replace(",", ""));
						} catch (NumberFormatException e) {
							// do nothing and leave it as zero.
						}
						data.get(seriesId[i]).put(dateFormat.parse(line[0]), value);
					}
				}
			}
			reader.close();
			reader = null;
		} catch (FileNotFoundException e) {
			// open file
			e.printStackTrace();
		} catch (IOException e) {
			// read next
			e.printStackTrace();
		} catch (ParseException e) {
			// parsing date from string
			e.printStackTrace();
		}
	}

	private int getBin(float value) {
		int bin = -1;

		while (value >= 0f) {
			value -= BIN_SIZE;
			bin++;
		}

		return bin;
	}

}
