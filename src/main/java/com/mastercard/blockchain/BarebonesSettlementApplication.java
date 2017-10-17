/**
 * 
 */
package com.mastercard.blockchain;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.codec.binary.Hex;
import org.json.simple.JSONObject;

import com.mastercard.api.blockchain.App;
import com.mastercard.api.blockchain.Node;
import com.mastercard.api.blockchain.Settle;
import com.mastercard.api.blockchain.TransactionEntry;
import com.mastercard.api.core.ApiConfig;
import com.mastercard.api.core.exception.ApiException;
import com.mastercard.api.core.model.RequestMap;
import com.mastercard.api.core.security.oauth.OAuthAuthentication;

/**
 * Copyright (c) 2017 Mastercard. All Rights Reserved.
 *
 */

public class BarebonesSettlementApplication {

	private String ENCODING = "base64";
	private String APP_ID = getAppIdFromProtoBuffer("settle.proto");

	public static void main(String... args) throws Exception {
		CommandLineParser parser = new DefaultParser();
		Options options = createOptions();
		CommandLine cmd = parser.parse(options, args);
		new BarebonesSettlementApplication().start(cmd, options);
	}

	private void start(CommandLine cmd, Options options) throws FileNotFoundException {
		System.out.println(readResourceToString("/help.txt"));
		System.out.println();
		initApi(cmd);
		
		
		updateNode();
		menu(cmd, options);
	}

	private void menu(CommandLine cmd, Options options) {
		final String quit = "0";
		String option = "";
		while (!option.equals(quit)) {
			printHeading("MENU");
			System.out.println("1. Create node (optional, onetime)");
			System.out.println("2. Update protocol buffer definition");
			System.out.println("3. Create settlement request");
			System.out.println("4. Confirm settlement");
			System.out.println("5. Show Protocol Buffer Definition");
			System.out.println("6. Re-initialize API");
			System.out.println("7. Print Command Line Options");
			System.out.println(quit + ". Quit");
			option = captureInput("Option", quit);
			switch (option) {
			case "0":
				System.out.println("Goodbye");
				break;
			case "1":
				createNode();
				break;
			case "2":
				updateNode();
				break;
			case "3":
				createSettlementRequest();
				break;
			case "4":
				confirmSettlement();
				break;
			case "5":
				printHeading("SHOW PROTOCOL BUFFER");
				System.out.println(readResourceToString("/settle.proto"));
				captureInput("(press return to continue)", null);
				break;
			case "6":
				try {
					printHeading("INITIALIZE API");
					initApi(cmd);
				} catch (FileNotFoundException e) {
					System.err.println(e.getMessage());
				}
				captureInput("(press return to continue)", null);
				break;
			case "7":
				printHeading("COMMAND LINE OPTIONS");
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("java -jar <jarfile>", options);
				captureInput("(press return to continue)", null);
				break;
			default:
				System.out.println("Unrecognised option");
				break;
			}
		}
	}

	private void createNode() {
		RequestMap request = new RequestMap();
		request.set("network", "Z0NE");
		request.set("application.name", APP_ID);
		request.set("application.description", "");
		request.set("application.version", 0);
		request.set("application.definition.format", "proto3");
		request.set("application.definition.encoding", "base64");
		request.set("application.definition.messages",
				encode(readResourceToString("/settle.proto").replace(APP_ID, APP_ID).getBytes(), "base64"));
		Node response;
		try {
			response = Node.provision(request);
			System.out.println("Created node: " + response.get("address") + " of type: " + response.get("type"));
		} catch (ApiException e) {
			System.err.println("API Exception " + e.getMessage());
		}
	}

	private void updateNode() {
		APP_ID = updateNode("Update Settlement Protocol Definition", "/settle.proto", APP_ID);
		captureInput("(press return to continue)", null);
	}

	private String updateNode(String title, String file, String appId) {
		printHeading(title);
		String protoPath = captureInput("Protocol Definition Path", file);
		String newAppId = captureInput("App Id", appId);

		try {
			RequestMap map = new RequestMap();
			map.set("id", newAppId);
			map.set("name", newAppId);
			map.set("description", "");
			map.set("version", 0);
			map.set("definition.format", "proto3");
			map.set("definition.encoding", "base64");
			map.set("definition.messages",
					Base64.getEncoder().encodeToString(readResourceToString(protoPath).getBytes()));
			new App(map).update();
			System.out.println("Node updated");
			App app = App.read(newAppId);
			JSONObject definition = (JSONObject) app.get("definition");
			System.out.println("New Format: " + definition.get("format"));
			System.out.println("New Encoding: " + definition.get("encoding"));
			System.out.println("New Messages: " + definition.get("messages"));
		} catch (ApiException e) {
			System.err.println("API Exception " + e.getMessage());
		}
		return newAppId;
	}

	String createSettlementRequestBuffer(String from, String to, int amount, String currency, String description,
			int nonce) {
		SettleProtocolBuffer.Request.Builder builder = SettleProtocolBuffer.Request.newBuilder();
		SettleProtocolBuffer.Request protocolBuffer = builder.setFrom(from).setTo(to).setAmountMinorUnits(amount)
				.setCurrency(currency).setDescription(description).setNonce(nonce).build();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			protocolBuffer.writeTo(baos);
		} catch (IOException e) {
			System.out.println("IOException writing buffer " + e.getMessage());
		}
		return encode(baos.toByteArray(), ENCODING);
	}

	private void createSettlementRequest() {
		printHeading("Create Settlement Request");
		Random random = new Random();
		String from = captureInput("from", "GB111111111111");
		String to = captureInput("to", "IE222222222222");
		int amount = Integer.parseInt(captureInput("amount_minor_units", "1234"));
		String currency = captureInput("currency", "USD");
		String description = captureInput("description", "This is a test.");
		int nonce = random.nextInt();

		String req = createSettlementRequestBuffer(from, to, amount, currency, description, nonce);

		RequestMap map = new RequestMap();
		map.set("app", APP_ID);
		map.set("encoding", ENCODING);
		map.set("value", req);

		try {
			TransactionEntry response = TransactionEntry.create(map);
			System.out.println("Hash: " + response.get("hash").toString());
			System.out.println("Slot: " + response.get("slot").toString());
			System.out.println("Status: " + response.get("status").toString());
		} catch (ApiException e) {
			System.err.println(e.getMessage());
		}
		captureInput("(press return to continue)", null);
	}

	private void confirmSettlement() {
		RequestMap map = new RequestMap();
		String hash = captureInput("hash", null);
		map.set("encoding", ENCODING);
		map.set("hash", hash);

		try {
			Settle response = Settle.create(map);
			System.out.println("Encoding: " + response.get("encoding").toString());
			System.out.println("nPublic Key: " + response.get("public_key").toString());
			System.out.println("nSignature: " + response.get("signature").toString());
		} catch (ApiException e) {
			System.err.println(e.getMessage());
		}
		captureInput("(press return to continue)", null);
	}

	private void initApi(CommandLine cmd) throws FileNotFoundException {
		String keystorePath = captureInputFile("Keystore", cmd.getOptionValue("keystorePath", ""));
		String storePass = captureInput("Keystore Password", cmd.getOptionValue("storePass", "keystorepassword"));
		String consumerKey = captureInput("Consumer Key", cmd.getOptionValue("consumerKey", ""));
		String keyAlias = captureInput("Key Alias", cmd.getOptionValue("keyAlias", "keyalias"));

		ApiConfig.setAuthentication(
				new OAuthAuthentication(consumerKey, new FileInputStream(keystorePath), keyAlias, storePass));
		ApiConfig.setDebug(cmd.hasOption("verbosity"));
		ApiConfig.setSandbox(true);
	}

	private String captureInputFile(String question, String defaultAnswer) {
		boolean noFile = true;
		String keystorePath = null;
		while (noFile) {
			keystorePath = captureInput(question, defaultAnswer);
			keystorePath = keystorePath.replaceFirst("^~/", System.getProperty("user.home") + "/");
			if (Files.notExists(Paths.get(keystorePath))) {
				System.out.println("File Not Found");
			} else {
				noFile = false;
			}
		}
		return keystorePath;
	}

	private String captureInput(String question, String defaultAnswer) {
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		if (defaultAnswer == null) {
			System.out.print(question + ": ");
		} else {
			System.out.print(question + " [" + defaultAnswer + "]: ");
		}
		String s;
		try {
			s = br.readLine();
			if (s == null || "".equals(s)) {
				s = defaultAnswer;
			}
		} catch (IOException e) {
			s = defaultAnswer;
		}
		return s;
	}

	private void printHeading(String heading) {
		System.out.println("============ " + heading + " ============");
	}

	private static String readResourceToString(String path) {
		return new BufferedReader(new InputStreamReader(BarebonesSettlementApplication.class.getResourceAsStream(path)))
				.lines().collect(Collectors.joining("\n"));
	}

	public static String getAppIdFromProtoBuffer(String bufferFile) {
		String protoBuf = readResourceToString("/" + bufferFile);
		Pattern pattern = Pattern.compile("package\\s(.[A-Za-z0-9]+);", Pattern.MULTILINE);
		Matcher m = pattern.matcher(protoBuf);
		if (m.find()) {
			return m.group(1);
		}
		return "";
	}

	private String encode(byte[] bytes, String encoding) {
		if (encoding.equals("hex")) {
			return Hex.encodeHexString(bytes);
		} else {
			return Base64.getEncoder().encodeToString(bytes);
		}
	}

	private static Options createOptions() {
		Options options = new Options();

		options.addOption("ck", "consumerKey", true, "consumer key (mastercard developers)");
		options.addOption("kp", "keystorePath", true, "the path to your keystore (mastercard developers)");
		options.addOption("ka", "keyAlias", true, "key alias (mastercard developers)");
		options.addOption("sp", "storePass", true, "keystore password (mastercard developers)");
		options.addOption("v", "verbosity", false, "log mastercard developers sdk to console");

		return options;
	}

}
