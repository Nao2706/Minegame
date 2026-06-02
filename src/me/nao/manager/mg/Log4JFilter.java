package me.nao.manager.mg;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.message.Message;

public class Log4JFilter implements Filter {
	   
	   private Result check(String msg) {
	      if (msg == null) return Result.NEUTRAL;
	      String lower = msg.toLowerCase();
	      if (!lower.contains("issued server command:")) return Result.NEUTRAL;
	      
	      String[] cmds = {"/login ", "/reg ", "/register ", "/changepassword ", "/unregister ", 
	                       "/oplogin ", "/authme register ", "/authme reg ", "/authme cp ", "/authme changepassword "};
	      for (String cmd : cmds) {
	         if (lower.contains(cmd)) return Result.DENY;
	      }
	      return Result.NEUTRAL;
	   }

	   @Override
	   public Result filter(LogEvent record) {
	      return record != null && record.getMessage() != null 
	             ? check(record.getMessage().getFormattedMessage()) 
	             : Result.NEUTRAL;
	   }

	   @Override
	   public Result filter(Logger logger, Level level, Marker marker, String message, Object... params) {
	      return check(message);
	   }

	   // Los otros 2 filter solo devuelven NEUTRAL para no romper nada
	   @Override public Result filter(Logger l, Level lv, Marker m, Object msg, Throwable t) { return Result.NEUTRAL; }
	   @Override public Result filter(Logger l, Level lv, Marker m, Message msg, Throwable t) { return Result.NEUTRAL; }
	   
	   @Override public Result getOnMatch() { return Result.NEUTRAL; }
	   @Override public Result getOnMismatch() { return Result.NEUTRAL; }
	   @Override public State getState() { return State.STARTED; }
	   @Override public void initialize() {}
	   @Override public boolean isStarted() { return true; }
	   @Override public boolean isStopped() { return false; }
	   @Override public void start() {}
	   @Override public void stop() {}

	   @Override
	   public Result filter(Logger logger, Level level, Marker marker, String message, Object p0) {
		// TODO Auto-generated method stub
		return null;
	   }

	   @Override
	   public Result filter(Logger logger, Level level, Marker marker, String message, Object p0, Object p1) {
		// TODO Auto-generated method stub
		return null;
	   }

	   @Override
	   public Result filter(Logger logger, Level level, Marker marker, String message, Object p0, Object p1, Object p2) {
		// TODO Auto-generated method stub
		return null;
	   }

	   @Override
	   public Result filter(Logger logger, Level level, Marker marker, String message, Object p0, Object p1, Object p2,
			Object p3) {
		// TODO Auto-generated method stub
		return null;
	   }

	   @Override
	   public Result filter(Logger logger, Level level, Marker marker, String message, Object p0, Object p1, Object p2,
			Object p3, Object p4) {
		// TODO Auto-generated method stub
		return null;
	   }

	   @Override
	   public Result filter(Logger logger, Level level, Marker marker, String message, Object p0, Object p1, Object p2,
			Object p3, Object p4, Object p5) {
		// TODO Auto-generated method stub
		return null;
	   }

	   @Override
	   public Result filter(Logger logger, Level level, Marker marker, String message, Object p0, Object p1, Object p2,
			Object p3, Object p4, Object p5, Object p6) {
		// TODO Auto-generated method stub
		return null;
	   }

	   @Override
	   public Result filter(Logger logger, Level level, Marker marker, String message, Object p0, Object p1, Object p2,
			Object p3, Object p4, Object p5, Object p6, Object p7) {
		// TODO Auto-generated method stub
		return null;
	   }

	   @Override
	   public Result filter(Logger logger, Level level, Marker marker, String message, Object p0, Object p1, Object p2,
			Object p3, Object p4, Object p5, Object p6, Object p7, Object p8) {
		// TODO Auto-generated method stub
		return null;
	   }

	   @Override
	   public Result filter(Logger logger, Level level, Marker marker, String message, Object p0, Object p1, Object p2,
			Object p3, Object p4, Object p5, Object p6, Object p7, Object p8, Object p9) {
		// TODO Auto-generated method stub
		return null;
	   }
	}





////para usar filtro con comandos 
//Logger coreLogger = (Logger)LogManager.getRootLogger();
//  coreLogger.addFilter(new Log4JFilter());
