package ch.elexis.core.jpa.entities;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import ch.elexis.core.jpa.entities.converter.BooleanCharacterConverterSafe;
import ch.elexis.core.jpa.entities.id.ElexisIdGenerator;
import ch.elexis.core.jpa.entities.listener.EntityWithIdListener;

@Entity
@Table(name = "DIAGNOSEN")
@EntityListeners(EntityWithIdListener.class)
public class Diagnosis implements EntityWithId, EntityWithDeleted {

	// Transparently updated by the EntityListener
	protected Long lastupdate;
	
	@Id
	@GeneratedValue(generator = "system-uuid")
	@Column(unique = true, nullable = false, length = 25)
	private String id = ElexisIdGenerator.generateId();
	
	@Column
	@Convert(converter = BooleanCharacterConverterSafe.class)
	protected boolean deleted = false;
	
	@Column(length = 255, name = "DG_TXT")
	private String text;

	@Column(length = 25, name = "DG_CODE")
	private String code;

	@Column(length = 80, name = "KLASSE")
	private String diagnosisClass;

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getDiagnosisClass() {
		return diagnosisClass;
	}

	public void setDiagnosisClass(String diagnosisClass) {
		this.diagnosisClass = diagnosisClass;
	}

	@Override
	public boolean isDeleted(){
		return deleted;
	}
	
	@Override
	public void setDeleted(boolean deleted){
		this.deleted = deleted;
	}
	
	@Override
	public String getId(){
		return id;
	}
	
	@Override
	public void setId(String id){
		this.id = id;
	}
	
	@Override
	public Long getLastupdate(){
		return lastupdate;
	}
	
	@Override
	public void setLastupdate(Long lastupdate){
		this.lastupdate = lastupdate;
	}
	
	@Override
	public int hashCode(){
		return EntityWithId.idHashCode(this);
	}
	
	@Override
	public boolean equals(Object obj){
		return EntityWithId.idEquals(this, obj);
	}
}
