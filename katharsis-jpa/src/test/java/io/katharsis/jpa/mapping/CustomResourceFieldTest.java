package io.katharsis.jpa.mapping;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

import javax.persistence.EntityManager;

import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.katharsis.core.internal.resource.ResourceFieldImpl;
import io.katharsis.jpa.AbstractJpaJerseyTest;
import io.katharsis.jpa.JpaModule;
import io.katharsis.jpa.internal.JpaResourceInformationBuilder;
import io.katharsis.jpa.model.CountryEntity;
import io.katharsis.jpa.model.CountryTranslationEntity;
import io.katharsis.jpa.model.CountryTranslationPK;
import io.katharsis.jpa.model.LangEntity;
import io.katharsis.jpa.query.AbstractJpaTest;
import io.katharsis.jpa.util.EntityManagerProducer;
import io.katharsis.jpa.util.SpringTransactionRunner;
import io.katharsis.meta.MetaLookup;
import io.katharsis.meta.model.MetaAttribute;
import io.katharsis.meta.model.MetaDataObject;
import io.katharsis.resource.information.ResourceField;
import io.katharsis.resource.information.ResourceFieldAccess;
import io.katharsis.resource.information.ResourceFieldAccessor;
import io.katharsis.resource.information.ResourceFieldType;
import io.katharsis.rs.type.JsonApiMediaType;
import io.restassured.RestAssured;
import io.restassured.response.Response;

/**
 * Example of how to add a custom ResourceField and in turn change the from an
 * entity resulting resource.
 */
public class CustomResourceFieldTest extends AbstractJpaJerseyTest {

	@Override
	@Before
	public void setup() {
		super.setup();

		SpringTransactionRunner transactionRunner = context.getBean(SpringTransactionRunner.class);
		transactionRunner.doInTransaction(new Callable<Object>() {

			@Override
			public Object call() throws Exception {
				EntityManager em = context.getBean(EntityManagerProducer.class).getEntityManager();
				AbstractJpaTest.clear(em);

				LangEntity en = new LangEntity();
				en.setLangCd("en");
				em.persist(en);
				LangEntity de = new LangEntity();
				de.setLangCd("de");
				em.persist(de);

				CountryEntity ch = new CountryEntity();
				ch.setCountryCd("ch");
				ch.setCtlActCd(true);
				em.persist(ch);

				CountryTranslationEntity chDe = new CountryTranslationEntity();
				CountryTranslationPK chDePk = new CountryTranslationPK();
				chDePk.setCountry(ch);
				chDePk.setLang(de);
				chDe.setCountryTranslationPk(chDePk);
				chDe.setTxt("Schweiz");
				em.persist(chDe);

				CountryTranslationEntity chEn = new CountryTranslationEntity();
				CountryTranslationPK chEnPk = new CountryTranslationPK();
				chEnPk.setCountry(ch);
				chEnPk.setLang(en);
				chEn.setCountryTranslationPk(chEnPk);
				chEn.setTxt("Switzerland");
				em.persist(chEn);

				em.flush();
				return null;
			}
		});

	}

	@Override
	protected void setupModule(final JpaModule module, boolean server) {
		super.setupModule(module, server);

		if (server) {
			MetaLookup metaLookup = module.getJpaMetaLookup();
			module.setResourceInformationBuilder(new JpaResourceInformationBuilder(metaLookup) {

				@Override
				protected List<ResourceField> buildFields(MetaDataObject meta) {
					List<ResourceField> fields = super.buildFields(meta);

					if (meta.getImplementationClass() == CountryEntity.class) {
						List<String> languages = Arrays.asList("en", "de");
						for (final String language : languages) {
							ResourceFieldType resourceFieldType = ResourceFieldType.ATTRIBUTE;
							String name = language + "Text";
							Class<?> type = String.class;
							boolean lazy = false;
							ResourceFieldAccess access = new ResourceFieldAccess(true, true, false, false);

							ResourceFieldImpl field = new ResourceFieldImpl(name, name, resourceFieldType, type, type, null, null, lazy, false, null, access);
							field.setAccessor(new ResourceFieldAccessor() {

								@Override
								public String getValue(Object resource) {
									CountryEntity country = (CountryEntity) resource;
									List<CountryTranslationEntity> translations = country.getTranslations();
									CountryTranslationEntity translation = getTranslation(translations, language);
									return translation != null ? translation.getTxt() : null;
								}

								@Override
								public void setValue(Object resource, Object fieldValue) {
									CountryEntity country = (CountryEntity) resource;
									List<CountryTranslationEntity> translations = country.getTranslations();
									CountryTranslationEntity translation = getTranslation(translations, language);
									if (translation == null) {

										EntityManager em = module.getEntityManager();
										LangEntity langEntity = em.find(LangEntity.class, language);
										if (langEntity == null) {
											throw new IllegalStateException("language not found: " + language);
										}

										translation = new CountryTranslationEntity();
										CountryTranslationPK pk = new CountryTranslationPK();
										pk.setCountry(country);
										pk.setLang(langEntity);
										translation.setCountryTranslationPk(pk);
										translations.add(translation);
									}
									translation.setTxt((String) fieldValue);
								}

								private CountryTranslationEntity getTranslation(List<CountryTranslationEntity> translations, String language) {
									for (CountryTranslationEntity translation : translations) {
										CountryTranslationPK translationPk = translation.getCountryTranslationPk();
										String langCd = translationPk.getLang().getLangCd();
										if (langCd.equals(language)) {
											return translation;
										}
									}
									return null;
								}
							});
							fields.add(field);
						}
					}
					return fields;
				}
			});
		}
	}

	@Test
	public void test() throws InstantiationException, IllegalAccessException {
		String url = getBaseUri() + "country/ch";
		io.restassured.response.Response getResponse = RestAssured.get(url);
		Assert.assertEquals(200, getResponse.getStatusCode());

		getResponse.then().assertThat().body("data.attributes.deText", Matchers.equalTo("Schweiz"));
		getResponse.then().assertThat().body("data.attributes.enText", Matchers.equalTo("Switzerland"));

		String patchData = "{'data':{'id':'ch','type':'country','attributes':{'deText':'Test','enText':'Switzerland','ctlActCd':true}}}".replaceAll("'", "\"");

		Response patchResponse = RestAssured.given().body(patchData.getBytes()).header("content-type", JsonApiMediaType.APPLICATION_JSON_API).when().patch(url);
		patchResponse.then().statusCode(HttpStatus.SC_OK);

		getResponse = RestAssured.get(url);
		Assert.assertEquals(200, getResponse.getStatusCode());
		getResponse.then().assertThat().body("data.attributes.deText", Matchers.equalTo("Test"));
		getResponse.then().assertThat().body("data.attributes.enText", Matchers.equalTo("Switzerland"));
	}
}