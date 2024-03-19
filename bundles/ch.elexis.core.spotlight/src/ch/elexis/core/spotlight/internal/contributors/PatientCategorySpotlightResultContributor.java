package ch.elexis.core.spotlight.internal.contributors;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import ch.elexis.core.model.IPatient;
import ch.elexis.core.model.ModelPackage;
import ch.elexis.core.services.IModelService;
import ch.elexis.core.services.IQuery;
import ch.elexis.core.services.IQuery.COMPARATOR;
import ch.elexis.core.services.IQuery.ORDER;
import ch.elexis.core.spotlight.ISpotlightResult;
import ch.elexis.core.spotlight.ISpotlightResultContributor;
import ch.elexis.core.spotlight.ISpotlightResultEntry.Category;
import ch.elexis.core.spotlight.ISpotlightService;

@Component
public class PatientCategorySpotlightResultContributor implements ISpotlightResultContributor {

	@Reference(target = "(" + IModelService.SERVICEMODELNAME + "=ch.elexis.core.model)")
	private IModelService modelService;

	@Override
	public void computeResult(List<String> stringTerms, List<LocalDate> dateTerms, List<Number> numericTerms,
			ISpotlightResult spotlightResult, Map<String, String> searchParams) {

		if (searchParams != null && searchParams.containsKey(ISpotlightService.CONTEXT_FILTER_PATIENT_ID)) {
			// search context is limited to a specific patient
			// no interest in the list of the patients ...
			return;
		}

		IQuery<IPatient> query = modelService.getQuery(IPatient.class);
		boolean isFirstGroup = true;

		if (!stringTerms.isEmpty()) {
			query.startGroup();
			if (stringTerms.size() == 1) {
				query.or(ModelPackage.Literals.IPERSON__FIRST_NAME, COMPARATOR.LIKE, stringTerms.get(0) + "%", true)
						.or(ModelPackage.Literals.IPERSON__LAST_NAME, COMPARATOR.LIKE, stringTerms.get(0) + "%", true);
			} else {
				query.and(ModelPackage.Literals.IPERSON__FIRST_NAME, COMPARATOR.LIKE, stringTerms.get(0) + "%", true)
						.and(ModelPackage.Literals.IPERSON__LAST_NAME, COMPARATOR.LIKE, stringTerms.get(1) + "%", true)
						.startGroup()
						.and(ModelPackage.Literals.IPERSON__FIRST_NAME, COMPARATOR.LIKE, stringTerms.get(1) + "%", true)
						.and(ModelPackage.Literals.IPERSON__LAST_NAME, COMPARATOR.LIKE, stringTerms.get(0) + "%", true);
				query.orJoinGroups();
			}
			isFirstGroup = false;
		}

		if (!dateTerms.isEmpty()) {
			if (!isFirstGroup) {
				query.andJoinGroups();
			}
			query.startGroup().and(ModelPackage.Literals.IPERSON__DATE_OF_BIRTH, COMPARATOR.EQUALS, dateTerms.get(0));
			isFirstGroup = false;
		}

		if (!numericTerms.isEmpty()) {
			if (!isFirstGroup) {
				query.andJoinGroups();
			}
			query.startGroup().and(ModelPackage.Literals.ICONTACT__CODE, COMPARATOR.LIKE,
					numericTerms.get(0).intValue() + "%");
		}

		query.orderBy(ModelPackage.Literals.IPERSON__LAST_NAME, ORDER.ASC).limit(5);

		List<IPatient> patients = query.execute();
		for (IPatient patient : patients) {
			spotlightResult.addEntry(Category.PATIENT, patient.getLabel(), patient.getId(), patient);
		}
	}

}
