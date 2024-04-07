package cz.pts.ptscontrol;

import cz.pts.ptscontrol.config.PtsControlConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@Import(PtsControlConfig.class)
@SpringBootApplication
public class PtsControlApplication {

	public static void main(String[] args) {
		SpringApplication.run(PtsControlApplication.class, args);
	}

}
