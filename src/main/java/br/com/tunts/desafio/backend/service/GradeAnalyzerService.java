package br.com.tunts.desafio.backend.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import br.com.tunts.desafio.backend.domain.Student;
import br.com.tunts.desafio.backend.domain.StudentSituation;
import br.com.tunts.desafio.backend.google.sheets.SheetsReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Class responsible for conducting the logic of analyzing each student's grades
 * and determining their situation. It will also calculate the necessary grade
 * requirement if the student in question hasn't obtained the passing grade but
 * also hasn't flunked.
 * 
 * @author Leonardo A. Alcantara
 *
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class GradeAnalyzerService {

	private static final long NAF_FORMULA_DIVISOR = 2l;

	private static final long MINIMUM_GRADE_FOR_NAF = 50l;

	private final SheetsReader sheetsReader;

	private final AttendanceAnalyzerService attendanceAnalyzerService;

	private static final Long NUM_OF_GRADES = 3l;

	public static final Long FAILING_GRADE = 50l;

	public static final Long PASSING_GRADE = 70l;

	/**
	 * Starts the analysis of the students' grades in order to determine their
	 * situation. It will obtain the students and their grades from a Google
	 * Spreadsheet and do the appropriate processing, at the end it will command
	 * that the derived values be written in the spreadsheet again.
	 */
	public void doAnalysis() {

		try {

			List<Student> students = this.sheetsReader.readSheet();

			students = this.attendanceAnalyzerService.analyzeAttendances(students);

			students = students.stream()
					.filter(student -> student.getSituation() == null
							|| !student.getSituation().equals(StudentSituation.REPROVADO_POR_FALTA))
					.map(this::analyzeGrade)
					.collect(Collectors.toList());

			this.sheetsReader.updateSheets(students);

		} catch (IOException e) {

			log.error("There was an error while reading the spreadsheet", e);

		} catch (GeneralSecurityException e) {

			log.error("There was a security exception error while reading the spreadsheet", e);
		}

	}

	/**
	 * Calculates the average of the student's grades and determines their situation
	 * given the average obtained.
	 * 
	 * @param student - the student being analyzed
	 * @return the received student with an updated status and necessary grade, if
	 *         it is the case
	 */
	private Student analyzeGrade(Student student) {

		log.info("Analisando a nota do(a) Aluno(a) {} - Matricula {}", student.getName(), student.getEnrollmentId());

		Long averageGrade = BigDecimal.valueOf(student.getFirstGrade())
				.add(BigDecimal.valueOf(student.getSecondGrade()))
				.add(BigDecimal.valueOf(student.getThirdGrade()))
				.divide(BigDecimal.valueOf(GradeAnalyzerService.NUM_OF_GRADES), 0, RoundingMode.CEILING)
				.setScale(0, RoundingMode.CEILING)
				.longValue();

		log.info("Aluno(a) {} obteve média {}", student.getName(), averageGrade);

		if (averageGrade < GradeAnalyzerService.FAILING_GRADE) {

			student.setSituation(StudentSituation.REPROVADO_POR_NOTA);
		} else if (averageGrade >= GradeAnalyzerService.PASSING_GRADE) {

			student.setSituation(StudentSituation.APROVADO);
		} else {

			student.setNecessaryGrade(this.calculateNecessaryGrade(averageGrade));
			student.setSituation(StudentSituation.EXAME_FINAL);
		}

		log.info("Aluno(a) {} - Matricula {} - situação final: {}, NAF (se aplicável): {}", student.getName(),
				student.getEnrollmentId(), student.getSituation(), student.getNecessaryGrade() != null ? student.getNecessaryGrade() : "N/A");

		return student;

	}

	/**
	 * Calculates the necessary grade the student has to achieve in order to pass.
	 * It is determined by the following formula:
	 * <p>
	 * {@code 5 <= (m + naf) / 2 }
	 * 
	 * @param averageGrade - the student's average
	 * @return the necessary passing grade
	 */
	private Long calculateNecessaryGrade(Long averageGrade) {

		return (MINIMUM_GRADE_FOR_NAF * NAF_FORMULA_DIVISOR) - averageGrade;

	}

}
