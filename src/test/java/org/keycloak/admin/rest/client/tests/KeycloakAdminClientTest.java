package org.keycloak.admin.rest.client.tests;

import org.keycloak.admin.rest.client.*;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.util.List;
import java.util.Map;

public class KeycloakAdminClientTest 
{

  private KeycloakAdminClient client;

  @Before
  public void setUp() throws Exception
  {
    client = new KeycloakAdminClient("http://localhost:8080");
    //System.out.println("Test were run..." + 
    client.login("admin", "admin").getToken();
  }

  @Test
  public void testGetRoles() throws Exception
  {
    List<RoleRepresentation> roles = client.getRealmRoles("master");
    for(RoleRepresentation role : roles)
    {
      System.out.println("Id: " + role.getId() + " Name: " + role.getName());
    }
  }

  @Test
  public void testGetRealms() throws Exception 
  {
    List<RealmRepresentation> realms = client.getRealms();
    for(RealmRepresentation realm : realms)
    {
      System.out.println("Id: " + realm.getId() + " Name: " + realm.getRealm());
    }
  } 

  @Test
  public void testGetRealm() throws Exception
  {
    RealmRepresentation realm = client.getRealm("master");
    System.out.println("Id: " + realm.getId() + " Name: " + realm.getRealm());
  }

  @Test
  public void testCreateRole() throws Exception 
  {
    System.out.println("\n\n\n\n\n\n\n CREATE REALM ROLE:\n");
    RoleRepresentation role = new RoleRepresentation();
    role.setName("driver");
    role.setDescription("Give access to driver UI.");
    //role.setScopeParamRequired(Boolean.FALSE);
    client.createRealmRole( "master", role );
    
    client.deleteRealmRole("master", "driver");
  } 

  @Test
  public void testGetUsers() throws Exception
  {
    List<UserRepresentation> users = client.getUsers("master", "");
    for(UserRepresentation user : users)
    {
      System.out.println("Found user: " + user.getUsername());
    }
  }
}
