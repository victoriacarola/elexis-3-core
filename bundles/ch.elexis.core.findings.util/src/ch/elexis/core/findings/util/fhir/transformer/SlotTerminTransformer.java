package ch.elexis.core.findings.util.fhir.transformer;

import java.util.Optional;
import java.util.Set;

import org.hl7.fhir.r4.model.Slot;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.rest.api.SummaryEnum;
import ch.elexis.core.findings.util.fhir.IFhirTransformer;
import ch.elexis.core.findings.util.fhir.transformer.mapper.IAppointmentSlotAttributeMapper;
import ch.elexis.core.model.IAppointment;
import ch.elexis.core.services.IAppointmentService;
import ch.elexis.core.services.IModelService;

@Component(property = IFhirTransformer.TRANSFORMERID + "=Slot.IAppointment")
public class SlotTerminTransformer implements IFhirTransformer<Slot, IAppointment> {
	
	@org.osgi.service.component.annotations.Reference(target = "(" + IModelService.SERVICEMODELNAME
		+ "=ch.elexis.core.model)")
	private IModelService modelService;
	
	@org.osgi.service.component.annotations.Reference
	private IAppointmentService appointmentService;
	
	private IAppointmentSlotAttributeMapper attributeMapper;
	
	@Activate
	private void activate(){
		attributeMapper = new IAppointmentSlotAttributeMapper(appointmentService);
	}
	
	@Override
	public Optional<Slot> getFhirObject(IAppointment localObject, SummaryEnum summaryEnum,
		Set<Include> includes){
		Slot slot = new Slot();
		attributeMapper.elexisToFhir(localObject, slot, summaryEnum, includes);
		return Optional.of(slot);
	}
	
	@Override
	public Optional<IAppointment> getLocalObject(Slot fhirObject){
		String id = fhirObject.getIdElement().getIdPart();
		if (id != null && !id.isEmpty()) {
			return modelService.load(id, IAppointment.class);
		}
		return Optional.empty();
	}
	
	@Override
	public Optional<IAppointment> updateLocalObject(Slot fhirObject, IAppointment localObject){
		return Optional.empty();
	}
	
	@Override
	public Optional<IAppointment> createLocalObject(Slot fhirObject){
		IAppointment create = modelService.create(IAppointment.class);
		attributeMapper.fhirToElexis(fhirObject, create);
		modelService.save(create);
		return Optional.of(create);
	}
	
	@Override
	public boolean matchesTypes(Class<?> fhirClazz, Class<?> localClazz){
		return Slot.class.equals(fhirClazz) && IAppointment.class.equals(localClazz);
	}
	
}
