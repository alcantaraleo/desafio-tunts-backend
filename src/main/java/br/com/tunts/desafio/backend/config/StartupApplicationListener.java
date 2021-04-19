package br.com.tunts.desafio.backend.config;

import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import br.com.tunts.desafio.backend.service.GradeAnalyzerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Component
public class StartupApplicationListener implements ApplicationListener<ContextRefreshedEvent> {
	
	private final GradeAnalyzerService gradeAnalyzerService;

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {

		log.info("Inicialização concluída, dando início a análise da planilha...");
		
		gradeAnalyzerService.doAnalysis();
		
		log.info("Análise concluída...");
		

	}

}
