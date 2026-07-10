package com.studentregistration.web;

import java.util.List;

import com.studentregistration.dto.CreateStudentRequest;
import com.studentregistration.dto.StudentDto;
import com.studentregistration.dto.UpdateStudentRequest;
import com.studentregistration.service.ProgramChangeService;
import com.studentregistration.service.StudentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * StudentController — maps the {@code /api/students...} HTTP routes to service
 * calls. This is the Spring MVC equivalent of the {@code @app.route} functions in
 * app.py. The controller is intentionally THIN: it only translates HTTP in/out and
 * delegates all logic to the services. Errors are thrown as {@link ApiException}
 * and converted to JSON centrally by {@link GlobalExceptionHandler}.
 *
 * <ul>
 *   <li>{@code @RestController} — every handler's return value is serialized to the
 *       response body (as JSON) rather than treated as a view name.</li>
 *   <li>{@code @RequestMapping("/api/students")} — the shared path prefix.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final StudentService studentService;
    private final ProgramChangeService programChangeService;

    public StudentController(StudentService studentService,
                            ProgramChangeService programChangeService) {
        this.studentService = studentService;
        this.programChangeService = programChangeService;
    }

    /** GET /api/students → 200 with a JSON array of students (newest first). */
    @GetMapping
    public List<StudentDto> list() {
        return studentService.listStudents();
    }

    /**
     * POST /api/students → 201 with the created student.
     * {@code @RequestBody} deserializes the JSON body into the request record.
     * We use {@link ResponseEntity} to set the 201 status (Flask returned 201);
     * plain returns default to 200.
     */
    @PostMapping
    public ResponseEntity<StudentDto> create(@RequestBody(required = false) CreateStudentRequest request) {
        // Tolerate a totally empty body (Flask did `request.get_json() or {}`).
        CreateStudentRequest safe = request != null
                ? request
                : new CreateStudentRequest(null, null, null, null, null, null);
        StudentDto created = studentService.createStudent(safe);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * PUT /api/students/{id} → 200 with the updated student.
     * {@code @PathVariable} binds the {id} URL segment to the method parameter.
     */
    @PutMapping("/{id}")
    public StudentDto update(@PathVariable long id,
                             @RequestBody(required = false) UpdateStudentRequest request) {
        UpdateStudentRequest safe = request != null
                ? request
                : new UpdateStudentRequest(null, null, null);
        return studentService.updateStudent(id, safe);
    }

    /**
     * POST /api/students/{id}/program-change → 200 with the updated student.
     * This endpoint is {@code multipart/form-data}, so we bind form fields instead of
     * a JSON body:
     * <ul>
     *   <li>{@code @RequestParam("program")} — the new program name (text field).</li>
     *   <li>{@code @RequestParam("forms")} — the list of uploaded PDFs. The frontend
     *       appends every file under the same {@code "forms"} key, so this arrives as
     *       a {@code List<MultipartFile>}. {@code required = false} lets the service
     *       produce the exact "1 to 2 PDF file(s)" message when none are sent, rather
     *       than Spring rejecting the request first.</li>
     * </ul>
     */
    @PostMapping("/{id}/program-change")
    public StudentDto changeProgram(
            @PathVariable long id,
            @RequestParam(value = "program", required = false) String program,
            @RequestParam(value = "forms", required = false) List<MultipartFile> forms) {
        return programChangeService.changeProgram(id, program, forms);
    }
}
