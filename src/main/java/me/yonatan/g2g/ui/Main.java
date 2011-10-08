package me.yonatan.g2g.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.LogManager;

import javax.enterprise.event.Observes;
import javax.inject.Inject;

import lombok.SneakyThrows;
import me.yonatan.g2g.core.Runner;
import me.yonatan.g2g.core.model.GmailCredentials;

import org.jboss.weld.environment.se.events.ContainerInitialized;

public class Main {
	public static boolean run = true;

	@Inject
	private Runner runner;

	@SneakyThrows(IOException.class)
	public void start(@Observes ContainerInitialized e) {
		InputStreamReader istream = new InputStreamReader(System.in);
		BufferedReader bufRead = new BufferedReader(istream);

		System.out.println("Please enter the source account: ");
		String sourceEmail = bufRead.readLine();
		System.out.println(sourceEmail);
		
		System.out.println("Please enter the source password: ");
		String sourcePassword = bufRead.readLine();
		System.out.println(sourcePassword);
		
		System.out.println("Please enter the target account: ");
		String targetEmail = bufRead.readLine();
		System.out.println(targetEmail);

		System.out.println("Please enter the target password: ");
		String targetPassword = bufRead.readLine();
		System.out.println(targetPassword);
		
		runner.setSourceAccount(new GmailCredentials(sourceEmail, sourcePassword));
		runner.setTargetAccount(new GmailCredentials(targetEmail, targetPassword));

		System.out.println("Please press ENTER to start coping messages");
		bufRead.readLine();
		runner.start();
		


	}

	public static void main(String[] args) throws InterruptedException {
		java.util.logging.LogManager lm = LogManager.getLogManager();
		try {
			lm.readConfiguration(Main.class.getResourceAsStream("/logging.properties"));
		} catch (Exception e) {
			// Whatever...
		}
		org.jboss.weld.environment.se.StartMain.main(args);
	}

}

