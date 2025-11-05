package channal.bfs.controller;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

//db 연결 테스트용 api 
@RestController
@RequestMapping("/api/db")
public class DbPingController {

    @PersistenceContext
    private EntityManager entityManager;

    @GetMapping("/ping")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> ping() {
        Map<String, Object> response = new HashMap<>();
        try {
            // Query minimal metadata to verify connectivity
            List<Object[]> rows = entityManager.createNativeQuery(
                    "select current_database(), current_user, now(), version()"
            ).getResultList();

            Object[] row = rows.getFirst();
            response.put("ok", true);
            response.put("database", row[0]);
            response.put("user", row[1]);
            response.put("now", row[2]);
            response.put("version", row[3]);
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            response.put("ok", false);
            response.put("error", ex.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}


