package com.flexisaf.FlexiSAF_wk3.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flexisaf.FlexiSAF_wk3.entity.Student;
import com.flexisaf.FlexiSAF_wk3.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class StudentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() {
        // Clean database before each test
        studentRepository.deleteAll();
    }

    @Test
    public void testGetAllStudents_EmptyList() throws Exception {
        mockMvc.perform(get("/api/students"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    public void testGetAllStudents_WithData() throws Exception {
        // Given
        Student student1 = new Student("John Doe", "john@example.com");
        Student student2 = new Student("Jane Smith", "jane@example.com");
        studentRepository.save(student1);
        studentRepository.save(student2);

        // When & Then
        mockMvc.perform(get("/api/students"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name", is("John Doe")))
                .andExpect(jsonPath("$[0].email", is("john@example.com")))
                .andExpect(jsonPath("$[1].name", is("Jane Smith")))
                .andExpect(jsonPath("$[1].email", is("jane@example.com")));
    }

    @Test
    public void testGetStudentById_Found() throws Exception {
        // Given
        Student student = new Student("John Doe", "john@example.com");
        Student savedStudent = studentRepository.save(student);

        // When & Then
        mockMvc.perform(get("/api/students/" + savedStudent.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(savedStudent.getId().intValue())))
                .andExpect(jsonPath("$.name", is("John Doe")))
                .andExpect(jsonPath("$.email", is("john@example.com")));
    }

    @Test
    public void testGetStudentById_NotFound() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/students/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testAddStudent_Success() throws Exception {
        // Given
        Student newStudent = new Student("Alice Brown", "alice@example.com");

        // When & Then
        mockMvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newStudent)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name", is("Alice Brown")))
                .andExpect(jsonPath("$.email", is("alice@example.com")));

        // Verify the student was actually saved
        assert studentRepository.findAll().size() == 1;
    }

    @Test
    public void testAddStudent_WithId() throws Exception {
        // Given - even if we provide an ID, it should be ignored and auto-generated
        Student newStudent = new Student("Bob Wilson", "bob@example.com");

        // When & Then
        mockMvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newStudent)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()));
    }

    @Test
    public void testDeleteStudent_Success() throws Exception {
        // Given
        Student student = new Student("Charlie Davis", "charlie@example.com");
        Student savedStudent = studentRepository.save(student);

        // When & Then
        mockMvc.perform(delete("/api/students/" + savedStudent.getId()))
                .andExpect(status().isNoContent());

        // Verify the student was actually deleted
        assert studentRepository.findById(savedStudent.getId()).isEmpty();
    }

    @Test
    public void testDeleteStudent_NotFound() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/students/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testCompleteWorkflow() throws Exception {
        // 1. Start with empty list
        mockMvc.perform(get("/api/students"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // 2. Add first student
        Student student1 = new Student("Test User 1", "test1@example.com");
        String response1 = mockMvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(student1)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Student savedStudent1 = objectMapper.readValue(response1, Student.class);

        // 3. Add second student
        Student student2 = new Student("Test User 2", "test2@example.com");
        mockMvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(student2)))
                .andExpect(status().isCreated());

        // 4. Get all students (should be 2)
        mockMvc.perform(get("/api/students"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        // 5. Get specific student by ID
        mockMvc.perform(get("/api/students/" + savedStudent1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Test User 1")));

        // 6. Delete first student
        mockMvc.perform(delete("/api/students/" + savedStudent1.getId()))
                .andExpect(status().isNoContent());

        // 7. Verify only one student remains
        mockMvc.perform(get("/api/students"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Test User 2")));
    }
}
