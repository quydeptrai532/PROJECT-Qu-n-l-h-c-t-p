package com.edu.registration.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class LecturerStudentScopeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String login(String username, String password) throws Exception {
        MvcResult res = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", username, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        return json(res).get("token").asText();
    }

    private JsonNode json(MvcResult res) throws Exception {
        return objectMapper.readTree(res.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    @Test
    void lecturerCanViewAndFilterStudentsInAssignedClassWithMinimalData() throws Exception {
        String tokenGv01 = login("gv01", "123456");

        // 1. Get gv01's assigned classes
        JsonNode classes = json(mockMvc.perform(get("/api/giang-vien/me/lop-hoc-phan")
                        .header("Authorization", "Bearer " + tokenGv01))
                .andExpect(status().isOk())
                .andReturn());

        assertFalse(classes.isEmpty(), "gv01 should have assigned classes");
        long gv01ClassId = -1;
        for (JsonNode c : classes) {
            if (c.get("siSoHienTai").asInt() > 0) {
                gv01ClassId = c.get("id").asLong();
                break;
            }
        }
        assertTrue(gv01ClassId > 0, "gv01 should have at least one class with registered students");

        // 2. View student list in assigned class
        JsonNode students = json(mockMvc.perform(get("/api/giang-vien/lop-hoc-phan/" + gv01ClassId + "/sinh-vien")
                        .header("Authorization", "Bearer " + tokenGv01))
                .andExpect(status().isOk())
                .andReturn());

        assertFalse(students.isEmpty(), "Class student list should not be empty");
        JsonNode firstStudent = students.get(0);
        assertNotNull(firstStudent.get("dangKyId"));
        assertNotNull(firstStudent.get("sinhVienId"));
        assertNotNull(firstStudent.get("maSinhVien"));
        assertNotNull(firstStudent.get("hoTen"));
        assertNotNull(firstStudent.get("email"));

        // Verify data minimization: no sensitive private fields
        assertNull(firstStudent.get("password"));
        assertNull(firstStudent.get("soDienThoai"));
        assertNull(firstStudent.get("token"));
        assertNull(firstStudent.get("cccd"));

        // 3. Filter with matching keyword
        String targetMa = firstStudent.get("maSinhVien").asText();
        JsonNode filtered = json(mockMvc.perform(get("/api/giang-vien/lop-hoc-phan/" + gv01ClassId + "/sinh-vien")
                        .param("tuKhoa", targetMa)
                        .header("Authorization", "Bearer " + tokenGv01))
                .andExpect(status().isOk())
                .andReturn());

        assertFalse(filtered.isEmpty());
        for (JsonNode item : filtered) {
            assertTrue(item.get("maSinhVien").asText().contains(targetMa)
                    || item.get("hoTen").asText().contains(targetMa));
        }

        // 4. Filter with non-matching keyword
        JsonNode noMatch = json(mockMvc.perform(get("/api/giang-vien/lop-hoc-phan/" + gv01ClassId + "/sinh-vien")
                        .param("tuKhoa", "KHONG_TON_TAI_XYZ")
                        .header("Authorization", "Bearer " + tokenGv01))
                .andExpect(status().isOk())
                .andReturn());
        assertTrue(noMatch.isEmpty(), "Non-matching search should yield empty list without errors");
    }

    @Test
    void lecturerCannotAccessStudentsOfOtherLecturersClass() throws Exception {
        String tokenGv01 = login("gv01", "123456");
        String tokenGv02 = login("gv02", "123456");

        // Get gv01's class
        JsonNode gv01Classes = json(mockMvc.perform(get("/api/giang-vien/me/lop-hoc-phan")
                        .header("Authorization", "Bearer " + tokenGv01))
                .andExpect(status().isOk())
                .andReturn());
        long gv01ClassId = gv01Classes.get(0).get("id").asLong();

        // gv02 attempts to access gv01's class student list -> 403 Forbidden
        mockMvc.perform(get("/api/giang-vien/lop-hoc-phan/" + gv01ClassId + "/sinh-vien")
                        .header("Authorization", "Bearer " + tokenGv02))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Bạn không phụ trách lớp học phần này"));

        // gv02 attempts to export gv01's class student list to CSV -> 403 Forbidden
        mockMvc.perform(get("/api/giang-vien/lop-hoc-phan/" + gv01ClassId + "/sinh-vien/csv")
                        .header("Authorization", "Bearer " + tokenGv02))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Bạn không phụ trách lớp học phần này"));
    }

    @Test
    void nonExistentClassOrStudentReturnsNotFound() throws Exception {
        String tokenGv01 = login("gv01", "123456");

        // Non-existent class ID
        mockMvc.perform(get("/api/giang-vien/lop-hoc-phan/999999/sinh-vien")
                        .header("Authorization", "Bearer " + tokenGv01))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Lớp học phần không tồn tại"));

        mockMvc.perform(get("/api/giang-vien/lop-hoc-phan/999999/sinh-vien/csv")
                        .header("Authorization", "Bearer " + tokenGv01))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Lớp học phần không tồn tại"));

        // Non-existent student ID
        mockMvc.perform(get("/api/giang-vien/sinh-vien/999999")
                        .header("Authorization", "Bearer " + tokenGv01))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Không tìm thấy hồ sơ sinh viên"));
    }

    @Test
    void lecturerCanViewAssignedStudentMinimalProfileAndTamperingUrlReturnsForbidden() throws Exception {
        String tokenGv01 = login("gv01", "123456");

        // Find a student enrolled in gv01's class
        JsonNode gv01Classes = json(mockMvc.perform(get("/api/giang-vien/me/lop-hoc-phan")
                        .header("Authorization", "Bearer " + tokenGv01))
                .andExpect(status().isOk())
                .andReturn());

        long gv01ClassId = -1;
        long assignedStudentId = -1;
        for (JsonNode c : gv01Classes) {
            JsonNode list = json(mockMvc.perform(get("/api/giang-vien/lop-hoc-phan/" + c.get("id").asLong() + "/sinh-vien")
                            .header("Authorization", "Bearer " + tokenGv01))
                    .andReturn());
            if (!list.isEmpty()) {
                gv01ClassId = c.get("id").asLong();
                assignedStudentId = list.get(0).get("sinhVienId").asLong();
                break;
            }
        }
        assertTrue(assignedStudentId > 0, "Found an assigned student for gv01");

        // 1. Detail in class context: /giang-vien/lop-hoc-phan/{lopId}/sinh-vien/{studentId}
        JsonNode profileInClass = json(mockMvc.perform(
                        get("/api/giang-vien/lop-hoc-phan/" + gv01ClassId + "/sinh-vien/" + assignedStudentId)
                                .header("Authorization", "Bearer " + tokenGv01))
                .andExpect(status().isOk())
                .andReturn());

        assertEquals(assignedStudentId, profileInClass.get("id").asLong());
        assertNotNull(profileInClass.get("ma"));
        assertNotNull(profileInClass.get("hoTen"));
        assertNotNull(profileInClass.get("email"));
        assertNotNull(profileInClass.get("khoaMa"));
        assertNotNull(profileInClass.get("nganhMa"));
        assertNotNull(profileInClass.get("lopMa"));
        assertNotNull(profileInClass.get("trangThaiHocTap"));
        // Privacy check: no phone number, no password
        assertNull(profileInClass.get("soDienThoai"));
        assertNull(profileInClass.get("password"));

        // 2. Direct student lookup: /giang-vien/sinh-vien/{studentId}
        JsonNode directProfile = json(mockMvc.perform(
                        get("/api/giang-vien/sinh-vien/" + assignedStudentId)
                                .header("Authorization", "Bearer " + tokenGv01))
                .andExpect(status().isOk())
                .andReturn());
        assertEquals(assignedStudentId, directProfile.get("id").asLong());
        assertNull(directProfile.get("soDienThoai"));

        // 3. Academic training profile: /giang-vien/sinh-vien/{studentId}/ho-so
        JsonNode hoSoDaoTao = json(mockMvc.perform(
                        get("/api/giang-vien/sinh-vien/" + assignedStudentId + "/ho-so")
                                .header("Authorization", "Bearer " + tokenGv01))
                .andExpect(status().isOk())
                .andReturn());
        assertNotNull(hoSoDaoTao.get("khoaMa"));
        assertNotNull(hoSoDaoTao.get("nganhMa"));
        assertNull(hoSoDaoTao.get("soDienThoai"));

        // 4. Create an unassigned student using admin account
        String tokenAdmin = login("admin", "admin123");
        String uniqueSuffix = String.valueOf(System.currentTimeMillis() % 100000);
        MvcResult createdSvRes = mockMvc.perform(post("/api/admin/sinh-vien")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "ma", "SV_UNASSIGNED_" + uniqueSuffix,
                                "hoTen", "Sinh vien Khong Thuoc Lop",
                                "email", "sv_unassigned_" + uniqueSuffix + "@student.edu.vn",
                                "username", "sv_unassigned_" + uniqueSuffix,
                                "matKhau", "123456"
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        long unassignedStudentId = json(createdSvRes).get("id").asLong();

        // gv01 changes studentId on URL -> 403 Forbidden!
        mockMvc.perform(get("/api/giang-vien/sinh-vien/" + unassignedStudentId)
                        .header("Authorization", "Bearer " + tokenGv01))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(
                        "Bạn không có quyền xem thông tin sinh viên này do không phụ trách lớp của sinh viên"));

        // gv01 changes studentId on class-scoped URL -> 403 Forbidden!
        mockMvc.perform(get("/api/giang-vien/lop-hoc-phan/" + gv01ClassId + "/sinh-vien/" + unassignedStudentId)
                        .header("Authorization", "Bearer " + tokenGv01))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(
                        "Sinh viên không thuộc lớp học phần mà bạn phụ trách"));
    }

    @Test
    void assignedStudentSearchDoesNotLeakData() throws Exception {
        String tokenGv01 = login("gv01", "123456");

        // List all students assigned to gv01
        JsonNode assignedList = json(mockMvc.perform(get("/api/giang-vien/sinh-vien")
                        .header("Authorization", "Bearer " + tokenGv01))
                .andExpect(status().isOk())
                .andReturn());

        assertFalse(assignedList.isEmpty(), "gv01 should have assigned students");
        for (JsonNode item : assignedList) {
            assertNotNull(item.get("id"));
            assertNotNull(item.get("ma"));
            assertNotNull(item.get("hoTen"));
            assertNull(item.get("soDienThoai"));
            assertNull(item.get("password"));
        }

        // Create an unassigned student using admin account
        String tokenAdmin = login("admin", "admin123");
        String uniqueSuffix = String.valueOf(System.currentTimeMillis() % 100000);
        String secretCode = "SV_SECRET_" + uniqueSuffix;
        mockMvc.perform(post("/api/admin/sinh-vien")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "ma", secretCode,
                                "hoTen", "Sinh vien Secret",
                                "email", "secret_" + uniqueSuffix + "@student.edu.vn",
                                "username", "secret_" + uniqueSuffix,
                                "matKhau", "123456"
                        ))))
                .andExpect(status().isOk());

        // Search with keyword of the unassigned student -> must return empty list without leaking
        JsonNode leakCheck = json(mockMvc.perform(get("/api/giang-vien/sinh-vien")
                        .param("tuKhoa", secretCode)
                        .header("Authorization", "Bearer " + tokenGv01))
                .andExpect(status().isOk())
                .andReturn());

        assertTrue(leakCheck.isEmpty(), "Search must not leak unassigned students");
    }

    @Test
    void studentCannotAccessLecturerScopedEndpoints() throws Exception {
        String tokenSv01 = login("sv01", "123456");

        mockMvc.perform(get("/api/giang-vien/sinh-vien")
                        .header("Authorization", "Bearer " + tokenSv01))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/giang-vien/sinh-vien/1")
                        .header("Authorization", "Bearer " + tokenSv01))
                .andExpect(status().isForbidden());
    }
}
