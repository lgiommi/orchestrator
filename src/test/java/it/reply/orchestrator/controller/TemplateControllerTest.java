package it.reply.orchestrator.controller;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseTearDown;

import java.nio.charset.Charset;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import es.upv.i3m.grycap.file.FileIO;
import it.reply.orchestrator.config.WebAppConfigurationAware;

@DatabaseTearDown("/data/database-empty.xml")
@DatabaseSetup("/data/database-resource-init.xml")
public class TemplateControllerTest extends WebAppConfigurationAware {

  // @Autowired
  // private WebApplicationContext wac;

  @Rule
  public JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation(
      "target/generated-snippets");

  private MockMvc mockMvc;

  private final MediaType APPLICATION_JSON_UTF8 = new MediaType(
      MediaType.APPLICATION_JSON.getType(), MediaType.APPLICATION_JSON.getSubtype(),
      Charset.forName("utf8"));

  private final String templatePath = "./src/test/resources/tosca/galaxy_tosca.yaml";
  private final String deploymentId = "0748fbe9-6c1d-4298-b88f-06188734ab42";

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    mockMvc = MockMvcBuilders.webAppContextSetup(wac).dispatchOptions(true)
        .apply(documentationConfiguration(this.restDocumentation))
        .alwaysDo(document("{method-name}/{step}/", preprocessRequest(prettyPrint()),
            preprocessResponse(prettyPrint())))
        .build();
  }

  @Test
  @DatabaseSetup("/data/database-resource-init.xml")
  public void getTemplate() throws Exception {

    String template = FileIO.readUTF8File(templatePath);
    MvcResult result = mockMvc.perform(get("/deployments/" + deploymentId + "/template"))
        .andExpect(status().isOk()).andExpect(content().contentType(MediaType.TEXT_PLAIN))
        .andDo(document("index")).andReturn();

    String content = result.getResponse().getContentAsString();
    assertEquals(content, template);

  }

  @Test
  public void getTemplateNotFoundNotDeployment() throws Exception {
    mockMvc.perform(get("/deployments/aaaaaaaa-bbbb-ccccc-dddd-eeeeeeeeeeee/template"))
        .andExpect(status().isNotFound()).andExpect(content().contentType(APPLICATION_JSON_UTF8))
        .andExpect(jsonPath("$.code", is(404))).andExpect(jsonPath("$.title", is("Not Found")))
        .andExpect(jsonPath("$.message",
            is("The deployment <aaaaaaaa-bbbb-ccccc-dddd-eeeeeeeeeeee> doesn't exist")));
  }
}
