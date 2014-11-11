package com.qaprosoft.carina.core.foundation.utils.android.recorder.utils;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Locale;

import org.apache.log4j.Logger;

import com.sun.jna.Pointer;

/**
 *
 */
public class Platform {

	private static final Logger LOGGER = Logger.getLogger(Platform.class);

	public static final String OS_NAME_KEY = "os.name";
	public static String name;
	
	public static final boolean IS_WINDOWS = name.startsWith("windows");
	public static final boolean IS_MAC_OS_X = name.startsWith("mac os x");
	public static final boolean IS_LINUX = name.startsWith("linux");

	private static final String[] WIN_CMD = { };
	private static final String[] MAC_CMD = { };
//	private static final String[] WIN_CMD = { "cmd", "/c" };
//	private static final String[] MAC_CMD = { "/bin/bash", "-c" };
	
	static
	{
		String osName = System.getProperty(OS_NAME_KEY);
		if (osName == null)
		{
			throw new RuntimeException(OS_NAME_KEY + " system property not specified");
		}
		name = osName.toLowerCase(Locale.US);
	}

	public static String[] getCmd() {
		if (Platform.IS_WINDOWS) {
			return WIN_CMD;
		} else if (Platform.IS_MAC_OS_X || Platform.IS_LINUX) {
			return MAC_CMD;
		}
		throw new RuntimeException("Unsupported platform detected.");
	}

	public static int getPID(Process process) {
		if (IS_MAC_OS_X || IS_LINUX) {
			return getUnixProcessPID(process);
		} else if (IS_WINDOWS) {
			return getWinPid(process);
		}
		throw new RuntimeException("Can't get PID properly.");
	}

    public static void killProcesses(Collection<Integer> PIDs) {
        if (IS_MAC_OS_X || IS_LINUX){
            killUnixProcessesTree(PIDs);
        }else if(IS_WINDOWS){
            killWindowsProcessesTree(PIDs);
        }else{
            throw new RuntimeException("Unsupported platform detected.");
        }
    }

	private static void killWindowsProcessesTree(Collection<Integer> PIDs) {
		try {
			StringBuilder sb = new StringBuilder("taskkill");
			for (Integer pid : PIDs) {
				//sb.append(" /pid ").append(pid).append(" /f /t");
				sb.append(" /pid ").append(pid);
			}
			//String cmd = sb.append(" /f /t").toString();
            String cmd = sb.append(" /F /T").toString();
			LOGGER.debug(cmd);
			Process p = Runtime.getRuntime().exec(cmd);
			int processStatus = p.waitFor();
			if(processStatus != 0)
			{
				throw new RuntimeException("Process not terminated properly. Code: " + processStatus);
			}
		} catch (Exception e) {
			LOGGER.error(e);
		}
	}

    private static void killUnixProcessesTree(Collection<Integer> PIDs){
        try{
            StringBuilder sb = new StringBuilder("kill -9");
            for (Integer pid : PIDs) {
                sb.append(" ").append(pid);
            }
            String cmd = sb.toString();
            LOGGER.debug(cmd);
			Process p = Runtime.getRuntime().exec(cmd);
			int processStatus = p.waitFor();
			if (processStatus != 0)
			{
				throw new RuntimeException("Process not terminated properly. Code: " + processStatus);
			}
		} catch (Exception e)
		{
			LOGGER.error(e);
		}
    }

	private static int getUnixProcessPID(Process process) {
		if (process.getClass().getName().equals("java.lang.UNIXProcess")) {
			try {
				Field f = process.getClass().getDeclaredField("pid");
				f.setAccessible(true);
				int pid = f.getInt(process);
				return pid;
			} catch (Exception e) {
			}
		}
		return -1;
	}

	private static int getWinPid(Process process) {
		int pid = -1;
		if (process.getClass().getName().equals("java.lang.Win32Process") || process.getClass().getName().equals("java.lang.ProcessImpl")) {
			try {
				Field f = process.getClass().getDeclaredField("handle");
				f.setAccessible(true);
				long handlePtr = f.getLong(process);

				Kernel32 kernel = Kernel32.INSTANCE;
				W32API.HANDLE handle = new W32API.HANDLE();
				handle.setPointer(Pointer.createConstant(handlePtr));
				pid = kernel.GetProcessId(handle);
				return pid;
			} catch (Throwable e) {
			}
		}
		return -1;
	}

}
