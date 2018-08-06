package ch.elexis.core.model.service;

import java.util.HashMap;

import ch.elexis.core.jpa.entities.Artikel;
import ch.elexis.core.jpa.entities.ArtikelstammItem;
import ch.elexis.core.jpa.entities.Behandlung;
import ch.elexis.core.jpa.entities.Brief;
import ch.elexis.core.jpa.entities.DocHandle;
import ch.elexis.core.jpa.entities.Eigenleistung;
import ch.elexis.core.jpa.entities.EntityWithId;
import ch.elexis.core.jpa.entities.Fall;
import ch.elexis.core.jpa.entities.Invoice;
import ch.elexis.core.jpa.entities.Kontakt;
import ch.elexis.core.jpa.entities.LabResult;
import ch.elexis.core.jpa.entities.Labor2009Tarif;
import ch.elexis.core.jpa.entities.PhysioLeistung;
import ch.elexis.core.jpa.entities.Prescription;
import ch.elexis.core.jpa.entities.Reminder;
import ch.elexis.core.jpa.entities.StockEntry;
import ch.elexis.core.jpa.entities.TarmedLeistung;
import ch.elexis.core.jpa.entities.Termin;
import ch.elexis.core.jpa.entities.User;
import ch.elexis.core.jpa.entities.Verrechnet;
import ch.elexis.core.services.IModelService;
import ch.elexis.core.types.ArticleTyp;

/**
 * Map type names from new {@link AbstractDBObjectIdDeleted} subclasses to PersistentObject legacy
 * type names. Use by {@link IModelService#loadFromString(String)} and
 * {@link IModelService#storeToString(ch.elexis.core.model.Identifiable)}.
 * 
 * @author thomas
 *
 */
public class ElexisTypeMap {
	
	private static final HashMap<String, Class<? extends EntityWithId>> stsToClassMap;
	private static final HashMap<Class<? extends EntityWithId>, String> classToStsMap;

	public static final String TYPE_ARTIKEL = "ch.elexis.data.Artikel";
	public static final String TYPE_ARTIKELSTAMM = "ch.artikelstamm.elexis.common.ArtikelstammItem";
	public static final String TYPE_BRIEF = "ch.elexis.data.Brief";
	public static final String TYPE_DOCHANDLE = "ch.elexis.omnivore.data.DocHandle";
	public static final String TYPE_EIGENARTIKEL = "ch.elexis.core.eigenartikel.Eigenartikel";
	public static final String TYPE_EIGENLEISTUNG = "ch.elexis.data.Eigenleistung";
	public static final String TYPE_MEDIKAMENT = "ch.elexis.artikel_ch.data.Medikament";
	public static final String TYPE_MEDICAL = "ch.elexis.artikel_ch.data.Medical";
	public static final String TYPE_MIGEL = "ch.elexis.artikel_ch.data.MiGelArtikel";
	public static final String TYPE_FALL = "ch.elexis.data.Fall";
	public static final String TYPE_KONSULTATION = "ch.elexis.data.Konsultation";
	public static final String TYPE_KONTAKT = "ch.elexis.data.Kontakt";
	public static final String TYPE_LABORATORY = "ch.elexis.data.Labor";
	public static final String TYPE_LABOR2009TARIF = "ch.elexis.labortarif2009.data.Labor2009Tarif";
	public static final String TYPE_LABRESULT = "ch.elexis.data.LabResult";
	public static final String TYPE_ORGANISATION = "ch.elexis.data.Organisation";
	public static final String TYPE_PATIENT = "ch.elexis.data.Patient";
	public static final String TYPE_PERSON = "ch.elexis.data.Person";
	public static final String TYPE_RECHNUNG = "ch.elexis.data.Rechnung";
	public static final String TYPE_REMINDER = "ch.elexis.data.Reminder";
	public static final String TYPE_PHYSIOLEISTUNG = "ch.elexis.data.PhysioLeistung";
	public static final String TYPE_PRESCRIPTION = "ch.elexis.data.Prescription";
	public static final String TYPE_STOCK_ENTRY = "ch.elexis.data.StockEntry";
	public static final String TYPE_TARMEDLEISTUNG = "ch.elexis.data.TarmedLeistung";
	public static final String TYPE_TERMIN = "ch.elexis.agenda.data.Termin";
	public static final String TYPE_TESSINER_CODE = "ch.elexis.data.TICode";
	public static final String TYPE_USER = "ch.elexis.data.User";
	public static final String TYPE_VERRECHNET = "ch.elexis.data.Verrechnet"; 
	
	static {
		stsToClassMap = new HashMap<String, Class<? extends EntityWithId>>();
		classToStsMap = new HashMap<Class<? extends EntityWithId>, String>();

		// bi-directional mappable
		stsToClassMap.put(TYPE_ARTIKELSTAMM, ArtikelstammItem.class);
		classToStsMap.put(ArtikelstammItem.class, TYPE_ARTIKELSTAMM);
		stsToClassMap.put(TYPE_BRIEF, Brief.class);
		classToStsMap.put(Brief.class, TYPE_BRIEF);
		stsToClassMap.put(TYPE_DOCHANDLE, DocHandle.class);
		classToStsMap.put(DocHandle.class, TYPE_DOCHANDLE);
		stsToClassMap.put(TYPE_EIGENLEISTUNG, Eigenleistung.class);
		classToStsMap.put(Eigenleistung.class, TYPE_EIGENLEISTUNG);
		stsToClassMap.put(TYPE_FALL, Fall.class);
		classToStsMap.put(Fall.class, TYPE_FALL);
		stsToClassMap.put(TYPE_LABOR2009TARIF, Labor2009Tarif.class);
		classToStsMap.put(Labor2009Tarif.class, TYPE_LABOR2009TARIF);
		stsToClassMap.put(TYPE_LABRESULT, LabResult.class);
		classToStsMap.put(LabResult.class, TYPE_LABRESULT);
		stsToClassMap.put(TYPE_KONSULTATION, Behandlung.class);
		classToStsMap.put(Behandlung.class, TYPE_KONSULTATION);
		stsToClassMap.put(TYPE_PHYSIOLEISTUNG, PhysioLeistung.class);
		classToStsMap.put(PhysioLeistung.class, TYPE_PHYSIOLEISTUNG);
		stsToClassMap.put(TYPE_PRESCRIPTION, Prescription.class);
		classToStsMap.put(Prescription.class, TYPE_PRESCRIPTION);
		stsToClassMap.put(TYPE_RECHNUNG, Invoice.class);
		classToStsMap.put(Invoice.class, TYPE_RECHNUNG);
		stsToClassMap.put(TYPE_REMINDER, Reminder.class);
		classToStsMap.put(Reminder.class, TYPE_REMINDER);
		stsToClassMap.put(TYPE_STOCK_ENTRY, StockEntry.class);
		classToStsMap.put(StockEntry.class, TYPE_STOCK_ENTRY);
		stsToClassMap.put(TYPE_TARMEDLEISTUNG, TarmedLeistung.class);
		classToStsMap.put(TarmedLeistung.class, TYPE_TARMEDLEISTUNG);
		stsToClassMap.put(TYPE_TERMIN, Termin.class);
		classToStsMap.put(Termin.class, TYPE_TERMIN);
		stsToClassMap.put(TYPE_USER, User.class);
		classToStsMap.put(User.class, TYPE_USER);
		stsToClassMap.put(TYPE_VERRECHNET, Verrechnet.class);
		classToStsMap.put(Verrechnet.class, TYPE_VERRECHNET);

		// uni-directional mappable
		stsToClassMap.put(TYPE_MEDIKAMENT, Artikel.class);
		stsToClassMap.put(TYPE_EIGENARTIKEL, Artikel.class);
		stsToClassMap.put(TYPE_MEDICAL, Artikel.class);
		stsToClassMap.put(TYPE_MIGEL, Artikel.class);
		stsToClassMap.put(TYPE_KONTAKT, Kontakt.class);
		stsToClassMap.put(TYPE_ORGANISATION, Kontakt.class);
		stsToClassMap.put(TYPE_PATIENT, Kontakt.class);	
		stsToClassMap.put(TYPE_PERSON, Kontakt.class);
		stsToClassMap.put(TYPE_LABORATORY, Kontakt.class);
	}

	/**
	 * 
	 * @param obj
	 * @return <code>null</code> if not resolvable, else the resp. Entity Type
	 */
	public static String getKeyForObject(EntityWithId obj){
		if (obj instanceof Kontakt) {
			// TODO we can not deterministically map person to patient, anwender, mandant as
			// we do not know what was initially intended
			Kontakt k = (Kontakt) obj;
			if (k.isPerson()) {
				if (k.isPatient()) {
					return TYPE_PATIENT;
				}
				return TYPE_PERSON;
			} else if (k.isOrganisation()) {
				if(k.isLaboratory()) {
					return TYPE_LABORATORY;
				}
				return TYPE_ORGANISATION;
			}
			return TYPE_KONTAKT;
		} else if (obj instanceof Artikel) {
			Artikel art = (Artikel) obj;
			ArticleTyp typ = art.getTyp();
			switch (typ) {
			case EIGENARTIKEL:
				return TYPE_EIGENARTIKEL;
			case MEDIKAMENT:
				return TYPE_MEDIKAMENT;
			case MEDICAL:
				return TYPE_MEDICAL;
			case MIGEL:
				return TYPE_MIGEL;
			default:
				return TYPE_ARTIKEL;
			}
		}
		
		if(obj != null) {
			return classToStsMap.get(obj.getClass());
		}

		return null;
	}

	public static Class<? extends EntityWithId> get(String value){
		return stsToClassMap.get(value);
	}
}