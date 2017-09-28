package utility;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

import net.sanpei.tor.Operator;

public class MyProxySelector extends ProxySelector {
    // Keep a reference on the previous default
    ProxySelector defsel = null;
	private static SystemLogger log = SystemLogger.getLogger(MyProxySelector.class);
        /*
         * A list of proxies, indexed by their address.
         */
        HashMap<SocketAddress, InnerProxy> proxies = new HashMap<SocketAddress, InnerProxy>();

		public MyProxySelector(ProxySelector def) {
		  // Save the previous default
		  defsel = def;
		}
		 public void init(Properties properties){
			String httpsProxyHosts=properties.getProperty("https_proxy_host","127.0.0.1");
			String httpsProxyPorts=properties.getProperty("https_proxy_port","9050");
			String torControlPorts=properties.getProperty("tor_control_port","8118");
			String torCfgPaths=properties.getProperty("tor_cfg_path","/etc/tor/torrc");
			String []httpsProxyHostsTemp=httpsProxyHosts.split(",");
			String []httpsProxyPortsTemp=httpsProxyPorts.split(",");
			String []torControlPortsTemp=torControlPorts.split(",");
			String []torCfgPathsTemp=torCfgPaths.split(",");
			for(int i=0;i<httpsProxyHostsTemp.length;i++){
				InnerProxy innerProxy=new InnerProxy(httpsProxyHostsTemp[i],httpsProxyPortsTemp[i],torControlPortsTemp[i],torCfgPathsTemp[i]);
				if(!this.proxies.containsKey(innerProxy.addr)){
					this.proxies.put(innerProxy.addr, innerProxy);
				}
			}
				
		 }
          
          /*
           * This is the method that the handlers will call.
           * Returns a List of proxy.
           */
          public java.util.List<Proxy> select(URI uri) {
                // Let's stick to the specs. 
                if (uri == null) {
                        throw new IllegalArgumentException("URI can't be null.");
                }
                
                /*
                 * If it's a http (or https) URL, then we use our own
                 * list.
                 */
                String protocol = uri.getScheme();
                if ("http".equalsIgnoreCase(protocol) ||
                        "https".equalsIgnoreCase(protocol)) {
                        ArrayList<Proxy> l = new ArrayList<Proxy>();
                        InnerProxy temp=null;
                        do{
                           int minConn=Integer.MAX_VALUE;
	                        for (InnerProxy p : proxies.values()) {
	                          if(p.isProxyAvailable()){
	                        	  if(p.getLoad()<minConn){
	                        		  minConn=p.getLoad();
	                        		  temp=p;
	                        	  }
	                          }
	                        }
	                        if(temp==null){
	                        	log.info("No available proxy.");
	                        	try {
									Thread.sleep(30000);
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									log.error(e);
								}
	                        	System.gc();
	                        }
                        }while(temp==null);
                       	temp.increaseLoad();
                       	log.debug(temp.toString());
                        l.add(temp.toProxy());
                        return l;
                }
                
                /*
                 * Not HTTP or HTTPS (could be SOCKS or FTP)
                 * defer to the default selector.
                 */
                if (defsel != null) {
                        return defsel.select(uri);
                } else {
                        ArrayList<Proxy> l = new ArrayList<Proxy>();
                        l.add(Proxy.NO_PROXY);
                        return l;
                }
        }
        
        /*
         * Method called by the handlers when it failed to connect
         * to one of the proxies returned by select().
         */
        public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
                // Let's stick to the specs again.
                if (uri == null || sa == null) {
                        throw new IllegalArgumentException("Arguments can't be null.");
                }
                /*
                 * Let's lookup for the proxy 
                 */
                InnerProxy p = proxies.get(sa); 
                        if (p != null) {
                                /*
                                 * It's one of ours, if it failed more than 3 times
                                 * let's remove it from the list.
                                 */
                                if (p.isCrash()){
                                	proxies.remove(sa);
                                }else{
                                	if(p.isChangingIp()){
                                		try {
											Thread.sleep(2000);
										} catch (InterruptedException e) {
											// TODO Auto-generated catch block
											e.printStackTrace();
										}
                                	}else{
                                		p.changeIpAddress();
                                	}
                                }
                        } else {
                                /*
                                 * Not one of ours, let's delegate to the default.
                                 */
                                if (defsel != null)
                                  defsel.connectFailed(uri, sa, ioe);
                        }
     }
    public void decreaseLoad(SocketAddress sa) {
    	  InnerProxy p = proxies.get(sa); 
          if (p != null) {
        	  p.decreaseLoad();
          }
    }

}
/*
 * Inner class representing a Proxy and a few extra data
 */
class InnerProxy {
	private static SystemLogger log = SystemLogger.getLogger(InnerProxy.class);

		Proxy proxy;
        SocketAddress addr;
        String ipAddress=null;
        int listenPort=-1;
        int controlPort=-1;
        String configFilePath=null;
        // How many uri has assigned this proxy?
        int connectionCount = 0;
        int status=0;//current status 0-available 1-changing ip 2-unnable to connect
        InnerProxy(String ipAddr,String listenPort,String controlPort,String configFilePath) {
        	
            this.ipAddress=ipAddr;
            this.listenPort=Integer.parseInt(listenPort);
            this.controlPort=Integer.parseInt(controlPort);
            this.configFilePath=configFilePath;
            addr = new InetSocketAddress(this.ipAddress,this.listenPort);
            proxy = new Proxy(Proxy.Type.HTTP, addr);
        }
        SocketAddress address() {
                return addr;
        }
        
        Proxy toProxy() {
                return proxy;
        }
        boolean isProxyAvailable(){
        	return status==0;
        }
        boolean isCrash(){
        	return status==2;
        }
        boolean isChangingIp(){
        	return status==1;
        }
        void changeIpAddress(){
        	if(status==0){
        		status=1;
        		connectionCount=0;
        		Thread thread=new Thread(new Runnable(){
        			public void run(){
        				try{
        		        	Operator operations= new Operator(controlPort, configFilePath);
        					if(operations.connect()){
        						if(operations.newIdentity()){
        							log.info("Successfully change the ip address.");
        							status=0;
        						}else{
        							log.error("Fail to change the ip address, refresh tor instead");
        							operations.refreshTor();
        						}
        						operations.disconnect();
        					}else{
        						log.info("Proxy control cannot be accessed, close tor.");
        						status=2;
        					}
        	        	}catch(Exception ex){
        	        		log.error(ex);
        	        	}
        			}
        		});
        		thread.setName("Proxy_"+this.listenPort);
        		thread.start();
        	}else{
        		log.info("The proxy is being refresh ip.");
        	}
        }
        void decreaseLoad(){
        	connectionCount--;
        }
        void increaseLoad(){
        	connectionCount++;
        }
        int getLoad() {
            return connectionCount;
        }
		@Override
		public String toString() {
			return "InnerProxy [ipAddress=" + ipAddress + ", listenPort="
					+ listenPort + ", controlPort=" + controlPort
					+ ", connectionCount=" + connectionCount + ", status="
					+ status + "]";
		}
        
        
}
