package org.jivesoftware.openfire.plugin;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.TimerTask;

import org.dom4j.Element;
import org.jivesoftware.util.JiveGlobals;
import org.jivesoftware.util.TaskEngine;
import org.jivesoftware.database.DbConnectionManager;
import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginManager;
import org.jivesoftware.openfire.IQHandlerInfo;
import org.jivesoftware.openfire.IQRouter;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.auth.UnauthorizedException;
import org.jivesoftware.openfire.handler.IQHandler;
import org.xmpp.packet.IQ;
import org.xmpp.packet.PacketError;
import org.slf4j.*;

/**
 * Anonymous user e-mail registration plugin.
 * 
 * @author <a href="mailto:pnspinelli@prodam.sp.gov.br">Paulo Spinelli</a>
 */
@SuppressWarnings("deprecation")
public class RegisterUser implements Plugin {
  private IQHandler customIqHandler;
  protected Logger logger;
  
  public RegisterUser() {
	  
  }
  
  public void initializePlugin(PluginManager manager, File pluginDirectory) {
	  logger = LoggerFactory.getLogger(RegisterUser.class);
	  logger.info("Initializing plugin");
	  customIqHandler = new CustomIqHandler();
	  IQRouter iqRouter = XMPPServer.getInstance().getIQRouter();
	  iqRouter.addHandler(customIqHandler);
   }

   public void destroyPlugin() {
	   logger.info("Destroying plugin");
	   IQRouter iqRouter = XMPPServer.getInstance().getIQRouter();
	   iqRouter.removeHandler(customIqHandler);
	   customIqHandler = null;
	   logger = null;
   }
   
   public class CustomIqHandler extends IQHandler {
	   private IQHandlerInfo info;
	   private final String tableName = "ANONUSERREGISTRATION";
	   private final String IDField = "ID";
	   private final String JIDField = "jid";
	   private final String nickField = "nick";
	   private final String mailField = "email";
	   private final String stampField = "stamp";
	   private final String iqNamespace = "custom:iq:anonregistration";
	   
	   public CustomIqHandler() {
		   super("My Custom IQ Handler");
		   logger.info("Constructing handler");
		   info= new IQHandlerInfo("query",iqNamespace);
		   
		   CreateTableIfDoesNotExist();
	   }	    

	   @Override
	   public IQHandlerInfo getInfo() {
		   return info;
	   }	    
	   
	   @Override
	   public IQ handleIQ(IQ packet) throws UnauthorizedException {
		   logger.info("Received packet");
		   IQ result = IQ.createResultIQ(packet);	
		   IQ.Type type = packet.getType();	
		   if(type.equals(IQ.Type.set))	
		   {
			   Element email= packet.getChildElement().element("email");
			   Element nick= packet.getChildElement().element("nick");
			   if (email != null &&
				   nick != null &&
				   email.getText() != null &&
				   nick.getText() != null) {				   
				   		PersistInformation(packet.getFrom().getNode(),
				   						   nick.getText(),
				   						   email.getText());				   
				   		Element el = result.setChildElement("query", iqNamespace);	
				   		el.addText("success");
			   }
			   else {
				   result.setChildElement(packet.getChildElement().createCopy());	
				   result.setError(PacketError.Condition.bad_request);	
			   }			   
		   }		   
		   else{	
			   result.setChildElement(packet.getChildElement().createCopy());	
			   result.setError(PacketError.Condition.not_acceptable);	
		   }
	
		   return result;
	   }
	   
	   private void PersistInformation(String jid,String nick,String email) {
		   	Connection conn = null;
		   	PreparedStatement stmt = null;
		   	ResultSet rs = null;
		   	try {
		   		conn = DbConnectionManager.getConnection();
		   		stmt = conn.prepareStatement("insert into " + tableName + " (" + 
							     JIDField + "," +
							     nickField + "," + 
							     mailField + ") values (?, ?, ?);");
		   		stmt.setString(1, jid);
		   		stmt.setString(2, nick);
		   		stmt.setString(3, email);
		   		stmt.execute();
		   	}
		   	catch(Exception ex){
		   		logger.error("Error while persisting information");
		   		logger.error(ex.getMessage());
		   	}
		   	finally {
		   		DbConnectionManager.closeConnection(rs, stmt,conn);
		   	}
	   }
	   
	   private void CreateTableIfDoesNotExist() {
		   	Connection conn = null;
		   	Statement stmt = null;
		   	ResultSet rs = null;
		   	try {
		   		logger.info("Is table creation necessary?");
		   		if (!TableExists()) {
		   			conn = DbConnectionManager.getConnection();
			   		stmt = conn.createStatement();
			   		stmt.execute("create table " + tableName + " ("
							+ IDField + " integer identity, "
			   				+ JIDField + " varchar(50), "
			   				+ nickField + " varchar(50), "
			   				+ mailField + " varchar(50),"
			   				+ stampField + " TIMESTAMP default 'now');");
			   		logger.info("Table created");
		   		}
		   	}
		   	catch(Exception ex){
		   		logger.error("Error while creating DB table");
		   		logger.error(ex.getMessage());
		   	}
		   	finally {
		   		DbConnectionManager.closeConnection(rs, stmt,conn);
		   	}
	   }
	   
	   public Boolean TableExists() {
		   	Connection conn = null;
		   	Statement stmt = null;
		   	ResultSet rs = null;
		   	Boolean foundTable = false;
		   	try {
		   		logger.info("Verifying if table exists");
		   		conn = DbConnectionManager.getConnection();
		   		stmt = conn.createStatement();
		   		stmt.execute("select count(*) from information_schema.system_tables where table_schem = 'PUBLIC' and table_name = '" + tableName + "'");
		   		rs = stmt.getResultSet();
		   		if (rs != null) {
		   			rs.next();
		   			int numTables = rs.getInt(1);
		   			if (numTables > 0) {		   				
		   				logger.info("Table exists");
		   				foundTable = true;
		   			}
		   		}			   		
		   	}
		   	catch(Exception ex){
		   		logger.error("Error while locating DB information");
			   	logger.error(ex.getMessage());
		   	}
		   	finally {
		   		DbConnectionManager.closeConnection(rs, stmt,conn);
		   	}
		   	return foundTable; 
	   }
   }
}
