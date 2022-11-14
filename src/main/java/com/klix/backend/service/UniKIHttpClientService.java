package com.klix.backend.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Base64;

import javax.imageio.ImageIO;

/**
 * HTTPClient für die Verbindung zum Uni-KI-Server. Die Klasse ist selbständig (wieder-) verwendbar
 * (ohne Start der gesamten Klix Applikation, Eingaben in die html-Seiten etc.), daher auch
 * verwendbar für statistische Tests etc.
 * 
 * @author Andreas
 * @since 09.06.2021
 *
 */
public class UniKIHttpClientService {
	
	private String authHeaderValue;
	private URI uri;
	
	public UniKIHttpClientService (String name, String password) throws URISyntaxException {
		
		this(new URI("https://klix.cogsci.uni-osnabrueck.de/service/"), name, password);
	}
	
	public UniKIHttpClientService (URI uri, String name, String password) {
		
		this.uri=uri;
		setAuth(name, password);
	}
	
	public HttpResponse<String> send (HttpRequest request) throws IOException,
	InterruptedException {

		HttpResponse<String> response=HttpClient.newHttpClient()
				.send(request, HttpResponse.BodyHandlers.ofString());
		return(response);
	}
	
	/**
	 * Get-Request.
	 * @return A "landing page", which is just a extremly simple HTML-Page that is send to you if you
	 * request a ``GET`` on the ``/``-route (Aus der Doku des Uni-servers)
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public HttpResponse<String> hello () throws IOException, InterruptedException {
		
		HttpRequest request=HttpRequest.newBuilder(uri)
				.header("Authorization", authHeaderValue)
				.GET().build();
		return(send(request));
	}
	
	/**
	 * Institution hinzufügen / updaten. Aus der Doku des Uni-Servers:
	 * /update_institution?institution=${INSTITUTION}`` This command will update an institution.
	 * The IDs of the institutions members are provided as a json-string in the data, which is
	 * simply a list containing the integers that resemble the ids of the persons.
	 * The list is considered an exhaustive list of members. Members that are not present in this 
	 * list will be removed from the institution.
	 * @param institution
	 * @param ids der Mitglieder
	 * @return Die Antwort des Servers (aktuell "{}", wenn kein Fehler vorliegt.)
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public HttpResponse<String> updateInstitution (int institution, int[] ids)
			throws IOException, InterruptedException {
		
		String jsonString=Arrays.toString(ids);
		HttpRequest request=HttpRequest
				.newBuilder(uri.resolve("update_institution?institution="+institution))
				.header("Authorization", authHeaderValue)
				.POST(HttpRequest.BodyPublishers.ofString(jsonString)).build();
		return(send(request));
	}
	
	/**
	 * Wandelt das Bild auf dem Pfad in einen Bytestream um und ruft
	 * {@link #uploadImage(int, int, byte[])} auf.
	 * @see {@link #uploadImage(int, int, byte[])}
	 * @param institution
	 * @param id
	 * @param pathString mittels 
	 * @return Die Antwort des Servers (aktuell "{}", wenn kein Fehler vorliegt.)
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public HttpResponse<String> uploadImage (int institution, int id, String pathString)
				throws IOException, InterruptedException {
		
		return uploadImage(institution, id, imageToByteArray(pathString));
	}
	
	/**
	 * Bild für ein konkretes Mitglied einer konkreten Institution an den Server schicken.
	 * Aus der Doku des Uni-Servers:
	 * ``/upload?institution=${INSTITUTION}&id=${ID}`` uploads an image that is provided as a 
	 * bytestream in the data of the POST-request. The image belongs to an identity that is part 
	 * of an institution.
	 * @param institution
	 * @param id des Mitglieds
	 * @param byteArray des Bildes
	 * @return Die Antwort des Servers (aktuell "{}", wenn kein Fehler vorliegt.)
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public HttpResponse<String> uploadImage (int institution, int id, byte[] byteArray)
				throws IOException, InterruptedException {
		
		HttpRequest request=HttpRequest
				.newBuilder(uri.resolve("upload?institution="+institution+"&id="+id))
				.header("Authorization", authHeaderValue)
				.POST(HttpRequest.BodyPublishers.ofByteArray(byteArray))
				.build();
		return(send(request));
	}
	
	/**
	 * Wandelt das Bild auf dem Pfad in einen Bytestream um und ruft
	 * {@link #identifyImage(int, byte[])} auf.
	 * @see {@link #identifyImage(int, byte[])}
	 * @param institution
	 * @param pathString
	 * @return Die Antwort des Servers
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public HttpResponse<String> identifyImage (int institution, String pathString)
			throws IOException, InterruptedException {
		
		return identifyImage(institution, imageToByteArray(pathString));
	}
	
	/**
	 * Versucht, Mitglieder der Institution anhand des Bildes zu identifizieren.
	 * Aus der Doku des Uni-Servers: 
	 * ``/identify?institution=${INSTITUTION}`` this command must contain also a image-file as a 
	 * bytestream. POST will yield the 5 nearest neighbour images to the query image alongside 
	 * their confidences.
	 * @param institution
	 * @param byteArray des Bildes
	 * @return Die Antwort des Servers. Maximal 5 beste Ergebnisse mit deren "Wahrscheinlichkeiten"
	 * im JSON-Format, sofern kein Fehler vorliegt. 
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public HttpResponse<String> identifyImage (int institution, byte[] byteArray)
			throws IOException, InterruptedException {
	
		HttpRequest request=HttpRequest
				.newBuilder(uri.resolve("identify?institution="+institution))
				.header("Authorization", authHeaderValue)
				.POST(HttpRequest.BodyPublishers.ofByteArray(byteArray))
				.build();
		return(send(request));
	}
	
	public URI getUri() {
		return uri;
	}

	public void setUri(URI uri) {
		this.uri = uri;
	}

	public void setAuth(String name, String password) {
		String auth = name + ":" + password;
		byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
		authHeaderValue = "Basic " + new String(encodedAuth);
	}
	
	/**
	 * Simple Methode zur Ausgabe der Antwort vom Uni-Server als String.
	 * 
	 */
	public static String stringOf (HttpResponse<String> response) {
		
		StringBuffer buffy=new StringBuffer("Status Code: "+response.statusCode());
		buffy.append("\nHttp-Version: "+response.version()+"\nHeaders:");
		response.headers().map().forEach((key,value) -> buffy.append("\n\t"+key+": "+value));
		buffy.append("\nBody:\n"+response.body());
		return buffy.toString();
	}
	
	/**
	 * Umwandlung des Bildes auf dem Dateipfad in ein Byte Array.
	 * @param pathString
	 * @return
	 * @throws IOException
	 */
	private byte[] imageToByteArray (String pathString) throws IOException {
		
		Path localFile = Paths.get(pathString);
		if (!Files.exists(localFile)) throw new IOException("File "+localFile+" existiert nicht.");
		BufferedImage bImage = ImageIO.read(localFile.toFile());
		if (bImage==null) throw new IOException("Image ist Null.");
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ImageIO.write(bImage, "jpg", bos);
		return bos.toByteArray();
	}
}