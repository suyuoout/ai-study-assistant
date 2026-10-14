
package com.study.assisiant;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.study.assisiant.mapper")
public class StudyAssisiantApplication {

	public static void main(String[] args) {
		SpringApplication.run(StudyAssisiantApplication.class, args);
	}
}