package ch.elexis.core.ui.laboratory.controls.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import ch.elexis.data.LabItem;
import ch.elexis.data.LabResult;
import ch.rgw.tools.TimeTool;

public class LaborItemResults implements Comparable<LaborItemResults> {
	private HashMap<String, List<LabResult>> results;
	private String item;
	private boolean showCheckboxes = false;

	public LaborItemResults(String item, HashMap<String, List<LabResult>> results) {
		this.results = results;
		this.item = item;

		this.results.values().forEach(list -> {
			Collections.sort(list, (l, r) -> {
				return TimeTool.compare(r.getObservationTime(), l.getObservationTime());
			});
		});
	}

	public LabItem getLabItem() {
		return (LabItem) getFirstResult().getItem();
	}

	public String getItem() {
		return item;
	}

	public boolean isVisible() {
		return results.values().iterator().next().get(0).getItem().isVisible();
	}

	public LabResult getFirstResult() {
		return results.values().iterator().next().get(0);
	}

	public List<LabResult> getResult(String date) {
		return results.get(date);
	}

	public List<String> getDays() {
		return new ArrayList<String>(results.keySet());
	}

	@Override
	public int compareTo(LaborItemResults o) {
		return item.compareTo(o.getItem());
	}

	public List<Double> getMostRecentValues(int count) {
		Collection<LabResult> allResults = getAllResults();
		List<LabResult> sortedResults = new ArrayList<>(allResults);
		Collections.sort(sortedResults, (r1, r2) -> r2.getObservationTime().compareTo(r1.getObservationTime()));

		List<Double> recentValues = new ArrayList<>();
		for (int i = 0; i < Math.min(count, sortedResults.size()); i++) {
			LabResult result = sortedResults.get(i);
			try {
				double value = Double.parseDouble(result.getResult());
				recentValues.add(value);
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
		}
		return recentValues;
	}

	public LaborItemResults(HashMap<String, List<LabResult>> results) {
		this.results = results;
	}

	public Collection<LabResult> getResults() {
		List<LabResult> allResults = new ArrayList<>();
		for (List<LabResult> resultList : results.values()) {
			allResults.addAll(resultList);
		}
		return allResults;
	}

	public Collection<LabResult> getAllResults() {
		List<LabResult> allResults = new ArrayList<>();
		for (List<LabResult> resultList : results.values()) {
			allResults.addAll(resultList);
		}
		return allResults;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("LaborItemResults for item: ").append(item).append(", Results: ");
		results.forEach((date, labResults) -> {
			sb.append("\nDate: ").append(date).append(", Results: ");
			labResults.forEach(result -> {
				sb.append(result.toString()).append("; ");
			});
		});
		return sb.toString();
	}
}