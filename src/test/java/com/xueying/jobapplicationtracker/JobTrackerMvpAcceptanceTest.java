package com.xueying.jobapplicationtracker;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xueying.jobapplicationtracker.entity.ApplicationEntity;
import com.xueying.jobapplicationtracker.entity.DocumentEntity;
import com.xueying.jobapplicationtracker.entity.User;
import com.xueying.jobapplicationtracker.mapper.ApplicationMapper;
import com.xueying.jobapplicationtracker.mapper.DocumentMapper;
import com.xueying.jobapplicationtracker.mapper.UserMapper;
import com.xueying.jobapplicationtracker.service.MinIOService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:jobtracker;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.sql.init.mode=always",
        "minio.endpoint=http://localhost:9000",
        "minio.accessKey=test-access-key",
        "minio.secretKey=test-secret-key",
        "minio.bucket=test-bucket"
})
class JobTrackerMvpAcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ApplicationMapper applicationMapper;

    @Autowired
    private DocumentMapper documentMapper;

    @MockBean
    private MinIOService minIOService;

    @BeforeEach
    void setUpMinioMock() throws Exception {
        doNothing().when(minIOService).createBucketIfNotExists();
        doNothing().when(minIOService).delete(anyString());
        when(minIOService.upload(anyString(), any(InputStream.class), anyLong(), anyString()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(minIOService.download(anyString()))
                .thenReturn(new ByteArrayInputStream("mock-file".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void unauthenticatedAccessRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        mockMvc.perform(get("/applications"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        mockMvc.perform(multipart("/applications/create")
                        .file(new MockMultipartFile("attachments", "blocked.txt", "text/plain", "x".getBytes(StandardCharsets.UTF_8)))
                        .param("company", "Blocked Co")
                        .param("role", "Blocked Role")
                        .param("region", "UK")
                        .param("status", "Applied")
                .param("appliedDate", LocalDate.now().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void dashboardEmptyStateAndRegionStatusUpdateAfterCreate() throws Exception {
        MockHttpSession session = registerAndLogin();

        MvcResult emptyDashboard = mockMvc.perform(get("/dashboard").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Application Dashboard")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("No applications yet.")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Recent Added")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Cross-Region"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Recent Updated"))))
                .andReturn();

        long[] beforeCounts = extractUkStatusCounts(emptyDashboard.getResponse().getContentAsString());
        assertThat(beforeCounts).containsExactly(0L, 0L, 0L, 0L);

        mockMvc.perform(post("/applications/create")
                        .session(session)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("company", "Acme Systems")
                        .param("role", "Backend Engineer")
                        .param("region", "UK")
                        .param("status", "Applied")
                        .param("appliedDate", LocalDate.now().toString())
                        .param("link", "https://example.com/jobs/1")
                        .param("country", "United Kingdom")
                        .param("companySummary", "Cloud platform")
                        .param("notes", "Stage 1 complete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/applications"));

        MvcResult updatedDashboard = mockMvc.perform(get("/dashboard").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Acme Systems / Backend Engineer")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Recent Updated"))))
                .andReturn();

        long[] afterCounts = extractUkStatusCounts(updatedDashboard.getResponse().getContentAsString());
        assertThat(afterCounts).containsExactly(1L, 0L, 0L, 0L);
    }

    @Test
    void attachmentsVisibleInDetailEditAndOverviewAfterMultiUpload() throws Exception {
        MockHttpSession session = registerAndLogin();
        String email = currentSessionEmail(session);
        User user = userMapper.findByEmail(email);
        assertThat(user).isNotNull();

        mockMvc.perform(multipart("/applications/create")
                        .file(new MockMultipartFile("attachments", "cv.txt", "text/plain", "cv-content".getBytes(StandardCharsets.UTF_8)))
                        .file(new MockMultipartFile("attachments", "cover-letter.txt", "text/plain", "cover-content".getBytes(StandardCharsets.UTF_8)))
                        .session(session)
                        .param("company", "Northwind")
                        .param("role", "Software Engineer")
                        .param("region", "UK")
                        .param("status", "Applied")
                        .param("appliedDate", LocalDate.now().toString())
                        .param("country", "UK")
                        .param("companySummary", "B2B SaaS")
                        .param("notes", "Uploaded both files"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/applications"));

        ApplicationEntity application = findLatestApplication(user.getId());
        assertThat(application).isNotNull();

        List<DocumentEntity> documents = findDocumentsByApplication(user.getId(), application.getId());
        assertThat(documents).hasSize(2);

        mockMvc.perform(get("/applications/" + application.getId()).session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("cv.txt")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("cover-letter.txt")));

        mockMvc.perform(get("/applications/" + application.getId() + "/edit").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Current Attachments")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("cv.txt")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("cover-letter.txt")));

        mockMvc.perform(get("/documents").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Northwind / Software Engineer")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("cv.txt")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("cover-letter.txt")));
    }

    private MockHttpSession registerAndLogin() throws Exception {
        String email = "u-" + UUID.randomUUID().toString().replace("-", "") + "@example.com";
        String password = "Passw0rd!";

        mockMvc.perform(post("/register")
                        .param("email", email)
                        .param("password", password))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        MvcResult loginResult = mockMvc.perform(post("/login")
                        .param("email", email)
                        .param("password", password))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertThat(session).isNotNull();
        session.setAttribute("TEST_USER_EMAIL", email);
        return session;
    }

    private String currentSessionEmail(MockHttpSession session) {
        Object value = session.getAttribute("TEST_USER_EMAIL");
        return value == null ? "" : value.toString();
    }

    private ApplicationEntity findLatestApplication(Long userId) {
        LambdaQueryWrapper<ApplicationEntity> query = new LambdaQueryWrapper<>();
        query.eq(ApplicationEntity::getUserId, userId)
                .orderByDesc(ApplicationEntity::getId)
                .last("LIMIT 1");
        return applicationMapper.selectOne(query);
    }

    private List<DocumentEntity> findDocumentsByApplication(Long userId, Long applicationId) {
        LambdaQueryWrapper<DocumentEntity> query = new LambdaQueryWrapper<>();
        query.eq(DocumentEntity::getUserId, userId)
                .eq(DocumentEntity::getApplicationId, applicationId)
                .orderByDesc(DocumentEntity::getId);
        return documentMapper.selectList(query);
    }

    private long[] extractUkStatusCounts(String html) {
        Pattern ukRowPattern = Pattern.compile("(?s)<tr>\\s*<td>UK</td>\\s*<td>(\\d+)</td>\\s*<td>(\\d+)</td>\\s*<td>(\\d+)</td>\\s*<td>(\\d+)</td>\\s*</tr>");
        Matcher matcher = ukRowPattern.matcher(html);
        assertThat(matcher.find()).isTrue();
        return new long[]{
                Long.parseLong(matcher.group(1)),
                Long.parseLong(matcher.group(2)),
                Long.parseLong(matcher.group(3)),
                Long.parseLong(matcher.group(4))
        };
    }
}

