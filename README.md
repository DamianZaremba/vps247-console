VPS247 Console
==============

The java source for the vps247 console... using for reference atm on another project.

Slightly reverse engineered so I give no garuntee it will work or is ok to use.

I'm currently using it to build a VNC proxy so I don't have to use their retarded java console app thing (this
basically) as it just crashes chrome and makes my life a pain.

It appears citrix use some none RFC stuff around the HTTP CONNECT (basically proxy) that connects your API session to
the VNC server listening on the host so I'm trying to figure out how to make it work as I'm just getting API errors atm.

I have edited out some stuff as they appear to hard code valid API keys into some of the class files and I don't want to
be responsible for getting their vms hacked. No method changes have been made though, just blanking out vars.
