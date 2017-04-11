package padfsThreads;

import java.util.List;

import restInterface.RestServer;
import system.SystemEnvironment;
import system.SystemEnvironment.Variables;
import system.containers.Server;
import system.logger.PadFsLogger;
import system.logger.PadFsLogger.LogLevel;
import system.managementOp.SynchGlobal;
import system.managers.SqlManager;

public class HeartBit extends StoppableThread{

	public HeartBit(){
		super();
	}
	
	public void run(){
		String 		ip;
		String 		port;
		long 		id;
		List<Server>serverList;
		
		while(getExitingRequested() ==false){	
			serverList= SqlManager.getServerList();
			
			if(serverList == null){
				PadFsLogger.log(LogLevel.ERROR, "HEARTBIT NULL SERVER LIST THREAD SLEEP: "+Variables.getWaitMillisecondsHeartBeat()+" BEFORE RETRY");
				try {
					Thread.sleep(Variables.getWaitMillisecondsHeartBeat());
				} catch (InterruptedException e) {
					PadFsLogger.log(LogLevel.ERROR, "HEARTBIT NULL SERVER LIST - WAIT BEFORE RETRY ERROR - "+e.getMessage());
				}
			}else{
				for (Server s : serverList) {
					if(getExitingRequested() == true){
						break;
					}
					ip = s.getIp();
					port = s.getPort();
					id = s.getId();

					String totalSpace = SystemEnvironment.getCurrentServerTotalSpace();
					if (totalSpace.compareTo("0") != 0) {
						Variables.setTotalSpace(totalSpace);
					}

					String availableSpace = SystemEnvironment.getCurrentServerAvailableSpace();
					if (!availableSpace.equals("0")) {
						Variables.setAvailableSpace(availableSpace);
					}

					RestServer.pingUpdateServerKeepAlive(id, ip, port);
				}
			}
				
			
			if(Variables.getNeedToGlobalSync() && getExitingRequested()==false){
				PadFsLogger.log(LogLevel.DEBUG, "HeartBit start globalConsensusSync");
				SynchGlobal.delayedGlobalSynch();
			}
			
			
			//sleep for the given amount of time or until a requestToStop call will arrive 
			PadFsLogger.log(LogLevel.DEBUG, "HEARTBIT SLEEP FOR: "+Variables.getWaitMillisecondsHeartBeat()+"ms BEFORE RECHECK");
			waitFor(Variables.getWaitMillisecondsHeartBeat());

	
		}
		PadFsLogger.log(LogLevel.INFO, "HeartBit shutdown"); 
		signalStopCompleted();
	}
}
