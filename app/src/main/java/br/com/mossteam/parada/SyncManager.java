package br.com.mossteam.parada;

import android.content.Context;
import android.util.Log;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Document;
import com.couchbase.lite.Manager;
import com.couchbase.lite.android.AndroidContext;

import java.io.IOException;
import java.util.HashMap;

/**
 *
 * @author Willian Paixao <willian@ufpa.br>
 * @version 1.0
 */
public class SyncManager {

    private final String DB_NAME = "parada";
    private Database database = null;
    private Manager manager = null;
    private Context context;

    public SyncManager(Context context) {
        this.context = context;
        manager = getManager();
        database = getDatabase();
    }

    public Document createDocument() {
        Document document = getDatabase().createDocument();
        return document;
    }

    public Database getDatabase() {
        if ((database == null) & (manager != null)) {
            try {
                manager = getManager();
                database = manager.getDatabase(DB_NAME);
            } catch (Exception e) {
                Log.d("couchbase", e.toString());
            }
        }
        return database;
    }

    public Document getDocument(String documentId) {
        return database.getExistingDocument(documentId);
    }

    public Manager getManager() {
        try {
            if (manager == null) {
                manager = new Manager(new AndroidContext(context), Manager.DEFAULT_OPTIONS);
            }
        } catch (IOException e) {
            Log.d("couchbase", e.toString());
        }
        return manager;
    }

    public void updateDocument(Document document, HashMap<String, Object> updatedProperties) {
        try {
            document.putProperties(updatedProperties);
        } catch (CouchbaseLiteException e) {
            Log.e("couchbase", e.toString());
        }
    }
}
