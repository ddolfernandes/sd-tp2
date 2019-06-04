package microgram.impl.dropbox;

import static microgram.api.java.Result.error;
import static microgram.api.java.Result.ErrorCode.CONFLICT;
import static microgram.api.java.Result.ErrorCode.INTERNAL_ERROR;
import static microgram.api.java.Result.ErrorCode.NOT_FOUND;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.pac4j.scribe.builder.api.DropboxApi20;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.sun.net.httpserver.HttpServer;

import microgram.impl.dropbox.msgs.*;
import microgram.api.java.Media;
import microgram.api.java.Result;
import utils.Hash;
import utils.JSON;

/**
 * Client for DropBox, using REST API. It allows to access the DropBox of a
 * user.
 * 
 * Documentation for the Dropbox API can be found at:
 * 
 * https://www.dropbox.com/developers/documentation/http/documentation
 * 
 * For this code to work, there should be an app created at DropBox. You can
 * create your own app from this URL:
 * 
 * https://www.dropbox.com/developers/apps
 * 
 */
public class DropboxMedia implements Media // este cliente vai servir para falar com a dropbox em vez de utilizar o
											// media storage
{
	private static final String apiKey = "to03dk6r62n1qdh";
	private static final String apiSecret = "omumsnhg2badwrn";
	private static final String accessTokenStr = "GYu1ymPIR3AAAAAAAAAADP6g-H6QqQPa-RS7__N_tKyWiLc5mRaMHcH8OF72BXu7";

	protected static final String JSON_CONTENT_TYPE = "application/json; charset=utf-8";
	protected static final String OCTETSTREAM_CONTENT_TYPE = "application/octet-stream";

	private static final String CREATE_FOLDER_V2_URL = "https://api.dropboxapi.com/2/files/create_folder_v2";
	private static final String LIST_FOLDER_V2_URL = "https://api.dropboxapi.com/2/files/list_folder";
	private static final String LIST_FOLDER_CONTINUE_V2_URL = "https://api.dropboxapi.com/2/files/list_folder/continue";
	private static final String CREATE_FILE_V2_URL = "https://content.dropboxapi.com/2/files/upload";
	private static final String DELETE_FILE_V2_URL = "https://api.dropboxapi.com/2/files/delete_v2";
	private static final String DOWNLOAD_FILE_V2_URL = "https://content.dropboxapi.com/2/files/download";
	private static final String GET_TEMPORARY_LINK_FILE_V2_URL = "https://api.dropboxapi.com/2/files/get_temporary_link";

	private static final String DROPBOX_API_ARG = "Dropbox-API-Arg";

	protected OAuth20Service service; // o nosso ponto de contacto com o servico que vamos usar
	protected OAuth2AccessToken accessToken;

	/**
	 * Creates a dropbox client, given the access token.
	 * 
	 * @param accessTokenStr String with the previously obtained access token.
	 * @throws Exception Throws exception if something failed.
	 */
	public static DropboxMedia createClientWithAccessToken() throws Exception {
		try {
			OAuth20Service service = new ServiceBuilder(apiKey).apiSecret(apiSecret).build(DropboxApi20.INSTANCE);
			OAuth2AccessToken accessToken = new OAuth2AccessToken(accessTokenStr);

			System.err.println(accessToken.getAccessToken());
			System.err.println(accessToken.toString());
			return new DropboxMedia(service, accessToken);

		} catch (Exception x) {
			x.printStackTrace();
			throw new Exception(x);
		}
	}

	/**
	 * Creates a dropbox client, given the access token.
	 * 
	 * @param accessTokenStr String with the previously obtained access token.
	 * @throws Exception Throws exception if something failed.
	 */
	public static DropboxMedia createClientWithAccessToken(String accessTokenStr) throws Exception {
		try {
			OAuth20Service service = new ServiceBuilder(apiKey).apiSecret(apiSecret).build(DropboxApi20.INSTANCE);
			OAuth2AccessToken accessToken = new OAuth2AccessToken(accessTokenStr);

			System.err.println(accessToken.getAccessToken());
			System.err.println(accessToken.toString());
			return new DropboxMedia(service, accessToken);

		} catch (Exception x) {
			x.printStackTrace();
			throw new Exception(x);
		}
	}

	/**
	 * Creates a dropbox client, given a file containing an access token.
	 * 
	 * @param accessTokenFile File containing the previously obtained access token.
	 * @throws Exception Throws exception if something failed.
	 */
	public static DropboxMedia createClientWithAccessTokenFile(File accessTokenFile) throws Exception {
		try {
			String accessTokenStr = new String(Files.readAllBytes(accessTokenFile.toPath()), StandardCharsets.UTF_8);
			return createClientWithAccessToken(accessTokenStr);
		} catch (Exception x) {
			x.printStackTrace();
			throw new Exception(x);
		}
	}

	protected DropboxMedia(OAuth20Service service, OAuth2AccessToken accessToken) {
		this.service = service;
		this.accessToken = accessToken;
	}

	/**
	 * Create a directory in dropbox.
	 * 
	 * @param path Path for the directory to create.
	 * @return Returns a Result object.
	 */
	public Result<Void> createDirectory(String path) {
		try {
			OAuthRequest createFolder = new OAuthRequest(Verb.POST, CREATE_FOLDER_V2_URL);
			createFolder.addHeader("Content-Type", JSON_CONTENT_TYPE);

			createFolder.setPayload(JSON.encode(new CreateFolderV2Args(path, false)));

			service.signRequest(accessToken, createFolder);
			Response r = service.execute(createFolder);

			if (r.getCode() == 409) {
				System.err.println("Dropbox directory already exists");
				return Result.error(Result.ErrorCode.CONFLICT);
			} else if (r.getCode() == 200) {
				System.err.println("Dropbox directory was created with success");
				return Result.ok();
			} else {
				System.err.println("Unexpected error HTTP: " + r.getCode());
				return Result.error(Result.ErrorCode.INTERNAL_ERROR);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return Result.error(Result.ErrorCode.INTERNAL_ERROR);
		}
	}

	/**
	 * Lists diretory at Dropbox.
	 * 
	 * @param path Dropbix path to list.
	 * @return Resturns a list of string with the contents of the directory.
	 */
	public Result<List<String>> listDirectory(String path) {
		try {
			List<String> list = new ArrayList<String>();
			OAuthRequest listFolder = new OAuthRequest(Verb.POST, LIST_FOLDER_V2_URL);
			listFolder.addHeader("Content-Type", JSON_CONTENT_TYPE);
			listFolder.setPayload(JSON.encode(new ListFolderV2Args(path, true)));

			for (;;) {
				service.signRequest(accessToken, listFolder);
				Response r = service.execute(listFolder);
				if (r.getCode() != 200) {
					System.err.println("Failed list directory: " + path + " : " + r.getMessage());
					return Result.error(Result.ErrorCode.INTERNAL_ERROR);
				}

				ListFolderV2Return result = JSON.decode(r.getBody(), ListFolderV2Return.class);
				result.getEntries().forEach(entry -> {
					list.add(entry.toString());
					System.out.println(entry);
				});

				if (result.has_more()) {
					System.err.println("continuing...");
					listFolder = new OAuthRequest(Verb.POST, LIST_FOLDER_CONTINUE_V2_URL);
					listFolder.addHeader("Content-Type", JSON_CONTENT_TYPE);
					listFolder.setPayload(JSON.encode(new ListFolderContinueV2Args(result.getCursor())));
				} else
					break;
			}
			return Result.ok(list);
		} catch (Exception e) {
			e.printStackTrace();
			return Result.error(Result.ErrorCode.INTERNAL_ERROR);
		}
	}

	/**
	 * Write the contents of file name.
	 * https://www.dropbox.com/developers/documentation/http/documentation#files-upload
	 * 
	 * @param filename File name.
	 * @param bytes    Contents of the file.
	 */
	public Result<String> upload(byte[] bytes) {

		try {

			String filename = Hash.of(bytes);

			OAuthRequest upload = new OAuthRequest(Verb.POST, CREATE_FILE_V2_URL);

			upload.addHeader("Content-Type", OCTETSTREAM_CONTENT_TYPE);

			// o dropbox_ap_arg vai em json, codificado num objeto do tipo json (tem que ser
			// dinamico)
			upload.addHeader(DROPBOX_API_ARG, JSON.encode(new CreateFileV2Args(filename)));

			upload.setPayload(bytes);

			service.signRequest(accessToken, upload);
			Response r = service.execute(upload);

			if (r.getCode() == 409) {

				System.out.println("Dropbox file already exists");
				return Result.error(Result.ErrorCode.CONFLICT);

			} else if (r.getCode() == 200) {
				System.out.println("file uploaded");
				return Result.ok(filename);

			} else {

				System.err.println("Unexpected error HTTP: " + r.getCode());
				System.err.println(r.getBody());
				return Result.error(Result.ErrorCode.INTERNAL_ERROR);

			}

		} catch (Exception e) {
			e.printStackTrace();
			return Result.error(Result.ErrorCode.INTERNAL_ERROR);

		}
	}

	/**
	 * Reads the contents of file name.
	 * https://www.dropbox.com/developers/documentation/http/documentation#files-download
	 * https://www.dropbox.com/developers/documentation/http/documentation#files-get_temporary_link
	 * 
	 * @param filename File name.
	 * @return Returns the file contents.
	 */
	public Result<byte[]> download(String filename) { // ACABAR---------

		try {
			OAuthRequest downloadFile = new OAuthRequest(Verb.POST, DOWNLOAD_FILE_V2_URL);

			downloadFile.addHeader("Content-Type", OCTETSTREAM_CONTENT_TYPE); // na documentacao nao aparece, mas e um
																				// erro, devia
			downloadFile.addHeader(DROPBOX_API_ARG, JSON.encode(new AccessFileV2Args(filename)));

			service.signRequest(accessToken, downloadFile);
			Response r = service.execute(downloadFile);

			if (r.getCode() == 404) {
				return Result.error(Result.ErrorCode.NOT_FOUND);
			}

			if (r.getCode() != 200) {

				// vai servir para ler o ficheiro aos bocados para ver se fez bem o download
				// porque o buffer da stream nao e suficientemente grande para ver tudo duma vez
				// InputStream in = r.getStream();

				System.out.println(r.getBody());
				System.out.println(Result.ErrorCode.INTERNAL_ERROR);
				return Result.error(Result.ErrorCode.INTERNAL_ERROR);

			}

			int filesize = Integer.parseInt(r.getHeader("Content-Length"));
			byte[] file = new byte[filesize];

			int read = 0;
			InputStream readBytes = r.getStream();
			System.out.println(filesize);

			while (read < filesize) {
				read += readBytes.read(file, read, filesize);
				filesize = filesize - read;
				System.out.println(read);

			}
			System.out.println("file downloaded " + file.length);
			return Result.ok(file);

		} catch (Exception e) {
			e.printStackTrace();
			return Result.error(Result.ErrorCode.INTERNAL_ERROR);
		}
	}

	/**
	 * Deletes the file name.
	 * https://www.dropbox.com/developers/documentation/http/documentation#files-delete
	 * 
	 * @param filename File name.
	 */
	public Result<Void> delete(String filename) {

		try {
			OAuthRequest deleteFile = new OAuthRequest(Verb.POST, DELETE_FILE_V2_URL);

			deleteFile.addHeader("Content-Type", JSON_CONTENT_TYPE); // na documentacao nao aparece, mas e um erro,
																		// devia
			deleteFile.setPayload(JSON.encode(new AccessFileV2Args(filename)));

			service.signRequest(accessToken, deleteFile);
			Response r = service.execute(deleteFile);

			if (r.getCode() == 404) {

				System.out.println("Dropbox file does not exists");
				return Result.error(Result.ErrorCode.NOT_FOUND);

			} else if (r.getCode() == 200) {
				System.out.println("file delete");
				return Result.ok();

			} else {

				System.err.println("Unexpected error HTTP: " + r.getCode());
				System.err.println(r.getBody());
				return Result.error(Result.ErrorCode.INTERNAL_ERROR);

			}

		} catch (Exception e) {
			e.printStackTrace();
			return Result.error(Result.ErrorCode.INTERNAL_ERROR);
		}

		// return Result.error(Result.ErrorCode.NOT_IMPLEMENTED);
	}
}