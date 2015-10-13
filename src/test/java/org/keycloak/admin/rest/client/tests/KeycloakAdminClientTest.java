package org.keycloak.admin.rest.client.tests;

import org.keycloak.admin.rest.client.*;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.util.List;
import java.util.ArrayList;
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
    RoleRepresentation role = new RoleRepresentation();
    role.setName("driver");
    role.setDescription("Give access to driver UI.");
    //role.setScopeParamRequired(Boolean.FALSE);
    client.createRealmRole( "trucklogger", role );
    
    //client.deleteRealmRole("master", "driver");
  } 

  @Test
  public void testGetUsers() throws Exception
  {
    List<UserRepresentation> users = client.getRealmUsers("master", "");
    for(UserRepresentation user : users)
    {
      System.out.println("Found user: " + user.getUsername());
    }
  }

  @Test
  public void createRealm() throws Exception
  {
    List<RealmRepresentation> realms = client.getRealms();
    boolean found = false;
    for( RealmRepresentation realm : realms )
    {
      if( realm.getRealm().equals("trucklogger") )
      {
        found = true;
      }
    }
    if( !found )
    {
      RealmRepresentation realm = new RealmRepresentation();
      realm.setRealm("trucklogger");
      client.createRealm( realm );
    }
  }

  @Test 
  public void createClient() throws Exception
  {
    List<ClientRepresentation> clients = client.getRealmClients("trucklogger");
    boolean found = false;
    for( ClientRepresentation cl : clients )
    {
      if( cl.getClientId().equals("trucklogger-app") )
      {
        found = true;
      }
    }
    if( !found )
    {
      ClientRepresentation cl = new ClientRepresentation();
      cl.setClientId("trucklogger-app");
      cl.setName("trucklogger-app");
      cl.setEnabled( true );
      cl.setDirectGrantsOnly( true );
      cl.setConsentRequired( false );
      client.createRealmClient( "trucklogger", cl );
    } 
  }

  @Test
  public void createUser() throws Exception
  {
    List<UserRepresentation> users = client.getRealmUsers("trucklogger", "?username=lmetzger");
        boolean found = false;
    for( UserRepresentation user : users )
    {
      if( user.getUsername().equals("lmetzger") )
      {
        found = true;
        List<RoleRepresentation> roles = client.getAvailableRealmRolesForUser(
            "trucklogger", user.getId());
        for(RoleRepresentation role : roles)
        {
          System.out.println("\n\nAvailable role: " + role.getName() + "\n\n");
        }
        client.addRealmRolesToUser("trucklogger", user.getId(), roles);
        client.deleteRealmRolesForUser("trucklogger", user.getId(), roles);
      }
    }
    if( !found )
    {
      UserRepresentation user = new UserRepresentation();
      user.setUsername("lmetzger");
      user.setFirstName("Lorin");
      user.setLastName("Metzger");
      user.setEmail("lorinmetzger@gmail.com");
      user.setEnabled(true);
      List<String> roles = new ArrayList<String>();
      roles.add("driver");
      user.setRealmRoles( roles );
      client.createRealmUser("trucklogger", user);
    }
  }
}
