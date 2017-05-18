package org.matthiaszimmermann.web3j.demo;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Map.Entry;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.matthiaszimmermann.web3j.util.Web3jConstants;
import org.matthiaszimmermann.web3j.util.Web3jUtils;
import org.web3j.codegen.SolidityFunctionWrapperGenerator;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.methods.response.EthCompileSolidity;
import org.web3j.protocol.core.methods.response.EthGetCompilers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class CompileDemo extends AbstractDemo {

	static final String CONTRACT = "Greeter";
	static final String BASE_PACKAGE = "org.matthiaszimmermann.web3j.demo.contract";

	static final String FOLDER_BASE = System.getProperty("user.home") + "/Desktop/private/github/web3j_demo";
	static final String FOLDER_SOURCE = FOLDER_BASE + "/src/main/resources";
	static final String FOLDER_TARGET = FOLDER_BASE + "/src/main/java";

	public static void main(String args []) throws Exception {
		new CompileDemo(args).run();
	}

	public CompileDemo(String [] args) {
		super(args);
	}

	@Override
	public void run() throws Exception {
		super.run();

		// get and print installed compilers
		EthGetCompilers compilers = web3j
				.ethGetCompilers()
				.sendAsync()
				.get();

		System.out.println("Available compilers:");
		for(String compiler: compilers.getResult()) {
			System.out.println("- " + compiler);
		}
		System.out.println();

		// compile solidity code
		String sourceFile = String.format("%s/%s.%s", FOLDER_SOURCE, CONTRACT, Web3jConstants.EXT_SOLIDITY);
		String sourceCode = Web3jUtils.readSolidityFile(sourceFile);
		compileSolidity(sourceCode, CONTRACT, FOLDER_SOURCE);

		// generate java wrapper class
		String binaryFile = getBinaryFileName(CONTRACT, FOLDER_SOURCE);
		String abiFile = getAbiFileName(CONTRACT, FOLDER_SOURCE);
		String [] cmdLine = {binaryFile, abiFile, "-p", BASE_PACKAGE, "-o", FOLDER_TARGET};

		System.out.printf("Running SolidityFunctionWrapperGenerator " + String.join(" ", cmdLine) + " ... ");
		SolidityFunctionWrapperGenerator.main(cmdLine);
	}

	/**
	 * Compiles solidity source code using web3j.
	 * Currently not working for testrpc see {@link https://github.com/web3j/web3j/issues/53}.
	 */
	public void compileNotWorkding(String source) {
		Request<?, EthCompileSolidity> result = web3j.ethCompileSolidity(source);
		System.out.println(result.toString());
	}

	/**
	 * Compiles solidity source code using JSON-RPC API directly.
	 */
	private JsonObject compileSolidity(String source, String contractName, String path) throws Exception {
		String compileCommandTemplate = "{\"jsonrpc\":\"2.0\",\"method\":\"eth_compileSolidity\",\"params\":[\"%s\"],\"id\":1}";
		String compileCommand = String.format(compileCommandTemplate, source);

		System.out.println("Compile command " + compileCommand);
		System.out.printf("Sending compile request to " + clientUrl + " ... ");

		StringEntity requestEntity = new StringEntity(compileCommand, ContentType.create("text/plain").withCharset(Charset.forName("UTF-8")));
		HttpUriRequest request = RequestBuilder
				.post(clientUrl)
				.setEntity(requestEntity)
				.build();

		ResponseHandler<JsonObject> rh = new ResponseHandler<JsonObject>() {

			@Override
			public JsonObject handleResponse(final HttpResponse response) throws IOException {
				StatusLine statusLine = response.getStatusLine();
				HttpEntity entity = response.getEntity();

				if (statusLine.getStatusCode() >= 300) {
					throw new HttpResponseException(
							statusLine.getStatusCode(),
							statusLine.getReasonPhrase());
				}

				if (entity == null) {
					throw new ClientProtocolException("Response contains no content");
				}

				Gson gson = new GsonBuilder().create();
				Reader reader = new InputStreamReader(entity.getContent(), Charset.forName("UTF-8"));
				return gson.fromJson(reader, JsonObject.class);
			}
		};

		JsonObject response = HttpClients.createDefault().execute(request, rh);
		System.out.println(" done");
		checkForErrors(response);
		checkForResult(response, contractName, path);

		return response;
	}

	private void checkForErrors(JsonObject response) {
		String ERROR = "error";
		if(response.has(ERROR)) {
			JsonObject error = response.get(ERROR).getAsJsonObject();
			System.out.println("Error code: " + error.get("code").toString());
			System.out.println("Error message:\n" + error.get("message").getAsString());
		}
	}

	private void checkForResult(JsonObject response, String contractName, String path) throws Exception {
		String RESULT = "result";
		if(response.has(RESULT)) {
			printJsonElement("JSON-RPC response\n", response, "", contractName, path);
		}
	}

	/**
	 * prints content to console
	 * for abiDefinition and code elements the value is written to the file system.
	 * TODO extract file writing ops to separate method which are called from checkForResult --> processResult 
	 */
	private void printJsonElement(String id, JsonElement e, String ident, String contractName, String path)
			throws Exception
	{
		if(id.equals("abiDefinition:") || id.equals("code:")) {
			File file = null;
			String value = e.toString();
			verifyValue(id, value);

			if(id.equals("abiDefinition:")) {
				file = new File(getAbiFileName(contractName, path));
			}
			else {
				// strip initial '"'
				if(value.startsWith("\"")) {
					value = value.substring(1);
				}

				// strip tailing '"'
				if(value.endsWith("\"")) {
					value = value.substring(0, value.length() - 1);
				}

				file = new File(getBinaryFileName(contractName, path));
			}

			// System.out.printf("-- writing " + id + "value output to '" + file.getAbsolutePath() + "' ... ");

			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write(value);
			bw.close();

			if(value.length() >= 3000) {
				System.out.println(ident + id + value.substring(0,  3000) + " ...");
			}
			else {
				System.out.println(ident + id + value);
			}
		}
		else if(e.isJsonPrimitive()) {
			String value = e.getAsString();
			value = verifyValue(id, value);
			System.out.println(ident + id + value);
		}
		else if(e.isJsonObject()) {
			System.out.println(ident + id + " {");
			JsonObject o = (JsonObject)e;
			for(Entry<String, JsonElement> entry: o.entrySet()) {
				String key = entry.getKey();
				printJsonElement(key + ":", entry.getValue(), ident + "  ", contractName, path);
			}
			System.out.println(ident + "}");
		}
		else if(e.isJsonArray()) {
			System.out.println(ident + id + " [");
			JsonArray a= (JsonArray)e;
			a.forEach(child -> {
				try {
					printJsonElement("", child, ident + "  ", contractName, path);
				} 
				catch (Exception e1) {
					e1.printStackTrace();
				}
			});
			System.out.println(ident + "}");
		}
		else {
			System.out.println(ident + id + "<!!!> " + e.toString());
		}
	}

	private String getAbiFileName(String contractName, String path) {
		return String.format("%s\\%s.%s", path, contractName, Web3jConstants.EXT_ABI);
	}

	private String getBinaryFileName(String contractName, String path) {
		return String.format("%s\\%s.%s", path, contractName, Web3jConstants.EXT_BINARY);
	}

	private String verifyValue(String id, String s) {
		if(s.startsWith("0x")) {
			String allowedChars = "0123456789abcdefg";
			int unexpectedChars = 0;
			for(int i = 2; i < s.length(); i++) {
				if(allowedChars.indexOf(s.charAt(i)) < 0) {
					unexpectedChars++;
				}
			}

			if(unexpectedChars != 0) {
				System.out.println("// " + unexpectedChars + " unexpected chars found");
			}
		}

		if(id.equals("code:") || id.equals("abiDefinition:")) {
			try {
				File tmp = null;

				if(id.equals("code:")) { tmp = File.createTempFile("contract", ".bin");}
				else                   { tmp = File.createTempFile("contract", ".abi");}

				BufferedWriter bw = new BufferedWriter(new FileWriter(tmp));
				bw.write(s);
				bw.close();

				// System.out.println("// wrote content of '" + id + "' to file " + tmp.getAbsolutePath());
			} 
			catch (IOException e) {
				e.printStackTrace();
			}
		}

		return s.length() <= 3000 ? s : s.substring(0, 3000); 
	}
}
