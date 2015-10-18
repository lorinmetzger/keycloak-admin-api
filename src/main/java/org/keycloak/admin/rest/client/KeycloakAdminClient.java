package org.keycloak.admin.rest.client;

import static us.monoid.web.Resty.*;

import us.monoid.web.Resty.*;
import us.monoid.web.Resty;
import us.monoid.web.Content;
import us.monoid.web.JSONResource;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;

import java.io.OutputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import org.keycloak.OAuth2Constants;
import org.keycloak.constants.ServiceUrlConstants;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.util.HostUtils;
import org.keycloak.util.JsonSerialization;
import org.keycloak.util.KeycloakUriBuilder;
import org.keycloak.util.UriUtils;

import java.net.URI;
import java.net.HttpURLConnection;
import java.net.URLConnection;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.type.TypeFactory;
/*
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.type.TypeFactory;
*/

public class KeycloakAdminClient 
{

  private String baseUrl;

  private AccessTokenResponse token;

  private ObjectMapper mapper;

  private String username;

  private String password;

  private String realm;

  private String realmClient;

  public KeycloakAdminClient(String url, String username, String password, String realm, String client)
  {
    this.baseUrl = url;
    this.mapper = new ObjectMapper();
    this.username = username;
    this.password = password;
    this.realm = realm;
    this.realmClient = client;
  }

  public String getUrl(String base)
  {
    return (baseUrl + base);
  }

  public Resty getResty()
  {
    Resty resty = new Resty(new Opts());
    return resty;
  }

  public String getContent(HttpEntity entity) throws IOException 
  {
    if (entity == null) return null;
    InputStream is = entity.getContent();
    try 
    {
      ByteArrayOutputStream os = new ByteArrayOutputStream();
      int c;
      while ((c = is.read()) != -1) 
      {
        os.write(c);
      }
      byte[] bytes = os.toByteArray();
      String data = new String(bytes);
      return data;
    } 
    finally 
    {
      try 
      {
        is.close();
      }
      catch (IOException ignored) 
      {
      }
    }
  }

  public void delete(String url) throws IOException, JSONException
  {
    getResty().json(getUrl(url), Resty.delete());
  }
  
  public <T> void delete(Class<T> type, T data, String url) throws IOException, JSONException
  {
    String json = mapper.writer().writeValueAsString(data);
    getResty().json(getUrl(url), new DeleteContent("application/json", json.getBytes("UTF-8")));
  }

  public <T> T create(Class<T> type, T data, String url) throws IOException, JSONException
  {
    T result = null;
    String json = mapper.writer().writeValueAsString(data);
    JSONResource res = getResty().json(getUrl(url), new Content("application/json", json.getBytes("UTF-8")));
    if( res != null )
    {
      //json = res.object().toString();
      //System.out.println("Response content: " + json);
      //result = mapper.readValue(json, type);
    }
    return result;
  }

  public <T> void put(Class<T> type, T data, String url) throws IOException, JSONException
  {
    String json = mapper.writer().writeValueAsString(data);
    JSONResource res = getResty().json(getUrl(url), 
        getResty().put(new Content("application/json", json.getBytes("UTF-8"))));
  }

  public <T> T getObject(Class<T> type, String url) throws IOException, JSONException
  {
    T result = null;
    JSONResource res = getResty().json(getUrl(url));
    if( res != null )
    {
      String json = res.object().toString();
      //result = JsonSerialization.readValue(json, type);
      result = mapper.readValue(json, type);
    }
    return result;
  }

  public <T> List<T> getAll(Class<T> type, String url) throws IOException, JSONException
  {
    List<T> result = new ArrayList();
    JSONResource res = getResty().json(getUrl(url));
    if( res != null )
    {
      String sjson = res.array().toString();
      result = mapper.readValue(sjson, TypeFactory.defaultInstance().constructCollectionType(List.class, type));
    }
    return result;
  }

  public void logout() throws IOException 
  {
    HttpClient client = new DefaultHttpClient();
    try 
    {
      HttpPost post = new HttpPost(KeycloakUriBuilder.fromUri(baseUrl + "/auth")
          .path(ServiceUrlConstants.TOKEN_SERVICE_LOGOUT_PATH)
          .build(realm));
      List<NameValuePair> formparams = new ArrayList<NameValuePair>();
      formparams.add(new BasicNameValuePair(OAuth2Constants.REFRESH_TOKEN, token.getRefreshToken()));
      formparams.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ID, realmClient));
      UrlEncodedFormEntity form = new UrlEncodedFormEntity(formparams, "UTF-8");
      post.setEntity(form);
      HttpResponse response = client.execute(post);
      boolean status = response.getStatusLine().getStatusCode() != 204;
      HttpEntity entity = response.getEntity();
      if (entity == null) {
        return;
      }
      InputStream is = entity.getContent();
      if (is != null) is.close();
      if (status) {
        throw new RuntimeException("failed to logout");
      }
    } finally {
        client.getConnectionManager().shutdown();
    }
  }

  public AccessTokenResponse login() throws IOException 
  {
    token = null;
    HttpClient client = new DefaultHttpClient();
    try 
    {
      URI uri =  KeycloakUriBuilder.fromUri((baseUrl + "/auth"))
          .path(ServiceUrlConstants.TOKEN_PATH).build(realm);
      HttpPost post = new HttpPost(uri);
      List <NameValuePair> formparams = new ArrayList <NameValuePair>();
      formparams.add(new BasicNameValuePair("username", username));
      formparams.add(new BasicNameValuePair("password", password));
      formparams.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, "password"));
      formparams.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ID, realmClient));
      UrlEncodedFormEntity form = new UrlEncodedFormEntity(formparams, "UTF-8");
      post.setEntity(form);
      HttpResponse response = client.execute(post);
      int status = response.getStatusLine().getStatusCode();
      HttpEntity entity = response.getEntity();
      if (status != 200) 
      {
        String json = getContent(entity);
        throw new IOException("Bad status: " + status + " response: " + json);
      }
      if (entity == null) 
      {
        throw new IOException("No Entity");
      }
      String json = getContent(entity);
      //token = JsonSerialization.readValue(json, AccessTokenResponse.class);
      token = mapper.readValue(json, AccessTokenResponse.class);
    } 
    finally 
    {
      client.getConnectionManager().shutdown();
    }
    return token;
  }

  public void deleteRealmRole(String realm, String role) throws Exception
  {
    delete(String.format("/auth/admin/realms/%s/roles/%s", realm, role));
  }

  public void createRealmRole(String realm, RoleRepresentation role) throws Exception 
  {
    create(RoleRepresentation.class, role, 
        String.format("/auth/admin/realms/%s/roles", realm));
  }

  public List<RoleRepresentation> getRealmRoles(String realm) throws Exception
  {
    return getAll(RoleRepresentation.class, 
        String.format("/auth/admin/realms/%s/roles", realm) );
  }

  public void createRealmClient(String realm, ClientRepresentation client) throws Exception
  {
    create(ClientRepresentation.class, client, 
        String.format("/auth/admin/realms/%s/clients", realm));
  }
  
  public void deleteRealmClient(String realm, String client) throws Exception
  {
    delete(String.format("/auth/admin/realms/%s/clients/%s", realm, client));
  }

  public List<ClientRepresentation> getRealmClients(String realm) throws Exception
  {
    return getAll(ClientRepresentation.class, 
        String.format("/auth/admin/realms/%s/clients", realm));
  }

  public ClientRepresentation getRealmClient(String realm, String id) throws Exception
  {
    return getObject(ClientRepresentation.class,
        String.format("/auth/admin/realms/%s/clients/%s", realm, id));
  }

  public void createRealm(RealmRepresentation realm) throws Exception
  {
    create(RealmRepresentation.class, realm,
        String.format("/auth/admin/realms"));
  }

  public List<RealmRepresentation> getRealms() throws Exception
  {
    return getAll(RealmRepresentation.class, "/auth/admin/realms");
  }

  public RealmRepresentation getRealm(String name) throws Exception
  {
    return getObject(RealmRepresentation.class, 
        String.format("/auth/admin/realms/%s", name));
  }
 
  public void deleteRealm(String realm) throws Exception
  {
    delete(String.format("/auth/admin/realms/%s", realm));
  }

  public UserRepresentation getRealmUser(String realm, String userId) throws Exception
  {
    return getObject(UserRepresentation.class,
        String.format("/auth/admin/realms/%s/users/%s", realm, userId));
  }

  public List<UserRepresentation> getRealmUsers(String realm, String query) throws Exception
  {
    return getAll(UserRepresentation.class, String.format(
        "/auth/admin/realms/%s/users%s", realm, query));
  }

  public void createRealmUser(String realm, UserRepresentation user) throws Exception
  {
    create(UserRepresentation.class, user,
        String.format("/auth/admin/realms/%s/users", realm));
  }

  public void updateRealmUser(String realm, UserRepresentation user) throws Exception
  {
    put(UserRepresentation.class, user, 
        String.format("/auth/admin/realms/%s/users/%s", realm, user.getId()));
  }

  public void deleteRealmUser(String realm, String userId) throws Exception
  {
    delete(String.format("/auth/admin/realms/%s/users/%s", realm, userId));
  }

  public List<RoleRepresentation> getAvailableRealmRolesForUser(String realm, String userId) throws Exception
  {
    return getAll(RoleRepresentation.class, String.format(
        "/auth/admin/realms/%s/users/%s/role-mappings/realm/available", realm, userId));
  }

  public List<RoleRepresentation> getRealmRolesForUser(String realm, String userId) throws Exception
  {
    return getAll(RoleRepresentation.class, String.format(
        "/auth/admin/realms/%s/users/%s/role-mappings/realm", realm, userId));
  }

  public void addRealmRolesToUser(String realm, String userId, List<RoleRepresentation> roles) throws Exception
  {
    create(List.class, roles, String.format(
        "/auth/admin/realms/%s/users/%s/role-mappings/realm", realm, userId));
  }

  public void deleteRealmRolesForUser(String realm, String userId, List<RoleRepresentation> roles) throws Exception
  {
    delete(List.class, roles, String.format(
        "/auth/admin/realms/%s/users/%s/role-mappings/realm", realm, userId));
  }

  public void resetPassword(String realm, String user, CredentialRepresentation creds) throws Exception
  {
    put(CredentialRepresentation.class, creds,
        String.format("/auth/admin/realms/%s/users/%s/reset-password", realm, user));
  }

  private class Opts extends Resty.Option
  {
    public void apply(URLConnection uConnection)
    {
      //System.out.println("Adding header: " + token.getToken());
      uConnection.setRequestProperty("Authorization", "Bearer " + token.getToken());
    }
  }

  public static void main(String[] args)
  {
  }

  private class DeleteContent extends Content
  {
    public DeleteContent(String mime, byte[] data)
    {
      super(mime, data);
    }

    @Override 
    public void addContent(URLConnection con) throws IOException
    {
      con.setDoOutput(true);
      ((HttpURLConnection)con).setRequestMethod("DELETE");
      con.addRequestProperty("Content-Type", mime);
      con.addRequestProperty("Content-Length", String.valueOf(content.length));
      OutputStream os = con.getOutputStream();
      writeContent(os);
      os.close();
      //super.addContent(con);
    }
  }
}
