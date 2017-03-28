package io.katharsis.jpa.model;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.validation.constraints.NotNull;

@Entity
public class CountryTranslationEntity {

	@EmbeddedId
	private CountryTranslationPK countryTranslationPk;

	@Column(name = "txt", insertable = false, updatable = false)
	@NotNull
	private String txt;

	public String getTxt() {
		return txt;
	}

	public void setTxt(String txt) {
		this.txt = txt;
	}

	public CountryTranslationPK getCountryTranslationPk() {
		return countryTranslationPk;
	}

	public void setCountryTranslationPk(CountryTranslationPK countryTranslationPk) {
		this.countryTranslationPk = countryTranslationPk;
	}
}
