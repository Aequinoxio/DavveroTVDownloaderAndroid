package com.aequinoxio.davverotvdownloader.DownloadLogic;


public interface WorkerUpdateCallback {
    void update(UpdateEvent updateEvent, String message);
  //  public void update (UpdateInfo updateInfo);

    class UpdateInfo{
        private final UpdateEvent updateEvent;
        private final String message;

        public UpdateEvent getUpdateEvent() {
            return updateEvent;
        }

        public String getMessage() {
            return message;
        }

        public UpdateInfo(UpdateEvent updateEvent, String message) {
            this.updateEvent = updateEvent;
            this.message = message;
        }
    }
}
