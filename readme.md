RegisterUser
============

Description
-----------

This is a plugin for Openfire XMPP server, intended for registering visitor users
e-mail by using an IQ.

The visitor who wants to register his e-mail into the XMPP server shall send an
IQ query:

```xml
<iq type='set' xmlns='jabber:client'>
    <query xmlns='custom:iq:anonregistration'>
	<email>"email address"</email>
	<nick>"nickname"</nick>
    </query>
</iq>
```

Upon success, the following response is issued:

```xml
<iq xmlns="jabber:client" type="result" to="c5657b60@p4hell-pc/c5657b60">
    <query xmlns="custom:iq:anonregistration">success</query>
</iq>
```

Records will be added to ANONUSERREGISTRATION table (created if does not exist)
on Openfire DB. 

Build
-----
Add to Openfire src/plugins directory and build with

`ant plugins`
