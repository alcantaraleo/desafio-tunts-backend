package br.com.tunts.desafio.backend.google.sheets;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.UpdateValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;

import br.com.tunts.desafio.backend.domain.Student;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SpreadsheetManipulatorService {

	private static final String ACCESS_TYPE = "offline";

	private static final int OAUTH_SERVER_PORT = 8888;

	private static final String APPLICATION_NAME = "Tunts-Backend-Java-Leonardo-Alcantara";

	private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

	private static final String TOKENS_DIRECTORY_PATH = "tokens";

	/**
	 * The scope of what is be available for this program to do when manipulating the Google Spreadsheet. Since we need to update it ,
	 * it is required full access. 
	 */
	private static final List<String> SCOPES = Arrays.asList(SheetsScopes.SPREADSHEETS, SheetsScopes.DRIVE);

	private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

	@Value("${spreadsheet.id}")
	private String spreadsheetId;

	@Value("${spreadsheet.range}")
	private String spreadsheetRange;

	@Value("${spreadsheet.numberOfClasses.range}")
	private String spreadhSheetNumberOfClassesRange;
	
	@Value("${spreadsheet.valueInputOption}")
	private String valueInputOption;
	
	@Value("${credential.user}")
	private String credentialUser;

	/**
	 * Creates an authorized Credential object in order to manipulate a Google Spreadsheet. It reads the credentials
	 * from a file located in {@code /src/main/resources} 
	 * 
	 * @param HTTP_TRANSPORT The network HTTP Transport.
	 * @return An authorized Credential object.
	 * @throws IOException If the credentials.json file cannot be found.
	 */
	private Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {

		// Load client secrets.
		InputStream in = SpreadsheetManipulatorService.class.getResourceAsStream(CREDENTIALS_FILE_PATH);

		if (in == null) {

			throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
		}
		GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

		// Build flow and trigger user authorization request.
		GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY,
				clientSecrets, SCOPES)
						.setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
						.setAccessType(ACCESS_TYPE)
						.build();
		LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(OAUTH_SERVER_PORT).build();
		return new AuthorizationCodeInstalledApp(flow, receiver).authorize(this.credentialUser);

	}

	/**
	 * Reads all the rows containing student data from a Google Spreadsheet and
	 * returns them as proper {@link Student} objects.
	 * 
	 * @return a list of @{link {@link Student} objects, as read from the
	 *         spreadsheet
	 * @throws IOException
	 * @throws GeneralSecurityException
	 */
	public List<Student> readSheet() throws IOException, GeneralSecurityException {

		// Build a new authorized API client service.
		final NetHttpTransport HTTP_TRANSPORT = buildHTTPTransport();

		Sheets service = createSheetsService(HTTP_TRANSPORT);
		
		ValueRange response = loadValuesFromSheet(service, spreadsheetRange);

		List<List<Object>> values = response.getValues();

		if (values == null || values.isEmpty()) {

			log.warn("No data found.");
			return Collections.emptyList();
		}

		return values.stream().map(row -> Student.mapToStudent(row)).collect(Collectors.toList());

	}

	private ValueRange loadValuesFromSheet(Sheets service, String spreadsheetRange) throws IOException {

		return service.spreadsheets().values().get(spreadsheetId, spreadsheetRange).execute();

	}

	private Sheets createSheetsService(final NetHttpTransport HTTP_TRANSPORT) throws IOException {

		Sheets service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
				.setApplicationName(APPLICATION_NAME)
				.build();
		return service;

	}

	

	/**
	 * Specific method to derive the total number of classes that the course had.
	 * This information is located in a specific cell in the spreadsheet and may be
	 * prone to tempering by the user.
	 * 
	 * @return the total number of classes that were made available for the course
	 * @throws IOException
	 * @throws GeneralSecurityException
	 */
	public Long readNumberOfClasses() throws IOException, GeneralSecurityException {

		// Build a new authorized API client service.
		final NetHttpTransport HTTP_TRANSPORT = buildHTTPTransport();

		Sheets service = createSheetsService(HTTP_TRANSPORT);
		ValueRange response = loadValuesFromSheet(service, spreadhSheetNumberOfClassesRange);

		List<List<Object>> values = response.getValues();

		if (values == null || values.isEmpty()) {

			log.error("It wasn't possible to locate the total number of classes");
			return Long.MIN_VALUE;
		}

		String numOfClass = ((String) values.get(0).get(0)).split(":")[1];
		
		log.info("Total amount of classes read from spreadsheet :: {}", numOfClass);

		return Long.valueOf(numOfClass.trim());

	}

	private NetHttpTransport buildHTTPTransport() throws GeneralSecurityException, IOException {

		return GoogleNetHttpTransport.newTrustedTransport();

	}

	/**
	 * Writes the updated values for each students' situation, after the analysis
	 * has been completed on their grades.
	 * 
	 * @param students the students being recorded back into the spreadsheet.
	 * @throws IOException 
	 * @throws GeneralSecurityException 
	 */
	public void updateSheets(List<Student> students) throws GeneralSecurityException, IOException {

		log.info("Updating the spreadsheet...");
		
		// transforms from Student objects to List<Object>
		List<List<Object>> rows = students.stream().map(Student::mapToRow).collect(Collectors.toList());
		
		// necessary values/config for updating the sheet
		ValueRange body = new ValueRange().setValues(rows);
		final NetHttpTransport HTTP_TRANSPORT = buildHTTPTransport();
		Sheets service = createSheetsService(HTTP_TRANSPORT);
		
		//performs the update and get the response back.
		UpdateValuesResponse response =
		        service.spreadsheets().values().update(spreadsheetId, this.spreadsheetRange, body)
		                .setValueInputOption(valueInputOption)
		                .execute();
		
		log.info("{} rows were updated in the spreadsheet", response.getUpdatedCells());
		

	}

}
