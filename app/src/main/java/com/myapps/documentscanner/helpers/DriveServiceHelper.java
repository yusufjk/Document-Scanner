package com.myapps.documentscanner.helpers;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class DriveServiceHelper {
    private final Executor mExecutor = Executors.newSingleThreadExecutor();
    private Drive mDriveService;
    public static final String SHARED_PREFS = "sharedPrefs";
    public static final String FOLDER_ID = "";
    private Context context;

    public DriveServiceHelper(Drive mDriveService, Context context) {
        this.mDriveService = mDriveService;
        this.context = context;
    }

    public Task<String> uploadDocument(String filePath) {
        return Tasks.call(mExecutor, () -> {
            java.io.File file = new java.io.File(filePath + ".pdf");
            FileContent mediaContent = new FileContent("application/pdf", file);
            File myFile = null;
            String folderId = getFolderId("Scannerify");
            try {
                if (folderId != null) {
                    File fileMetaData = new File();
                    fileMetaData.setName(file.getName());
                    fileMetaData.setParents(Collections.singletonList(folderId));
                    myFile = mDriveService.files().create(fileMetaData, mediaContent)
                            .setFields("id,parents")
                            .execute();
                } else {
                    folderId = createFolder();
                    File fileMetaData = new File();
                    fileMetaData.setName(file.getName());
                    fileMetaData.setParents(Collections.singletonList(folderId));
                    myFile = mDriveService.files().create(fileMetaData, mediaContent)
                            .setFields("id,parents")
                            .execute();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            if (myFile == null) {
                throw new IOException("Null result when requesting file creation");
            }
            return myFile.getId();
        });
    }

    private String getFolderId(String folderName) {
         String query = " name = '" + folderName + "' " + " and mimeType = 'application/vnd.google-apps.folder' "+ " and 'root' in parents";
        FileList result = null;
        ArrayList<File> fileArr = new ArrayList<>();
        try {
            result = mDriveService.files().list().setQ(query).setSpaces("drive")
                    .setFields("nextPageToken, files(id, name, createdTime,trashed)")
                    .setPageToken(null).execute();
            if(result.getFiles().size() > 0) {
                if(!result.getFiles().get(0).getTrashed()) {
                    for (File file : result.getFiles()) {
                        fileArr.add(file);
                    }
                    return fileArr.get(0).getId();
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String createFolder() {
        File fileMetadata = new File();
        fileMetadata.setName("Scannerify");
        fileMetadata.setMimeType("application/vnd.google-apps.folder");
        File file = null;
        try {
            file = mDriveService.files().create(fileMetadata)
                    .setFields("id")
                    .execute();
            SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(FOLDER_ID, file.getId());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file.getId();
    }

}
