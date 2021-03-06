package io.katharsis.example.springboot.simple;

import static com.jayway.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchema;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;

import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.ValidatableResponse;

import io.katharsis.client.KatharsisClient;
import io.katharsis.jpa.JpaModule;
import io.katharsis.validation.ValidationModule;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = SpringBootSimpleExampleApplication.class)
@WebIntegrationTest("server.port:0")
public abstract class BaseTest {

	@Value("${local.server.port}")
	protected int port;

	protected String jsonApiSchema;

	protected KatharsisClient client;

	@Before
	public final void before() {
		RestAssured.port = port;
		loadJsonApiSchema();

		client = new KatharsisClient("http://localhost:" + port + "/api");
		client.addModule(ValidationModule.newInstance());
		client.addModule(JpaModule.newClientModule("io.katharsis.example.springboot.simple.domain.jpa"));
	}

	private void loadJsonApiSchema() {
		try {
			jsonApiSchema = loadFile("json-api-schema.json");
		}
		catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	private static String loadFile(String filename) throws Exception {
		InputStream inputStream = BaseTest.class.getClassLoader().getResourceAsStream(filename);
		return IOUtils.toString(inputStream);
	}

	protected void testFindOne(String url) {
		ValidatableResponse response = RestAssured.given().contentType("application/json").when().get(url).then()
				.statusCode(OK.value());
		response.assertThat().body(matchesJsonSchema(jsonApiSchema));
	}

	protected void testFindOne_NotFound(String url) {
		RestAssured.given().contentType("application/json").when().get(url).then().statusCode(NOT_FOUND.value());
	}

	protected void testFindMany(String url) {
		ValidatableResponse response = RestAssured.given().contentType("application/json").when().get(url).then()
				.statusCode(OK.value());
		response.assertThat().body(matchesJsonSchema(jsonApiSchema));
	}

	protected void testDelete(String url) {
		RestAssured.given().contentType("application/json").when().delete(url).then().statusCode(NO_CONTENT.value());
	}
}
