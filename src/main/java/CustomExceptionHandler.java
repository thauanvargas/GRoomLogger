public class CustomExceptionHandler implements Thread.UncaughtExceptionHandler {
    @Override
    public void uncaughtException(Thread t, Throwable e) {
        RoomLogger.logToFile("Uncaught exception in thread '" + t.getName() + "': " + e.getMessage());
    }
}