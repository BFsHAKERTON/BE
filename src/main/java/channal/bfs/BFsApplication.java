package channal.bfs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class BFsApplication {

    public static void main(String[] args) {
        SpringApplication.run(BFsApplication.class, args);
    }

}
