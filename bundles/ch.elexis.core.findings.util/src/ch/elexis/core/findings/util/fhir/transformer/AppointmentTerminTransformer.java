package ch.elexis.core.findings.util.fhir.transformer;

import java.util.Optional;
import java.util.Set;

import org.hl7.fhir.r4.model.Appointment;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.rest.api.SummaryEnum;
import ch.elexis.core.findings.util.fhir.IFhirTransformer;
import ch.elexis.core.findings.util.fhir.transformer.mapper.IAppointmentAppointmentAttributeMapper;
import ch.elexis.core.model.IAppointment;
import ch.elexis.core.services.IAppointmentService;
import ch.elexis.core.services.IConfigService;
import ch.elexis.core.services.IContextService;
import ch.elexis.core.services.IModelService;

@Component
public class AppointmentTerminTransformer implements IFhirTransformer<Appointment, IAppointment> {
	
	@org.osgi.service.component.annotations.Reference(target = "(" + IModelService.SERVICEMODELNAME
		+ "=ch.elexis.core.model)")
	private IModelService coreModelService;
	
	@org.osgi.service.component.annotations.Reference
	private IAppointmentService appointmentService;
	
	@org.osgi.service.component.annotations.Reference
	private IContextService contextService;
	
	@org.osgi.service.component.annotations.Reference
	private IConfigService configService;
	
	private IAppointmentAppointmentAttributeMapper attributeMapper;
	
	@Activate
	private void activate(){
		attributeMapper = new IAppointmentAppointmentAttributeMapper(appointmentService,
			coreModelService, configService);
	}
	
	@Override
	public boolean matchesTypes(Class<?> fhirClazz, Class<?> localClazz){
		return Appointment.class.equals(fhirClazz) && IAppointment.class.equals(localClazz);
	}
	
	@Override
	public Optional<IAppointment> getLocalObject(Appointment fhirObject){
		String id = fhirObject.getIdElement().getIdPart();
		if (id != null && !id.isEmpty()) {
			return coreModelService.load(id, IAppointment.class);
		}
		return Optional.empty();
	}
	
	@Override
	public Optional<IAppointment> createLocalObject(Appointment fhirObject){
		
		//		fhirObject.getExtensionByUrl("")
		
		//				String area = appointmentHelper.mapSchedule(coreModelService, appointmentService, fhirObject);
		//		System.out.println(area);
		// TODO to determine bereich may require Slot first?
		throw new UnsupportedOperationException(
			"Create Slot first, then perform update operation returned Slot Id");
	}
	
	@Override
	public Optional<Appointment> getFhirObject(IAppointment localObject, SummaryEnum summaryEnum,
		Set<Include> includes){
		
		Appointment appointment = new Appointment();
		attributeMapper.elexisToFhir(localObject, appointment, summaryEnum, includes);
		return Optional.of(appointment);
	}
	
	@Override
	public Optional<IAppointment> updateLocalObject(Appointment fhirObject,
		IAppointment localObject){
		
		attributeMapper.fhirToElexis(fhirObject, localObject);
		// TODO more
		
		coreModelService.save(localObject);
		return Optional.of(localObject);
	}
	
}
